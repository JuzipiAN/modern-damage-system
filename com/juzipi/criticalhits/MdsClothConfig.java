package com.juzipi.criticalhits;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

import java.util.HashMap;

/**
 * 现代化伤害系统Cloth Config配置类。
 * 由Configured自动生成配置界面。
 */
@Config(name = "modern-damage-system")
public class MdsClothConfig implements ConfigData {

    // ==================== HUD配置 ====================
    @ConfigEntry.Category("hud")
    public boolean hud_enabled = true;

    @ConfigEntry.Category("hud")
    public String hud_side = "right";

    @ConfigEntry.Category("hud")
    public int hud_x_offset = 0;

    @ConfigEntry.Category("hud")
    public int hud_y_offset = 0;

    @ConfigEntry.Category("hud")
    public int hud_background_alpha = 144;

    @ConfigEntry.Category("hud")
    public float hud_scale = 1.0f;

    // HUD可见属性配置
    @ConfigEntry.Category("hud_visible")

    public boolean show_critical_rate = true;

    @ConfigEntry.Category("hud_visible")

    public boolean show_critical_damage = true;

    @ConfigEntry.Category("hud_visible")

    public boolean show_damage_bonus = true;

    @ConfigEntry.Category("hud_visible")

    public boolean show_armor_penetration = true;

    @ConfigEntry.Category("hud_visible")

    public boolean show_life_steal = true;

    @ConfigEntry.Category("hud_visible")

    public boolean show_vulnerability = true;

    @ConfigEntry.Category("hud_visible")

    public boolean show_attack_speed = true;

    @ConfigEntry.Category("hud_visible")

    public boolean show_attack_range = true;

    @ConfigEntry.Category("hud_visible")

    public boolean show_bonus_damage = true;

    @ConfigEntry.Category("hud_visible")

    public boolean show_damage_reduction = true;

    @ConfigEntry.Category("hud_visible")

    public boolean show_armor_defense = true;

    @ConfigEntry.Category("hud_visible")

    public boolean show_max_health = true;

    @ConfigEntry.Category("hud_visible")

    public boolean show_critical_resistance = true;

    @ConfigEntry.Category("hud_visible")

    public boolean show_movement_speed = true;

    @ConfigEntry.Category("hud_visible")

    public boolean show_true_damage = true;

    @ConfigEntry.Category("hud_visible")

    public boolean show_splash_damage = true;

    @ConfigEntry.Category("hud_visible")

    public boolean show_execute = true;

    @ConfigEntry.Category("hud_visible")

    public boolean show_frost = true;

    @ConfigEntry.Category("hud_visible")

    public boolean show_poison = true;

    @ConfigEntry.Category("hud_visible")

    public boolean show_armor_shatter = true;

    @ConfigEntry.Category("hud_visible")

    public boolean show_thorns_reflect = true;

    @ConfigEntry.Category("hud_visible")

    public boolean show_shield = true;

    @ConfigEntry.Category("hud_visible")

    public boolean show_knockback_resistance = true;

    // ==================== 战利品配置 ====================
    @ConfigEntry.Category("loot")

    @ConfigEntry.BoundedDiscrete(min = 1, max = 5)
    public int loot_enchantment_max_level = 5;

    @ConfigEntry.Category("loot")

    @ConfigEntry.BoundedDiscrete(min = 1, max = 5)
    public int loot_enchantment_max_count = 2;

    @ConfigEntry.Category("loot")

    public int coin_shard_weight = 50;

    @ConfigEntry.Category("loot")

    @ConfigEntry.BoundedDiscrete(min = 1, max = 5)
    public int coin_shard_max_count = 2;

    // 战利品附魔开关
    @ConfigEntry.Category("loot_enchantments")

    public boolean loot_critical_rate = false;

    @ConfigEntry.Category("loot_enchantments")

    public boolean loot_critical_damage = true;

    @ConfigEntry.Category("loot_enchantments")

    public boolean loot_critical_resistance = true;

    @ConfigEntry.Category("loot_enchantments")

    public boolean loot_percentage_damage = true;

    @ConfigEntry.Category("loot_enchantments")

    public boolean loot_armor_penetration = false;

    @ConfigEntry.Category("loot_enchantments")

    public boolean loot_life_steal = true;

    @ConfigEntry.Category("loot_enchantments")

    public boolean loot_vulnerability = true;

    @ConfigEntry.Category("loot_enchantments")

    public boolean loot_damage_reduction = true;

    @ConfigEntry.Category("loot_enchantments")

    public boolean loot_armor_defense = true;

    @ConfigEntry.Category("loot_enchantments")

    public boolean loot_max_health = true;

    @ConfigEntry.Category("loot_enchantments")

    public boolean loot_bonus_damage = true;

    @ConfigEntry.Category("loot_enchantments")

    public boolean loot_attack_range = true;

    @ConfigEntry.Category("loot_enchantments")

    public boolean loot_attack_speed = true;

    @ConfigEntry.Category("loot_enchantments")

    public boolean loot_movement_speed = true;

    @ConfigEntry.Category("loot_enchantments")

    public boolean loot_true_damage = true;

    @ConfigEntry.Category("loot_enchantments")

    public boolean loot_splash_damage = true;

    @ConfigEntry.Category("loot_enchantments")

    public boolean loot_execute = true;

    @ConfigEntry.Category("loot_enchantments")

    public boolean loot_frost = true;

    @ConfigEntry.Category("loot_enchantments")

    public boolean loot_poison = true;

    @ConfigEntry.Category("loot_enchantments")

    public boolean loot_armor_shatter = true;

    @ConfigEntry.Category("loot_enchantments")

    public boolean loot_thorns_reflect = true;

    @ConfigEntry.Category("loot_enchantments")

    public boolean loot_shield = true;

    @ConfigEntry.Category("loot_enchantments")

    public boolean loot_knockback_resistance = true;

    // ==================== 战利品最高等级（每个附魔单独配置） ====================
    @ConfigEntry.Category("loot_max_level")

    @ConfigEntry.BoundedDiscrete(min = 1, max = 20)
    public int loot_max_level_critical_rate = 5;

    @ConfigEntry.Category("loot_max_level")

    @ConfigEntry.BoundedDiscrete(min = 1, max = 20)
    public int loot_max_level_critical_damage = 5;

    @ConfigEntry.Category("loot_max_level")

    @ConfigEntry.BoundedDiscrete(min = 1, max = 20)
    public int loot_max_level_critical_resistance = 5;

    @ConfigEntry.Category("loot_max_level")

    @ConfigEntry.BoundedDiscrete(min = 1, max = 20)
    public int loot_max_level_percentage_damage = 5;

    @ConfigEntry.Category("loot_max_level")

    @ConfigEntry.BoundedDiscrete(min = 1, max = 20)
    public int loot_max_level_armor_penetration = 5;

    @ConfigEntry.Category("loot_max_level")

    @ConfigEntry.BoundedDiscrete(min = 1, max = 20)
    public int loot_max_level_life_steal = 5;

    @ConfigEntry.Category("loot_max_level")

    @ConfigEntry.BoundedDiscrete(min = 1, max = 20)
    public int loot_max_level_vulnerability = 5;

    @ConfigEntry.Category("loot_max_level")

    @ConfigEntry.BoundedDiscrete(min = 1, max = 20)
    public int loot_max_level_damage_reduction = 5;

    @ConfigEntry.Category("loot_max_level")

    @ConfigEntry.BoundedDiscrete(min = 1, max = 20)
    public int loot_max_level_armor_defense = 5;

    @ConfigEntry.Category("loot_max_level")

    @ConfigEntry.BoundedDiscrete(min = 1, max = 20)
    public int loot_max_level_max_health = 5;

    @ConfigEntry.Category("loot_max_level")

    @ConfigEntry.BoundedDiscrete(min = 1, max = 20)
    public int loot_max_level_bonus_damage = 5;

    @ConfigEntry.Category("loot_max_level")

    @ConfigEntry.BoundedDiscrete(min = 1, max = 20)
    public int loot_max_level_attack_range = 5;

    @ConfigEntry.Category("loot_max_level")

    @ConfigEntry.BoundedDiscrete(min = 1, max = 20)
    public int loot_max_level_attack_speed = 5;

    @ConfigEntry.Category("loot_max_level")

    @ConfigEntry.BoundedDiscrete(min = 1, max = 20)
    public int loot_max_level_movement_speed = 5;

    @ConfigEntry.Category("loot_max_level")

    @ConfigEntry.BoundedDiscrete(min = 1, max = 20)
    public int loot_max_level_true_damage = 5;

    @ConfigEntry.Category("loot_max_level")

    @ConfigEntry.BoundedDiscrete(min = 1, max = 20)
    public int loot_max_level_splash_damage = 5;

    @ConfigEntry.Category("loot_max_level")

    @ConfigEntry.BoundedDiscrete(min = 1, max = 20)
    public int loot_max_level_execute = 3;

    @ConfigEntry.Category("loot_max_level")

    @ConfigEntry.BoundedDiscrete(min = 1, max = 20)
    public int loot_max_level_frost = 5;

    @ConfigEntry.Category("loot_max_level")

    @ConfigEntry.BoundedDiscrete(min = 1, max = 20)
    public int loot_max_level_poison = 5;

    @ConfigEntry.Category("loot_max_level")

    @ConfigEntry.BoundedDiscrete(min = 1, max = 20)
    public int loot_max_level_armor_shatter = 5;

    @ConfigEntry.Category("loot_max_level")

    @ConfigEntry.BoundedDiscrete(min = 1, max = 20)
    public int loot_max_level_thorns_reflect = 5;

    @ConfigEntry.Category("loot_max_level")

    @ConfigEntry.BoundedDiscrete(min = 1, max = 20)
    public int loot_max_level_shield = 3;

    @ConfigEntry.Category("loot_max_level")

    @ConfigEntry.BoundedDiscrete(min = 1, max = 20)
    public int loot_max_level_knockback_resistance = 5;

    // ==================== 战利品权重（每个附魔总权重，按比例自动分配到各等级） ====================
    @ConfigEntry.Category("loot_weight")
    @ConfigEntry.BoundedDiscrete(min = 0, max = 100)
    public int loot_weight_critical_rate = 0;

    @ConfigEntry.Category("loot_weight")
    @ConfigEntry.BoundedDiscrete(min = 0, max = 100)
    public int loot_weight_critical_damage = 20;

    @ConfigEntry.Category("loot_weight")
    @ConfigEntry.BoundedDiscrete(min = 0, max = 100)
    public int loot_weight_critical_resistance = 30;

    @ConfigEntry.Category("loot_weight")
    @ConfigEntry.BoundedDiscrete(min = 0, max = 100)
    public int loot_weight_percentage_damage = 10;

    @ConfigEntry.Category("loot_weight")
    @ConfigEntry.BoundedDiscrete(min = 0, max = 100)
    public int loot_weight_armor_penetration = 0;

    @ConfigEntry.Category("loot_weight")
    @ConfigEntry.BoundedDiscrete(min = 0, max = 100)
    public int loot_weight_life_steal = 15;

    @ConfigEntry.Category("loot_weight")
    @ConfigEntry.BoundedDiscrete(min = 0, max = 100)
    public int loot_weight_vulnerability = 30;

    @ConfigEntry.Category("loot_weight")
    @ConfigEntry.BoundedDiscrete(min = 0, max = 100)
    public int loot_weight_damage_reduction = 10;

    @ConfigEntry.Category("loot_weight")
    @ConfigEntry.BoundedDiscrete(min = 0, max = 100)
    public int loot_weight_armor_defense = 20;

    @ConfigEntry.Category("loot_weight")
    @ConfigEntry.BoundedDiscrete(min = 0, max = 100)
    public int loot_weight_max_health = 20;

    @ConfigEntry.Category("loot_weight")
    @ConfigEntry.BoundedDiscrete(min = 0, max = 100)
    public int loot_weight_bonus_damage = 10;

    @ConfigEntry.Category("loot_weight")
    @ConfigEntry.BoundedDiscrete(min = 0, max = 100)
    public int loot_weight_attack_range = 10;

    @ConfigEntry.Category("loot_weight")
    @ConfigEntry.BoundedDiscrete(min = 0, max = 100)
    public int loot_weight_attack_speed = 20;

    @ConfigEntry.Category("loot_weight")
    @ConfigEntry.BoundedDiscrete(min = 0, max = 100)
    public int loot_weight_movement_speed = 20;

    @ConfigEntry.Category("loot_weight")
    @ConfigEntry.BoundedDiscrete(min = 0, max = 100)
    public int loot_weight_true_damage = 10;

    @ConfigEntry.Category("loot_weight")
    @ConfigEntry.BoundedDiscrete(min = 0, max = 100)
    public int loot_weight_splash_damage = 20;

    @ConfigEntry.Category("loot_weight")
    @ConfigEntry.BoundedDiscrete(min = 0, max = 100)
    public int loot_weight_execute = 10;

    @ConfigEntry.Category("loot_weight")
    @ConfigEntry.BoundedDiscrete(min = 0, max = 100)
    public int loot_weight_frost = 20;

    @ConfigEntry.Category("loot_weight")
    @ConfigEntry.BoundedDiscrete(min = 0, max = 100)
    public int loot_weight_poison = 20;

    @ConfigEntry.Category("loot_weight")
    @ConfigEntry.BoundedDiscrete(min = 0, max = 100)
    public int loot_weight_armor_shatter = 20;

    @ConfigEntry.Category("loot_weight")
    @ConfigEntry.BoundedDiscrete(min = 0, max = 100)
    public int loot_weight_thorns_reflect = 20;

    @ConfigEntry.Category("loot_weight")
    @ConfigEntry.BoundedDiscrete(min = 0, max = 100)
    public int loot_weight_shield = 10;

    @ConfigEntry.Category("loot_weight")
    @ConfigEntry.BoundedDiscrete(min = 0, max = 100)
    public int loot_weight_knockback_resistance = 30;

    // ==================== 其他配置 ====================
    @ConfigEntry.Category("other")

    public float mob_enchant_chance = 0.5f;

    @ConfigEntry.Category("other")

    public float mob_hard_bonus_health = 20f;

    @ConfigEntry.Category("other")

    public boolean mob_adaptation_enabled = true;

    @ConfigEntry.Category("other")

    public boolean anvil_cost_halved = true;

    @ConfigEntry.Category("other")

    public boolean critical_particles_enabled = true;

    @ConfigEntry.Category("other")

    public boolean debug_mode = false;

    /**
     * 同步配置到旧的CriticalHitsConfig。
     * 保持向后兼容。
     */
    public void syncToLegacyConfig() {
        CriticalHitsConfig.ConfigData legacy = CriticalHitsConfig.get();
        legacy.hud_enabled = this.hud_enabled;
        legacy.hud_side = this.hud_side;
        legacy.hud_x_offset = this.hud_x_offset;
        legacy.hud_y_offset = this.hud_y_offset;
        legacy.hud_background_alpha = this.hud_background_alpha;
        legacy.hud_scale = this.hud_scale;
        legacy.loot_enchantment_max_level = this.loot_enchantment_max_level;
        legacy.loot_enchantment_max_count = this.loot_enchantment_max_count;
        legacy.coin_shard_weight = this.coin_shard_weight;
        legacy.coin_shard_max_count = this.coin_shard_max_count;
        legacy.mob_enchant_chance = this.mob_enchant_chance;
        legacy.mob_hard_bonus_health = this.mob_hard_bonus_health;
        legacy.mob_adaptation_enabled = this.mob_adaptation_enabled;
        legacy.anvil_cost_halved = this.anvil_cost_halved;
        legacy.critical_particles_enabled = this.critical_particles_enabled;
        legacy.debug_mode = this.debug_mode;

        // 同步HUD可见属性
        if (legacy.hud_visible_attributes == null) {
            legacy.hud_visible_attributes = new HashMap<>();
        }
        legacy.hud_visible_attributes.put("critical_rate", this.show_critical_rate);
        legacy.hud_visible_attributes.put("critical_damage", this.show_critical_damage);
        legacy.hud_visible_attributes.put("damage_bonus", this.show_damage_bonus);
        legacy.hud_visible_attributes.put("armor_penetration", this.show_armor_penetration);
        legacy.hud_visible_attributes.put("life_steal", this.show_life_steal);
        legacy.hud_visible_attributes.put("vulnerability", this.show_vulnerability);
        legacy.hud_visible_attributes.put("attack_speed", this.show_attack_speed);
        legacy.hud_visible_attributes.put("attack_range", this.show_attack_range);
        legacy.hud_visible_attributes.put("bonus_damage", this.show_bonus_damage);
        legacy.hud_visible_attributes.put("damage_reduction", this.show_damage_reduction);
        legacy.hud_visible_attributes.put("armor_defense", this.show_armor_defense);
        legacy.hud_visible_attributes.put("max_health", this.show_max_health);
        legacy.hud_visible_attributes.put("critical_resistance", this.show_critical_resistance);
        legacy.hud_visible_attributes.put("movement_speed", this.show_movement_speed);
        legacy.hud_visible_attributes.put("true_damage", this.show_true_damage);
        legacy.hud_visible_attributes.put("splash_damage", this.show_splash_damage);
        legacy.hud_visible_attributes.put("execute", this.show_execute);
        legacy.hud_visible_attributes.put("frost", this.show_frost);
        legacy.hud_visible_attributes.put("poison", this.show_poison);
        legacy.hud_visible_attributes.put("armor_shatter", this.show_armor_shatter);
        legacy.hud_visible_attributes.put("thorns_reflect", this.show_thorns_reflect);
        legacy.hud_visible_attributes.put("shield", this.show_shield);
        legacy.hud_visible_attributes.put("knockback_resistance", this.show_knockback_resistance);

        // 同步战利品附魔开关
        syncLootEnchantment(legacy, "criticalhits:critical_rate", this.loot_critical_rate);
        syncLootEnchantment(legacy, "criticalhits:critical_damage", this.loot_critical_damage);
        syncLootEnchantment(legacy, "criticalhits:critical_resistance", this.loot_critical_resistance);
        syncLootEnchantment(legacy, "criticalhits:percentage_damage", this.loot_percentage_damage);
        syncLootEnchantment(legacy, "criticalhits:armor_penetration", this.loot_armor_penetration);
        syncLootEnchantment(legacy, "criticalhits:life_steal", this.loot_life_steal);
        syncLootEnchantment(legacy, "criticalhits:vulnerability", this.loot_vulnerability);
        syncLootEnchantment(legacy, "criticalhits:damage_reduction", this.loot_damage_reduction);
        syncLootEnchantment(legacy, "criticalhits:armor_defense", this.loot_armor_defense);
        syncLootEnchantment(legacy, "criticalhits:max_health", this.loot_max_health);
        syncLootEnchantment(legacy, "criticalhits:bonus_damage", this.loot_bonus_damage);
        syncLootEnchantment(legacy, "criticalhits:attack_range", this.loot_attack_range);
        syncLootEnchantment(legacy, "criticalhits:attack_speed", this.loot_attack_speed);
        syncLootEnchantment(legacy, "criticalhits:movement_speed", this.loot_movement_speed);
        syncLootEnchantment(legacy, "criticalhits:true_damage", this.loot_true_damage);
        syncLootEnchantment(legacy, "criticalhits:splash_damage", this.loot_splash_damage);
        syncLootEnchantment(legacy, "criticalhits:execute", this.loot_execute);
        syncLootEnchantment(legacy, "criticalhits:frost", this.loot_frost);
        syncLootEnchantment(legacy, "criticalhits:poison", this.loot_poison);
        syncLootEnchantment(legacy, "criticalhits:armor_shatter", this.loot_armor_shatter);
        syncLootEnchantment(legacy, "criticalhits:thorns_reflect", this.loot_thorns_reflect);
        syncLootEnchantment(legacy, "criticalhits:shield", this.loot_shield);
        syncLootEnchantment(legacy, "criticalhits:knockback_resistance", this.loot_knockback_resistance);

        // 同步每个附魔的战利品最高等级
        syncLootMaxLevel(legacy, "criticalhits:critical_rate", this.loot_max_level_critical_rate);
        syncLootMaxLevel(legacy, "criticalhits:critical_damage", this.loot_max_level_critical_damage);
        syncLootMaxLevel(legacy, "criticalhits:critical_resistance", this.loot_max_level_critical_resistance);
        syncLootMaxLevel(legacy, "criticalhits:percentage_damage", this.loot_max_level_percentage_damage);
        syncLootMaxLevel(legacy, "criticalhits:armor_penetration", this.loot_max_level_armor_penetration);
        syncLootMaxLevel(legacy, "criticalhits:life_steal", this.loot_max_level_life_steal);
        syncLootMaxLevel(legacy, "criticalhits:vulnerability", this.loot_max_level_vulnerability);
        syncLootMaxLevel(legacy, "criticalhits:damage_reduction", this.loot_max_level_damage_reduction);
        syncLootMaxLevel(legacy, "criticalhits:armor_defense", this.loot_max_level_armor_defense);
        syncLootMaxLevel(legacy, "criticalhits:max_health", this.loot_max_level_max_health);
        syncLootMaxLevel(legacy, "criticalhits:bonus_damage", this.loot_max_level_bonus_damage);
        syncLootMaxLevel(legacy, "criticalhits:attack_range", this.loot_max_level_attack_range);
        syncLootMaxLevel(legacy, "criticalhits:attack_speed", this.loot_max_level_attack_speed);
        syncLootMaxLevel(legacy, "criticalhits:movement_speed", this.loot_max_level_movement_speed);
        syncLootMaxLevel(legacy, "criticalhits:true_damage", this.loot_max_level_true_damage);
        syncLootMaxLevel(legacy, "criticalhits:splash_damage", this.loot_max_level_splash_damage);
        syncLootMaxLevel(legacy, "criticalhits:execute", this.loot_max_level_execute);
        syncLootMaxLevel(legacy, "criticalhits:frost", this.loot_max_level_frost);
        syncLootMaxLevel(legacy, "criticalhits:poison", this.loot_max_level_poison);
        syncLootMaxLevel(legacy, "criticalhits:armor_shatter", this.loot_max_level_armor_shatter);
        syncLootMaxLevel(legacy, "criticalhits:thorns_reflect", this.loot_max_level_thorns_reflect);
        syncLootMaxLevel(legacy, "criticalhits:shield", this.loot_max_level_shield);
        syncLootMaxLevel(legacy, "criticalhits:knockback_resistance", this.loot_max_level_knockback_resistance);

        // 同步每个附魔的战利品总权重（按比例自动分配到各等级）
        syncLootWeight(legacy, "criticalhits:critical_rate", this.loot_weight_critical_rate);
        syncLootWeight(legacy, "criticalhits:critical_damage", this.loot_weight_critical_damage);
        syncLootWeight(legacy, "criticalhits:critical_resistance", this.loot_weight_critical_resistance);
        syncLootWeight(legacy, "criticalhits:percentage_damage", this.loot_weight_percentage_damage);
        syncLootWeight(legacy, "criticalhits:armor_penetration", this.loot_weight_armor_penetration);
        syncLootWeight(legacy, "criticalhits:life_steal", this.loot_weight_life_steal);
        syncLootWeight(legacy, "criticalhits:vulnerability", this.loot_weight_vulnerability);
        syncLootWeight(legacy, "criticalhits:damage_reduction", this.loot_weight_damage_reduction);
        syncLootWeight(legacy, "criticalhits:armor_defense", this.loot_weight_armor_defense);
        syncLootWeight(legacy, "criticalhits:max_health", this.loot_weight_max_health);
        syncLootWeight(legacy, "criticalhits:bonus_damage", this.loot_weight_bonus_damage);
        syncLootWeight(legacy, "criticalhits:attack_range", this.loot_weight_attack_range);
        syncLootWeight(legacy, "criticalhits:attack_speed", this.loot_weight_attack_speed);
        syncLootWeight(legacy, "criticalhits:movement_speed", this.loot_weight_movement_speed);
        syncLootWeight(legacy, "criticalhits:true_damage", this.loot_weight_true_damage);
        syncLootWeight(legacy, "criticalhits:splash_damage", this.loot_weight_splash_damage);
        syncLootWeight(legacy, "criticalhits:execute", this.loot_weight_execute);
        syncLootWeight(legacy, "criticalhits:frost", this.loot_weight_frost);
        syncLootWeight(legacy, "criticalhits:poison", this.loot_weight_poison);
        syncLootWeight(legacy, "criticalhits:armor_shatter", this.loot_weight_armor_shatter);
        syncLootWeight(legacy, "criticalhits:thorns_reflect", this.loot_weight_thorns_reflect);
        syncLootWeight(legacy, "criticalhits:shield", this.loot_weight_shield);
        syncLootWeight(legacy, "criticalhits:knockback_resistance", this.loot_weight_knockback_resistance);
    }

    private void syncLootEnchantment(CriticalHitsConfig.ConfigData legacy, String id, boolean enabled) {
        CriticalHitsConfig.LootEnchantmentConfig config = legacy.loot_enchantments.get(id);
        if (config != null) {
            config.enabled = enabled;
        }
    }

    private void syncLootMaxLevel(CriticalHitsConfig.ConfigData legacy, String id, int maxLevel) {
        CriticalHitsConfig.LootEnchantmentConfig config = legacy.loot_enchantments.get(id);
        if (config != null) {
            config.max_level = maxLevel;
        }
    }

    private void syncLootWeight(CriticalHitsConfig.ConfigData legacy, String id, int totalWeight) {
        CriticalHitsConfig.LootEnchantmentConfig config = legacy.loot_enchantments.get(id);
        if (config != null) {
            // 按比例分配总权重到各等级（等级越高权重越低）
            // 比例：等级1=40%, 等级2=30%, 等级3=20%, 等级4=10%, 等级5=3%, 等级6+=1%
            double[] ratios = {0.40, 0.30, 0.20, 0.10, 0.03, 0.01, 0.01, 0.01, 0.01, 0.01,
                0.01, 0.01, 0.01, 0.01, 0.01, 0.01, 0.01, 0.01, 0.01, 0.01};
            int maxLevel = Math.max(config.max_level, 5);
            int[] weights = new int[maxLevel];
            for (int i = 0; i < maxLevel; i++) {
                double ratio = (i < ratios.length) ? ratios[i] : 0.01;
                weights[i] = (int) Math.max(1, Math.round(totalWeight * ratio));
            }
            config.weights = weights;
        }
    }
}
