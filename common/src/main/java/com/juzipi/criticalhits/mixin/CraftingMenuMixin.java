package com.juzipi.criticalhits.mixin;

import com.juzipi.criticalhits.CraftingRecipeState;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

/**
 * 工作台合成附魔书：结果槽注入。
 *
 * 2.0.5 修改：
 * 1. 附魔 Holder 改为通过 {@code ServerLevel.registryAccess()} 获取——
 *    旧代码调用 {@code Minecraft.getInstance()}（客户端类），在专用服务器上会
 *    NoClassDefFoundError，且在服务端逻辑里取客户端注册表本身就是错的。
 * 2. 注入结果后同步 remote slot 并发送数据包（与同版本原版 CraftingMenu 行为一致），
 *    避免客户端结果槽延迟/闪烁。
 * 3. 配方识别与结果构建移到 {@link CraftingRecipeState}，便于自测复用。
 *
 * 点击取出逻辑在 {@link AbstractContainerMenuMixin}（必须拦截，否则原版
 * ResultSlot.onTake 会复制材料）。
 */
@Mixin(CraftingMenu.class)
public abstract class CraftingMenuMixin {

    @Inject(method = "slotChangedCraftingGrid", at = @At("TAIL"))
    private static void criticalhits$slotChangedCraftingGrid(
            AbstractContainerMenu menu,
            ServerLevel level,
            Player player,
            CraftingContainer craftingContainer,
            ResultContainer resultContainer,
            RecipeHolder<?> recipeHolder,
            CallbackInfo ci) {

        Map<net.minecraft.world.item.Item, Integer> all = CraftingRecipeState.countGridItems(menu);
        int coinCount = all.getOrDefault(com.juzipi.criticalhits.CriticalHitsMod.CRITICAL_COIN, 0);
        int bookCount = all.getOrDefault(net.minecraft.world.item.Items.BOOK, 0);
        if (coinCount < 1 || bookCount < 1) return;

        Map<net.minecraft.world.item.Item, Integer> ingredients = CraftingRecipeState.countIngredients(menu);
        String enchantmentId = CraftingRecipeState.detectEnchantment(ingredients, craftingContainer);
        if (enchantmentId == null) return;
        if (CraftingRecipeState.getRecipeCost(enchantmentId) == null) return;

        ItemStack result = CraftingRecipeState.buildResult(enchantmentId, level.registryAccess());
        if (result.isEmpty()) return;

        resultContainer.setItem(0, result);
        menu.setRemoteSlot(0, result);
        if (player instanceof ServerPlayer serverPlayer && serverPlayer.connection != null) {
            serverPlayer.connection.send(new ClientboundContainerSetSlotPacket(
                menu.containerId, menu.incrementStateId(), 0, result));
        }
    }
}
