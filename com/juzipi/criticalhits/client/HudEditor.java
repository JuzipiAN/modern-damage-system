package com.juzipi.criticalhits.client;

import com.juzipi.criticalhits.CriticalHitsConfig;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * HUD编辑器。
 * 支持按键切换HUD显示、进入编辑模式、鼠标拖动调整位置和大小。
 * 按键可在游戏控制设置中自定义修改。
 */
public class HudEditor {

    // 编辑模式状态
    private static boolean editMode = false;
    private static boolean dragging = false;
    private static int dragStartX = 0;
    private static int dragStartY = 0;
    private static int originalXOffset = 0;
    private static int originalYOffset = 0;

    // 可配置按键
    private static KeyMapping toggleHudKey;
    private static KeyMapping editHudKey;
    private static KeyMapping openConfigKey;
    private static final KeyMapping.Category CRITICALHITS_CATEGORY =
        new KeyMapping.Category(net.minecraft.resources.Identifier.fromNamespaceAndPath("criticalhits", "hud"));

    public static void register() {
        // 使用Fabric KeyMapping API注册可配置按键
        toggleHudKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.criticalhits.toggle_hud",
            GLFW.GLFW_KEY_H,
            CRITICALHITS_CATEGORY
        ));
        editHudKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.criticalhits.edit_hud",
            GLFW.GLFW_KEY_C,
            CRITICALHITS_CATEGORY
        ));
        openConfigKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.criticalhits.open_config",
            GLFW.GLFW_KEY_K,
            CRITICALHITS_CATEGORY
        ));

        com.juzipi.criticalhits.CriticalHitsMod.LOGGER.info("[Modern Damage System] 按键绑定已通过Fabric API注册");

        // 注册客户端tick事件，检查按键
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            checkKeys(client);
        });
    }

    private static void checkKeys(Minecraft client) {
        // 切换HUD显示按键
        while (toggleHudKey.consumeClick()) {
            CriticalHitsConfig.get().hud_enabled = !CriticalHitsConfig.get().hud_enabled;
            CriticalHitsConfig.save();
            if (client.player != null) {
                client.player.sendSystemMessage(
                    Component.literal("HUD已" + (CriticalHitsConfig.get().hud_enabled ? "开启" : "关闭"))
                );
            }
        }

        // 进入/退出编辑模式按键
        while (editHudKey.consumeClick()) {
            editMode = !editMode;
            dragging = false;
            if (client.player != null) {
                client.player.sendSystemMessage(
                    Component.literal("HUD编辑模式已" + (editMode ? "开启" : "关闭（已保存）"))
                );
            }
            if (editMode) {
                // 进入编辑模式：释放鼠标，方便拖动
                if (client.mouseHandler != null && client.mouseHandler.isMouseGrabbed()) {
                    client.mouseHandler.releaseMouse();
                }
            } else {
                // 退出编辑模式：保存配置，重新锁定鼠标
                CriticalHitsConfig.save();
                if (client.mouseHandler != null && !client.mouseHandler.isMouseGrabbed()) {
                    client.mouseHandler.grabMouse();
                }
            }
        }

        // 打开配置界面按键（按K键打开Cloth Config配置界面）
        while (openConfigKey.consumeClick()) {
            try {
                // 获取父界面
                net.minecraft.client.gui.screens.Screen parentScreen = null;
                try {
                    java.lang.reflect.Field screenField = client.getClass().getDeclaredField("screen");
                    screenField.setAccessible(true);
                    parentScreen = (net.minecraft.client.gui.screens.Screen) screenField.get(client);
                } catch (Exception e) {
                    com.juzipi.criticalhits.CriticalHitsMod.LOGGER.warn("[Modern Damage System] 获取父界面失败", e);
                }

                // 获取配置界面
                Object screenResult = me.shedaniel.autoconfig.AutoConfigClient.getConfigScreen(
                    com.juzipi.criticalhits.MdsClothConfig.class, parentScreen);

                net.minecraft.client.gui.screens.Screen configScreen = null;
                if (screenResult instanceof net.minecraft.client.gui.screens.Screen) {
                    configScreen = (net.minecraft.client.gui.screens.Screen) screenResult;
                } else if (screenResult instanceof java.util.function.Supplier) {
                    Object supplied = ((java.util.function.Supplier<?>) screenResult).get();
                    if (supplied instanceof net.minecraft.client.gui.screens.Screen) {
                        configScreen = (net.minecraft.client.gui.screens.Screen) supplied;
                    }
                }

                if (configScreen == null) {
                    throw new RuntimeException("无法创建配置界面，结果类型: " +
                        (screenResult != null ? screenResult.getClass().getName() : "null"));
                }

                // 调用setScreenAndShow方法（26.2中使用setScreenAndShow而不是setScreen）
                boolean setScreenCalled = false;
                try {
                    java.lang.reflect.Method setScreenMethod = client.getClass().getDeclaredMethod("setScreenAndShow",
                        net.minecraft.client.gui.screens.Screen.class);
                    setScreenMethod.setAccessible(true);
                    setScreenMethod.invoke(client, configScreen);
                    setScreenCalled = true;
                } catch (Exception e) {
                    com.juzipi.criticalhits.CriticalHitsMod.LOGGER.error("[Modern Damage System] setScreenAndShow 调用失败", e);
                }

                if (!setScreenCalled) {
                    throw new RuntimeException("setScreenAndShow调用失败");
                }

            } catch (Exception e) {
                com.juzipi.criticalhits.CriticalHitsMod.LOGGER.error("[Modern Damage System] 打开配置界面失败", e);
                if (client.player != null) {
                    client.player.sendSystemMessage(Component.literal("§c打开配置界面失败: " + e.getMessage()));
                    client.player.sendSystemMessage(Component.literal("§c请查看日志获取详细信息"));
                }
            }
        }
    }

    public static boolean isEditMode() {
        return editMode;
    }

    public static boolean isDragging() {
        return dragging;
    }

    /**
     * 开始拖动。
     */
    public static void startDrag(int mouseX, int mouseY) {
        if (!editMode) return;
        dragging = true;
        dragStartX = mouseX;
        dragStartY = mouseY;
        originalXOffset = CriticalHitsConfig.get().hud_x_offset;
        originalYOffset = CriticalHitsConfig.get().hud_y_offset;
    }

    /**
     * 更新拖动位置。
     */
    public static void updateDrag(int mouseX, int mouseY) {
        if (!editMode || !dragging) return;
        int deltaX = mouseX - dragStartX;
        int deltaY = mouseY - dragStartY;
        CriticalHitsConfig.get().hud_x_offset = originalXOffset + deltaX;
        CriticalHitsConfig.get().hud_y_offset = originalYOffset + deltaY;
    }

    /**
     * 结束拖动。
     */
    public static void endDrag() {
        dragging = false;
    }

    /**
     * 滚轮调整缩放。
     */
    public static void onScroll(double delta) {
        if (!editMode) return;
        float currentScale = CriticalHitsConfig.get().hud_scale;
        float newScale = currentScale + (float) (delta * 0.1f);
        newScale = Math.max(0.5f, Math.min(2.0f, newScale));
        CriticalHitsConfig.get().hud_scale = newScale;
    }
}
