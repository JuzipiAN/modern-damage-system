package com.juzipi.criticalhits.mixin;

import com.juzipi.criticalhits.CriticalHitsEnchantments;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 属性修饰符 Mixin。
 * 根据附魔等级动态修改攻击速度、攻击范围、移动速度属性。
 */
@Mixin(Player.class)
public abstract class AttributeModifierMixin {

    @Unique
    private static final Identifier ATTACK_SPEED_ID = Identifier.fromNamespaceAndPath("criticalhits", "attack_speed_modifier");

    @Unique
    private static final Identifier ATTACK_RANGE_ID = Identifier.fromNamespaceAndPath("criticalhits", "attack_range_modifier");

    @Unique
    private static final Identifier MOVEMENT_SPEED_ID = Identifier.fromNamespaceAndPath("criticalhits", "movement_speed_modifier");

    @Unique
    private int lastAttackSpeedLevel = Integer.MIN_VALUE;

    @Unique
    private int lastAttackRangeLevel = Integer.MIN_VALUE;

    @Unique
    private int lastMovementSpeedLevel = Integer.MIN_VALUE;


    @Inject(method = "tick", at = @At("HEAD"))
    private void criticalhits$updateAttributeModifiers(CallbackInfo ci) {
        Player player = (Player) (Object) this;

        // ===== 攻击速度修饰符 =====
        ItemStack weapon = player.getMainHandItem();
        int attackSpeedLevel = CriticalHitsEnchantments.getAttackSpeedLevel(weapon);
        int attackRangeLevel = CriticalHitsEnchantments.getAttackRangeLevel(weapon);

        // 攻击范围带来的攻速惩罚
        double rangeSpeedPenalty = CriticalHitsEnchantments.calculateAttackRangeSpeedPenalty(attackRangeLevel);

        if (attackSpeedLevel != lastAttackSpeedLevel || attackRangeLevel != lastAttackRangeLevel) {
            updateAttackSpeedModifier(player, attackSpeedLevel, rangeSpeedPenalty);
            lastAttackSpeedLevel = attackSpeedLevel;
        }

        // ===== 攻击范围修饰符 =====
        if (attackRangeLevel != lastAttackRangeLevel) {
            updateAttackRangeModifier(player, attackRangeLevel);
            lastAttackRangeLevel = attackRangeLevel;
        }

        // ===== 移动速度修饰符 =====
        int movementSpeedLevel = CriticalHitsEnchantments.getMovementSpeedLevel(player);
        if (movementSpeedLevel != lastMovementSpeedLevel) {
            updateMovementSpeedModifier(player, movementSpeedLevel);
            lastMovementSpeedLevel = movementSpeedLevel;
        }
    }

    @Unique
    private void updateAttackSpeedModifier(Player player, int attackSpeedLevel, double rangeSpeedPenalty) {
        AttributeInstance attr = player.getAttribute(Attributes.ATTACK_SPEED);
        if (attr == null) return;

        // 移除旧修饰符
        attr.removeModifier(ATTACK_SPEED_ID);

        if (attackSpeedLevel != 0 || rangeSpeedPenalty != 1.0) {
            double speedMultiplier = CriticalHitsEnchantments.calculateAttackSpeedMultiplier(attackSpeedLevel);
            double finalSpeed = speedMultiplier * rangeSpeedPenalty;

            // 攻速上下限：最低30%，最高300%，避免极端情况
            finalSpeed = Math.max(0.3, Math.min(3.0, finalSpeed));

            double finalMultiplier = finalSpeed - 1.0; // 转换为增量

            if (Math.abs(finalMultiplier) > 0.001) {
                AttributeModifier modifier = new AttributeModifier(
                    ATTACK_SPEED_ID,
                    finalMultiplier,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
                );
                attr.addTransientModifier(modifier);
            }
        }
    }

    @Unique
    private void updateAttackRangeModifier(Player player, int attackRangeLevel) {
        AttributeInstance attr = player.getAttribute(Attributes.ENTITY_INTERACTION_RANGE);
        if (attr == null) return;

        // 移除旧修饰符
        attr.removeModifier(ATTACK_RANGE_ID);

        if (attackRangeLevel > 0) {
            double rangeBonus = CriticalHitsEnchantments.calculateAttackRangeBonus(attackRangeLevel);
            AttributeModifier modifier = new AttributeModifier(
                ATTACK_RANGE_ID,
                rangeBonus,
                AttributeModifier.Operation.ADD_VALUE
            );
            attr.addTransientModifier(modifier);
        }
    }

    @Unique
    private void updateMovementSpeedModifier(Player player, int movementSpeedLevel) {
        AttributeInstance attr = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attr == null) return;

        // 移除旧修饰符
        attr.removeModifier(MOVEMENT_SPEED_ID);

        if (movementSpeedLevel != 0) {
            double speedMultiplier = CriticalHitsEnchantments.calculateMovementSpeedMultiplier(movementSpeedLevel);
            double finalMultiplier = speedMultiplier - 1.0; // 转换为增量

            if (Math.abs(finalMultiplier) > 0.001) {
                AttributeModifier modifier = new AttributeModifier(
                    MOVEMENT_SPEED_ID,
                    finalMultiplier,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
                );
                attr.addTransientModifier(modifier);
            }
        }
    }
}
