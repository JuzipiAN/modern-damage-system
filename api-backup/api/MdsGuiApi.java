package com.juzipi.criticalhits.api;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.ItemStack;

/**
 * 现代化伤害系统 - GUI API工具类。
 * 专门解决26.2中GUI API变化带来的兼容性问题。
 * 提供打开自定义Screen、渲染物品图标等通用功能。
 * 
 * @version 1.0.0.1-fabric
 * @author Juzipi_AN
 */
public class MdsGuiApi {

    /**
     * 打开自定义Screen。
     * 26.2中使用setScreenAndShow方法，自动兼容新旧版本。
     *
     * @param screen 要打开的Screen
     * @return 是否成功打开
     */
    public static boolean openScreen(Screen screen) {
        try {
            Minecraft client = Minecraft.getInstance();
            java.lang.reflect.Method setScreenMethod = client.getClass().getDeclaredMethod(
                "setScreenAndShow", Screen.class);
            setScreenMethod.setAccessible(true);
            setScreenMethod.invoke(client, screen);
            return true;
        } catch (Exception e) {
            try {
                Minecraft client = Minecraft.getInstance();
                java.lang.reflect.Method setScreenMethod = client.getClass().getDeclaredMethod(
                    "setScreen", Screen.class);
                setScreenMethod.setAccessible(true);
                setScreenMethod.invoke(client, screen);
                return true;
            } catch (Exception e2) {
                MdsApiMain.LOGGER.error("[MDS API] 打开Screen失败", e2);
                return false;
            }
        }
    }

    /**
     * 获取当前打开的Screen。
     *
     * @return 当前Screen，如果没有则返回null
     */
    public static Screen getCurrentScreen() {
        try {
            Minecraft client = Minecraft.getInstance();
            java.lang.reflect.Field screenField = client.getClass().getDeclaredField("screen");
            screenField.setAccessible(true);
            return (Screen) screenField.get(client);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 关闭当前Screen，返回游戏。
     *
     * @return 是否成功关闭
     */
    public static boolean closeScreen() {
        try {
            Minecraft client = Minecraft.getInstance();
            java.lang.reflect.Method setScreenMethod = client.getClass().getDeclaredMethod(
                "setScreen", Screen.class);
            setScreenMethod.setAccessible(true);
            setScreenMethod.invoke(client, (Object) null);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 渲染物品图标。
     * 兼容26.2的GuiGraphicsExtractor API。
     *
     * @param extractor GuiGraphicsExtractor实例
     * @param itemStack 要渲染的物品
     * @param x X坐标
     * @param y Y坐标
     */
    public static void renderItem(GuiGraphicsExtractor extractor, ItemStack itemStack, int x, int y) {
        if (extractor == null || itemStack == null || itemStack.isEmpty()) return;
        try {
            extractor.item(itemStack, x, y);
        } catch (Exception e) {
            MdsApiMain.LOGGER.warn("[MDS API] 渲染物品图标失败", e);
        }
    }

    /**
     * 检查鼠标是否在指定区域内。
     *
     * @param mouseX 鼠标X坐标
     * @param mouseY 鼠标Y坐标
     * @param x 区域X坐标
     * @param y 区域Y坐标
     * @param width 区域宽度
     * @param height 区域高度
     * @return 是否在区域内
     */
    public static boolean isMouseInArea(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }
}
