package com.juzipi.criticalhits.client;

import com.juzipi.criticalhits.CriticalHitsConfig;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * HUD 编辑器（平台无关部分）。
 *
 * 2.0.5：从 fabric 模块移到 common，按键的创建与注册由平台层负责：
 * Fabric 用 KeyMappingHelper + ClientTickEvents，NeoForge 用
 * RegisterKeyMappingsEvent + ClientTickEvent，创建好 KeyMapping 后调用 {@link #init}。
 */
public class HudEditor {

    // 编辑模式状态
    private static boolean editMode = false;
    private static boolean dragging = false;
    private static int dragStartX = 0;
    private static int dragStartY = 0;
    private static int originalXOffset = 0;
    private static int originalYOffset = 0;

    // 可配置按键（由平台层创建并注册）
    private static KeyMapping toggleHudKey;
    private static KeyMapping editHudKey;
    private static KeyMapping openConfigKey;

    private static final KeyMapping.Category CRITICALHITS_CATEGORY =
        new KeyMapping.Category(Identifier.fromNamespaceAndPath("criticalhits", "hud"));

    private HudEditor() {
    }

    /** 按键分类（平台层创建 KeyMapping 时需要）。 */
    public static KeyMapping.Category category() {
        return CRITICALHITS_CATEGORY;
    }

    /** 由平台层在注册完按键后调用。 */
    public static void init(KeyMapping toggle, KeyMapping edit, KeyMapping openConfig) {
        toggleHudKey = toggle;
        editHudKey = edit;
        openConfigKey = openConfig;
        com.juzipi.criticalhits.CriticalHitsMod.LOGGER.info("[Modern Damage System] HUD 编辑器按键已注册");
    }

    /** 客户端 tick 调用（平台层注册 tick 事件）。 */
    public static void tick(Minecraft client) {
        if (client == null || toggleHudKey == null) return;
        checkKeys(client);
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
            openConfigScreen(client);
        }
    }

    private static void openConfigScreen(Minecraft client) {
        try {
            net.minecraft.client.gui.screens.Screen parentScreen = null;
            try {
                java.lang.reflect.Field screenField = client.getClass().getDeclaredField("screen");
                screenField.setAccessible(true);
                parentScreen = (net.minecraft.client.gui.screens.Screen) screenField.get(client);
            } catch (Exception e) {
                com.juzipi.criticalhits.CriticalHitsMod.LOGGER.warn("[Modern Damage System] 获取父界面失败", e);
            }

            // 2.1.1：改用自定义配置界面，带「全局权重」联动（置灰/倍率）
            net.minecraft.client.gui.screens.Screen configScreen =
                com.juzipi.criticalhits.MdsConfigGui.create(parentScreen);

            if (configScreen == null) {
                throw new RuntimeException("无法创建配置界面（自定义与默认均失败）");
            }

            com.juzipi.criticalhits.MdsClientScreens.showScreen(configScreen);
        } catch (Exception e) {
            com.juzipi.criticalhits.CriticalHitsMod.LOGGER.error("[Modern Damage System] 打开配置界面失败", e);
            if (client.player != null) {
                client.player.sendSystemMessage(Component.literal("§c打开配置界面失败: " + e.getMessage()));
                client.player.sendSystemMessage(Component.literal("§c请查看日志获取详细信息"));
            }
        }
    }

    public static boolean isEditMode() {
        return editMode;
    }

    public static boolean isDragging() {
        return dragging;
    }

    /** 切换 HUD 风格（森林 <-> 血战），并保存配置，可来回切换。 */
    public static void toggleStyle() {
        String cur = CriticalHitsConfig.get().hud_style;
        CriticalHitsConfig.get().hud_style = "blood".equalsIgnoreCase(cur) ? "forest" : "blood";
        CriticalHitsConfig.save();
        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.player != null) {
            mc.player.sendSystemMessage(Component.literal(
                "HUD风格已切换: " + ("blood".equalsIgnoreCase(CriticalHitsConfig.get().hud_style) ? "血战" : "森林")));
        }
    }

    /** 开始拖动。 */
    public static void startDrag(int mouseX, int mouseY) {
        if (!editMode) return;
        dragging = true;
        dragStartX = mouseX;
        dragStartY = mouseY;
        originalXOffset = CriticalHitsConfig.get().hud_x_offset;
        originalYOffset = CriticalHitsConfig.get().hud_y_offset;
    }

    /** 更新拖动位置。 */
    public static void updateDrag(int mouseX, int mouseY) {
        if (!editMode || !dragging) return;
        int deltaX = mouseX - dragStartX;
        int deltaY = mouseY - dragStartY;
        CriticalHitsConfig.get().hud_x_offset = originalXOffset + deltaX;
        CriticalHitsConfig.get().hud_y_offset = originalYOffset + deltaY;
    }

    /** 结束拖动。 */
    public static void endDrag() {
        dragging = false;
    }

    /** 滚轮调整缩放。 */
    public static void onScroll(double delta) {
        if (!editMode) return;
        float currentScale = CriticalHitsConfig.get().hud_scale;
        float newScale = currentScale + (float) (delta * 0.1f);
        newScale = Math.max(0.5f, Math.min(2.0f, newScale));
        CriticalHitsConfig.get().hud_scale = newScale;
    }
}
