package com.juzipi.criticalhits;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * 破甲效果：降低目标护甲值。
 * 1级：-15%护甲，2级：-25%护甲，3级：-35%护甲
 * 实际护甲降低在PlayerEntityAttackMixin中处理
 */
public class ArmorBreakEffect extends MobEffect {

    public ArmorBreakEffect() {
        super(MobEffectCategory.HARMFUL, 0x808080);
    }

    public void applyEffectTick(LivingEntity entity, int amplifier) {
        // 破甲效果在伤害计算时通过Mixin处理
    }
}
