package com.juzipi.criticalhits;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 暴击台方块实体。
 */
public class CriticalStationBlockEntity extends BlockEntity implements MenuProvider {

    public CriticalStationBlockEntity(BlockPos pos, BlockState state) {
        super(CriticalHitsMod.CRITICAL_STATION_BLOCK_ENTITY, pos, state);
    }

    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        return new CriticalStationMenu(syncId, playerInventory, this);
    }

    @Override
    public Component getDisplayName() {
        return CriticalStationBlock.TITLE;
    }
}
