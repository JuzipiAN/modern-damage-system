package com.juzipi.criticalhits;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/**
 * 现代化伤害系统附魔工具类。
 * 提供所有附魔的标识、等级查询与数值计算。
 */
public final class CriticalHitsEnchantments {

    public static final String MOD_ID = "criticalhits";

    // ===== 暴击系列 =====
    public static final Identifier CRITICAL_RATE_ID = Identifier.fromNamespaceAndPath(MOD_ID, "critical_rate");
    public static final Identifier CRITICAL_DAMAGE_ID = Identifier.fromNamespaceAndPath(MOD_ID, "critical_damage");
    public static final Identifier CRITICAL_RESISTANCE_ID = Identifier.fromNamespaceAndPath(MOD_ID, "critical_resistance");

    // ===== 攻击系列 =====
    public static final Identifier PERCENTAGE_DAMAGE_ID = Identifier.fromNamespaceAndPath(MOD_ID, "percentage_damage");
    public static final Identifier ARMOR_PENETRATION_ID = Identifier.fromNamespaceAndPath(MOD_ID, "armor_penetration");
    public static final Identifier LIFE_STEAL_ID = Identifier.fromNamespaceAndPath(MOD_ID, "life_steal");
    public static final Identifier VULNERABILITY_ID = Identifier.fromNamespaceAndPath(MOD_ID, "vulnerability");

    // ===== 防御系列 =====
    public static final Identifier DAMAGE_REDUCTION_ID = Identifier.fromNamespaceAndPath(MOD_ID, "damage_reduction");
    public static final Identifier ARMOR_DEFENSE_ID = Identifier.fromNamespaceAndPath(MOD_ID, "armor_defense");
    public static final Identifier MAX_HEALTH_ID = Identifier.fromNamespaceAndPath(MOD_ID, "max_health");

    // ===== 属性系列 =====
    public static final Identifier ATTACK_SPEED_ID = Identifier.fromNamespaceAndPath(MOD_ID, "attack_speed");
    public static final Identifier MOVEMENT_SPEED_ID = Identifier.fromNamespaceAndPath(MOD_ID, "movement_speed");
    public static final Identifier ATTACK_RANGE_ID = Identifier.fromNamespaceAndPath(MOD_ID, "attack_range");
    public static final Identifier BONUS_DAMAGE_ID = Identifier.fromNamespaceAndPath(MOD_ID, "bonus_damage");

    // ===== 属性系列（负等级/诅咒） =====
    public static final Identifier ATTACK_SPEED_CURSE_ID = Identifier.fromNamespaceAndPath(MOD_ID, "attack_speed_curse");
    public static final Identifier MOVEMENT_SPEED_CURSE_ID = Identifier.fromNamespaceAndPath(MOD_ID, "movement_speed_curse");
    public static final Identifier ATTACK_RANGE_CURSE_ID = Identifier.fromNamespaceAndPath(MOD_ID, "attack_range_curse");

    // ===== v2.0.0 新增攻击系列 =====
    public static final Identifier TRUE_DAMAGE_ID = Identifier.fromNamespaceAndPath(MOD_ID, "true_damage");
    public static final Identifier SPLASH_DAMAGE_ID = Identifier.fromNamespaceAndPath(MOD_ID, "splash_damage");
    public static final Identifier EXECUTE_ID = Identifier.fromNamespaceAndPath(MOD_ID, "execute");
    public static final Identifier FROST_ID = Identifier.fromNamespaceAndPath(MOD_ID, "frost");
    public static final Identifier POISON_ID = Identifier.fromNamespaceAndPath(MOD_ID, "poison");
    public static final Identifier ARMOR_SHATTER_ID = Identifier.fromNamespaceAndPath(MOD_ID, "armor_shatter");

    // ===== v2.0.0 新增防御系列 =====
    public static final Identifier THORNS_REFLECT_ID = Identifier.fromNamespaceAndPath(MOD_ID, "thorns_reflect");
    public static final Identifier SHIELD_ID = Identifier.fromNamespaceAndPath(MOD_ID, "shield");
    public static final Identifier KNOCKBACK_RESISTANCE_ID = Identifier.fromNamespaceAndPath(MOD_ID, "knockback_resistance");

    // ===== 暴击系列常量 =====
    public static final double DEFAULT_CRITICAL_RATE = 0.05;
    public static final double DEFAULT_CRITICAL_DAMAGE = 0.50;
    public static final double CRITICAL_RATE_PER_LEVEL = 0.05;
    public static final double CRITICAL_DAMAGE_PER_LEVEL = 0.10;
    public static final double CRITICAL_RESIST_PER_LEVEL = 0.04; // 修正：每级-4%
    public static final double MAX_CRITICAL_RESIST = 0.80; // 修正：上限80%

    // ===== 攻击系列常量 =====
    public static final double PERCENTAGE_DAMAGE_PER_LEVEL = 0.02; // 每级+2%
    public static final double ARMOR_PENETRATION_PER_LEVEL = 0.02; // 每级+2%
    public static final double LIFE_STEAL_PER_LEVEL = 0.02; // 每级+2%
    public static final double VULNERABILITY_ENCHANT_PER_LEVEL = 0.03; // 易伤附魔每级+3%
    public static final double VULNERABILITY_POTION_PER_LEVEL = 0.15; // 易伤药水每级+15%
    public static final int VULNERABILITY_DURATION = 100; // 易伤效果持续时间（5秒）

    // ===== 防御系列常量 =====
    public static final double DAMAGE_REDUCTION_PER_LEVEL = 0.01; // 伤害减免每级-1%
    public static final double ARMOR_DEFENSE_PER_LEVEL = 0.5; // 护甲防御每级+0.5韧性
    public static final double MAX_HEALTH_PER_LEVEL = 1.0; // 最大生命值每级+1点
    public static final double MAX_TOTAL_DAMAGE_REDUCTION = 0.95; // 总减伤上限95%
    public static final float MIN_DAMAGE = 1.0F; // 最小伤害1点

    // ===== 属性系列常量 =====
    public static final double ATTACK_SPEED_PER_LEVEL = 0.08; // 攻击速度每级±8%
    public static final double ATTACK_SPEED_DAMAGE_PENALTY = 0.05; // 攻速每+10%伤害-5%
    public static final double MOVEMENT_SPEED_PER_LEVEL = 0.05; // 移动速度每级±5%
    public static final double MOVEMENT_SPEED_RESIST_BONUS = 0.03; // 移速每-5%抗性+3%
    public static final double ATTACK_RANGE_PER_LEVEL = 0.2; // 攻击范围每级+0.2格
    public static final double ATTACK_RANGE_SPEED_PENALTY = 0.05; // 范围每+1格攻速-5%
    public static final double BONUS_DAMAGE_PER_LEVEL = 0.03; // 额外伤害每级+3%
    public static final double MIN_ATTACK_SPEED = 0.5; // 最低攻速0.5/秒
    public static final double MAX_ATTACK_SPEED = 20.0; // 最高攻速20/秒
    public static final double MIN_MOVEMENT_SPEED_MULTIPLIER = 0.5; // 最低移速50%
    public static final double MAX_MOVEMENT_SPEED_MULTIPLIER = 3.0; // 最高移速300%

    // ===== v2.0.0 新增攻击系列常量 =====
    public static final double TRUE_DAMAGE_PER_LEVEL = 0.01; // 真实伤害每级+1%
    public static final double SPLASH_DAMAGE_PER_LEVEL = 0.08; // 溅射伤害每级+8%
    public static final double SPLASH_RANGE_PER_LEVEL = 0.75; // 溅射范围每级+0.75格
    public static final double MAX_SPLASH_RANGE = 6.0; // 最大溅射范围6格
    public static final double EXECUTE_THRESHOLD_LV1_2 = 0.20; // 处决1/2级阈值20%
    public static final double EXECUTE_THRESHOLD_LV3 = 0.15; // 处决3级阈值15%
    public static final double EXECUTE_BONUS_LV1 = 0.30; // 处决1级+30%伤害
    public static final double EXECUTE_BONUS_LV2 = 0.50; // 处决2级+50%伤害
    public static final int EXECUTE_COOLDOWN_TICKS = 20; // 处决3级冷却1秒(20tick)
    public static final double FROST_CHANCE_PER_LEVEL = 0.08; // 冰霜每级+8%触发概率
    public static final double FROST_SLOW_PER_LEVEL = 0.05; // 冰霜每级+5%减速
    public static final int FROST_DURATION = 60; // 冰霜持续3秒(60tick)
    public static final double POISON_CHANCE_PER_LEVEL = 0.08; // 中毒每级+8%触发概率
    public static final double POISON_DPS_PER_LEVEL = 0.01; // 中毒每级+1%每秒伤害
    public static final int POISON_DURATION = 100; // 中毒持续5秒(100tick)
    public static final double ARMOR_SHATTER_PER_LEVEL = 0.03; // 破甲每级+3%护甲降低
    public static final int ARMOR_SHATTER_DURATION = 100; // 破甲持续5秒
    public static final double MAX_ARMOR_REDUCTION = 0.50; // 护甲穿透+破甲总上限50%

    // ===== v2.0.0 新增防御系列常量 =====
    public static final double THORNS_REFLECT_PER_LEVEL = 0.03; // 反伤每级+3%
    public static final int THORNS_REFLECT_COOLDOWN_TICKS = 20; // 反伤冷却1秒
    public static final double SHIELD_PER_LEVEL = 0.10; // 护盾每级+10%最大生命值
    public static final int SHIELD_REGEN_SERVER_TICKS = 200; // 服务器护盾恢复10秒
    public static final int SHIELD_REGEN_EASY_TICKS = 100; // 简单难度5秒
    public static final int SHIELD_REGEN_NORMAL_TICKS = 160; // 普通难度8秒
    public static final int SHIELD_REGEN_HARD_TICKS = 260; // 困难/极限13秒
    public static final double KNOCKBACK_RESIST_PER_LEVEL = 0.05; // 击退抗性每级+5%
    public static final double MAX_KNOCKBACK_RESIST = 0.80; // 击退抗性总上限80%

    // ===== v2.0.0 新药水效果ID =====
    public static final Identifier FROST_EFFECT_ID = Identifier.fromNamespaceAndPath(MOD_ID, "frost");
    public static final Identifier POISON_PLUS_EFFECT_ID = Identifier.fromNamespaceAndPath(MOD_ID, "poison_plus");
    public static final Identifier ARMOR_BREAK_EFFECT_ID = Identifier.fromNamespaceAndPath(MOD_ID, "armor_break");

    private CriticalHitsEnchantments() {
    }

    /**
     * 获取物品上指定附魔的等级。
     */
    public static int getEnchantmentLevel(Identifier enchantmentId, ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 0;
        }
        try {
            ItemEnchantments enchantments = stack.getEnchantments();
            if (enchantments.isEmpty()) {
                return 0;
            }
            for (Object2IntMap.Entry<Holder<Enchantment>> entry : enchantments.entrySet()) {
                Holder<Enchantment> ench = entry.getKey();
                if (ench == null) continue;
                if (ench.is(enchantmentId)) {
                    return entry.getIntValue();
                }
            }
        } catch (Exception e) {
            // 捕获所有异常，防止获取附魔等级时崩溃
        }
        return 0;
    }

    // ===== 暴击系列方法 =====

    public static int getCriticalRateLevel(ItemStack weapon) {
        return getEnchantmentLevel(CRITICAL_RATE_ID, weapon);
    }

    public static int getCriticalDamageLevel(ItemStack weapon) {
        return getEnchantmentLevel(CRITICAL_DAMAGE_ID, weapon);
    }

    public static int getCriticalResistanceLevel(LivingEntity entity) {
        if (entity == null) {
            return 0;
        }
        int total = 0;
        EquipmentSlot[] armorSlots = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
        };
        for (EquipmentSlot slot : armorSlots) {
            ItemStack stack = entity.getItemBySlot(slot);
            total += getEnchantmentLevel(CRITICAL_RESISTANCE_ID, stack);
        }
        return total;
    }

    public static double calculateCriticalRate(int enchantmentLevel) {
        return DEFAULT_CRITICAL_RATE + enchantmentLevel * CRITICAL_RATE_PER_LEVEL;
    }

    public static double calculateCriticalDamageMultiplier(int enchantmentLevel) {
        return DEFAULT_CRITICAL_DAMAGE + enchantmentLevel * CRITICAL_DAMAGE_PER_LEVEL;
    }

    public static double calculateCriticalResistance(int enchantmentLevel) {
        return Math.min(enchantmentLevel * CRITICAL_RESIST_PER_LEVEL, MAX_CRITICAL_RESIST);
    }

    // ===== 攻击系列方法 =====

    /** 百分比伤害加成等级 */
    public static int getPercentageDamageLevel(ItemStack weapon) {
        return getEnchantmentLevel(PERCENTAGE_DAMAGE_ID, weapon);
    }

    /** 计算百分比伤害加成（返回0.02表示+2%） */
    public static float calculatePercentageDamage(int enchantmentLevel) {
        return (float) (enchantmentLevel * PERCENTAGE_DAMAGE_PER_LEVEL);
    }

    /** 护甲穿透等级 */
    public static int getArmorPenetrationLevel(ItemStack weapon) {
        return getEnchantmentLevel(ARMOR_PENETRATION_ID, weapon);
    }

    /** 计算护甲穿透比例（返回0.02表示无视2%护甲） */
    public static float calculateArmorPenetration(int enchantmentLevel) {
        return (float) (enchantmentLevel * ARMOR_PENETRATION_PER_LEVEL);
    }

    /** 生命吸取等级 */
    public static int getLifeStealLevel(ItemStack weapon) {
        return getEnchantmentLevel(LIFE_STEAL_ID, weapon);
    }

    /** 计算生命吸取比例（返回0.02表示吸取2%伤害） */
    public static float calculateLifeSteal(int enchantmentLevel) {
        return (float) (enchantmentLevel * LIFE_STEAL_PER_LEVEL);
    }

    /** 易伤附魔等级 */
    public static int getVulnerabilityEnchantLevel(ItemStack weapon) {
        return getEnchantmentLevel(VULNERABILITY_ID, weapon);
    }

    /**
     * 获取易伤效果的Holder。
     */
    @SuppressWarnings("unchecked")
    private static net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> getVulnerabilityHolder() {
        if (CriticalHitsMod.VULNERABILITY_EFFECT == null) {
            return null;
        }
        return (net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect>)
            net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT
                .get(Identifier.fromNamespaceAndPath(MOD_ID, "vulnerability"))
                .orElse(null);
    }

    /**
     * 获取目标的易伤效果总加成（药水效果）。
     */
    public static float getVulnerabilityBonus(LivingEntity target) {
        if (target == null) {
            return 0;
        }
        net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> holder = getVulnerabilityHolder();
        if (holder != null) {
            MobEffectInstance effect = target.getEffect(holder);
            if (effect != null) {
                int amplifier = effect.getAmplifier();
                return (float) ((amplifier + 1) * VULNERABILITY_POTION_PER_LEVEL);
            }
        }
        return 0;
    }

    /**
     * 给目标应用易伤效果。
     */
    public static void applyVulnerabilityEffect(LivingEntity target, int enchantLevel) {
        if (target == null || enchantLevel <= 0) {
            return;
        }
        net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> holder = getVulnerabilityHolder();
        if (holder == null) {
            return;
        }
        int potionLevel = Math.min(3, (enchantLevel - 1) / 3 + 1);
        MobEffectInstance existing = target.getEffect(holder);
        if (existing != null && existing.getAmplifier() >= potionLevel - 1) {
            target.removeEffect(holder);
        }
        target.addEffect(new MobEffectInstance(
            holder,
            VULNERABILITY_DURATION,
            potionLevel - 1,
            false,
            true
        ));
    }

    // ===== 防御系列方法 =====

    /**
     * 获取生物全身护甲的伤害减免总等级。
     */
    public static int getDamageReductionLevel(LivingEntity entity) {
        return getTotalArmorEnchantmentLevel(entity, DAMAGE_REDUCTION_ID);
    }

    /**
     * 计算伤害减免比例（乘法叠加，上限50%）。
     * 公式：1 - (1 - 每级减伤)^总等级，最多50%
     */
    public static float calculateDamageReduction(int enchantmentLevel) {
        if (enchantmentLevel <= 0) return 0;
        float reduction = (float) (1.0 - Math.pow(1.0 - DAMAGE_REDUCTION_PER_LEVEL, enchantmentLevel));
        return Math.min(reduction, 0.5F); // 上限50%
    }

    /**
     * 获取生物全身护甲的护甲防御总等级。
     */
    public static int getArmorDefenseLevel(LivingEntity entity) {
        return getTotalArmorEnchantmentLevel(entity, ARMOR_DEFENSE_ID);
    }

    /**
     * 计算护甲防御附魔增加的盔甲韧性值。
     * 每级+0.5韧性，4件×20级=80级=+40韧性
     */
    public static float calculateArmorToughness(int enchantmentLevel) {
        return (float) (enchantmentLevel * ARMOR_DEFENSE_PER_LEVEL);
    }

    /**
     * 获取生物全身护甲的最大生命值总等级。
     */
    public static int getMaxHealthLevel(LivingEntity entity) {
        return getTotalArmorEnchantmentLevel(entity, MAX_HEALTH_ID);
    }

    /**
     * 计算最大生命值加成（返回10表示+10点生命值）。
     */
    public static float calculateMaxHealth(int enchantmentLevel) {
        return (float) (enchantmentLevel * MAX_HEALTH_PER_LEVEL);
    }

    /**
     * 获取生物全身护甲的指定附魔总等级。
     */
    private static int getTotalArmorEnchantmentLevel(LivingEntity entity, Identifier enchantmentId) {
        if (entity == null) {
            return 0;
        }
        int total = 0;
        EquipmentSlot[] armorSlots = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
        };
        for (EquipmentSlot slot : armorSlots) {
            ItemStack stack = entity.getItemBySlot(slot);
            total += getEnchantmentLevel(enchantmentId, stack);
        }
        return total;
    }

    /**
     * 应用防御类附魔（伤害减免+暴击减免），返回最终伤害。
     * 护甲防御（增加韧性）在攻击端临时修改目标属性实现。
     * 顺序：伤害减免 → 暴击减免 → 总减伤上限 → 最小伤害
     * @param entity 受伤害的生物
     * @param damage 原始伤害（已经过攻击端乘区）
     * @param isCritical 是否为暴击伤害
     * @return 最终伤害
     */
    public static float applyDefenseEnchantments(LivingEntity entity, float damage, boolean isCritical) {
        if (entity == null || damage <= 0) {
            return damage;
        }

        float result = damage;

        // v2.0.0 盾牌减伤：玩家举盾牌时，伤害减免和暴击减免效果+50%
        boolean isBlocking = false;
        if (entity instanceof net.minecraft.world.entity.player.Player player) {
            isBlocking = player.isBlocking();
        }
        float shieldMultiplier = isBlocking ? 1.5f : 1.0f;

        // 伤害减免（乘法减伤，上限50%，举盾时效果+50%）
        int damageReductionLevel = getDamageReductionLevel(entity);
        if (damageReductionLevel > 0) {
            float damageReduction = calculateDamageReduction(damageReductionLevel);
            damageReduction = Math.min(damageReduction * shieldMultiplier, 0.75f); // 举盾时上限75%
            result = result * (1.0F - damageReduction);
        }

        // 移动速度带来的抗性加成（负等级加抗性）
        int movementSpeedLevel = getMovementSpeedLevel(entity);
        if (movementSpeedLevel < 0) {
            double movementResistBonus = calculateMovementSpeedResistBonus(movementSpeedLevel);
            result = result * (float) (1.0 - movementResistBonus);
        }

        // 暴击减免（仅对暴击伤害，举盾时效果+50%）
        if (isCritical) {
            int critResistLevel = getCriticalResistanceLevel(entity);
            if (critResistLevel > 0) {
                double critResist = calculateCriticalResistance(critResistLevel);
                critResist = Math.min(critResist * shieldMultiplier, MAX_CRITICAL_RESIST);
                result = result * (float) (1.0 - critResist);
            }
        }

        // 总减伤上限95%，最小伤害1点
        float maxReducedDamage = damage * (1.0F - (float) MAX_TOTAL_DAMAGE_REDUCTION);
        if (result < maxReducedDamage) {
            result = maxReducedDamage;
        }
        if (result < MIN_DAMAGE) {
            result = MIN_DAMAGE;
        }

        return result;
    }

    // ===== 属性系列方法 =====

    /** 攻击速度附魔等级（正等级优先，负等级返回负数） */
    public static int getAttackSpeedLevel(ItemStack weapon) {
        int positive = getEnchantmentLevel(ATTACK_SPEED_ID, weapon);
        int negative = getEnchantmentLevel(ATTACK_SPEED_CURSE_ID, weapon);
        if (positive > 0) return positive; // 正等级优先，强制互斥
        return -negative;
    }

    /** 移动速度附魔等级（武器+4件装备总和，正等级优先，负等级返回负数；装备每件最高2级） */
    public static int getMovementSpeedLevel(LivingEntity entity) {
        if (entity == null) return 0;
        int positive = 0;
        int negative = 0;
        // 武器上的移动速度（最高10级）
        positive += getEnchantmentLevel(MOVEMENT_SPEED_ID, entity.getMainHandItem());
        negative += getEnchantmentLevel(MOVEMENT_SPEED_CURSE_ID, entity.getMainHandItem());
        // 装备上的移动速度（每件最高2级）
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack armor = entity.getItemBySlot(slot);
            int posLevel = Math.min(2, getEnchantmentLevel(MOVEMENT_SPEED_ID, armor));
            int negLevel = Math.min(2, getEnchantmentLevel(MOVEMENT_SPEED_CURSE_ID, armor));
            positive += posLevel;
            negative += negLevel;
        }
        if (positive > 0) return positive; // 正等级优先，强制互斥
        return -negative;
    }

    /** 攻击范围附魔等级（正等级优先，负等级返回负数） */
    public static int getAttackRangeLevel(ItemStack weapon) {
        int positive = getEnchantmentLevel(ATTACK_RANGE_ID, weapon);
        int negative = getEnchantmentLevel(ATTACK_RANGE_CURSE_ID, weapon);
        if (positive > 0) return positive; // 正等级优先，强制互斥
        return -negative;
    }

    /** 额外伤害附魔等级 */
    public static int getBonusDamageLevel(ItemStack weapon) {
        return getEnchantmentLevel(BONUS_DAMAGE_ID, weapon);
    }

    /** 计算攻击速度倍率（正等级加速，负等级减速） */
    public static double calculateAttackSpeedMultiplier(int enchantmentLevel) {
        double multiplier = 1.0 + enchantmentLevel * ATTACK_SPEED_PER_LEVEL;
        return Math.max(MIN_ATTACK_SPEED / 4.0, Math.min(MAX_ATTACK_SPEED / 4.0, multiplier));
    }

    /** 计算移动速度倍率（正等级加速，负等级减速） */
    public static double calculateMovementSpeedMultiplier(int enchantmentLevel) {
        double multiplier = 1.0 + enchantmentLevel * MOVEMENT_SPEED_PER_LEVEL;
        return Math.max(MIN_MOVEMENT_SPEED_MULTIPLIER, Math.min(MAX_MOVEMENT_SPEED_MULTIPLIER, multiplier));
    }

    /** 计算攻击范围加成（格数） */
    public static double calculateAttackRangeBonus(int enchantmentLevel) {
        return enchantmentLevel * ATTACK_RANGE_PER_LEVEL;
    }

    /** 计算额外伤害倍率 */
    public static double calculateBonusDamageMultiplier(int enchantmentLevel) {
        return 1.0 + enchantmentLevel * BONUS_DAMAGE_PER_LEVEL;
    }

    /** 计算攻击速度带来的伤害修正（正等级每级-5%伤害，负等级每级+5%伤害） */
    public static double calculateAttackSpeedDamageModifier(int enchantmentLevel) {
        return 1.0 - enchantmentLevel * 0.05;
    }

    /** 计算移动速度带来的抗性加成（负等级每级+3%抗性） */
    public static double calculateMovementSpeedResistBonus(int enchantmentLevel) {
        if (enchantmentLevel >= 0) return 0;
        return Math.abs(enchantmentLevel) * 0.03;
    }

    /** 计算攻击范围带来的攻速惩罚（正等级每级-5%攻速，负等级每级+5%攻速） */
    public static double calculateAttackRangeSpeedPenalty(int enchantmentLevel) {
        return 1.0 - enchantmentLevel * 0.05;
    }

    // ===== v2.0.0 新增攻击系列方法 =====

    /** 真实伤害等级 */
    public static int getTrueDamageLevel(ItemStack weapon) {
        return getEnchantmentLevel(TRUE_DAMAGE_ID, weapon);
    }

    /** 计算真实伤害比例（返回0.01表示+1%） */
    public static float calculateTrueDamage(int enchantmentLevel) {
        return (float) (enchantmentLevel * TRUE_DAMAGE_PER_LEVEL);
    }

    /** 溅射伤害等级 */
    public static int getSplashDamageLevel(ItemStack weapon) {
        return getEnchantmentLevel(SPLASH_DAMAGE_ID, weapon);
    }

    /** 计算溅射伤害比例 */
    public static float calculateSplashDamage(int enchantmentLevel) {
        return (float) (enchantmentLevel * SPLASH_DAMAGE_PER_LEVEL);
    }

    /** 计算溅射范围（格数） */
    public static double calculateSplashRange(int enchantmentLevel) {
        return Math.min(2.0 + enchantmentLevel * SPLASH_RANGE_PER_LEVEL, MAX_SPLASH_RANGE);
    }

    /** 处决等级 */
    public static int getExecuteLevel(ItemStack weapon) {
        return getEnchantmentLevel(EXECUTE_ID, weapon);
    }

    /** 冰霜等级 */
    public static int getFrostLevel(ItemStack weapon) {
        return getEnchantmentLevel(FROST_ID, weapon);
    }

    /** 计算冰霜触发概率 */
    public static float calculateFrostChance(int enchantmentLevel) {
        return (float) (enchantmentLevel * FROST_CHANCE_PER_LEVEL);
    }

    /** 计算冰霜减速比例 */
    public static float calculateFrostSlow(int enchantmentLevel) {
        return (float) (enchantmentLevel * FROST_SLOW_PER_LEVEL);
    }

    /** 中毒等级 */
    public static int getPoisonLevel(ItemStack weapon) {
        return getEnchantmentLevel(POISON_ID, weapon);
    }

    /** 计算中毒触发概率 */
    public static float calculatePoisonChance(int enchantmentLevel) {
        return (float) (enchantmentLevel * POISON_CHANCE_PER_LEVEL);
    }

    /** 计算中毒每秒伤害比例 */
    public static float calculatePoisonDps(int enchantmentLevel) {
        return (float) (enchantmentLevel * POISON_DPS_PER_LEVEL);
    }

    /** 破甲等级 */
    public static int getArmorShatterLevel(ItemStack weapon) {
        return getEnchantmentLevel(ARMOR_SHATTER_ID, weapon);
    }

    /** 计算破甲比例 */
    public static float calculateArmorShatter(int enchantmentLevel) {
        return (float) (enchantmentLevel * ARMOR_SHATTER_PER_LEVEL);
    }

    // ===== v2.0.0 新增防御系列方法 =====

    /** 反伤等级（全身护甲总和） */
    public static int getThornsReflectLevel(LivingEntity entity) {
        return getTotalArmorEnchantmentLevel(entity, THORNS_REFLECT_ID);
    }

    /** 计算反伤比例 */
    public static float calculateThornsReflect(int enchantmentLevel) {
        return (float) (enchantmentLevel * THORNS_REFLECT_PER_LEVEL);
    }

    /** 护盾等级（全身护甲总和） */
    public static int getShieldLevel(LivingEntity entity) {
        return getTotalArmorEnchantmentLevel(entity, SHIELD_ID);
    }

    /** 计算护盾值（最大生命值百分比） */
    public static float calculateShieldValue(int enchantmentLevel, float maxHealth) {
        return maxHealth * (float) (enchantmentLevel * SHIELD_PER_LEVEL);
    }

    /** 击退抗性等级（全身护甲总和） */
    public static int getKnockbackResistanceLevel(LivingEntity entity) {
        return getTotalArmorEnchantmentLevel(entity, KNOCKBACK_RESISTANCE_ID);
    }

    /** 计算击退抗性比例 */
    public static float calculateKnockbackResistance(int enchantmentLevel) {
        return (float) Math.min(enchantmentLevel * KNOCKBACK_RESIST_PER_LEVEL, MAX_KNOCKBACK_RESIST);
    }

    /**
     * 检查目标是否为Boss（处决3级对Boss无效）
     */
    public static boolean isBoss(LivingEntity entity) {
        if (entity == null) return false;
        String typeName = entity.getType().toShortString();
        return typeName.equals("wither") || typeName.equals("ender_dragon")
            || typeName.equals("warden") || typeName.equals("elder_guardian")
            || entity.getType().getCategory().getName().equals("boss");
    }
}
