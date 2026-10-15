package com.juzipi.criticalhits.mixin.client;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

/**
 * 物品Tooltip Mixin。
 * 在最终返回tooltip之前，强制删除所有附魔描述行（包括附魔描述mod添加的）。
 * 附魔描述统一移至MDS配方书的"附魔书展示"分类中。
 */
@Mixin(ItemStack.class)
public abstract class ItemStackTooltipMixin {

    @Inject(method = "getTooltip", at = @At("RETURN"), cancellable = true)
    private void criticalhits$removeEnchantmentDescriptions(Object context, TooltipFlag flag, CallbackInfoReturnable<List<Component>> cir) {
        List<Component> original = cir.getReturnValue();
        if (original == null || original.isEmpty()) return;

        List<Component> filtered = new ArrayList<>();
        for (int i = 0; i < original.size(); i++) {
            Component line = original.get(i);
            if (line == null) {
                filtered.add(line);
                continue;
            }

            boolean shouldRemove = false;
            String text = line.getString();

            // 1. 删除以.desc/.desc2结尾的翻译键
            if (line.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents translatable) {
                String key = translatable.getKey();
                if (key != null && (key.endsWith(".desc") || key.endsWith(".desc2"))) {
                    shouldRemove = true;
                }
            }

            // 2. 删除灰色斜体的描述行（附魔描述mod添加的）
            // 描述行通常是§7§o或§8§o开头，且不是第一行（物品名称）
            if (!shouldRemove && i > 0 && text.contains("§o") && (text.startsWith("§7") || text.startsWith("§8"))) {
                shouldRemove = true;
            }

            if (!shouldRemove) {
                filtered.add(line);
            }
        }

        // 只有确实删除了行才设置返回值
        if (filtered.size() != original.size()) {
            cir.setReturnValue(filtered);
        }
    }
}
