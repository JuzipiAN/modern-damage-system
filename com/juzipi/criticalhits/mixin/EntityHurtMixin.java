package com.juzipi.criticalhits.mixin;

import com.juzipi.criticalhits.CriticalHitsEnchantments;
import com.juzipi.criticalhits.ShieldManager;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 实体受伤Mixin。
 * 处理生物攻击玩家时，玩家的防御类附魔（伤害减免、护甲防御、暴击减免）生效。
 * 玩家攻击生物时的防御应用在PlayerEntityAttackMixin中处理。
 */
@Mixin(Entity.class)
public class EntityHurtMixin {
    @Unique
    private float criticalhits_originalToughness = 0;
    @Unique
    private double criticalhits_originalKnockbackResist = 0;
    @Unique
    private double criticalhits_originalArmor = 0;
    @Unique
    private boolean criticalhits_armorPenetrationActive = false;

    @Inject(method = "hurtOrSimulate", at = @At("HEAD"))
    private void criticalhits_onHurtHead(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        Entity entity = (Entity) (Object) this;
        if (!(entity instanceof Player player)) return;
        // 玩家造成的伤害已在PlayerEntityAttackMixin中处理
        if (source.getEntity() instanceof Player) return;

        // 临时增加玩家的盔甲韧性（护甲防御附魔）
        int armorDefenseLevel = CriticalHitsEnchantments.getArmorDefenseLevel(player);
        if (armorDefenseLevel > 0) {
            AttributeInstance toughnessAttr = player.getAttribute(Attributes.ARMOR_TOUGHNESS);
            if (toughnessAttr != null) {
                criticalhits_originalToughness = (float) toughnessAttr.getBaseValue();
                float bonusToughness = CriticalHitsEnchantments.calculateArmorToughness(armorDefenseLevel);
                toughnessAttr.setBaseValue(criticalhits_originalToughness + bonusToughness);
            }
        }

        // v2.0.0 临时增加击退抗性
        int knockbackResistLevel = CriticalHitsEnchantments.getKnockbackResistanceLevel(player);
        if (knockbackResistLevel > 0) {
            AttributeInstance knockbackAttr = player.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
            if (knockbackAttr != null) {
                criticalhits_originalKnockbackResist = knockbackAttr.getBaseValue();
                float bonusKnockbackResist = CriticalHitsEnchantments.calculateKnockbackResistance(knockbackResistLevel);
                knockbackAttr.setBaseValue(Math.min(1.0, criticalhits_originalKnockbackResist + bonusKnockbackResist));
            }
        }

        // v2.0.0 怪物武器护甲穿透：临时降低玩家护甲值
        if (source.getEntity() instanceof net.minecraft.world.entity.LivingEntity attacker) {
            ItemStack attackerWeapon = attacker.getMainHandItem();
            int armorPenLevel = CriticalHitsEnchantments.getArmorPenetrationLevel(attackerWeapon);
            if (armorPenLevel > 0) {
                AttributeInstance armorAttr = player.getAttribute(Attributes.ARMOR);
                if (armorAttr != null) {
                    criticalhits_originalArmor = armorAttr.getBaseValue();
                    float penPercent = CriticalHitsEnchantments.calculateArmorPenetration(armorPenLevel);
                    double reducedArmor = criticalhits_originalArmor * (1.0 - penPercent);
                    armorAttr.setBaseValue(reducedArmor);
                    criticalhits_armorPenetrationActive = true;
                }
            }
        }
    }

    @Inject(method = "hurtOrSimulate", at = @At("RETURN"))
    private void criticalhits_onHurtReturn(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        Entity entity = (Entity) (Object) this;
        if (!(entity instanceof Player player)) return;
        if (source.getEntity() instanceof Player) return;

        // 恢复玩家的原始盔甲韧性
        int armorDefenseLevel = CriticalHitsEnchantments.getArmorDefenseLevel(player);
        if (armorDefenseLevel > 0) {
            AttributeInstance toughnessAttr = player.getAttribute(Attributes.ARMOR_TOUGHNESS);
            if (toughnessAttr != null) {
                toughnessAttr.setBaseValue(criticalhits_originalToughness);
            }
        }

        // v2.0.0 恢复击退抗性
        int knockbackResistLevel = CriticalHitsEnchantments.getKnockbackResistanceLevel(player);
        if (knockbackResistLevel > 0) {
            AttributeInstance knockbackAttr = player.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
            if (knockbackAttr != null) {
                knockbackAttr.setBaseValue(criticalhits_originalKnockbackResist);
            }
        }

        // v2.0.0 恢复玩家护甲值（护甲穿透）
        if (criticalhits_armorPenetrationActive) {
            AttributeInstance armorAttr = player.getAttribute(Attributes.ARMOR);
            if (armorAttr != null) {
                armorAttr.setBaseValue(criticalhits_originalArmor);
            }
            criticalhits_armorPenetrationActive = false;
        }
    }

    @ModifyVariable(method = "hurtOrSimulate", at = @At("HEAD"), argsOnly = true)
    private float criticalhits_applyPlayerDefense(float amount, DamageSource source) {
        Entity entity = (Entity) (Object) this;

        // ===== 处理玩家弓箭攻击：应用百分比伤害加成和易伤效果 =====
        if (entity instanceof net.minecraft.world.entity.LivingEntity livingTarget) {
            Entity sourceEntity = source.getEntity();
            if (sourceEntity instanceof Player player) {
                Entity directEntity = source.getDirectEntity();
                if (directEntity != null) {
                    String directName = directEntity.getType().toShortString();
                    if (directName.contains("arrow")) {
                        ItemStack weapon = player.getMainHandItem();
                        // 百分比伤害加成
                        int percentageDamageLevel = CriticalHitsEnchantments.getPercentageDamageLevel(weapon);
                        float percentageDamageBonus = CriticalHitsEnchantments.calculatePercentageDamage(percentageDamageLevel);
                        float damageAfterPercentage = amount * (1.0F + percentageDamageBonus);
                        // 易伤附魔：给目标添加易伤效果
                        int vulnerabilityLevel = CriticalHitsEnchantments.getVulnerabilityEnchantLevel(weapon);
                        if (vulnerabilityLevel > 0) {
                            CriticalHitsEnchantments.applyVulnerabilityEffect(livingTarget, vulnerabilityLevel);
                        }
                        return damageAfterPercentage;
                    }
                }
            }
        }

        // ===== 处理生物攻击玩家时的防御 =====
        if (!(entity instanceof Player player)) return amount;
        // 玩家造成的伤害已在PlayerEntityAttackMixin中处理
        if (source.getEntity() instanceof Player) return amount;

        // ===== v2.0.0 怪物武器攻击附魔生效 =====
        float attackDamage = amount;
        if (source.getEntity() instanceof net.minecraft.world.entity.LivingEntity attacker) {
            ItemStack attackerWeapon = attacker.getMainHandItem();

            // 1. 百分比伤害加成
            int percentageDamageLevel = CriticalHitsEnchantments.getPercentageDamageLevel(attackerWeapon);
            if (percentageDamageLevel > 0) {
                float percentageBonus = CriticalHitsEnchantments.calculatePercentageDamage(percentageDamageLevel);
                attackDamage = attackDamage * (1.0F + percentageBonus);
            }

            // 2. 暴击系统（怪物也可以暴击）
            int criticalRateLevel = CriticalHitsEnchantments.getCriticalRateLevel(attackerWeapon);
            int criticalDamageLevel = CriticalHitsEnchantments.getCriticalDamageLevel(attackerWeapon);
            double baseCritRate = CriticalHitsEnchantments.calculateCriticalRate(criticalRateLevel);
            double baseCritDamage = CriticalHitsEnchantments.calculateCriticalDamageMultiplier(criticalDamageLevel);
            if (Math.random() < baseCritRate) {
                attackDamage = attackDamage * (1.0F + (float)baseCritDamage);
            }

            // 3. 额外伤害（独立乘区，暴击后计算）
            int bonusDamageLevel = CriticalHitsEnchantments.getBonusDamageLevel(attackerWeapon);
            if (bonusDamageLevel > 0) {
                double bonusPercent = CriticalHitsEnchantments.calculateBonusDamageMultiplier(bonusDamageLevel);
                attackDamage = attackDamage * (1.0F + (float)bonusPercent);
            }

            // 4. 给玩家添加debuff效果（易伤、冰霜、中毒、破甲）
            int vulnerabilityLevel = CriticalHitsEnchantments.getVulnerabilityEnchantLevel(attackerWeapon);
            if (vulnerabilityLevel > 0) {
                CriticalHitsEnchantments.applyVulnerabilityEffect(player, vulnerabilityLevel);
            }

            // 冰霜debuff（减速）
            int frostLevel = CriticalHitsEnchantments.getFrostLevel(attackerWeapon);
            if (frostLevel > 0) {
                float frostChance = CriticalHitsEnchantments.calculateFrostChance(frostLevel);
                if (Math.random() < frostChance) {
                    float frostSlow = CriticalHitsEnchantments.calculateFrostSlow(frostLevel);
                    int slowAmplifier = Math.min(4, (int) (frostSlow / 0.10) - 1);
                    if (slowAmplifier >= 0) {
                        var slowHolder = net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT
                            .get(net.minecraft.resources.Identifier.fromNamespaceAndPath("minecraft", "slowness"))
                            .orElse(null);
                        if (slowHolder != null) {
                            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                                (net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect>) slowHolder,
                                CriticalHitsEnchantments.FROST_DURATION,
                                slowAmplifier, false, true));
                        }
                    }
                }
            }

            // 中毒debuff
            int poisonLevel = CriticalHitsEnchantments.getPoisonLevel(attackerWeapon);
            if (poisonLevel > 0) {
                float poisonChance = CriticalHitsEnchantments.calculatePoisonChance(poisonLevel);
                if (Math.random() < poisonChance) {
                    float poisonDps = CriticalHitsEnchantments.calculatePoisonDps(poisonLevel);
                    int poisonAmplifier = Math.min(4, (int) (poisonDps / 0.01) - 1);
                    if (poisonAmplifier >= 0) {
                        var poisonHolder = net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT
                            .get(net.minecraft.resources.Identifier.fromNamespaceAndPath("minecraft", "poison"))
                            .orElse(null);
                        if (poisonHolder != null) {
                            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                                (net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect>) poisonHolder,
                                CriticalHitsEnchantments.POISON_DURATION,
                                poisonAmplifier, false, true));
                        }
                    }
                }
            }

            // 破甲debuff（护甲降低）
            int armorShatterLevel = CriticalHitsEnchantments.getArmorShatterLevel(attackerWeapon);
            if (armorShatterLevel > 0) {
                try {
                    var armorBreakHolder = net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT
                        .get(CriticalHitsEnchantments.ARMOR_BREAK_EFFECT_ID).orElse(null);
                    if (armorBreakHolder != null) {
                        float shatterPercent = CriticalHitsEnchantments.calculateArmorShatter(armorShatterLevel);
                        int amplifier = Math.min(2, (int) (shatterPercent / 0.10) - 1);
                        if (amplifier >= 0) {
                            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                                (net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect>) armorBreakHolder,
                                CriticalHitsEnchantments.ARMOR_SHATTER_DURATION,
                                amplifier, false, true));
                        }
                    }
                } catch (Exception e) {
                    // 忽略
                }
            }

            // 5. 生命吸取（怪物回血）
            int lifeStealLevel = CriticalHitsEnchantments.getLifeStealLevel(attackerWeapon);
            if (lifeStealLevel > 0) {
                float stealPercent = CriticalHitsEnchantments.calculateLifeSteal(lifeStealLevel);
                float healAmount = attackDamage * stealPercent;
                if (healAmount > 0) {
                    attacker.heal(healAmount);
                }
            }
        }

        // 生物攻击玩家时应用伤害减免和暴击减免
        float result = CriticalHitsEnchantments.applyDefenseEnchantments(player, attackDamage, false);

        // v2.0.0 护盾优先吸收伤害
        int shieldLevel = CriticalHitsEnchantments.getShieldLevel(player);
        if (shieldLevel > 0) {
            float maxShield = CriticalHitsEnchantments.calculateShieldValue(shieldLevel, player.getMaxHealth());
            ShieldManager.setMaxShield(player, maxShield);
            result = ShieldManager.absorbDamage(player, result);
        } else {
            ShieldManager.setMaxShield(player, 0);
        }

        // ===== v2.0.0 反伤效果 =====
        if (source.getEntity() instanceof net.minecraft.world.entity.LivingEntity attacker && result > 0 && attacker != player) {
            int thornsReflectLevel = CriticalHitsEnchantments.getThornsReflectLevel(player);
            if (thornsReflectLevel > 0) {
                float reflectPercent = CriticalHitsEnchantments.calculateThornsReflect(thornsReflectLevel);
                float reflectDamage = result * reflectPercent;
                if (reflectDamage > 0) {
                    // 反伤不超过自身受到的伤害，对弹射物无效
                    boolean isProjectile = source.getDirectEntity() instanceof net.minecraft.world.entity.projectile.Projectile;
                    if (!isProjectile) {
                        // 直接对攻击者造成反伤伤害
                        attacker.hurtOrSimulate(source, reflectDamage);
                        // 反伤粒子效果
                        for (int i = 0; i < 5; i++) {
                            player.level().addParticle(
                                net.minecraft.core.particles.ParticleTypes.CRIT,
                                player.getX() + (player.getRandom().nextDouble() - 0.5) * 0.5,
                                player.getY() + player.getRandom().nextDouble() * 1.0,
                                player.getZ() + (player.getRandom().nextDouble() - 0.5) * 0.5,
                                0, 0.1, 0);
                        }
                    }
                }
            }
        }

        return result;
    }
}
