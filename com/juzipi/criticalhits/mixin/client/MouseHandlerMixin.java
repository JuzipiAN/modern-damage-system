package com.juzipi.criticalhits.mixin.client;

import com.juzipi.criticalhits.client.HudEditor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 鼠标事件Mixin。
 * 监听鼠标点击和滚轮事件，用于HUD编辑模式。
 */
@Mixin(MouseHandler.class)
public class MouseHandlerMixin {

    @Inject(method = "onButton", at = @At("HEAD"), cancellable = true)
    private void onButton(long window, MouseButtonInfo button, int action, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;

        // 非编辑模式下：检测右键配方书，打开配方书GUI
        if (!HudEditor.isEditMode() && action == 1 && button.button() == 1 && mc.player != null) {
            try {
                if (com.juzipi.criticalhits.CriticalHitsMod.RECIPE_BOOK != null) {
                    net.minecraft.world.item.ItemStack mainHand = mc.player.getMainHandItem();
                    net.minecraft.world.item.ItemStack offHand = mc.player.getOffhandItem();
                    boolean isRecipeBook = (mainHand != null && !mainHand.isEmpty() && mainHand.is(com.juzipi.criticalhits.CriticalHitsMod.RECIPE_BOOK)) ||
                        (offHand != null && !offHand.isEmpty() && offHand.is(com.juzipi.criticalhits.CriticalHitsMod.RECIPE_BOOK));
                    if (isRecipeBook) {
                        com.juzipi.criticalhits.CriticalHitsMod.LOGGER.info("[MDS] 检测到右键配方书，正在打开GUI...");
                        // 取消右键事件，避免和其他mod冲突
                        ci.cancel();
                        com.juzipi.criticalhits.RecipeBookMenu dummyMenu = new com.juzipi.criticalhits.RecipeBookMenu(0, mc.player.getInventory());
                        com.juzipi.criticalhits.RecipeBookScreen screen = new com.juzipi.criticalhits.RecipeBookScreen(dummyMenu, mc.player.getInventory(),
                            net.minecraft.network.chat.Component.translatable("modern_damage_system.recipe.title"));
                        boolean result = com.juzipi.criticalhits.api.MdsGuiApi.openScreen(screen);
                        com.juzipi.criticalhits.CriticalHitsMod.LOGGER.info("[MDS] 打开配方书GUI结果: {}", result);
                        return;
                    }
                }
            } catch (Exception e) {
                com.juzipi.criticalhits.CriticalHitsMod.LOGGER.error("[MDS] 配方书右键处理异常", e);
            }
        }

        if (!HudEditor.isEditMode()) return;
        // 编辑模式下：取消鼠标点击事件传播，防止误触攻击/使用物品
        ci.cancel();
        if (mc.getWindow() == null) return;

        // 获取GUI缩放后的鼠标坐标
        double mouseX = MouseHandler.getScaledXPos(mc.getWindow(), mc.mouseHandler.xpos());
        double mouseY = MouseHandler.getScaledYPos(mc.getWindow(), mc.mouseHandler.ypos());

        // 左键按下：开始拖动（button()=0表示左键）
        if (action == 1 && button.button() == 0) {
            HudEditor.startDrag((int) mouseX, (int) mouseY);
        }
        // 左键释放：结束拖动
        else if (action == 0 && button.button() == 0) {
            HudEditor.endDrag();
        }
    }

    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void onScroll(long window, double xoffset, double yoffset, CallbackInfo ci) {
        if (!HudEditor.isEditMode()) return;
        // 编辑模式下：取消滚轮事件传播，防止物品栏切换
        ci.cancel();
        // 滚轮：调整HUD缩放
        HudEditor.onScroll(yoffset);
    }

    @Inject(method = "onMove", at = @At("TAIL"))
    private void onMove(long window, double xpos, double ypos, CallbackInfo ci) {
        if (!HudEditor.isEditMode() || !HudEditor.isDragging()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.getWindow() == null) return;

        // 获取GUI缩放后的鼠标坐标
        double mouseX = MouseHandler.getScaledXPos(mc.getWindow(), xpos);
        double mouseY = MouseHandler.getScaledYPos(mc.getWindow(), ypos);

        // 更新拖动位置
        HudEditor.updateDrag((int) mouseX, (int) mouseY);
    }
}
