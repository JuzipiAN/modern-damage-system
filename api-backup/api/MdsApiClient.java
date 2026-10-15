package com.juzipi.criticalhits.api;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.Minecraft;

/**
 * MDS API 客户端入口类。
 * Modern Damage System API - 客户端初始化与客户端工具方法。
 * 
 * @version 1.0.0.1-fabric
 * @author Juzipi_AN
 */
public class MdsApiClient implements ClientModInitializer {

    private static boolean clientInitialized = false;

    @Override
    public void onInitializeClient() {
        clientInitialized = true;
        MdsApiMain.LOGGER.info("[{}] 客户端初始化完成", MdsApiMain.MOD_NAME);
    }

    /**
     * 检查客户端API是否已初始化。
     *
     * @return 客户端API是否已初始化
     */
    public static boolean isClientApiAvailable() {
        return clientInitialized;
    }

    /**
     * 检查当前是否在游戏中（有玩家和世界）。
     *
     * @return 是否在游戏中
     */
    public static boolean isInGame() {
        Minecraft mc = Minecraft.getInstance();
        return mc != null && mc.player != null && mc.level != null;
    }

    /**
     * 获取当前游戏语言代码（使用反射兼容26.2）。
     *
     * @return 语言代码（如 "zh_cn", "en_us"），如果不可用则返回 "en_us"
     */
    public static String getCurrentLanguage() {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null) return "en_us";
            // 使用反射获取语言管理器
            java.lang.reflect.Method getLanguageManagerMethod = mc.getClass().getMethod("getLanguageManager");
            Object languageManager = getLanguageManagerMethod.invoke(mc);
            if (languageManager == null) return "en_us";
            java.lang.reflect.Method getSelectedMethod = languageManager.getClass().getMethod("getSelected");
            Object selected = getSelectedMethod.invoke(languageManager);
            if (selected instanceof String) {
                return (String) selected;
            }
        } catch (Exception e) {
            // 忽略异常，返回默认值
        }
        return "en_us";
    }

    /**
     * 检查当前游戏语言是否为中文。
     *
     * @return 是否为中文
     */
    public static boolean isChineseLanguage() {
        String lang = getCurrentLanguage();
        return lang != null && lang.toLowerCase().startsWith("zh");
    }

    /**
     * 检查当前是否暂停游戏。
     *
     * @return 是否暂停
     */
    public static boolean isGamePaused() {
        Minecraft mc = Minecraft.getInstance();
        return mc != null && mc.isPaused();
    }
}
