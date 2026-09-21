package com.juzipi.criticalhits.mixin;

import com.juzipi.criticalhits.CriticalHitsEnchantments;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 玩家攻击 Mixin。
 * <p>
 * 职责：
 * 1. 移除原版跳劈必暴击
 * 2. 实现基于暴击率 / 暴击伤害附魔的新暴击系统
 * 3. 应用百分比伤害加成
 * 4. 应用目标的易伤效果
 * 5. 应用生命吸取
 * 6. 应用目标装备的暴击减免
 */
@Mixin(Player.class)
public abstract class PlayerEntityAttackMixin {

    @Shadow
    protected abstract boolean canCriticalAttack(Entity target);

    @Redirect(
        method = "attack(Lnet/minecraft/world/entity/Entity;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;hurtOrSimulate(Lnet/minecraft/world/damagesource/DamageSource;F)Z"
        )
    )
    private boolean criticalhits$modifyAttackDamage(Entity target, DamageSource source, float amount) {
        Player attacker = (Player) (Object) this;

        if (!(target instanceof LivingEntity livingTarget)) {
            return target.hurtOrSimulate(source, amount);
        }

        // ===== 步骤1：移除原版跳劈必暴击 =====
        boolean isVanillaCritical = canCriticalAttack(target);
        float baseDamage = amount;
        if (isVanillaCritical) {
            baseDamage = amount / 1.5F;
        }

        ItemStack weapon = attacker.getMainHandItem();

        // ===== 步骤2：百分比伤害加成 =====
        int percentageDamageLevel = CriticalHitsEnchantments.getPercentageDamageLevel(weapon);
        float percentageDamageBonus = CriticalHitsEnchantments.calculatePercentageDamage(percentageDamageLevel);
        float damageAfterPercentage = baseDamage * (1.0F + percentageDamageBonus);

        // ===== 步骤2.5：攻击速度带来的伤害修正 =====
        int attackSpeedLevel = CriticalHitsEnchantments.getAttackSpeedLevel(weapon);
        double attackSpeedDamageModifier = CriticalHitsEnchantments.calculateAttackSpeedDamageModifier(attackSpeedLevel);
        float damageAfterAttackSpeed = damageAfterPercentage * (float) attackSpeedDamageModifier;

        // ===== 步骤3：新暴击系统 =====
        int critRateLevel = CriticalHitsEnchantments.getCriticalRateLevel(weapon);
        int critDamageLevel = CriticalHitsEnchantments.getCriticalDamageLevel(weapon);

        double critRate = CriticalHitsEnchantments.calculateCriticalRate(critRateLevel);
        double critDamageMultiplier = CriticalHitsEnchantments.calculateCriticalDamageMultiplier(critDamageLevel);

        boolean isCritical = attacker.getRandom().nextDouble() < critRate;
        float damageAfterCrit = damageAfterAttackSpeed;

        if (isCritical) {
            float extraDamage = damageAfterAttackSpeed * (float) critDamageMultiplier;
            // 暴击减免在applyDefenseEnchantments中统一应用，这里不重复应用
            damageAfterCrit = damageAfterAttackSpeed + extraDamage;
        }

        // ===== 步骤3.5：额外伤害（独立乘区）=====
        int bonusDamageLevel = CriticalHitsEnchantments.getBonusDamageLevel(weapon);
        double bonusDamageMultiplier = CriticalHitsEnchantments.calculateBonusDamageMultiplier(bonusDamageLevel);
        float damageAfterBonus = damageAfterCrit * (float) bonusDamageMultiplier;

        // ===== 步骤4：应用目标的易伤效果 =====
        float vulnerabilityBonus = CriticalHitsEnchantments.getVulnerabilityBonus(livingTarget);
        float damageAfterVulnerability = damageAfterBonus * (1.0F + vulnerabilityBonus);

        // ===== 步骤4.3：破甲debuff（目标已有的护甲降低效果）=====
        float armorShatterBonus = 0;
        try {
            var armorBreakHolder = net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT
                .get(CriticalHitsEnchantments.ARMOR_BREAK_EFFECT_ID).orElse(null);
            if (armorBreakHolder != null) {
                var effect = livingTarget.getEffect((net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect>) armorBreakHolder);
                if (effect != null) {
                    armorShatterBonus = (effect.getAmplifier() + 1) * 0.15f;
                }
            }
        } catch (Exception e) {
            // 忽略
        }

        // ===== 步骤4.5：护甲穿透（临时降低目标护甲值）=====
        int armorPenetrationLevel = CriticalHitsEnchantments.getArmorPenetrationLevel(weapon);
        int armorShatterLevel = CriticalHitsEnchantments.getArmorShatterLevel(weapon);
        float originalArmor = 0;
        float totalArmorReduction = armorShatterBonus;
        if (armorPenetrationLevel > 0) {
            totalArmorReduction += CriticalHitsEnchantments.calculateArmorPenetration(armorPenetrationLevel);
        }
        // 破甲附魔给目标挂debuff（在造成伤害后应用）
        if (totalArmorReduction > 0) {
            totalArmorReduction = Math.min(totalArmorReduction, (float) CriticalHitsEnchantments.MAX_ARMOR_REDUCTION);
            var armorAttr = livingTarget.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR);
            if (armorAttr != null) {
                originalArmor = (float) armorAttr.getBaseValue();
                float reducedArmor = Math.max(0, originalArmor * (1.0F - totalArmorReduction));
                armorAttr.setBaseValue(reducedArmor);
            }
        }

        // ===== 步骤5：应用目标的防御类附魔（伤害减免、暴击减免）=====
        float finalDamage = CriticalHitsEnchantments.applyDefenseEnchantments(
            livingTarget, damageAfterVulnerability, isCritical);

        // ===== 步骤5.5：临时增加目标的盔甲韧性（护甲防御附魔）=====
        int armorDefenseLevel = CriticalHitsEnchantments.getArmorDefenseLevel(livingTarget);
        float originalToughness = 0;
        if (armorDefenseLevel > 0) {
            float bonusToughness = CriticalHitsEnchantments.calculateArmorToughness(armorDefenseLevel);
            var toughnessAttr = livingTarget.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR_TOUGHNESS);
            if (toughnessAttr != null) {
                originalToughness = (float) toughnessAttr.getBaseValue();
                toughnessAttr.setBaseValue(originalToughness + bonusToughness);
            }
        }

        // ===== 步骤6：造成伤害 =====
        boolean hurtResult = target.hurtOrSimulate(source, finalDamage);

        // ===== 步骤6.5：恢复目标的原始盔甲韧性和护甲值 =====
        if (armorDefenseLevel > 0) {
            var toughnessAttr = livingTarget.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR_TOUGHNESS);
            if (toughnessAttr != null) {
                toughnessAttr.setBaseValue(originalToughness);
            }
        }
        if (armorPenetrationLevel > 0) {
            var armorAttr = livingTarget.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR);
            if (armorAttr != null) {
                armorAttr.setBaseValue(originalArmor);
            }
        }

        // ===== 步骤7：生命吸取 =====
        if (hurtResult && finalDamage > 0) {
            int lifeStealLevel = CriticalHitsEnchantments.getLifeStealLevel(weapon);
            if (lifeStealLevel > 0) {
                float lifeStealPercent = CriticalHitsEnchantments.calculateLifeSteal(lifeStealLevel);
                float healAmount = finalDamage * lifeStealPercent;
                if (healAmount > 0) {
                    // 血量上限校验
                    float currentHealth = attacker.getHealth();
                    float maxHealth = attacker.getMaxHealth();
                    if (currentHealth < maxHealth) {
                        float actualHeal = Math.min(healAmount, maxHealth - currentHealth);
                        attacker.heal(actualHeal);
                    }
                }
            }
        }

        // ===== 步骤7.5：处决效果 =====
        if (hurtResult) {
            int executeLevel = CriticalHitsEnchantments.getExecuteLevel(weapon);
            if (executeLevel > 0 && livingTarget.isAlive()) {
                float targetMaxHealth = livingTarget.getMaxHealth();
                float targetCurrentHealth = livingTarget.getHealth();
                float healthRatio = targetCurrentHealth / targetMaxHealth;

                if (executeLevel == 3) {
                    // 3级：血量<15%直接处决（对Boss无效，1秒冷却）
                    if (healthRatio < CriticalHitsEnchantments.EXECUTE_THRESHOLD_LV3
                        && !CriticalHitsEnchantments.isBoss(livingTarget)) {
                        // 直接处决（设置伤害为目标剩余血量+1）
                        livingTarget.hurtOrSimulate(source,
                            targetCurrentHealth + 1.0F);
                        // 处决特效：红色爆炸粒子
                        for (int i = 0; i < 20; i++) {
                            livingTarget.level().addParticle(
                                net.minecraft.core.particles.ParticleTypes.EXPLOSION,
                                livingTarget.getX() + (attacker.getRandom().nextDouble() - 0.5) * 1.0,
                                livingTarget.getY() + attacker.getRandom().nextDouble() * 1.5,
                                livingTarget.getZ() + (attacker.getRandom().nextDouble() - 0.5) * 1.0,
                                0, 0.1, 0);
                        }
                        // 处决音效：低沉的凋灵生成音效
                        livingTarget.level().playSound(
                            null,
                            livingTarget.getX(), livingTarget.getY(), livingTarget.getZ(),
                            net.minecraft.sounds.SoundEvents.WITHER_SPAWN,
                            livingTarget.getSoundSource(),
                            0.5F, 0.8F
                        );
                    }
                } else if (executeLevel == 1 || executeLevel == 2) {
                    // 1/2级：血量<20%时额外伤害
                    if (healthRatio < CriticalHitsEnchantments.EXECUTE_THRESHOLD_LV1_2) {
                        float executeBonus = (executeLevel == 1)
                            ? (float) CriticalHitsEnchantments.EXECUTE_BONUS_LV1
                            : (float) CriticalHitsEnchantments.EXECUTE_BONUS_LV2;
                        float executeDamage = finalDamage * executeBonus;
                        if (executeDamage > 0) {
                            livingTarget.hurtOrSimulate(source, executeDamage);
                        }
                    }
                }
            }
        }

        // ===== 步骤7.8：真实伤害（无视所有防御，直接扣血）=====
        if (hurtResult) {
            int trueDamageLevel = CriticalHitsEnchantments.getTrueDamageLevel(weapon);
            if (trueDamageLevel > 0 && livingTarget.isAlive()) {
                float trueDamagePercent = CriticalHitsEnchantments.calculateTrueDamage(trueDamageLevel);
                float trueDamage = baseDamage * trueDamagePercent;
                if (trueDamage > 0) {
                    // 真实伤害：直接扣血（无视防御）
                    livingTarget.setHealth(Math.max(0, livingTarget.getHealth() - trueDamage));
                    if (livingTarget.getHealth() <= 0) {
                        livingTarget.die(source);
                    }
                }
            }
        }

        // ===== 步骤8：易伤附魔（攻击时给目标挂易伤debuff）=====
        if (hurtResult) {
            int vulnerabilityEnchantLevel = CriticalHitsEnchantments.getVulnerabilityEnchantLevel(weapon);
            if (vulnerabilityEnchantLevel > 0) {
                CriticalHitsEnchantments.applyVulnerabilityEffect(livingTarget, vulnerabilityEnchantLevel);
            }
        }

        // ===== 步骤8.2：冰霜附魔（给目标挂减速debuff）=====
        if (hurtResult) {
            int frostLevel = CriticalHitsEnchantments.getFrostLevel(weapon);
            if (frostLevel > 0) {
                float frostChance = CriticalHitsEnchantments.calculateFrostChance(frostLevel);
                if (attacker.getRandom().nextFloat() < frostChance) {
                    float frostSlow = CriticalHitsEnchantments.calculateFrostSlow(frostLevel);
                    int slowAmplifier = Math.min(4, (int) (frostSlow / 0.10) - 1);
                    if (slowAmplifier >= 0) {
                        var slowHolder = net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT
                            .get(net.minecraft.resources.Identifier.fromNamespaceAndPath("minecraft", "slowness"))
                            .orElse(null);
                        if (slowHolder != null) {
                            livingTarget.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                                (net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect>) slowHolder,
                                CriticalHitsEnchantments.FROST_DURATION,
                                slowAmplifier, false, true));
                        }
                    }
                }
            }
        }

        // ===== 步骤8.4：中毒附魔（给目标挂中毒debuff）=====
        if (hurtResult) {
            int poisonLevel = CriticalHitsEnchantments.getPoisonLevel(weapon);
            if (poisonLevel > 0) {
                float poisonChance = CriticalHitsEnchantments.calculatePoisonChance(poisonLevel);
                if (attacker.getRandom().nextFloat() < poisonChance) {
                    float poisonDps = CriticalHitsEnchantments.calculatePoisonDps(poisonLevel);
                    int poisonAmplifier = Math.min(4, (int) (poisonDps / 0.01) - 1);
                    if (poisonAmplifier >= 0) {
                        var poisonHolder = net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT
                            .get(net.minecraft.resources.Identifier.fromNamespaceAndPath("minecraft", "poison"))
                            .orElse(null);
                        if (poisonHolder != null) {
                            livingTarget.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                                (net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect>) poisonHolder,
                                CriticalHitsEnchantments.POISON_DURATION,
                                poisonAmplifier, false, true));
                        }
                    }
                }
            }
        }

        // ===== 步骤8.6：破甲附魔（给目标挂护甲降低debuff）=====
        if (hurtResult && armorShatterLevel > 0) {
            try {
                var armorBreakHolder = net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT
                    .get(CriticalHitsEnchantments.ARMOR_BREAK_EFFECT_ID).orElse(null);
                if (armorBreakHolder != null) {
                    float shatterPercent = CriticalHitsEnchantments.calculateArmorShatter(armorShatterLevel);
                    int amplifier = Math.min(2, (int) (shatterPercent / 0.10) - 1);
                    if (amplifier >= 0) {
                        livingTarget.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                            (net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect>) armorBreakHolder,
                            CriticalHitsEnchantments.ARMOR_SHATTER_DURATION,
                            amplifier, false, true));
                    }
                }
            } catch (Exception e) {
                // 忽略
            }
        }

        // ===== 步骤9：溅射伤害（对周围敌人造成范围伤害）=====
        if (hurtResult) {
            int splashLevel = CriticalHitsEnchantments.getSplashDamageLevel(weapon);
            if (splashLevel > 0) {
                float splashDamagePercent = CriticalHitsEnchantments.calculateSplashDamage(splashLevel);
                double splashRange = CriticalHitsEnchantments.calculateSplashRange(splashLevel);
                float splashDamage = finalDamage * splashDamagePercent;

                if (splashDamage > 0) {
                    var nearbyEntities = livingTarget.level().getEntitiesOfClass(
                        net.minecraft.world.entity.LivingEntity.class,
                        livingTarget.getBoundingBox().inflate(splashRange));
                    for (var nearby : nearbyEntities) {
                        if (nearby == livingTarget) continue; // 不对主目标造成溅射
                        if (nearby == attacker) continue; // 不对自己造成溅射
                        double distance = nearby.distanceTo(livingTarget);
                        if (distance <= splashRange) {
                            // 距离衰减：越远伤害越低
                            float distanceFalloff = 1.0F - (float) (distance / splashRange) * 0.5F;
                            float actualSplashDamage = splashDamage * distanceFalloff;
                            if (actualSplashDamage > 0) {
                                nearby.hurtOrSimulate(source, actualSplashDamage);
                            }
                        }
                    }
                    // 溅射粒子效果
                    for (int i = 0; i < 10; i++) {
                        livingTarget.level().addParticle(
                            net.minecraft.core.particles.ParticleTypes.SWEEP_ATTACK,
                            livingTarget.getX() + (attacker.getRandom().nextDouble() - 0.5) * splashRange,
                            livingTarget.getY() + 0.5,
                            livingTarget.getZ() + (attacker.getRandom().nextDouble() - 0.5) * splashRange,
                            0, 0, 0);
                    }
                }
            }
        }

        return hurtResult;
    }
}
