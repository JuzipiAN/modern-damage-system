package com.juzipi.criticalhits;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/**
 * 客户端界面显示工具（双端通用）。
 *
 * 26.2 中原版的 {@code Minecraft.setScreen(Screen)} 已改名为
 * {@code setScreenAndShow(Screen)}（public 方法），这里统一封装，
 * 避免各处再写反射。
 */
public final class MdsClientScreens {

    private MdsClientScreens() {
    }

    public static void showScreen(Screen screen) {
        Minecraft.getInstance().setScreenAndShow(screen);
    }

    /** 关闭当前界面。 */
    public static void closeScreen() {
        Minecraft.getInstance().setScreenAndShow(null);
    }
}
