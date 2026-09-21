package com.juzipi.criticalhits;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

/**
 * 配方书菜单。
 * 没有任何物品槽位，只是为了让Screen能继承AbstractContainerScreen，
 * 从而自动处理鼠标点击和滚轮滚动事件。
 */
public class RecipeBookMenu extends AbstractContainerMenu {

    public RecipeBookMenu(int syncId, Inventory playerInventory) {
        super(CriticalHitsMod.RECIPE_BOOK_MENU, syncId);
        // 没有任何槽位
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}
