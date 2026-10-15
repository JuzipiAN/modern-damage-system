package com.juzipi.criticalhits.mixin;

import com.juzipi.criticalhits.CraftingRecipeState;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

/**
 * 工作台合成附魔书：点击取出拦截（材料复制 bug 的根治点）。
 *
 * 为什么要注入到 {@link AbstractContainerMenu}：
 * 26.2 的 {@code CraftingMenu} 已经不覆写 {@code clicked}，该方法声明在
 * {@code AbstractContainerMenu} 上。直接写 {@code @Mixin(CraftingMenu.class)}
 * 去注入一个"继承来的方法"是不可靠的（旧版本就因为找不到 slotClick 直接崩启动）。
 * 注入到声明类 + instanceof 守卫是 100% 可靠的做法。
 *
 * 为什么必须 cancel：
 * 结果槽里的附魔书不是原版配方产物。原版 {@code ResultSlot.onTake} 会用
 * {@code getRemainingItems(input, level)} 取剩余物，在没有匹配原版配方时会回退成
 * {@code copyAllInputItems(input)}（整格复制），然后
 * {@code replacement.grow(剩余数量)} 放回格子 —— 实测 6 钻石会变成 11 钻石。
 * 所以只要让原版逻辑碰到这本附魔书，材料就会翻倍：
 *   右键取、Q 丢弃、数字键交换、拖拽 —— 旧代码这几条路径全部漏掉，全部会复制。
 * 现在：只要点击的是我们的附魔书结果槽，一律先 cancel，再由我们自己发物品 + 精确扣材料。
 */
@Mixin(AbstractContainerMenu.class)
public abstract class AbstractContainerMenuMixin {

    @Inject(method = "clicked", at = @At("HEAD"), cancellable = true)
    private void criticalhits$onClicked(int slotIndex, int buttonNum, ContainerInput input, Player player, CallbackInfo ci) {
        if (!((Object) this instanceof CraftingMenu menu)) return;
        if (slotIndex != CraftingRecipeState.RESULT_SLOT) return;

        ItemStack result = menu.getSlot(CraftingRecipeState.RESULT_SLOT).getItem();
        if (result.isEmpty() || !result.is(Items.ENCHANTED_BOOK)) return;

        String enchantmentId = CraftingRecipeState.getMdsEnchantmentId(result);
        if (enchantmentId == null) return;
        Map<Item, Integer> cost = CraftingRecipeState.getRecipeCost(enchantmentId);
        if (cost == null) return;

        // 从这里开始，原版点击逻辑一律不执行
        ci.cancel();

        // 防数据不同步：格子里的材料不足以支付时什么都不做（避免"白给一本书"）
        Map<Item, Integer> gridCounts = CraftingRecipeState.countGridItems(menu);
        if (!CraftingRecipeState.canConsume(gridCounts, cost)) return;

        // 创造模式中键克隆：给书但不扣材料（与原版创造模式行为一致）
        if (input == ContainerInput.CLONE) {
            if (player.hasInfiniteMaterials()) {
                menu.setCarried(result.copy());
            }
            return;
        }

        // 投放到目标位置（光标/背包/快捷栏/掉落物）
        if (!criticalhits$deliver(menu, player, result, input, buttonNum)) {
            return; // 目标放不下 —— 什么都不做（点击已被取消，结果槽保持原样）
        }

        CraftingRecipeState.consume(menu, cost);
        // 明确清空结果槽：消耗过程可能已触发重算，这里先清空保证"取走即消失"，
        // 再按剩余材料重算（例如 6 钻石扣 2 剩 4，仍满足配方 → 结果槽重新出现附魔书）
        menu.getSlot(CraftingRecipeState.RESULT_SLOT).set(ItemStack.EMPTY);
        CraftingRecipeState.refreshResult(menu);
    }

    /** 把结果物品交给玩家。返回 false 表示目标放不下，此时不消耗材料。 */
    private static boolean criticalhits$deliver(AbstractContainerMenu menu, Player player, ItemStack result,
                                                ContainerInput input, int buttonNum) {
        switch (input) {
            case PICKUP, PICKUP_ALL:
                return criticalhits$toCarried(menu, result);
            case QUICK_MOVE: {
                ItemStack copy = result.copy();
                return player.getInventory().add(copy);
            }
            case SWAP: {
                if (buttonNum < 0 || (buttonNum > 8 && buttonNum != 40)) return false;
                Inventory inventory = player.getInventory();
                ItemStack target = inventory.getItem(buttonNum);
                if (target.isEmpty()) {
                    inventory.setItem(buttonNum, result.copy());
                    return true;
                }
                if (ItemStack.isSameItemSameComponents(target, result)
                        && target.getCount() + result.getCount() <= target.getMaxStackSize()) {
                    target.grow(result.getCount());
                    return true;
                }
                return false;
            }
            case THROW:
                player.drop(result.copy(), true);
                return true;
            default:
                // QUICK_CRAFT（拖拽）等其余类型：结果槽不可放置，直接吞掉点击
                return false;
        }
    }

    private static boolean criticalhits$toCarried(AbstractContainerMenu menu, ItemStack result) {
        ItemStack carried = menu.getCarried();
        if (carried.isEmpty()) {
            menu.setCarried(result.copy());
            return true;
        }
        if (ItemStack.isSameItemSameComponents(carried, result)
                && carried.getCount() + result.getCount() <= carried.getMaxStackSize()) {
            carried.grow(result.getCount());
            return true;
        }
        return false;
    }
}
