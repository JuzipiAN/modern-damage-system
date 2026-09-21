package com.juzipi.criticalhits.api;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * 现代化伤害系统 - 附魔API。
 * 提供附魔等级查询、附魔检查、附魔列表等功能。
 * 通过反射调用主mod的方法，确保版本兼容性。
 * 
 * @version 1.0.0.1-fabric
 * @author Juzipi_AN
 */
public class MdsEnchantmentApi {

    private static final String MOD_ID = "criticalhits";
    private static final String ENCHANTS_CLASS = "com.juzipi.criticalhits.CriticalHitsEnchantments";

    /**
     * 获取附魔的完整ID（包含命名空间）。
     *
     * @param enchantmentName 附魔名称（如 "critical_rate"）
     * @return 完整ID（如 "criticalhits:critical_rate"）
     */
    public static String getFullEnchantmentId(String enchantmentName) {
        if (enchantmentName == null || enchantmentName.isEmpty()) return null;
        if (enchantmentName.contains(":")) return enchantmentName;
        return MOD_ID + ":" + enchantmentName;
    }

    /**
     * 获取物品上指定附魔的等级（通过反射调用主mod方法）。
     *
     * @param itemStack 物品堆
     * @param enchantmentName 附魔名称（如 "critical_rate"）
     * @return 附魔等级（0表示没有该附魔）
     */
    public static int getEnchantmentLevel(ItemStack itemStack, String enchantmentName) {
        if (itemStack == null || itemStack.isEmpty() || enchantmentName == null) return 0;
        try {
            // 获取Identifier类
            Class<?> identifierClass = Class.forName("net.minecraft.util.Identifier");
            java.lang.reflect.Method fromNamespaceMethod = identifierClass.getMethod(
                "fromNamespaceAndPath", String.class, String.class);
            Object enchantmentId = fromNamespaceMethod.invoke(null, MOD_ID, enchantmentName);
            
            // 调用主mod的getEnchantmentLevel方法
            Class<?> enchantsClass = Class.forName(ENCHANTS_CLASS);
            java.lang.reflect.Method method = enchantsClass.getMethod(
                "getEnchantmentLevel", identifierClass, ItemStack.class);
            return (int) method.invoke(null, enchantmentId, itemStack);
        } catch (Exception e) {
            MdsApiMain.LOGGER.warn("[MDS API] 获取附魔等级失败: {}", enchantmentName, e);
            return 0;
        }
    }

    /**
     * 检查物品是否有指定附魔。
     *
     * @param itemStack 物品堆
     * @param enchantmentName 附魔名称（如 "critical_rate"）
     * @return 是否有该附魔
     */
    public static boolean hasEnchantment(ItemStack itemStack, String enchantmentName) {
        return getEnchantmentLevel(itemStack, enchantmentName) > 0;
    }

    /**
     * 获取实体手持物品上指定附魔的等级。
     *
     * @param entity 实体
     * @param enchantmentName 附魔名称
     * @return 附魔等级
     */
    public static int getHeldItemEnchantmentLevel(LivingEntity entity, String enchantmentName) {
        if (entity == null) return 0;
        return getEnchantmentLevel(entity.getMainHandItem(), enchantmentName);
    }

    /**
     * 获取实体所有盔甲装备上指定附魔的总等级。
     *
     * @param entity 实体
     * @param enchantmentName 附魔名称
     * @return 总附魔等级
     */
    public static int getTotalArmorEnchantmentLevel(LivingEntity entity, String enchantmentName) {
        if (entity == null) return 0;
        int total = 0;
        // 使用反射获取盔甲槽位，兼容26.2
        try {
            java.lang.reflect.Method getArmorSlotsMethod = entity.getClass().getMethod("getArmorSlots");
            Object armorSlots = getArmorSlotsMethod.invoke(entity);
            if (armorSlots instanceof Iterable) {
                for (Object obj : (Iterable<?>) armorSlots) {
                    if (obj instanceof ItemStack) {
                        total += getEnchantmentLevel((ItemStack) obj, enchantmentName);
                    }
                }
            }
        } catch (Exception e) {
            MdsApiMain.LOGGER.warn("[MDS API] 获取盔甲附魔等级失败", e);
        }
        return total;
    }

    /**
     * 获取实体所有装备（武器+盔甲）上指定附魔的总等级。
     *
     * @param entity 实体
     * @param enchantmentName 附魔名称
     * @return 总附魔等级
     */
    public static int getTotalEquipmentEnchantmentLevel(LivingEntity entity, String enchantmentName) {
        if (entity == null) return 0;
        int total = getHeldItemEnchantmentLevel(entity, enchantmentName);
        total += getTotalArmorEnchantmentLevel(entity, enchantmentName);
        return total;
    }

    /**
     * 检查附魔是否为MDS的附魔。
     *
     * @param enchantmentId 附魔ID
     * @return 是否为MDS附魔
     */
    public static boolean isMdsEnchantment(String enchantmentId) {
        if (enchantmentId == null) return false;
        return enchantmentId.startsWith(MOD_ID + ":");
    }

    /**
     * 获取所有MDS附魔的ID列表。
     *
     * @return MDS附魔ID数组
     */
    public static String[] getAllMdsEnchantmentIds() {
        return new String[] {
            "critical_rate", "critical_damage", "critical_resistance",
            "percentage_damage", "armor_penetration", "life_steal", "vulnerability",
            "true_damage", "splash_damage", "execute", "frost", "poison", "armor_shatter", "thorns_reflect",
            "damage_reduction", "armor_defense", "max_health", "shield", "knockback_resistance",
            "attack_speed", "movement_speed", "attack_range", "bonus_damage",
            "attack_speed_curse", "movement_speed_curse", "attack_range_curse"
        };
    }
}
