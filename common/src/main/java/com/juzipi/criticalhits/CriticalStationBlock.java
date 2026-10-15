package com.juzipi.criticalhits;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 暴击台方块。
 * 暴击师村民的工作站，支持4本同等级同类型暴击附魔书合成1本高一级书。
 */
public class CriticalStationBlock extends Block implements EntityBlock {

    public static final Component TITLE = Component.translatable("container.criticalhits.critical_station");

    public CriticalStationBlock() {
        super(Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.ENCHANTING_TABLE)
            .setId(ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(CriticalHitsEnchantments.MOD_ID, "critical_station"))));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CriticalStationBlockEntity(pos, state);
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                              Player player, BlockHitResult hit) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof MenuProvider menuProvider) {
                serverPlayer.openMenu(menuProvider);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}
