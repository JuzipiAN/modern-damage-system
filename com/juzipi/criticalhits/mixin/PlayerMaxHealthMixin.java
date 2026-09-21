package com.juzipi.criticalhits.mixin;

import com.juzipi.criticalhits.CriticalHitsEnchantments;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 玩家最大生命值附魔Mixin。
 * 穿上有生命强化附魔的护甲时增加最大生命值，脱下时恢复。
 */
@Mixin(Player.class)
public class PlayerMaxHealthMixin {
    @Unique
    private static final Identifier MAX_HEALTH_MODIFIER_ID = Identifier.fromNamespaceAndPath("criticalhits", "max_health");

    @Unique
    private int lastMaxHealthLevel = -1;

    @Inject(method = "tick", at = @At("HEAD"))
    private void criticalhits_onTick(CallbackInfo ci) {
        Player player = (Player) (Object) this;
        int level = CriticalHitsEnchantments.getMaxHealthLevel(player);

        if (level == lastMaxHealthLevel) return;
        lastMaxHealthLevel = level;

        AttributeInstance maxHealthAttr = player.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealthAttr == null) return;

        // 移除旧的修饰符
        maxHealthAttr.removeModifier(MAX_HEALTH_MODIFIER_ID);

        if (level > 0) {
            float bonus = CriticalHitsEnchantments.calculateMaxHealth(level);
            AttributeModifier modifier = new AttributeModifier(
                MAX_HEALTH_MODIFIER_ID,
                bonus,
                AttributeModifier.Operation.ADD_VALUE
            );
            maxHealthAttr.addTransientModifier(modifier);

            // 血量上限校验：如果当前血量超过新的最大生命值，就限制
            if (player.getHealth() > player.getMaxHealth()) {
                player.setHealth(player.getMaxHealth());
            }
        }
    }
}
