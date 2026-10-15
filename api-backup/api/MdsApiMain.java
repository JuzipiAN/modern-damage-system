package com.juzipi.criticalhits.api;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MDS API 主入口类。
 * Modern Damage System API - 为Minecraft 26.2 Fabric提供通用API。
 * 
 * <p>本API为开源项目（MIT协议），欢迎其他mod开发者使用。
 * 提供伤害计算、附魔查询、GUI工具、配置管理等通用功能。
 * 
 * <p>使用示例：
 * <pre>{@code
 * // 检查API是否可用
 * if (MdsApiMain.isApiAvailable()) {
 *     // 获取API版本
 *     String version = MdsApiMain.getApiVersion();
 *     // 使用伤害API
 *     float damage = MdsDamageApi.calculateWeaponDamage(player, target);
 * }
 * }</pre>
 * 
 * @version 1.0.0.1-fabric
 * @author Juzipi_AN
 * @license MIT (开源免费)
 */
public class MdsApiMain implements ModInitializer {

    public static final String MOD_ID = "mds-api";
    public static final String MOD_NAME = "MDS API";
    public static final String VERSION = "1.0.0.1-fabric";
    public static final String API_VERSION = "1.0.0";
    public static final String AUTHOR = "Juzipi_AN";
    public static final String LICENSE = "MIT";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);

    private static boolean initialized = false;
    private static long initializationTime = 0;

    @Override
    public void onInitialize() {
        initialized = true;
        initializationTime = System.currentTimeMillis();
        LOGGER.info("[{}] v{} 初始化完成 - 作者: {}", MOD_NAME, VERSION, AUTHOR);
        LOGGER.info("[{}] 开源协议: {} - 欢迎其他mod使用本API", MOD_NAME, LICENSE);
        LOGGER.info("[{}] API版本: {} - 支持Minecraft 26.2 Fabric", MOD_NAME, API_VERSION);
    }

    /**
     * 检查API是否已初始化。
     *
     * @return API是否已初始化
     */
    public static boolean isApiAvailable() {
        return initialized;
    }

    /**
     * 获取API版本号。
     *
     * @return API版本号
     */
    public static String getApiVersion() {
        return API_VERSION;
    }

    /**
     * 获取完整版本号（包含平台信息）。
     *
     * @return 完整版本号
     */
    public static String getFullVersion() {
        return VERSION;
    }

    /**
     * 获取作者信息。
     *
     * @return 作者ID
     */
    public static String getAuthor() {
        return AUTHOR;
    }

    /**
     * 获取开源协议。
     *
     * @return 协议名称
     */
    public static String getLicense() {
        return LICENSE;
    }

    /**
     * 获取API初始化时间戳。
     *
     * @return 初始化时间戳（毫秒）
     */
    public static long getInitializationTime() {
        return initializationTime;
    }

    /**
     * 检查API版本是否兼容。
     * 用于其他mod检查最低API版本要求。
     *
     * @param requiredVersion 最低要求的API版本（如 "1.0.0"）
     * @return 是否兼容
     */
    public static boolean isVersionCompatible(String requiredVersion) {
        if (requiredVersion == null || requiredVersion.isEmpty()) {
            return true;
        }
        String[] required = requiredVersion.split("\\.");
        String[] current = API_VERSION.split("\\.");
        int length = Math.max(required.length, current.length);
        for (int i = 0; i < length; i++) {
            int req = i < required.length ? Integer.parseInt(required[i]) : 0;
            int cur = i < current.length ? Integer.parseInt(current[i]) : 0;
            if (cur > req) return true;
            if (cur < req) return false;
        }
        return true;
    }

    /**
     * 记录API使用日志。
     * 用于其他mod标记自己使用了MDS API。
     *
     * @param modId 使用API的mod ID
     * @param feature 使用的功能描述
     */
    public static void logApiUsage(String modId, String feature) {
        LOGGER.info("[{}] Mod '{}' 使用了MDS API功能: {}", MOD_NAME, modId, feature);
    }
}
