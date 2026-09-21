package com.juzipi.criticalhits.api;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * 现代化伤害系统 - 伤害计算API。
 * 提供武器伤害、暴击、伤害减免等伤害相关的查询和计算功能。
 * 
 * <p>使用示例：
 * <pre>{@code
 * // 获取武器基础伤害
 * float baseDamage = MdsDamageApi.getWeaponBaseDamage(player.getMainHandItem());
 * 
 * // 计算暴击后的伤害
 * float critDamage = MdsDamageApi.calculateCriticalDamage(player, target, baseDamage);
 * 
 * // 检查是否暴击
 * boolean isCrit = MdsDamageApi.isCriticalHit(player);
 * }</pre>
 * 
 * @version 1.0.0.1-fabric
 * @author Juzipi_AN
 */
public class MdsDamageApi {

    /**
     * 获取武器的基础攻击伤害。
     * 包含武器本身伤害和锋利附魔加成。
     *
     * @param weapon 武器物品堆
     * @return 基础攻击伤害，如果不是武器则返回0
     */
    public static float getWeaponBaseDamage(ItemStack weapon) {
        if (weapon == null || weapon.isEmpty()) return 0;
        try {
            // 调用主mod的伤害计算方法
            Class<?> damageCalcClass = Class.forName("com.juzipi.criticalhits.DamageCalculator");
            java.lang.reflect.Method method = damageCalcClass.getMethod("getWeaponBaseDamage", ItemStack.class);
            return (float) method.invoke(null, weapon);
        } catch (Exception e) {
            MdsApiMain.LOGGER.warn("[MDS API] getWeaponBaseDamage 调用失败", e);
            return 0;
        }
    }

    /**
     * 获取玩家的暴击率。
     * 包含基础5%暴击率和附魔加成。
     *
     * @param entity 玩家实体
     * @return 暴击率（0.05表示5%）
     */
    public static float getCriticalRate(LivingEntity entity) {
        if (entity == null) return 0.05f;
        try {
            Class<?> enchantsClass = Class.forName("com.juzipi.criticalhits.CriticalHitsEnchantments");
            java.lang.reflect.Method method = enchantsClass.getMethod("getCriticalRate", LivingEntity.class);
            return (float) method.invoke(null, entity);
        } catch (Exception e) {
            MdsApiMain.LOGGER.warn("[MDS API] getCriticalRate 调用失败", e);
            return 0.05f;
        }
    }

    /**
     * 获取玩家的暴击伤害加成。
     * 包含基础50%暴击伤害和附魔加成。
     *
     * @param entity 玩家实体
     * @return 暴击伤害加成（0.5表示50%）
     */
    public static float getCriticalDamage(LivingEntity entity) {
        if (entity == null) return 0.5f;
        try {
            Class<?> enchantsClass = Class.forName("com.juzipi.criticalhits.CriticalHitsEnchantments");
            java.lang.reflect.Method method = enchantsClass.getMethod("getCriticalDamage", LivingEntity.class);
            return (float) method.invoke(null, entity);
        } catch (Exception e) {
            MdsApiMain.LOGGER.warn("[MDS API] getCriticalDamage 调用失败", e);
            return 0.5f;
        }
    }

    /**
     * 检查本次攻击是否暴击。
     * 基于暴击率进行随机判定。
     *
     * @param attacker 攻击者
     * @return 是否暴击
     */
    public static boolean isCriticalHit(LivingEntity attacker) {
        if (attacker == null) return false;
        float critRate = getCriticalRate(attacker);
        return attacker.getRandom().nextFloat() < critRate;
    }

    /**
     * 计算暴击后的伤害。
     *
     * @param attacker 攻击者
     * @param target 目标
     * @param baseDamage 基础伤害
     * @return 暴击后的伤害
     */
    public static float calculateCriticalDamage(LivingEntity attacker, LivingEntity target, float baseDamage) {
        if (attacker == null || baseDamage <= 0) return baseDamage;
        boolean isCrit = isCriticalHit(attacker);
        if (!isCrit) return baseDamage;
        float critDamage = getCriticalDamage(attacker);
        return baseDamage * (1 + critDamage);
    }

    /**
     * 获取目标的伤害减免百分比。
     * 包含伤害减免附魔和护甲减伤。
     *
     * @param entity 目标实体
     * @return 伤害减免百分比（0.5表示减免50%）
     */
    public static float getDamageReduction(LivingEntity entity) {
        if (entity == null) return 0;
        try {
            Class<?> enchantsClass = Class.forName("com.juzipi.criticalhits.CriticalHitsEnchantments");
            java.lang.reflect.Method method = enchantsClass.getMethod("getDamageReduction", LivingEntity.class);
            return (float) method.invoke(null, entity);
        } catch (Exception e) {
            MdsApiMain.LOGGER.warn("[MDS API] getDamageReduction 调用失败", e);
            return 0;
        }
    }

    /**
     * 获取目标的暴击减免百分比。
     *
     * @param entity 目标实体
     * @return 暴击减免百分比（0.8表示减免80%）
     */
    public static float getCriticalResistance(LivingEntity entity) {
        if (entity == null) return 0;
        try {
            Class<?> enchantsClass = Class.forName("com.juzipi.criticalhits.CriticalHitsEnchantments");
            java.lang.reflect.Method method = enchantsClass.getMethod("getCriticalResistance", LivingEntity.class);
            return (float) method.invoke(null, entity);
        } catch (Exception e) {
            MdsApiMain.LOGGER.warn("[MDS API] getCriticalResistance 调用失败", e);
            return 0;
        }
    }

    /**
     * 计算最终伤害（包含暴击、伤害减免、暴击减免）。
     *
     * @param attacker 攻击者
     * @param target 目标
     * @param baseDamage 基础伤害
     * @return 最终伤害
     */
    public static float calculateFinalDamage(LivingEntity attacker, LivingEntity target, float baseDamage) {
        if (attacker == null || target == null || baseDamage <= 0) return baseDamage;

        // 计算暴击伤害
        float damage = calculateCriticalDamage(attacker, target, baseDamage);

        // 计算伤害减免
        float damageReduction = getDamageReduction(target);
        damage *= (1 - damageReduction);

        // 如果是暴击，计算暴击减免
        if (damage > baseDamage) {
            float critResistance = getCriticalResistance(target);
            float extraCritDamage = damage - baseDamage;
            extraCritDamage *= (1 - critResistance);
            damage = baseDamage + extraCritDamage;
        }

        // 确保伤害不小于1
        return Math.max(1, damage);
    }

    /**
     * 获取玩家的百分比伤害加成。
     *
     * @param entity 玩家实体
     * @return 百分比伤害加成（0.4表示+40%）
     */
    public static float getPercentageDamageBonus(LivingEntity entity) {
        if (entity == null) return 0;
        try {
            Class<?> enchantsClass = Class.forName("com.juzipi.criticalhits.CriticalHitsEnchantments");
            java.lang.reflect.Method method = enchantsClass.getMethod("getPercentageDamageBonus", LivingEntity.class);
            return (float) method.invoke(null, entity);
        } catch (Exception e) {
            MdsApiMain.LOGGER.warn("[MDS API] getPercentageDamageBonus 调用失败", e);
            return 0;
        }
    }

    /**
     * 获取玩家的护甲穿透百分比。
     *
     * @param entity 玩家实体
     * @return 护甲穿透百分比（0.4表示忽略40%护甲）
     */
    public static float getArmorPenetration(LivingEntity entity) {
        if (entity == null) return 0;
        try {
            Class<?> enchantsClass = Class.forName("com.juzipi.criticalhits.CriticalHitsEnchantments");
            java.lang.reflect.Method method = enchantsClass.getMethod("getArmorPenetration", LivingEntity.class);
            return (float) method.invoke(null, entity);
        } catch (Exception e) {
            MdsApiMain.LOGGER.warn("[MDS API] getArmorPenetration 调用失败", e);
            return 0;
        }
    }
}
