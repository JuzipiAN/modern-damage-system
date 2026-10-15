package com.juzipi.criticalhits.mixin;

import net.minecraft.world.inventory.ResultSlot;
import org.spongepowered.asm.mixin.Mixin;

/**
 * ResultSlot 的材料消耗逻辑已迁移到 CraftingMenuMixin.slotClick 中。
 * 保留空 Mixin 以避免 mixins.json 中引用缺失。
 */
@Mixin(ResultSlot.class)
public abstract class ResultSlotMixin {
    //  intentionally empty — 取出和消耗逻辑在 CraftingMenuMixin.slotClick 中处理
}
