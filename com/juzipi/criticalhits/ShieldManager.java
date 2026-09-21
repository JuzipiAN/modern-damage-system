package com.juzipi.criticalhits;

import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 护盾系统管理器。
 * 跟踪每个实体的护盾值，处理护盾吸收和恢复。
 */
public class ShieldManager {

    // 实体UUID -> 当前护盾值
    private static final Map<UUID, Float> currentShield = new HashMap<>();
    // 实体UUID -> 最大护盾值
    private static final Map<UUID, Float> maxShield = new HashMap<>();
    // 实体UUID -> 脱战计时器(tick)
    private static final Map<UUID, Integer> outOfCombatTimer = new HashMap<>();
    // 实体UUID -> 是否在战斗中
    private static final Map<UUID, Boolean> inCombat = new HashMap<>();

    /**
     * 获取实体的当前护盾值。
     */
    public static float getCurrentShield(LivingEntity entity) {
        if (entity == null) return 0;
        return currentShield.getOrDefault(entity.getUUID(), 0f);
    }

    /**
     * 获取实体的最大护盾值。
     */
    public static float getMaxShield(LivingEntity entity) {
        if (entity == null) return 0;
        return maxShield.getOrDefault(entity.getUUID(), 0f);
    }

    /**
     * 设置实体的最大护盾值（根据附魔等级计算）。
     */
    public static void setMaxShield(LivingEntity entity, float max) {
        if (entity == null) return;
        UUID uuid = entity.getUUID();
        maxShield.put(uuid, max);
        // 如果当前护盾大于最大护盾，调整为最大护盾
        float current = currentShield.getOrDefault(uuid, max);
        currentShield.put(uuid, Math.min(current, max));
    }

    /**
     * 实体受到伤害时，优先用护盾吸收。
     * @return 实际造成的伤害（护盾吸收后剩余的伤害）
     */
    public static float absorbDamage(LivingEntity entity, float damage) {
        if (entity == null || damage <= 0) return damage;
        UUID uuid = entity.getUUID();
        float shield = currentShield.getOrDefault(uuid, 0f);

        if (shield <= 0) return damage;

        // 进入战斗状态
        inCombat.put(uuid, true);
        outOfCombatTimer.put(uuid, 0);

        if (shield >= damage) {
            // 护盾完全吸收伤害
            currentShield.put(uuid, shield - damage);
            return 0;
        } else {
            // 护盾部分吸收，剩余伤害继续
            currentShield.put(uuid, 0f);
            return damage - shield;
        }
    }

    /**
     * 每tick更新护盾状态（脱战恢复）。
     * @param entity 实体
     * @param regenTicks 恢复所需的tick数（200=10秒）
     */
    public static void tick(LivingEntity entity, int regenTicks) {
        if (entity == null) return;
        UUID uuid = entity.getUUID();
        float max = maxShield.getOrDefault(uuid, 0f);

        if (max <= 0) return;

        // 检查是否在战斗中
        boolean combat = inCombat.getOrDefault(uuid, false);
        if (combat) {
            int timer = outOfCombatTimer.getOrDefault(uuid, 0) + 1;
            outOfCombatTimer.put(uuid, timer);
            // 100 tick (5秒) 没有受到伤害则脱战
            if (timer >= 100) {
                inCombat.put(uuid, false);
                outOfCombatTimer.put(uuid, 0);
            }
        } else {
            // 脱战状态，逐渐恢复护盾
            float current = currentShield.getOrDefault(uuid, 0f);
            if (current < max) {
                // 每regenTicks恢复满
                float regenPerTick = max / regenTicks;
                currentShield.put(uuid, Math.min(max, current + regenPerTick));
            }
        }
    }

    /**
     * 实体受到伤害时标记为战斗状态。
     */
    public static void markInCombat(LivingEntity entity) {
        if (entity == null) return;
        UUID uuid = entity.getUUID();
        inCombat.put(uuid, true);
        outOfCombatTimer.put(uuid, 0);
    }

    /**
     * 清除实体的护盾数据（实体死亡时调用）。
     */
    public static void remove(LivingEntity entity) {
        if (entity == null) return;
        UUID uuid = entity.getUUID();
        currentShield.remove(uuid);
        maxShield.remove(uuid);
        outOfCombatTimer.remove(uuid);
        inCombat.remove(uuid);
    }
}
