package com.juzipi.criticalhits.mixin.client;

import com.juzipi.criticalhits.CriticalHitsEnchantments;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 物品渲染Mixin。
 * 在暴击系列附魔书图标右下角绘制等级数字。
 */
@Mixin(GuiGraphicsExtractor.class)
public abstract class ItemRendererMixin {

    private static final Logger LOG = LoggerFactory.getLogger("CriticalHitsRenderer");

    @Inject(method = "item(Lnet/minecraft/world/item/ItemStack;II)V",
        at = @At("HEAD"))
    private void criticalhits$itemHead(ItemStack stack, int x, int y, CallbackInfo ci) {
        if (stack != null && stack.is(Items.ENCHANTED_BOOK)) {
            ItemEnchantments enchants = stack.get(DataComponents.STORED_ENCHANTMENTS);
            if (enchants != null && !enchants.isEmpty()) {
                LOG.info("[CH Render] item() HEAD called for enchanted book, x={}, y={}, enchants={}", x, y, enchants);
            }
        }
    }

    @Inject(method = "itemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;II)V",
        at = @At("HEAD"))
    private void criticalhits$decorationsHead(Font font, ItemStack stack, int x, int y, CallbackInfo ci) {
        if (stack != null && stack.is(Items.ENCHANTED_BOOK)) {
            ItemEnchantments enchants = stack.get(DataComponents.STORED_ENCHANTMENTS);
            if (enchants != null && !enchants.isEmpty()) {
                LOG.info("[CH Render] itemDecorations() HEAD called for enchanted book, x={}, y={}", x, y);
            }
        }
    }

    @Inject(method = "itemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;II)V",
        at = @At("TAIL"))
    private void criticalhits$renderEnchantmentLevel(Font font, ItemStack stack, int x, int y, CallbackInfo ci) {
        if (stack == null || stack.isEmpty() || !stack.is(Items.ENCHANTED_BOOK)) return;

        ItemEnchantments enchants = stack.get(DataComponents.STORED_ENCHANTMENTS);
        if (enchants == null || enchants.isEmpty()) return;

        int level = -1;
        boolean isCurse = false;
        for (var entry : enchants.entrySet()) {
            var ench = entry.getKey();
            if (ench.is(CriticalHitsEnchantments.CRITICAL_RATE_ID)
                || ench.is(CriticalHitsEnchantments.CRITICAL_DAMAGE_ID)
                || ench.is(CriticalHitsEnchantments.CRITICAL_RESISTANCE_ID)
                || ench.is(CriticalHitsEnchantments.PERCENTAGE_DAMAGE_ID)
                || ench.is(CriticalHitsEnchantments.ARMOR_PENETRATION_ID)
                || ench.is(CriticalHitsEnchantments.LIFE_STEAL_ID)
                || ench.is(CriticalHitsEnchantments.VULNERABILITY_ID)
                || ench.is(CriticalHitsEnchantments.DAMAGE_REDUCTION_ID)
                || ench.is(CriticalHitsEnchantments.ARMOR_DEFENSE_ID)
                || ench.is(CriticalHitsEnchantments.MAX_HEALTH_ID)
                || ench.is(CriticalHitsEnchantments.ATTACK_SPEED_ID)
                || ench.is(CriticalHitsEnchantments.MOVEMENT_SPEED_ID)
                || ench.is(CriticalHitsEnchantments.ATTACK_RANGE_ID)
                || ench.is(CriticalHitsEnchantments.BONUS_DAMAGE_ID)) {
                level = entry.getIntValue();
                break;
            }
            // 负等级附魔（诅咒）
            if (ench.is(CriticalHitsEnchantments.ATTACK_SPEED_CURSE_ID)
                || ench.is(CriticalHitsEnchantments.MOVEMENT_SPEED_CURSE_ID)
                || ench.is(CriticalHitsEnchantments.ATTACK_RANGE_CURSE_ID)) {
                level = entry.getIntValue();
                isCurse = true;
                break;
            }
        }

        if (level <= 0) return;

        GuiGraphicsExtractor self = (GuiGraphicsExtractor) (Object) this;
        String levelStr = isCurse ? "-" + level : String.valueOf(level);
        int width = font.width(levelStr);
        int textColor = isCurse ? 0xFF5555 : 0xFFFF00; // 负等级红色，正等级黄色
        // 阴影
        self.text(font, levelStr, x + 17 - width + 1, y + 9 + 1, 0x000000);
        // 主文字
        self.text(font, levelStr, x + 17 - width, y + 9, textColor);
    }
}
