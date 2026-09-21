package com.juzipi.criticalhits;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * 配置系统。
 * 配置文件位置：config/criticalhits.json
 */
public class CriticalHitsConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    // 新配置文件名
    private static final File CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve("modern-damage-system.json").toFile();
    // 旧配置文件名（用于迁移）
    private static final File OLD_CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve("criticalhits.json").toFile();

    /**
     * 单个附魔的战利品配置。
     */
    public static class LootEnchantmentConfig {
        public boolean enabled = true;
        public int[] weights = {40, 30, 20, 10, 3};
        public int max_level = 5;

        public LootEnchantmentConfig() {}

        public LootEnchantmentConfig(boolean enabled, int[] weights) {
            this.enabled = enabled;
            this.weights = weights;
        }

        public LootEnchantmentConfig(boolean enabled, int[] weights, int max_level) {
            this.enabled = enabled;
            this.weights = weights;
            this.max_level = max_level;
        }
    }

    // 配置数据
    public static class ConfigData {
        // 每个附魔的战利品配置（key=附魔ID）
        public Map<String, LootEnchantmentConfig> loot_enchantments = new HashMap<>();
        // 战利品附魔书最高等级
        public int loot_enchantment_max_level = 5;
        // 每箱最多附魔书数量
        public int loot_enchantment_max_count = 2;
        // CC币碎片权重
        public int coin_shard_weight = 50;
        // 每箱最多CC币碎片数量
        public int coin_shard_max_count = 2;
        // 自动注入所有箱子战利品（开启后所有mod的箱子都有概率出现MDS战利品）
        public boolean auto_inject_all_chests = true;
        // 战利品注入黑名单（这些loot table不会被注入）
        public java.util.List<String> loot_table_blacklist = new java.util.ArrayList<>();
        // 额外战利品注入白名单（自动检测漏掉的loot table可以手动添加）
        public java.util.List<String> additional_loot_tables = new java.util.ArrayList<>();
        // 敌对生物添加附魔概率
        public float mob_enchant_chance = 0.5f;
        // 敌对生物困难难度额外生命值
        public float mob_hard_bonus_health = 20f;
        // HUD是否显示
        public boolean hud_enabled = true;
        // HUD显示位置："right"或"left"
        public String hud_side = "right";
        // HUD X轴偏移（正数向右，负数向左）
        public int hud_x_offset = 0;
        // HUD Y轴偏移（正数向下，负数向上）
        public int hud_y_offset = 0;
        // HUD背景透明度（0-255，0完全透明，255完全不透明）
        public int hud_background_alpha = 144;
        // HUD缩放（0.5-2.0，默认1.0）
        public float hud_scale = 1.0f;
        // 调试模式（开启详细日志输出）
        public boolean debug_mode = false;
        // 暴击粒子效果开关
        public boolean critical_particles_enabled = true;
        // 敌对生物适配开关
        public boolean mob_adaptation_enabled = true;
        // 铁砧合并经验减半开关
        public boolean anvil_cost_halved = true;
        // HUD可见属性配置（key: 属性名, value: 是否显示）
        public java.util.Map<String, Boolean> hud_visible_attributes = new java.util.HashMap<>();
    }

    private static ConfigData config = new ConfigData();

    static {
        // 初始化默认战利品配置
        // T0: 伤害加成、伤害减免
        int[] t0Weights = {15, 10, 6, 3, 1};
        // T1: 暴击伤害、护甲防御、生命强化
        int[] t1Weights = {25, 18, 12, 6, 2};
        // T1.5: 生命吸取
        int[] lifeStealWeights = {30, 22, 15, 8, 2};
        // T2: 暴击减免、易伤
        int[] t2Weights = {40, 30, 20, 10, 3};
        // 禁用（只能合成）: 暴击率、护甲穿透，权重设为T0级别但默认禁用
        int[] disabledWeights = {10, 6, 4, 2, 1};

        config.loot_enchantments.put("criticalhits:critical_rate", new LootEnchantmentConfig(false, disabledWeights));
        config.loot_enchantments.put("criticalhits:critical_damage", new LootEnchantmentConfig(true, t1Weights));
        config.loot_enchantments.put("criticalhits:critical_resistance", new LootEnchantmentConfig(true, t2Weights));
        config.loot_enchantments.put("criticalhits:percentage_damage", new LootEnchantmentConfig(true, t0Weights));
        config.loot_enchantments.put("criticalhits:armor_penetration", new LootEnchantmentConfig(false, disabledWeights));
        config.loot_enchantments.put("criticalhits:life_steal", new LootEnchantmentConfig(true, lifeStealWeights));
        config.loot_enchantments.put("criticalhits:vulnerability", new LootEnchantmentConfig(true, t2Weights));
        config.loot_enchantments.put("criticalhits:damage_reduction", new LootEnchantmentConfig(true, t0Weights));
        config.loot_enchantments.put("criticalhits:armor_defense", new LootEnchantmentConfig(true, t1Weights));
        config.loot_enchantments.put("criticalhits:max_health", new LootEnchantmentConfig(true, t1Weights));
        // ===== 属性系列 =====
        config.loot_enchantments.put("criticalhits:bonus_damage", new LootEnchantmentConfig(true, t0Weights));
        config.loot_enchantments.put("criticalhits:attack_range", new LootEnchantmentConfig(true, t0Weights));
        config.loot_enchantments.put("criticalhits:attack_speed", new LootEnchantmentConfig(true, t1Weights));
        config.loot_enchantments.put("criticalhits:movement_speed", new LootEnchantmentConfig(true, t1Weights));
        // ===== 属性系列（负等级/诅咒，低概率） =====
        int[] curseWeights = {10, 6, 3, 1, 1};
        config.loot_enchantments.put("criticalhits:attack_speed_curse", new LootEnchantmentConfig(true, curseWeights));
        config.loot_enchantments.put("criticalhits:movement_speed_curse", new LootEnchantmentConfig(true, curseWeights));
        config.loot_enchantments.put("criticalhits:attack_range_curse", new LootEnchantmentConfig(true, curseWeights));
        // ===== v2.0.0 新附魔 =====
        // 攻击系列
        config.loot_enchantments.put("criticalhits:true_damage", new LootEnchantmentConfig(true, t0Weights));
        config.loot_enchantments.put("criticalhits:splash_damage", new LootEnchantmentConfig(true, t1Weights));
        config.loot_enchantments.put("criticalhits:execute", new LootEnchantmentConfig(true, t0Weights));
        config.loot_enchantments.put("criticalhits:frost", new LootEnchantmentConfig(true, t1Weights));
        config.loot_enchantments.put("criticalhits:poison", new LootEnchantmentConfig(true, t1Weights));
        config.loot_enchantments.put("criticalhits:armor_shatter", new LootEnchantmentConfig(true, t1Weights));
        // 防御系列
        config.loot_enchantments.put("criticalhits:thorns_reflect", new LootEnchantmentConfig(true, t1Weights));
        config.loot_enchantments.put("criticalhits:shield", new LootEnchantmentConfig(true, t0Weights));
        config.loot_enchantments.put("criticalhits:knockback_resistance", new LootEnchantmentConfig(true, t2Weights));
    }

    /**
     * 加载配置文件。如果不存在则创建默认配置。
     * 支持从旧配置文件（criticalhits.json）自动迁移到新配置文件（modern-damage-system.json）。
     */
    public static void load() {
        try {
            // 配置迁移：如果旧配置存在但新配置不存在，自动迁移
            if (OLD_CONFIG_FILE.exists() && !CONFIG_FILE.exists()) {
                java.nio.file.Files.copy(OLD_CONFIG_FILE.toPath(), CONFIG_FILE.toPath());
                CriticalHitsMod.LOGGER.info("[Modern Damage System] 检测到旧配置文件，已自动迁移到 modern-damage-system.json");
            }

            if (CONFIG_FILE.exists()) {
                FileReader reader = new FileReader(CONFIG_FILE);
                ConfigData loaded = GSON.fromJson(reader, ConfigData.class);
                reader.close();
                if (loaded != null) {
                    // 合并：确保所有附魔都有配置（新增的附魔用默认值）
                    for (Map.Entry<String, LootEnchantmentConfig> entry : config.loot_enchantments.entrySet()) {
                        if (!loaded.loot_enchantments.containsKey(entry.getKey())) {
                            loaded.loot_enchantments.put(entry.getKey(), entry.getValue());
                        }
                    }
                    config = loaded;
                }
                CriticalHitsMod.LOGGER.info("[Modern Damage System] 配置文件加载成功");
            } else {
                save();
                CriticalHitsMod.LOGGER.info("[Modern Damage System] 配置文件不存在，已创建默认配置");
            }
        } catch (Exception e) {
            CriticalHitsMod.LOGGER.error("[Modern Damage System] 配置文件加载失败，使用默认配置", e);
        }
    }

    /**
     * 保存配置文件。
     */
    public static void save() {
        try {
            if (!CONFIG_FILE.getParentFile().exists()) {
                CONFIG_FILE.getParentFile().mkdirs();
            }
            FileWriter writer = new FileWriter(CONFIG_FILE);
            GSON.toJson(config, writer);
            writer.close();
        } catch (Exception e) {
            CriticalHitsMod.LOGGER.error("[Modern Damage System] 配置文件保存失败", e);
        }
    }

    public static ConfigData get() {
        return config;
    }

    /**
     * 获取指定附魔的战利品配置。
     */
    public static LootEnchantmentConfig getLootConfig(String enchantmentId) {
        return config.loot_enchantments.get(enchantmentId);
    }
}
