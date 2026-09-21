package com.juzipi.criticalhits;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * 配方书数据。
 * 包含所有附魔和物品的合成配方信息。
 */
public class RecipeBookData {

    /**
     * 配方分类。
     */
    public static class RecipeCategory {
        public final String titleKey;
        public final List<RecipeEntry> recipes;

        public RecipeCategory(String titleKey, List<RecipeEntry> recipes) {
            this.titleKey = titleKey;
            this.recipes = recipes;
        }
    }

    /**
     * 单个配方条目。
     */
    public static class RecipeEntry {
        public final String nameKey;
        public final String descriptionKey;
        public final ItemStack output;
        public final List<ItemStack> materials;
        public final String subCategory; // 二级分类

        public RecipeEntry(String nameKey, String descriptionKey, ItemStack output, List<ItemStack> materials, String subCategory) {
            this.nameKey = nameKey;
            this.descriptionKey = descriptionKey;
            this.output = output;
            this.materials = materials;
            this.subCategory = subCategory;
        }
    }

    /**
     * 获取所有配方分类。
     */
    public static List<RecipeCategory> getAllCategories() {
        List<RecipeCategory> categories = new ArrayList<>();

        // ===== 暴击系列 =====
        List<RecipeEntry> criticalRecipes = new ArrayList<>();
        criticalRecipes.add(new RecipeEntry(
            "modern_damage_system.recipe.critical_rate",
            "modern_damage_system.recipe.critical_rate.desc",
            new ItemStack(Items.ENCHANTED_BOOK),
            List.of(new ItemStack(Items.BOOK), new ItemStack(Items.NETHERITE_INGOT), new ItemStack(Items.DIAMOND), new ItemStack(CriticalHitsMod.CRITICAL_COIN)),
            "modern_damage_system.recipe.subcategory.attack"
        ));
        criticalRecipes.add(new RecipeEntry(
            "modern_damage_system.recipe.critical_damage",
            "modern_damage_system.recipe.critical_damage.desc",
            new ItemStack(Items.ENCHANTED_BOOK),
            List.of(new ItemStack(Items.BOOK), new ItemStack(Items.DIAMOND, 2), new ItemStack(CriticalHitsMod.CRITICAL_COIN)),
            "modern_damage_system.recipe.subcategory.attack"
        ));
        criticalRecipes.add(new RecipeEntry(
            "modern_damage_system.recipe.critical_resistance",
            "modern_damage_system.recipe.critical_resistance.desc",
            new ItemStack(Items.ENCHANTED_BOOK),
            List.of(new ItemStack(Items.BOOK), new ItemStack(Items.IRON_INGOT, 5), new ItemStack(CriticalHitsMod.CRITICAL_COIN)),
            "modern_damage_system.recipe.subcategory.defense"
        ));
        categories.add(new RecipeCategory("modern_damage_system.recipe.category.critical", criticalRecipes));

        // ===== 攻击系列 =====
        List<RecipeEntry> attackRecipes = new ArrayList<>();
        attackRecipes.add(new RecipeEntry(
            "modern_damage_system.recipe.percentage_damage",
            "modern_damage_system.recipe.percentage_damage.desc",
            new ItemStack(Items.ENCHANTED_BOOK),
            List.of(new ItemStack(Items.BOOK), new ItemStack(Items.DIAMOND, 3), new ItemStack(CriticalHitsMod.CRITICAL_COIN)),
            "modern_damage_system.recipe.subcategory.attack"
        ));
        attackRecipes.add(new RecipeEntry(
            "modern_damage_system.recipe.armor_penetration",
            "modern_damage_system.recipe.armor_penetration.desc",
            new ItemStack(Items.ENCHANTED_BOOK),
            List.of(new ItemStack(Items.BOOK), new ItemStack(Items.NETHERITE_INGOT), new ItemStack(CriticalHitsMod.CRITICAL_COIN)),
            "modern_damage_system.recipe.subcategory.attack"
        ));
        attackRecipes.add(new RecipeEntry(
            "modern_damage_system.recipe.life_steal",
            "modern_damage_system.recipe.life_steal.desc",
            new ItemStack(Items.ENCHANTED_BOOK),
            List.of(new ItemStack(Items.BOOK), new ItemStack(Items.GOLD_BLOCK), new ItemStack(CriticalHitsMod.CRITICAL_COIN)),
            "modern_damage_system.recipe.subcategory.attack"
        ));
        attackRecipes.add(new RecipeEntry(
            "modern_damage_system.recipe.vulnerability",
            "modern_damage_system.recipe.vulnerability.desc",
            new ItemStack(Items.ENCHANTED_BOOK),
            List.of(new ItemStack(Items.BOOK), new ItemStack(Items.GUNPOWDER, 2), new ItemStack(Items.DIAMOND), new ItemStack(CriticalHitsMod.CRITICAL_COIN)),
            "debuff类附魔"
        ));
        attackRecipes.add(new RecipeEntry(
            "modern_damage_system.recipe.true_damage",
            "modern_damage_system.recipe.true_damage.desc",
            new ItemStack(Items.ENCHANTED_BOOK),
            List.of(new ItemStack(Items.BOOK), new ItemStack(Items.NETHERITE_INGOT), new ItemStack(Items.DRAGON_BREATH, 5), new ItemStack(CriticalHitsMod.CRITICAL_COIN)),
            "modern_damage_system.recipe.subcategory.attack"
        ));
        attackRecipes.add(new RecipeEntry(
            "modern_damage_system.recipe.splash_damage",
            "modern_damage_system.recipe.splash_damage.desc",
            new ItemStack(Items.ENCHANTED_BOOK),
            List.of(new ItemStack(Items.BOOK), new ItemStack(Items.GUNPOWDER, 5), new ItemStack(Items.BLAZE_POWDER, 2), new ItemStack(Items.REDSTONE, 2), new ItemStack(CriticalHitsMod.CRITICAL_COIN)),
            "modern_damage_system.recipe.subcategory.attack"
        ));
        attackRecipes.add(new RecipeEntry(
            "modern_damage_system.recipe.execute",
            "modern_damage_system.recipe.execute.desc",
            new ItemStack(Items.ENCHANTED_BOOK),
            List.of(new ItemStack(Items.BOOK), new ItemStack(Items.NETHER_STAR), new ItemStack(CriticalHitsMod.CRITICAL_COIN)),
            "modern_damage_system.recipe.subcategory.attack"
        ));
        // 创建带有特定效果的药水ItemStack
        ItemStack slownessPotion = new ItemStack(Items.POTION);
        slownessPotion.set(DataComponents.POTION_CONTENTS, new PotionContents(Potions.SLOWNESS));
        ItemStack poisonPotion = new ItemStack(Items.POTION);
        poisonPotion.set(DataComponents.POTION_CONTENTS, new PotionContents(Potions.POISON));

        attackRecipes.add(new RecipeEntry(
            "modern_damage_system.recipe.frost",
            "modern_damage_system.recipe.frost.desc",
            new ItemStack(Items.ENCHANTED_BOOK),
            List.of(new ItemStack(Items.BOOK), new ItemStack(Items.BLUE_ICE, 2), new ItemStack(Items.LAPIS_LAZULI), slownessPotion, new ItemStack(CriticalHitsMod.CRITICAL_COIN)),
            "debuff类附魔"
        ));
        attackRecipes.add(new RecipeEntry(
            "modern_damage_system.recipe.poison",
            "modern_damage_system.recipe.poison.desc",
            new ItemStack(Items.ENCHANTED_BOOK),
            List.of(new ItemStack(Items.BOOK), new ItemStack(Items.SPIDER_EYE, 2), new ItemStack(Items.FERMENTED_SPIDER_EYE), poisonPotion, new ItemStack(CriticalHitsMod.CRITICAL_COIN)),
            "debuff类附魔"
        ));
        attackRecipes.add(new RecipeEntry(
            "modern_damage_system.recipe.armor_shatter",
            "modern_damage_system.recipe.armor_shatter.desc",
            new ItemStack(Items.ENCHANTED_BOOK),
            List.of(new ItemStack(Items.BOOK), new ItemStack(Items.NETHERITE_INGOT), new ItemStack(Items.ANVIL), new ItemStack(CriticalHitsMod.CRITICAL_COIN)),
            "debuff类附魔"
        ));
        categories.add(new RecipeCategory("modern_damage_system.recipe.category.attack", attackRecipes));

        // ===== 防御系列 =====
        List<RecipeEntry> defenseRecipes = new ArrayList<>();
        defenseRecipes.add(new RecipeEntry(
            "modern_damage_system.recipe.damage_reduction",
            "modern_damage_system.recipe.damage_reduction.desc",
            new ItemStack(Items.ENCHANTED_BOOK),
            List.of(new ItemStack(Items.BOOK), new ItemStack(Items.EMERALD_BLOCK), new ItemStack(CriticalHitsMod.CRITICAL_COIN)),
            "modern_damage_system.recipe.subcategory.defense"
        ));
        defenseRecipes.add(new RecipeEntry(
            "modern_damage_system.recipe.armor_defense",
            "modern_damage_system.recipe.armor_defense.desc",
            new ItemStack(Items.ENCHANTED_BOOK),
            List.of(new ItemStack(Items.BOOK), new ItemStack(Items.IRON_BLOCK, 3), new ItemStack(CriticalHitsMod.CRITICAL_COIN)),
            "modern_damage_system.recipe.subcategory.defense"
        ));
        defenseRecipes.add(new RecipeEntry(
            "modern_damage_system.recipe.max_health",
            "modern_damage_system.recipe.max_health.desc",
            new ItemStack(Items.ENCHANTED_BOOK),
            List.of(new ItemStack(Items.BOOK), new ItemStack(Items.GOLD_BLOCK), new ItemStack(Items.AMETHYST_SHARD, 2), new ItemStack(CriticalHitsMod.CRITICAL_COIN)),
            "modern_damage_system.recipe.subcategory.defense"
        ));
        defenseRecipes.add(new RecipeEntry(
            "modern_damage_system.recipe.thorns_reflect",
            "modern_damage_system.recipe.thorns_reflect.desc",
            new ItemStack(Items.ENCHANTED_BOOK),
            List.of(new ItemStack(Items.BOOK), new ItemStack(Items.CACTUS, 4), new ItemStack(Items.EMERALD), new ItemStack(Items.FLINT), new ItemStack(CriticalHitsMod.CRITICAL_COIN)),
            "modern_damage_system.recipe.subcategory.defense"
        ));
        defenseRecipes.add(new RecipeEntry(
            "modern_damage_system.recipe.shield",
            "modern_damage_system.recipe.shield.desc",
            new ItemStack(Items.ENCHANTED_BOOK),
            List.of(new ItemStack(Items.BOOK), new ItemStack(Items.GOLD_BLOCK), new ItemStack(Items.AMETHYST_SHARD, 2), new ItemStack(Items.TURTLE_HELMET, 3), new ItemStack(CriticalHitsMod.CRITICAL_COIN)),
            "modern_damage_system.recipe.subcategory.defense"
        ));
        defenseRecipes.add(new RecipeEntry(
            "modern_damage_system.recipe.knockback_resistance",
            "modern_damage_system.recipe.knockback_resistance.desc",
            new ItemStack(Items.ENCHANTED_BOOK),
            List.of(new ItemStack(Items.BOOK), new ItemStack(Items.IRON_BLOCK, 3), new ItemStack(Items.SLIME_BALL), new ItemStack(CriticalHitsMod.CRITICAL_COIN)),
            "modern_damage_system.recipe.subcategory.defense"
        ));
        categories.add(new RecipeCategory("modern_damage_system.recipe.category.defense", defenseRecipes));

        // ===== 属性系列 =====
        List<RecipeEntry> attributeRecipes = new ArrayList<>();
        attributeRecipes.add(new RecipeEntry(
            "modern_damage_system.recipe.attack_speed",
            "modern_damage_system.recipe.attack_speed.desc",
            new ItemStack(Items.ENCHANTED_BOOK),
            List.of(new ItemStack(Items.BOOK), new ItemStack(Items.WIND_CHARGE), new ItemStack(Items.EMERALD), new ItemStack(CriticalHitsMod.CRITICAL_COIN)),
            "modern_damage_system.recipe.subcategory.attribute"
        ));
        attributeRecipes.add(new RecipeEntry(
            "modern_damage_system.recipe.movement_speed",
            "modern_damage_system.recipe.movement_speed.desc",
            new ItemStack(Items.ENCHANTED_BOOK),
            List.of(new ItemStack(Items.BOOK), new ItemStack(Items.RABBIT_FOOT, 2), new ItemStack(Items.EMERALD), new ItemStack(CriticalHitsMod.CRITICAL_COIN)),
            "modern_damage_system.recipe.subcategory.attribute"
        ));
        attributeRecipes.add(new RecipeEntry(
            "modern_damage_system.recipe.attack_range",
            "modern_damage_system.recipe.attack_range.desc",
            new ItemStack(Items.ENCHANTED_BOOK),
            List.of(new ItemStack(Items.BOOK), new ItemStack(Items.LAPIS_BLOCK), new ItemStack(CriticalHitsMod.CRITICAL_COIN)),
            "modern_damage_system.recipe.subcategory.attribute"
        ));
        attributeRecipes.add(new RecipeEntry(
            "modern_damage_system.recipe.bonus_damage",
            "modern_damage_system.recipe.bonus_damage.desc",
            new ItemStack(Items.ENCHANTED_BOOK),
            List.of(new ItemStack(Items.BOOK), new ItemStack(Items.GOLD_BLOCK, 2), new ItemStack(CriticalHitsMod.CRITICAL_COIN)),
            "modern_damage_system.recipe.subcategory.attribute"
        ));
        categories.add(new RecipeCategory("modern_damage_system.recipe.category.attribute", attributeRecipes));

        // ===== 功能物品 =====
        List<RecipeEntry> utilityRecipes = new ArrayList<>();
        utilityRecipes.add(new RecipeEntry(
            "modern_damage_system.recipe.mds_station",
            "modern_damage_system.recipe.mds_station.desc",
            new ItemStack(CriticalHitsMod.CRITICAL_STATION_BLOCK),
            List.of(new ItemStack(CriticalHitsMod.CRITICAL_COIN, 4), new ItemStack(Items.GOLD_BLOCK, 4), new ItemStack(Items.REDSTONE_BLOCK)),
            "modern_damage_system.recipe.subcategory.utility"
        ));
        utilityRecipes.add(new RecipeEntry(
            "modern_damage_system.recipe.material_template",
            "modern_damage_system.recipe.material_template.desc",
            new ItemStack(CriticalHitsMod.CRITICAL_COIN),
            List.of(new ItemStack(Items.ROTTEN_FLESH, 8), new ItemStack(CriticalHitsMod.CRITICAL_COIN_SHARD)),
            "modern_damage_system.recipe.subcategory.material"
        ));
        utilityRecipes.add(new RecipeEntry(
            "modern_damage_system.recipe.material_shard",
            "modern_damage_system.recipe.material_shard.desc",
            new ItemStack(CriticalHitsMod.CRITICAL_COIN_SHARD),
            List.of(),
            "modern_damage_system.recipe.subcategory.material"
        ));

        // ===== 药水系列 =====
        utilityRecipes.add(new RecipeEntry(
            "modern_damage_system.recipe.potion_vulnerability",
            "modern_damage_system.recipe.potion_vulnerability.desc",
            new ItemStack(Items.POTION),
            List.of(new ItemStack(Items.POTION), new ItemStack(Items.SPIDER_EYE)),
            "modern_damage_system.recipe.subcategory.potion"
        ));
        utilityRecipes.add(new RecipeEntry(
            "modern_damage_system.recipe.potion_frost",
            "modern_damage_system.recipe.potion_frost.desc",
            new ItemStack(Items.POTION),
            List.of(new ItemStack(Items.POTION), new ItemStack(Items.BLUE_ICE)),
            "modern_damage_system.recipe.subcategory.potion"
        ));
        utilityRecipes.add(new RecipeEntry(
            "modern_damage_system.recipe.potion_poison",
            "modern_damage_system.recipe.potion_poison.desc",
            new ItemStack(Items.POTION),
            List.of(new ItemStack(Items.POTION), new ItemStack(Items.SPIDER_EYE), new ItemStack(Items.FERMENTED_SPIDER_EYE)),
            "modern_damage_system.recipe.subcategory.potion"
        ));
        utilityRecipes.add(new RecipeEntry(
            "modern_damage_system.recipe.potion_armor_break",
            "modern_damage_system.recipe.potion_armor_break.desc",
            new ItemStack(Items.POTION),
            List.of(new ItemStack(Items.POTION), new ItemStack(Items.IRON_INGOT)),
            "modern_damage_system.recipe.subcategory.potion"
        ));
        utilityRecipes.add(new RecipeEntry(
            "modern_damage_system.recipe.splash_potion",
            "modern_damage_system.recipe.splash_potion.desc",
            new ItemStack(Items.SPLASH_POTION),
            List.of(new ItemStack(Items.POTION), new ItemStack(Items.GUNPOWDER)),
            "modern_damage_system.recipe.subcategory.potion"
        ));
        utilityRecipes.add(new RecipeEntry(
            "modern_damage_system.recipe.lingering_potion",
            "modern_damage_system.recipe.lingering_potion.desc",
            new ItemStack(Items.LINGERING_POTION),
            List.of(new ItemStack(Items.SPLASH_POTION), new ItemStack(Items.DRAGON_BREATH)),
            "modern_damage_system.recipe.subcategory.potion"
        ));

        // ===== 药水箭系列 =====
        utilityRecipes.add(new RecipeEntry(
            "modern_damage_system.recipe.arrow_vulnerability",
            "modern_damage_system.recipe.arrow_vulnerability.desc",
            new ItemStack(Items.TIPPED_ARROW),
            List.of(new ItemStack(Items.ARROW, 8), new ItemStack(Items.LINGERING_POTION)),
            "modern_damage_system.recipe.subcategory.arrow"
        ));
        utilityRecipes.add(new RecipeEntry(
            "modern_damage_system.recipe.arrow_frost",
            "modern_damage_system.recipe.arrow_frost.desc",
            new ItemStack(Items.TIPPED_ARROW),
            List.of(new ItemStack(Items.ARROW, 8), new ItemStack(Items.LINGERING_POTION)),
            "modern_damage_system.recipe.subcategory.arrow"
        ));
        utilityRecipes.add(new RecipeEntry(
            "modern_damage_system.recipe.arrow_poison",
            "modern_damage_system.recipe.arrow_poison.desc",
            new ItemStack(Items.TIPPED_ARROW),
            List.of(new ItemStack(Items.ARROW, 8), new ItemStack(Items.LINGERING_POTION)),
            "modern_damage_system.recipe.subcategory.arrow"
        ));
        utilityRecipes.add(new RecipeEntry(
            "modern_damage_system.recipe.arrow_armor_break",
            "modern_damage_system.recipe.arrow_armor_break.desc",
            new ItemStack(Items.TIPPED_ARROW),
            List.of(new ItemStack(Items.ARROW, 8), new ItemStack(Items.LINGERING_POTION)),
            "modern_damage_system.recipe.subcategory.arrow"
        ));

        categories.add(new RecipeCategory("modern_damage_system.recipe.category.utility", utilityRecipes));

        // ===== 附魔书展示（v2.0.0 新增：展示所有附魔的1级书和完整描述） =====
        List<RecipeEntry> enchantmentShowcase = new ArrayList<>();

        // 暴击系列
        enchantmentShowcase.add(createEnchantmentShowcaseEntry(
            "modern_damage_system.showcase.critical_rate",
            "enchantment.criticalhits.critical_rate.desc",
            CriticalHitsEnchantments.CRITICAL_RATE_ID,
            "modern_damage_system.recipe.subcategory.critical"
        ));
        enchantmentShowcase.add(createEnchantmentShowcaseEntry(
            "modern_damage_system.showcase.critical_damage",
            "enchantment.criticalhits.critical_damage.desc",
            CriticalHitsEnchantments.CRITICAL_DAMAGE_ID,
            "modern_damage_system.recipe.subcategory.critical"
        ));
        enchantmentShowcase.add(createEnchantmentShowcaseEntry(
            "modern_damage_system.showcase.critical_resistance",
            "enchantment.criticalhits.critical_resistance.desc",
            CriticalHitsEnchantments.CRITICAL_RESISTANCE_ID,
            "modern_damage_system.recipe.subcategory.critical"
        ));

        // 攻击系列
        enchantmentShowcase.add(createEnchantmentShowcaseEntry(
            "modern_damage_system.showcase.percentage_damage",
            "enchantment.criticalhits.percentage_damage.desc",
            CriticalHitsEnchantments.PERCENTAGE_DAMAGE_ID,
            "modern_damage_system.recipe.subcategory.attack"
        ));
        enchantmentShowcase.add(createEnchantmentShowcaseEntry(
            "modern_damage_system.showcase.armor_penetration",
            "enchantment.criticalhits.armor_penetration.desc",
            CriticalHitsEnchantments.ARMOR_PENETRATION_ID,
            "modern_damage_system.recipe.subcategory.attack"
        ));
        enchantmentShowcase.add(createEnchantmentShowcaseEntry(
            "modern_damage_system.showcase.life_steal",
            "enchantment.criticalhits.life_steal.desc",
            CriticalHitsEnchantments.LIFE_STEAL_ID,
            "modern_damage_system.recipe.subcategory.attack"
        ));
        enchantmentShowcase.add(createEnchantmentShowcaseEntry(
            "modern_damage_system.showcase.vulnerability",
            "enchantment.criticalhits.vulnerability.desc",
            CriticalHitsEnchantments.VULNERABILITY_ID,
            "modern_damage_system.recipe.subcategory.attack"
        ));
        enchantmentShowcase.add(createEnchantmentShowcaseEntry(
            "modern_damage_system.showcase.true_damage",
            "enchantment.criticalhits.true_damage.desc",
            CriticalHitsEnchantments.TRUE_DAMAGE_ID,
            "modern_damage_system.recipe.subcategory.attack"
        ));
        enchantmentShowcase.add(createEnchantmentShowcaseEntry(
            "modern_damage_system.showcase.splash_damage",
            "enchantment.criticalhits.splash_damage.desc",
            CriticalHitsEnchantments.SPLASH_DAMAGE_ID,
            "modern_damage_system.recipe.subcategory.attack"
        ));
        enchantmentShowcase.add(createEnchantmentShowcaseEntry(
            "modern_damage_system.showcase.execute",
            "enchantment.criticalhits.execute.desc",
            CriticalHitsEnchantments.EXECUTE_ID,
            "modern_damage_system.recipe.subcategory.attack"
        ));
        enchantmentShowcase.add(createEnchantmentShowcaseEntry(
            "modern_damage_system.showcase.frost",
            "enchantment.criticalhits.frost.desc",
            CriticalHitsEnchantments.FROST_ID,
            "modern_damage_system.recipe.subcategory.attack"
        ));
        enchantmentShowcase.add(createEnchantmentShowcaseEntry(
            "modern_damage_system.showcase.poison",
            "enchantment.criticalhits.poison.desc",
            CriticalHitsEnchantments.POISON_ID,
            "modern_damage_system.recipe.subcategory.attack"
        ));
        enchantmentShowcase.add(createEnchantmentShowcaseEntry(
            "modern_damage_system.showcase.armor_shatter",
            "enchantment.criticalhits.armor_shatter.desc",
            CriticalHitsEnchantments.ARMOR_SHATTER_ID,
            "modern_damage_system.recipe.subcategory.attack"
        ));
        enchantmentShowcase.add(createEnchantmentShowcaseEntry(
            "modern_damage_system.showcase.bonus_damage",
            "enchantment.criticalhits.bonus_damage.desc",
            CriticalHitsEnchantments.BONUS_DAMAGE_ID,
            "modern_damage_system.recipe.subcategory.attack"
        ));

        // 防御系列
        enchantmentShowcase.add(createEnchantmentShowcaseEntry(
            "modern_damage_system.showcase.damage_reduction",
            "enchantment.criticalhits.damage_reduction.desc",
            CriticalHitsEnchantments.DAMAGE_REDUCTION_ID,
            "modern_damage_system.recipe.subcategory.defense"
        ));
        enchantmentShowcase.add(createEnchantmentShowcaseEntry(
            "modern_damage_system.showcase.armor_defense",
            "enchantment.criticalhits.armor_defense.desc",
            CriticalHitsEnchantments.ARMOR_DEFENSE_ID,
            "modern_damage_system.recipe.subcategory.defense"
        ));
        enchantmentShowcase.add(createEnchantmentShowcaseEntry(
            "modern_damage_system.showcase.max_health",
            "enchantment.criticalhits.max_health.desc",
            CriticalHitsEnchantments.MAX_HEALTH_ID,
            "modern_damage_system.recipe.subcategory.defense"
        ));
        enchantmentShowcase.add(createEnchantmentShowcaseEntry(
            "modern_damage_system.showcase.thorns_reflect",
            "enchantment.criticalhits.thorns_reflect.desc",
            CriticalHitsEnchantments.THORNS_REFLECT_ID,
            "modern_damage_system.recipe.subcategory.defense"
        ));
        enchantmentShowcase.add(createEnchantmentShowcaseEntry(
            "modern_damage_system.showcase.shield",
            "enchantment.criticalhits.shield.desc",
            CriticalHitsEnchantments.SHIELD_ID,
            "modern_damage_system.recipe.subcategory.defense"
        ));
        enchantmentShowcase.add(createEnchantmentShowcaseEntry(
            "modern_damage_system.showcase.knockback_resistance",
            "enchantment.criticalhits.knockback_resistance.desc",
            CriticalHitsEnchantments.KNOCKBACK_RESISTANCE_ID,
            "modern_damage_system.recipe.subcategory.defense"
        ));

        // 属性系列
        enchantmentShowcase.add(createEnchantmentShowcaseEntry(
            "modern_damage_system.showcase.attack_speed",
            "enchantment.criticalhits.attack_speed.desc",
            CriticalHitsEnchantments.ATTACK_SPEED_ID,
            "modern_damage_system.recipe.subcategory.attribute"
        ));
        enchantmentShowcase.add(createEnchantmentShowcaseEntry(
            "modern_damage_system.showcase.movement_speed",
            "enchantment.criticalhits.movement_speed.desc",
            CriticalHitsEnchantments.MOVEMENT_SPEED_ID,
            "modern_damage_system.recipe.subcategory.attribute"
        ));
        enchantmentShowcase.add(createEnchantmentShowcaseEntry(
            "modern_damage_system.showcase.attack_range",
            "enchantment.criticalhits.attack_range.desc",
            CriticalHitsEnchantments.ATTACK_RANGE_ID,
            "modern_damage_system.recipe.subcategory.attribute"
        ));

        categories.add(new RecipeCategory("modern_damage_system.recipe.category.showcase", enchantmentShowcase));

        return categories;
    }

    /**
     * 创建附魔书展示条目（v2.0.0 新增）。
     * 创建一个1级的附魔书，用于在配方书中展示附魔描述。
     */
    private static RecipeEntry createEnchantmentShowcaseEntry(String nameKey, String descriptionKey, Identifier enchantmentId, String subCategory) {
        ItemStack enchantedBook = CriticalHitsMod.makeEnchantedBook(enchantmentId, 1);
        if (enchantedBook == null) {
            enchantedBook = new ItemStack(Items.ENCHANTED_BOOK);
        }
        return new RecipeEntry(nameKey, descriptionKey, enchantedBook, List.of(), subCategory);
    }
}
