package com.juzipi.criticalhits.api;

/**
 * 现代化伤害系统 - API总入口。
 * 提供所有MDS API的统一访问入口，方便其他mod开发者使用。
 * 
 * <p>使用示例：
 * <pre>{@code
 * // 检查MDS API是否可用
 * if (MdsApi.isAvailable()) {
 *     // 获取API版本
 *     String version = MdsApi.getVersion();
 *     
 *     // 使用伤害API
 *     float damage = MdsApi.damage().calculateFinalDamage(player, target, 10.0f);
 *     
 *     // 使用附魔API
 *     int level = MdsApi.enchantment().getEnchantmentLevel(weapon, "critical_rate");
 *     
 *     // 使用GUI API
 *     MdsApi.gui().openScreen(new MyCustomScreen());
 *     
 *     // 使用配置API
 *     boolean enabled = MdsApi.config().getBoolean("hudEnabled", true);
 * }
 * }</pre>
 * 
 * @version 1.0.0.1-fabric
 * @author Juzipi_AN
 * @license MIT (开源免费)
 */
public final class MdsApi {

    private MdsApi() {
        // 私有构造函数，防止实例化
    }

    /**
     * 检查MDS API是否可用。
     *
     * @return API是否可用
     */
    public static boolean isAvailable() {
        return MdsApiMain.isApiAvailable();
    }

    /**
     * 获取API版本号。
     *
     * @return API版本号
     */
    public static String getVersion() {
        return MdsApiMain.getApiVersion();
    }

    /**
     * 获取完整版本号（包含平台信息）。
     *
     * @return 完整版本号
     */
    public static String getFullVersion() {
        return MdsApiMain.getFullVersion();
    }

    /**
     * 获取作者信息。
     *
     * @return 作者ID
     */
    public static String getAuthor() {
        return MdsApiMain.getAuthor();
    }

    /**
     * 获取开源协议。
     *
     * @return 协议名称
     */
    public static String getLicense() {
        return MdsApiMain.getLicense();
    }

    /**
     * 检查API版本是否兼容。
     *
     * @param requiredVersion 最低要求的API版本
     * @return 是否兼容
     */
    public static boolean isVersionCompatible(String requiredVersion) {
        return MdsApiMain.isVersionCompatible(requiredVersion);
    }

    /**
     * 记录API使用日志。
     *
     * @param modId 使用API的mod ID
     * @param feature 使用的功能描述
     */
    public static void logUsage(String modId, String feature) {
        MdsApiMain.logApiUsage(modId, feature);
    }

    /**
     * 获取伤害计算API。
     *
     * @return 伤害API实例
     */
    public static MdsDamageApi damage() {
        return new MdsDamageApi();
    }

    /**
     * 获取附魔API。
     *
     * @return 附魔API实例
     */
    public static MdsEnchantmentApi enchantment() {
        return new MdsEnchantmentApi();
    }

    /**
     * 获取GUI工具API。
     *
     * @return GUI API实例
     */
    public static MdsGuiApi gui() {
        return new MdsGuiApi();
    }

    /**
     * 获取配置API。
     *
     * @return 配置API实例
     */
    public static MdsConfigApi config() {
        return new MdsConfigApi();
    }

    /**
     * 检查客户端API是否可用。
     *
     * @return 客户端API是否可用
     */
    public static boolean isClientAvailable() {
        return MdsApiClient.isClientApiAvailable();
    }

    /**
     * 检查当前是否在游戏中。
     *
     * @return 是否在游戏中
     */
    public static boolean isInGame() {
        return MdsApiClient.isInGame();
    }

    /**
     * 获取当前游戏语言代码。
     *
     * @return 语言代码
     */
    public static String getCurrentLanguage() {
        return MdsApiClient.getCurrentLanguage();
    }

    /**
     * 检查当前游戏语言是否为中文。
     *
     * @return 是否为中文
     */
    public static boolean isChineseLanguage() {
        return MdsApiClient.isChineseLanguage();
    }
}
