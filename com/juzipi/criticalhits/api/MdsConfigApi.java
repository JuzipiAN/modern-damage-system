package com.juzipi.criticalhits.api;

import java.io.File;

/**
 * 现代化伤害系统 - 配置API。
 * 提供配置文件读取、配置项查询、配置重载等功能。
 * 
 * <p>使用示例：
 * <pre>{@code
 * // 检查配置是否已加载
 * if (MdsConfigApi.isConfigLoaded()) {
 *     // 获取配置值
 *     boolean mobAdaptation = MdsConfigApi.getBoolean("mobAdaptationEnabled", true);
 *     int mobEnchantChance = MdsConfigApi.getInt("mobEnchantChance", 50);
 * }
 * 
 * // 重载配置
 * MdsConfigApi.reloadConfig();
 * }</pre>
 * 
 * @version 1.0.0.1-fabric
 * @author Juzipi_AN
 */
public class MdsConfigApi {

    private static final String CONFIG_FILE_NAME = "modern-damage-system.json";
    private static boolean configLoaded = false;

    /**
     * 获取配置文件路径。
     *
     * @return 配置文件路径
     */
    public static String getConfigFilePath() {
        try {
            // Fabric的配置目录通常是 run/config 或 .minecraft/config
            String configDir = System.getProperty("user.dir") + File.separator + "config";
            return configDir + File.separator + CONFIG_FILE_NAME;
        } catch (Exception e) {
            return "config" + File.separator + CONFIG_FILE_NAME;
        }
    }

    /**
     * 检查配置文件是否存在。
     *
     * @return 配置文件是否存在
     */
    public static boolean doesConfigFileExist() {
        try {
            File configFile = new File(getConfigFilePath());
            return configFile.exists() && configFile.isFile();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 检查配置是否已加载。
     *
     * @return 配置是否已加载
     */
    public static boolean isConfigLoaded() {
        return configLoaded;
    }

    /**
     * 设置配置加载状态。
     * 由主mod在加载配置后调用。
     *
     * @param loaded 是否已加载
     */
    public static void setConfigLoaded(boolean loaded) {
        configLoaded = loaded;
    }

    /**
     * 重载配置文件。
     *
     * @return 是否成功重载
     */
    public static boolean reloadConfig() {
        try {
            // 调用主mod的配置重载方法
            Class<?> configClass = Class.forName("com.juzipi.criticalhits.CriticalHitsConfig");
            java.lang.reflect.Method method = configClass.getMethod("loadConfig");
            method.invoke(null);
            configLoaded = true;
            MdsApiMain.LOGGER.info("[MDS API] 配置文件已重载");
            return true;
        } catch (Exception e) {
            MdsApiMain.LOGGER.error("[MDS API] 重载配置失败", e);
            return false;
        }
    }

    /**
     * 获取布尔类型配置值。
     *
     * @param key 配置键
     * @param defaultValue 默认值
     * @return 配置值
     */
    public static boolean getBoolean(String key, boolean defaultValue) {
        try {
            Class<?> configClass = Class.forName("com.juzipi.criticalhits.CriticalHitsConfig");
            java.lang.reflect.Method method = configClass.getMethod("getBoolean", String.class, boolean.class);
            return (boolean) method.invoke(null, key, defaultValue);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * 获取整数类型配置值。
     *
     * @param key 配置键
     * @param defaultValue 默认值
     * @return 配置值
     */
    public static int getInt(String key, int defaultValue) {
        try {
            Class<?> configClass = Class.forName("com.juzipi.criticalhits.CriticalHitsConfig");
            java.lang.reflect.Method method = configClass.getMethod("getInt", String.class, int.class);
            return (int) method.invoke(null, key, defaultValue);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * 获取浮点数类型配置值。
     *
     * @param key 配置键
     * @param defaultValue 默认值
     * @return 配置值
     */
    public static float getFloat(String key, float defaultValue) {
        try {
            Class<?> configClass = Class.forName("com.juzipi.criticalhits.CriticalHitsConfig");
            java.lang.reflect.Method method = configClass.getMethod("getFloat", String.class, float.class);
            return (float) method.invoke(null, key, defaultValue);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * 获取双精度浮点数类型配置值。
     *
     * @param key 配置键
     * @param defaultValue 默认值
     * @return 配置值
     */
    public static double getDouble(String key, double defaultValue) {
        try {
            Class<?> configClass = Class.forName("com.juzipi.criticalhits.CriticalHitsConfig");
            java.lang.reflect.Method method = configClass.getMethod("getDouble", String.class, double.class);
            return (double) method.invoke(null, key, defaultValue);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * 获取字符串类型配置值。
     *
     * @param key 配置键
     * @param defaultValue 默认值
     * @return 配置值
     */
    public static String getString(String key, String defaultValue) {
        try {
            Class<?> configClass = Class.forName("com.juzipi.criticalhits.CriticalHitsConfig");
            java.lang.reflect.Method method = configClass.getMethod("getString", String.class, String.class);
            return (String) method.invoke(null, key, defaultValue);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * 检查配置键是否存在。
     *
     * @param key 配置键
     * @return 是否存在
     */
    public static boolean hasConfigKey(String key) {
        try {
            Class<?> configClass = Class.forName("com.juzipi.criticalhits.CriticalHitsConfig");
            java.lang.reflect.Method method = configClass.getMethod("hasKey", String.class);
            return (boolean) method.invoke(null, key);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取所有可配置的键列表。
     *
     * @return 配置键数组
     */
    public static String[] getConfigurableKeys() {
        return new String[] {
            // HUD相关
            "hudEnabled", "hudX", "hudY", "hudScale", "hudOpacity",
            // 战利品相关
            "lootEnabled", "lootMaxBooksPerChest", "lootMaxShardsPerChest",
            "criticalRateLootEnabled", "armorPenetrationLootEnabled",
            // 怪物适配相关
            "mobAdaptationEnabled", "mobEnchantChance", "hardModeBonusHealth",
            // 功能开关
            "shieldParticlesEnabled", "recipeBookEnabled"
        };
    }

    /**
     * 重置配置为默认值。
     *
     * @return 是否成功重置
     */
    public static boolean resetConfigToDefaults() {
        try {
            Class<?> configClass = Class.forName("com.juzipi.criticalhits.CriticalHitsConfig");
            java.lang.reflect.Method method = configClass.getMethod("resetToDefaults");
            method.invoke(null);
            MdsApiMain.LOGGER.info("[MDS API] 配置已重置为默认值");
            return true;
        } catch (Exception e) {
            MdsApiMain.LOGGER.error("[MDS API] 重置配置失败", e);
            return false;
        }
    }
}
