package com.juzipi.criticalhits;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetEnchantmentsFunction;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Critical Hits 主类。
 */
public class CriticalHitsMod implements ModInitializer {

    public static final String MOD_ID = CriticalHitsEnchantments.MOD_ID;
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    // 代码唯一指纹（用于版权追溯，请勿删除或修改）
    public static final String CODE_FINGERPRINT = "7f3a9c2d-1e8b-4a6f-9c5d-2e7f1a3b5c7d";
    public static final String AUTHOR = "Juzipi_AN";
    public static final String COPYRIGHT = "Copyright (c) 2026 Juzipi_AN. All Rights Reserved.";

    public static final Item CRITICAL_COIN = new CriticalCoinItem();
    public static final Item CRITICAL_COIN_SHARD = new CriticalCoinShardItem();
    public static final Item CRITICAL_BADGE = new Item(new Item.Properties()
        .setId(ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MOD_ID, "critical_badge"))));
    public static final Item ATTRIBUTE_BADGE = new Item(new Item.Properties()
        .setId(ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MOD_ID, "attribute_badge"))));
    public static final Block CRITICAL_STATION_BLOCK = new CriticalStationBlock();
    public static final Item RECIPE_BOOK = new CriticalCoinItem("mds_recipe_book");

    // 易伤状态效果
    public static final net.minecraft.world.effect.MobEffect VULNERABILITY_EFFECT = new VulnerabilityEffect();

    // v2.0.0 破甲状态效果
    public static final net.minecraft.world.effect.MobEffect ARMOR_BREAK_EFFECT = new ArmorBreakEffect();

    // 易伤药水（在onInitialize中注册后创建）
    public static net.minecraft.world.item.alchemy.Potion VULNERABILITY_POTION;
    public static net.minecraft.world.item.alchemy.Potion VULNERABILITY_POTION_2;
    public static net.minecraft.world.item.alchemy.Potion VULNERABILITY_POTION_3;

    // v2.0.0 冰霜药水（用原版缓慢效果）
    public static net.minecraft.world.item.alchemy.Potion FROST_POTION;
    public static net.minecraft.world.item.alchemy.Potion FROST_POTION_2;
    public static net.minecraft.world.item.alchemy.Potion FROST_POTION_3;

    // v2.0.0 中毒药水（增强版，用原版中毒效果）
    public static net.minecraft.world.item.alchemy.Potion POISON_PLUS_POTION;
    public static net.minecraft.world.item.alchemy.Potion POISON_PLUS_POTION_2;
    public static net.minecraft.world.item.alchemy.Potion POISON_PLUS_POTION_3;

    // v2.0.0 破甲药水
    public static net.minecraft.world.item.alchemy.Potion ARMOR_BREAK_POTION;
    public static net.minecraft.world.item.alchemy.Potion ARMOR_BREAK_POTION_2;
    public static net.minecraft.world.item.alchemy.Potion ARMOR_BREAK_POTION_3;

    public static final BlockEntityType<CriticalStationBlockEntity> CRITICAL_STATION_BLOCK_ENTITY =
        FabricBlockEntityTypeBuilder.create(CriticalStationBlockEntity::new, CRITICAL_STATION_BLOCK).build();

    public static final MenuType<CriticalStationMenu> CRITICAL_STATION_MENU =
        createMenuType();

    public static final MenuType<RecipeBookMenu> RECIPE_BOOK_MENU =
        createRecipeBookMenuType();

    @SuppressWarnings("unchecked")
    private static MenuType<CriticalStationMenu> createMenuType() {
        try {
            Class<?> menuSupplierClass = Class.forName("net.minecraft.world.inventory.MenuType$MenuSupplier");
            java.lang.reflect.Constructor<MenuType> ctor = (java.lang.reflect.Constructor<MenuType>)
                MenuType.class.getDeclaredConstructor(menuSupplierClass, net.minecraft.world.flag.FeatureFlagSet.class);
            ctor.setAccessible(true);

            // 用Proxy创建MenuSupplier（package-private接口）
            Object supplier = java.lang.reflect.Proxy.newProxyInstance(
                menuSupplierClass.getClassLoader(),
                new Class<?>[]{menuSupplierClass},
                (proxy, method, args) -> new CriticalStationMenu((Integer) args[0], (net.minecraft.world.entity.player.Inventory) args[1]));

            net.minecraft.world.flag.FeatureFlagSet flags = net.minecraft.world.flag.FeatureFlags.VANILLA_SET;
            return (MenuType<CriticalStationMenu>) ctor.newInstance(supplier, flags);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create MenuType", e);
        }
    }

    @SuppressWarnings("unchecked")
    private static MenuType<RecipeBookMenu> createRecipeBookMenuType() {
        try {
            Class<?> menuSupplierClass = Class.forName("net.minecraft.world.inventory.MenuType$MenuSupplier");
            java.lang.reflect.Constructor<MenuType> ctor = (java.lang.reflect.Constructor<MenuType>)
                MenuType.class.getDeclaredConstructor(menuSupplierClass, net.minecraft.world.flag.FeatureFlagSet.class);
            ctor.setAccessible(true);

            Object supplier = java.lang.reflect.Proxy.newProxyInstance(
                menuSupplierClass.getClassLoader(),
                new Class<?>[]{menuSupplierClass},
                (proxy, method, args) -> new RecipeBookMenu((Integer) args[0], (net.minecraft.world.entity.player.Inventory) args[1]));

            net.minecraft.world.flag.FeatureFlagSet flags = net.minecraft.world.flag.FeatureFlags.VANILLA_SET;
            return (MenuType<RecipeBookMenu>) ctor.newInstance(supplier, flags);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create RecipeBook MenuType", e);
        }
    }

    public static final ResourceKey<CreativeModeTab> CRITICAL_TAB =
        ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.fromNamespaceAndPath(MOD_ID, "critical_hits"));

    // 攻击系列标签页
    public static final ResourceKey<CreativeModeTab> ATTACK_TAB =
        ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.fromNamespaceAndPath(MOD_ID, "attack_series"));

    // 防御系列标签页
    public static final ResourceKey<CreativeModeTab> DEFENSE_TAB =
        ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.fromNamespaceAndPath(MOD_ID, "defense_series"));

    // 功能物品标签页
    public static final ResourceKey<CreativeModeTab> UTILITY_TAB =
        ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.fromNamespaceAndPath(MOD_ID, "utility_items"));

    // 属性系列标签页
    public static final ResourceKey<CreativeModeTab> ATTRIBUTE_TAB =
        ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.fromNamespaceAndPath(MOD_ID, "attribute_series"));

    // 预设附魔标签页（仅创造模式获取）
    public static final ResourceKey<CreativeModeTab> PRESET_TAB =
        ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.fromNamespaceAndPath(MOD_ID, "preset_enchants"));

    public static final CreativeModeTab CRITICAL_TAB_INSTANCE = CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
        .title(Component.translatable("itemGroup.criticalhits.critical_hits"))
        .icon(() -> new ItemStack(CRITICAL_BADGE))
        .build();

    public static final CreativeModeTab ATTACK_TAB_INSTANCE = CreativeModeTab.builder(CreativeModeTab.Row.TOP, 1)
        .title(Component.translatable("itemGroup.criticalhits.attack_series"))
        .icon(() -> new ItemStack(Items.DIAMOND_SWORD))
        .build();

    public static final CreativeModeTab DEFENSE_TAB_INSTANCE = CreativeModeTab.builder(CreativeModeTab.Row.TOP, 2)
        .title(Component.translatable("itemGroup.criticalhits.defense_series"))
        .icon(() -> new ItemStack(Items.SHIELD))
        .build();

    public static final CreativeModeTab UTILITY_TAB_INSTANCE = CreativeModeTab.builder(CreativeModeTab.Row.TOP, 3)
        .title(Component.translatable("itemGroup.criticalhits.utility_items"))
        .icon(() -> new ItemStack(CRITICAL_STATION_BLOCK))
        .build();

    public static final CreativeModeTab ATTRIBUTE_TAB_INSTANCE = CreativeModeTab.builder(CreativeModeTab.Row.TOP, 4)
        .title(Component.translatable("itemGroup.criticalhits.attribute_series"))
        .icon(() -> new ItemStack(ATTRIBUTE_BADGE))
        .build();

    // 预设附魔标签页（仅创造模式获取）
    public static final CreativeModeTab PRESET_TAB_INSTANCE = CreativeModeTab.builder(CreativeModeTab.Row.TOP, 5)
        .title(Component.translatable("itemGroup.criticalhits.preset_enchants"))
        .icon(() -> new ItemStack(Items.ENCHANTED_BOOK))
        .build();

    private static final List<ResourceKey<LootTable>> LOOT_TABLES_TO_INJECT = List.of(
        BuiltInLootTables.SIMPLE_DUNGEON,
        BuiltInLootTables.ABANDONED_MINESHAFT,
        BuiltInLootTables.NETHER_BRIDGE,
        BuiltInLootTables.DESERT_PYRAMID,
        BuiltInLootTables.JUNGLE_TEMPLE,
        BuiltInLootTables.VILLAGE_ARMORER,
        BuiltInLootTables.VILLAGE_WEAPONSMITH,
        BuiltInLootTables.VILLAGE_TOOLSMITH,
        BuiltInLootTables.VILLAGE_CARTOGRAPHER,
        BuiltInLootTables.VILLAGE_MASON,
        BuiltInLootTables.VILLAGE_FLETCHER,
        BuiltInLootTables.VILLAGE_SHEPHERD,
        BuiltInLootTables.VILLAGE_BUTCHER,
        BuiltInLootTables.STRONGHOLD_LIBRARY,
        BuiltInLootTables.STRONGHOLD_CROSSING,
        BuiltInLootTables.END_CITY_TREASURE,
        BuiltInLootTables.ANCIENT_CITY,
        BuiltInLootTables.WOODLAND_MANSION,
        BuiltInLootTables.PILLAGER_OUTPOST,
        BuiltInLootTables.BASTION_TREASURE,
        BuiltInLootTables.BASTION_OTHER,
        BuiltInLootTables.BASTION_BRIDGE,
        BuiltInLootTables.BASTION_HOGLIN_STABLE
    );

    @Override
    public void onInitialize() {
        LOGGER.info("[Modern Damage System] ========================================");
        LOGGER.info("[Modern Damage System] 现代化伤害系统 (Modern Damage System)");
        LOGGER.info("[Modern Damage System] 作者: {}", AUTHOR);
        LOGGER.info("[Modern Damage System] {}", COPYRIGHT);
        LOGGER.info("[Modern Damage System] 代码指纹: {}", CODE_FINGERPRINT);
        LOGGER.info("[Modern Damage System] 协议: 自定义源代码公开协议（禁止未经授权移植至网易中国版）");
        LOGGER.info("[Modern Damage System] ========================================");
        LOGGER.info("[Modern Damage System] 初始化中...");

        // 加载配置
        CriticalHitsConfig.load();

        // 注册Cloth Config配置（由Configured自动生成配置界面）
        try {
            me.shedaniel.autoconfig.AutoConfig.register(MdsClothConfig.class,
                me.shedaniel.autoconfig.serializer.GsonConfigSerializer::new);
            // 同步Cloth Config配置到旧配置
            MdsClothConfig clothConfig = me.shedaniel.autoconfig.AutoConfig
                .getConfigHolder(MdsClothConfig.class).getConfig();
            clothConfig.syncToLegacyConfig();
            CriticalHitsConfig.save();
            LOGGER.info("[Modern Damage System] Cloth Config配置已注册");
        } catch (Exception e) {
            LOGGER.warn("[Modern Damage System] Cloth Config注册失败，使用旧配置系统", e);
        }

        // 兼容性检查
        checkCompatibility();

        register(Registries.ITEM, "critical_coin", CRITICAL_COIN);
        register(Registries.ITEM, "critical_coin_shard", CRITICAL_COIN_SHARD);
        register(Registries.ITEM, "critical_badge", CRITICAL_BADGE);
        register(Registries.ITEM, "attribute_badge", ATTRIBUTE_BADGE);
        register(Registries.ITEM, "mds_recipe_book", RECIPE_BOOK);
        register(Registries.BLOCK, "critical_station", CRITICAL_STATION_BLOCK);
        register(Registries.ITEM, "critical_station", new net.minecraft.world.item.BlockItem(
            CRITICAL_STATION_BLOCK, new Item.Properties()
                .setId(ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MOD_ID, "critical_station")))));
        register(Registries.BLOCK_ENTITY_TYPE, "critical_station", CRITICAL_STATION_BLOCK_ENTITY);
        register(Registries.MENU, "critical_station", CRITICAL_STATION_MENU);
        register(Registries.MENU, "mds_recipe_book", RECIPE_BOOK_MENU);
        register(Registries.CREATIVE_MODE_TAB, "critical_hits", CRITICAL_TAB_INSTANCE);
        register(Registries.CREATIVE_MODE_TAB, "attack_series", ATTACK_TAB_INSTANCE);
        register(Registries.CREATIVE_MODE_TAB, "defense_series", DEFENSE_TAB_INSTANCE);
        register(Registries.CREATIVE_MODE_TAB, "utility_items", UTILITY_TAB_INSTANCE);
        register(Registries.CREATIVE_MODE_TAB, "attribute_series", ATTRIBUTE_TAB_INSTANCE);
        register(Registries.CREATIVE_MODE_TAB, "preset_enchants", PRESET_TAB_INSTANCE);
        register(Registries.MOB_EFFECT, "vulnerability", VULNERABILITY_EFFECT);
        register(Registries.MOB_EFFECT, "armor_break", ARMOR_BREAK_EFFECT);
        // 注册后创建易伤药水（1/2/3级，持续60秒）
        net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effectHolder =
            BuiltInRegistries.MOB_EFFECT.wrapAsHolder(VULNERABILITY_EFFECT);
        VULNERABILITY_POTION = new net.minecraft.world.item.alchemy.Potion(
            "vulnerability",
            new net.minecraft.world.effect.MobEffectInstance(effectHolder, 3600, 0));
        VULNERABILITY_POTION_2 = new net.minecraft.world.item.alchemy.Potion(
            "vulnerability_2",
            new net.minecraft.world.effect.MobEffectInstance(effectHolder, 3600, 1));
        VULNERABILITY_POTION_3 = new net.minecraft.world.item.alchemy.Potion(
            "vulnerability_3",
            new net.minecraft.world.effect.MobEffectInstance(effectHolder, 3600, 2));
        register(Registries.POTION, "vulnerability", VULNERABILITY_POTION);
        register(Registries.POTION, "vulnerability_2", VULNERABILITY_POTION_2);
        register(Registries.POTION, "vulnerability_3", VULNERABILITY_POTION_3);

        // v2.0.0 冰霜药水（用原版缓慢效果，1/2/3级，持续60秒）
        var slowHolder = BuiltInRegistries.MOB_EFFECT.get(
            net.minecraft.resources.Identifier.fromNamespaceAndPath("minecraft", "slowness")).orElse(null);
        if (slowHolder != null) {
            FROST_POTION = new net.minecraft.world.item.alchemy.Potion(
                "frost",
                new net.minecraft.world.effect.MobEffectInstance(
                    (net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect>) slowHolder, 3600, 1));
            FROST_POTION_2 = new net.minecraft.world.item.alchemy.Potion(
                "frost_2",
                new net.minecraft.world.effect.MobEffectInstance(
                    (net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect>) slowHolder, 3600, 3));
            FROST_POTION_3 = new net.minecraft.world.item.alchemy.Potion(
                "frost_3",
                new net.minecraft.world.effect.MobEffectInstance(
                    (net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect>) slowHolder, 3600, 4));
            register(Registries.POTION, "frost", FROST_POTION);
            register(Registries.POTION, "frost_2", FROST_POTION_2);
            register(Registries.POTION, "frost_3", FROST_POTION_3);
        }

        // v2.0.0 中毒药水（增强版，用原版中毒效果，1/2/3级，持续60秒）
        var poisonHolder = BuiltInRegistries.MOB_EFFECT.get(
            net.minecraft.resources.Identifier.fromNamespaceAndPath("minecraft", "poison")).orElse(null);
        if (poisonHolder != null) {
            POISON_PLUS_POTION = new net.minecraft.world.item.alchemy.Potion(
                "poison_plus",
                new net.minecraft.world.effect.MobEffectInstance(
                    (net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect>) poisonHolder, 3600, 0));
            POISON_PLUS_POTION_2 = new net.minecraft.world.item.alchemy.Potion(
                "poison_plus_2",
                new net.minecraft.world.effect.MobEffectInstance(
                    (net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect>) poisonHolder, 3600, 1));
            POISON_PLUS_POTION_3 = new net.minecraft.world.item.alchemy.Potion(
                "poison_plus_3",
                new net.minecraft.world.effect.MobEffectInstance(
                    (net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect>) poisonHolder, 3600, 2));
            register(Registries.POTION, "poison_plus", POISON_PLUS_POTION);
            register(Registries.POTION, "poison_plus_2", POISON_PLUS_POTION_2);
            register(Registries.POTION, "poison_plus_3", POISON_PLUS_POTION_3);
        }

        // v2.0.0 破甲药水（1/2/3级，持续60秒）
        var armorBreakHolder = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ARMOR_BREAK_EFFECT);
        ARMOR_BREAK_POTION = new net.minecraft.world.item.alchemy.Potion(
            "armor_break",
            new net.minecraft.world.effect.MobEffectInstance(armorBreakHolder, 3600, 0));
        ARMOR_BREAK_POTION_2 = new net.minecraft.world.item.alchemy.Potion(
            "armor_break_2",
            new net.minecraft.world.effect.MobEffectInstance(armorBreakHolder, 3600, 1));
        ARMOR_BREAK_POTION_3 = new net.minecraft.world.item.alchemy.Potion(
            "armor_break_3",
            new net.minecraft.world.effect.MobEffectInstance(armorBreakHolder, 3600, 2));
        register(Registries.POTION, "armor_break", ARMOR_BREAK_POTION);
        register(Registries.POTION, "armor_break_2", ARMOR_BREAK_POTION_2);
        register(Registries.POTION, "armor_break_3", ARMOR_BREAK_POTION_3);

        // 暴击系列标签页填充
        CreativeModeTabEvents.modifyOutputEvent(CRITICAL_TAB).register(output -> {
            int count = 0;
            for (int i = 1; i <= 20; i++) {
                ItemStack book = makeEnchantedBook(CriticalHitsEnchantments.CRITICAL_RATE_ID, i);
                if (book != null) { output.accept(book); count++; }
            }
            for (int i = 1; i <= 20; i++) {
                ItemStack book = makeEnchantedBook(CriticalHitsEnchantments.CRITICAL_DAMAGE_ID, i);
                if (book != null) { output.accept(book); count++; }
            }
            for (int i = 1; i <= 20; i++) {
                ItemStack book = makeEnchantedBook(CriticalHitsEnchantments.CRITICAL_RESISTANCE_ID, i);
                if (book != null) { output.accept(book); count++; }
            }
            LOGGER.info("[Modern Damage System] 暴击系列标签填充完成，共" + count + "本");
        });

        // 攻击系列标签页填充
        CreativeModeTabEvents.modifyOutputEvent(ATTACK_TAB).register(output -> {
            int count = 0;
            for (int i = 1; i <= 20; i++) {
                ItemStack book = makeEnchantedBook(CriticalHitsEnchantments.PERCENTAGE_DAMAGE_ID, i);
                if (book != null) { output.accept(book); count++; }
            }
            for (int i = 1; i <= 20; i++) {
                ItemStack book = makeEnchantedBook(CriticalHitsEnchantments.ARMOR_PENETRATION_ID, i);
                if (book != null) { output.accept(book); count++; }
            }
            for (int i = 1; i <= 10; i++) {
                ItemStack book = makeEnchantedBook(CriticalHitsEnchantments.LIFE_STEAL_ID, i);
                if (book != null) { output.accept(book); count++; }
            }
            for (int i = 1; i <= 10; i++) {
                ItemStack book = makeEnchantedBook(CriticalHitsEnchantments.VULNERABILITY_ID, i);
                if (book != null) { output.accept(book); count++; }
            }
            // v2.0.0 新增攻击附魔
            for (int i = 1; i <= 10; i++) {
                ItemStack book = makeEnchantedBook(CriticalHitsEnchantments.TRUE_DAMAGE_ID, i);
                if (book != null) { output.accept(book); count++; }
            }
            for (int i = 1; i <= 5; i++) {
                ItemStack book = makeEnchantedBook(CriticalHitsEnchantments.SPLASH_DAMAGE_ID, i);
                if (book != null) { output.accept(book); count++; }
            }
            for (int i = 1; i <= 3; i++) {
                ItemStack book = makeEnchantedBook(CriticalHitsEnchantments.EXECUTE_ID, i);
                if (book != null) { output.accept(book); count++; }
            }
            for (int i = 1; i <= 10; i++) {
                ItemStack book = makeEnchantedBook(CriticalHitsEnchantments.FROST_ID, i);
                if (book != null) { output.accept(book); count++; }
            }
            for (int i = 1; i <= 10; i++) {
                ItemStack book = makeEnchantedBook(CriticalHitsEnchantments.POISON_ID, i);
                if (book != null) { output.accept(book); count++; }
            }
            for (int i = 1; i <= 10; i++) {
                ItemStack book = makeEnchantedBook(CriticalHitsEnchantments.ARMOR_SHATTER_ID, i);
                if (book != null) { output.accept(book); count++; }
            }
            LOGGER.info("[Modern Damage System] 攻击系列标签填充完成，共" + count + "本");
        });

        // 防御系列标签页填充
        CreativeModeTabEvents.modifyOutputEvent(DEFENSE_TAB).register(output -> {
            int count = 0;
            for (int i = 1; i <= 20; i++) {
                ItemStack book = makeEnchantedBook(CriticalHitsEnchantments.DAMAGE_REDUCTION_ID, i);
                if (book != null) { output.accept(book); count++; }
            }
            for (int i = 1; i <= 20; i++) {
                ItemStack book = makeEnchantedBook(CriticalHitsEnchantments.ARMOR_DEFENSE_ID, i);
                if (book != null) { output.accept(book); count++; }
            }
            for (int i = 1; i <= 10; i++) {
                ItemStack book = makeEnchantedBook(CriticalHitsEnchantments.MAX_HEALTH_ID, i);
                if (book != null) { output.accept(book); count++; }
            }
            // v2.0.0 新增防御附魔
            for (int i = 1; i <= 10; i++) {
                ItemStack book = makeEnchantedBook(CriticalHitsEnchantments.THORNS_REFLECT_ID, i);
                if (book != null) { output.accept(book); count++; }
            }
            for (int i = 1; i <= 3; i++) {
                ItemStack book = makeEnchantedBook(CriticalHitsEnchantments.SHIELD_ID, i);
                if (book != null) { output.accept(book); count++; }
            }
            for (int i = 1; i <= 10; i++) {
                ItemStack book = makeEnchantedBook(CriticalHitsEnchantments.KNOCKBACK_RESISTANCE_ID, i);
                if (book != null) { output.accept(book); count++; }
            }
            LOGGER.info("[Modern Damage System] 防御系列标签填充完成，共" + count + "本");
        });

        // 功能物品标签页填充
        CreativeModeTabEvents.modifyOutputEvent(UTILITY_TAB).register(output -> {
            output.accept(CRITICAL_STATION_BLOCK);
            output.accept(CRITICAL_COIN);
            output.accept(CRITICAL_COIN_SHARD);
            output.accept(RECIPE_BOOK);
            // 易伤药水（1/2/3级，普通/喷溅/滞留）
            net.minecraft.world.item.alchemy.Potion[] vulnerabilityPotions = {
                VULNERABILITY_POTION, VULNERABILITY_POTION_2, VULNERABILITY_POTION_3
            };
            for (net.minecraft.world.item.alchemy.Potion potion : vulnerabilityPotions) {
                var potionHolder = BuiltInRegistries.POTION.wrapAsHolder(potion);
                var potionContents = new net.minecraft.world.item.alchemy.PotionContents(
                    java.util.Optional.of(potionHolder),
                    java.util.Optional.empty(),
                    java.util.List.of(),
                    java.util.Optional.empty()
                );
                // 普通药水
                ItemStack normalPotion = new ItemStack(Items.POTION);
                normalPotion.set(net.minecraft.core.component.DataComponents.POTION_CONTENTS, potionContents);
                output.accept(normalPotion);
                // 喷溅药水
                ItemStack splashPotion = new ItemStack(Items.SPLASH_POTION);
                splashPotion.set(net.minecraft.core.component.DataComponents.POTION_CONTENTS, potionContents);
                output.accept(splashPotion);
                // 滞留药水
                ItemStack lingeringPotion = new ItemStack(Items.LINGERING_POTION);
                lingeringPotion.set(net.minecraft.core.component.DataComponents.POTION_CONTENTS, potionContents);
                output.accept(lingeringPotion);
            }

            // v2.0.0 冰霜药水（1/2/3级）
            if (FROST_POTION != null) {
                net.minecraft.world.item.alchemy.Potion[] frostPotions = {
                    FROST_POTION, FROST_POTION_2, FROST_POTION_3
                };
                for (net.minecraft.world.item.alchemy.Potion potion : frostPotions) {
                    if (potion == null) continue;
                    var potionHolder = BuiltInRegistries.POTION.wrapAsHolder(potion);
                    var potionContents = new net.minecraft.world.item.alchemy.PotionContents(
                        java.util.Optional.of(potionHolder),
                        java.util.Optional.empty(),
                        java.util.List.of(),
                        java.util.Optional.empty()
                    );
                    ItemStack normalPotion = new ItemStack(Items.POTION);
                    normalPotion.set(net.minecraft.core.component.DataComponents.POTION_CONTENTS, potionContents);
                    output.accept(normalPotion);
                    ItemStack splashPotion = new ItemStack(Items.SPLASH_POTION);
                    splashPotion.set(net.minecraft.core.component.DataComponents.POTION_CONTENTS, potionContents);
                    output.accept(splashPotion);
                    ItemStack lingeringPotion = new ItemStack(Items.LINGERING_POTION);
                    lingeringPotion.set(net.minecraft.core.component.DataComponents.POTION_CONTENTS, potionContents);
                    output.accept(lingeringPotion);
                }
            }

            // v2.0.0 中毒药水（增强版，1/2/3级）
            if (POISON_PLUS_POTION != null) {
                net.minecraft.world.item.alchemy.Potion[] poisonPotions = {
                    POISON_PLUS_POTION, POISON_PLUS_POTION_2, POISON_PLUS_POTION_3
                };
                for (net.minecraft.world.item.alchemy.Potion potion : poisonPotions) {
                    if (potion == null) continue;
                    var potionHolder = BuiltInRegistries.POTION.wrapAsHolder(potion);
                    var potionContents = new net.minecraft.world.item.alchemy.PotionContents(
                        java.util.Optional.of(potionHolder),
                        java.util.Optional.empty(),
                        java.util.List.of(),
                        java.util.Optional.empty()
                    );
                    ItemStack normalPotion = new ItemStack(Items.POTION);
                    normalPotion.set(net.minecraft.core.component.DataComponents.POTION_CONTENTS, potionContents);
                    output.accept(normalPotion);
                    ItemStack splashPotion = new ItemStack(Items.SPLASH_POTION);
                    splashPotion.set(net.minecraft.core.component.DataComponents.POTION_CONTENTS, potionContents);
                    output.accept(splashPotion);
                    ItemStack lingeringPotion = new ItemStack(Items.LINGERING_POTION);
                    lingeringPotion.set(net.minecraft.core.component.DataComponents.POTION_CONTENTS, potionContents);
                    output.accept(lingeringPotion);
                }
            }

            // v2.0.0 破甲药水（1/2/3级）
            if (ARMOR_BREAK_POTION != null) {
                net.minecraft.world.item.alchemy.Potion[] armorBreakPotions = {
                    ARMOR_BREAK_POTION, ARMOR_BREAK_POTION_2, ARMOR_BREAK_POTION_3
                };
                for (net.minecraft.world.item.alchemy.Potion potion : armorBreakPotions) {
                    if (potion == null) continue;
                    var potionHolder = BuiltInRegistries.POTION.wrapAsHolder(potion);
                    var potionContents = new net.minecraft.world.item.alchemy.PotionContents(
                        java.util.Optional.of(potionHolder),
                        java.util.Optional.empty(),
                        java.util.List.of(),
                        java.util.Optional.empty()
                    );
                    ItemStack normalPotion = new ItemStack(Items.POTION);
                    normalPotion.set(net.minecraft.core.component.DataComponents.POTION_CONTENTS, potionContents);
                    output.accept(normalPotion);
                    ItemStack splashPotion = new ItemStack(Items.SPLASH_POTION);
                    splashPotion.set(net.minecraft.core.component.DataComponents.POTION_CONTENTS, potionContents);
                    output.accept(splashPotion);
                    ItemStack lingeringPotion = new ItemStack(Items.LINGERING_POTION);
                    lingeringPotion.set(net.minecraft.core.component.DataComponents.POTION_CONTENTS, potionContents);
                    output.accept(lingeringPotion);
                }
            }
            LOGGER.info("[Modern Damage System] 功能物品标签填充完成");
        });

        // 属性系列标签页填充
        CreativeModeTabEvents.modifyOutputEvent(ATTRIBUTE_TAB).register(output -> {
            int count = 0;
            // 攻击速度（最高15级）
            for (int i = 1; i <= 15; i++) {
                ItemStack book = makeEnchantedBook(CriticalHitsEnchantments.ATTACK_SPEED_ID, i);
                if (book != null) { output.accept(book); count++; }
            }
            // 攻击速度负等级（最高10级）
            for (int i = 1; i <= 10; i++) {
                ItemStack book = makeEnchantedBook(CriticalHitsEnchantments.ATTACK_SPEED_CURSE_ID, i);
                if (book != null) { output.accept(book); count++; }
            }
            // 移动速度（最高10级）
            for (int i = 1; i <= 10; i++) {
                ItemStack book = makeEnchantedBook(CriticalHitsEnchantments.MOVEMENT_SPEED_ID, i);
                if (book != null) { output.accept(book); count++; }
            }
            // 移动速度负等级（最高10级）
            for (int i = 1; i <= 10; i++) {
                ItemStack book = makeEnchantedBook(CriticalHitsEnchantments.MOVEMENT_SPEED_CURSE_ID, i);
                if (book != null) { output.accept(book); count++; }
            }
            // 攻击范围（最高10级）
            for (int i = 1; i <= 10; i++) {
                ItemStack book = makeEnchantedBook(CriticalHitsEnchantments.ATTACK_RANGE_ID, i);
                if (book != null) { output.accept(book); count++; }
            }
            // 攻击范围负等级（最高10级）
            for (int i = 1; i <= 10; i++) {
                ItemStack book = makeEnchantedBook(CriticalHitsEnchantments.ATTACK_RANGE_CURSE_ID, i);
                if (book != null) { output.accept(book); count++; }
            }
            // 额外伤害（最高20级）
            for (int i = 1; i <= 20; i++) {
                ItemStack book = makeEnchantedBook(CriticalHitsEnchantments.BONUS_DAMAGE_ID, i);
                if (book != null) { output.accept(book); count++; }
            }
            LOGGER.info("[Modern Damage System] 属性系列标签填充完成，共" + count + "本");
        });

        // 预设附魔标签页填充（仅创造模式获取）
        CreativeModeTabEvents.modifyOutputEvent(PRESET_TAB).register(output -> {
            int count = 0;
            java.util.List<ItemStack> presets = generateAllPresetBooks();
            for (ItemStack preset : presets) {
                output.accept(preset);
                count++;
            }
            LOGGER.info("[Modern Damage System] 预设附魔标签填充完成，共" + count + "本");
        });

        // 战利品注入
        LootTableEvents.MODIFY.register((key, tableBuilder, source, registries) -> {
            String keyStr = key.toString();
            boolean debug = CriticalHitsConfig.get().debug_mode;

            if (debug) {
                LOGGER.info("[MDS Loot] 处理: " + keyStr + " (source: " + source + ")");
            }

            boolean shouldInject = false;

            // 1. 检查是否在原版硬编码列表中
            if (LOOT_TABLES_TO_INJECT.contains(key)) {
                shouldInject = true;
                if (debug) LOGGER.info("[MDS Loot] 命中原版列表: " + keyStr);
            }

            // 2. 检查是否在额外白名单中
            for (String extraId : CriticalHitsConfig.get().additional_loot_tables) {
                if (keyStr.equals(extraId)) {
                    shouldInject = true;
                    if (debug) LOGGER.info("[MDS Loot] 命中额外白名单: " + keyStr);
                    break;
                }
            }

            // 3. 自动注入所有箱子（如果开启）
            if (CriticalHitsConfig.get().auto_inject_all_chests && !shouldInject) {
                String path = keyStr.toLowerCase();
                // 只注入chests/路径下的loot table（26.2版本路径是chests/）
                // 排除出生点奖励箱等特殊箱子
                if (path.contains("/chests/") && !path.contains("spawn_bonus_chest")) {
                    shouldInject = true;
                    if (debug) LOGGER.info("[MDS Loot] 命中自动检测(chests): " + keyStr);
                }
            }

            // 4. 检查黑名单
            for (String blackId : CriticalHitsConfig.get().loot_table_blacklist) {
                if (keyStr.equals(blackId)) {
                    shouldInject = false;
                    if (debug) LOGGER.info("[MDS Loot] 命中黑名单，排除: " + keyStr);
                    break;
                }
            }

            if (shouldInject) {
                injectCriticalBooks(tableBuilder, registries);
                injectCoinShards(tableBuilder);
                LOGGER.info("[MDS Loot] 注入成功: " + keyStr);
            }
        });

        // 注册命令
        registerCommands();

        LOGGER.info("[Modern Damage System] 初始化完成。作者: Juzipi_AN");
    }

    /**
     * 注册游戏内命令。
     * /mds reload - 重新加载配置文件
     * /mds info - 显示mod信息
     */
    private static void registerCommands() {
        net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            MdsCommand.register(dispatcher);
            // 额外注册 /mds info 命令
            dispatcher.register(net.minecraft.commands.Commands.literal("mds")
                .then(net.minecraft.commands.Commands.literal("info")
                    .executes(context -> {
                        context.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal("§b=== Modern Damage System ==="), false);
                        context.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal("§7作者: §f" + AUTHOR), false);
                        context.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal("§7代码指纹: §f" + CODE_FINGERPRINT), false);
                        context.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal("§7协议: §f自定义源代码公开协议"), false);
                        context.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal("§7配置文件: §fconfig/modern-damage-system.json"), false);
                        context.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal("§7命令: §f/mds help (显示帮助), /mds info (显示信息)"), false);
                        return 1;
                    }))
            );
        });
    }

    public static ItemStack makeEnchantedBook(Identifier enchantmentId, int level) {
        net.minecraft.core.Holder<Enchantment> holder = getEnchantmentHolder(enchantmentId);
        if (holder == null) {
            LOGGER.warn("[Modern Damage System] Enchantment holder not found: " + enchantmentId);
            return null;
        }
        ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
        ItemEnchantments.Mutable enchants = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        enchants.set(holder, level);
        book.set(DataComponents.STORED_ENCHANTMENTS, enchants.toImmutable());
        return book;
    }

    /**
     * 生成预设附魔书（仅创造模式获取）
     * @param name 书名
     * @param color 颜色代码（如§6、§d等）
     * @param starLevel 星级（1-5）
     * @param enchantments 附魔列表（附魔ID -> 等级）
     * @param description 定位描述
     * @param scenario 适用场景
     */
    public static ItemStack makePresetEnchantedBook(String name, String color, int starLevel,
        java.util.Map<Identifier, Integer> enchantments, String description, String scenario) {
        ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);

        // 设置自定义名字（带星级颜色和星级显示）
        StringBuilder starStr = new StringBuilder();
        for (int i = 0; i < starLevel; i++) starStr.append("★");
        String fullName = color + name + " " + starStr;
        book.set(DataComponents.CUSTOM_NAME, Component.literal(fullName));

        // 设置附魔
        ItemEnchantments.Mutable enchants = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        for (var entry : enchantments.entrySet()) {
            net.minecraft.core.Holder<Enchantment> holder = getEnchantmentHolder(entry.getKey());
            if (holder != null) {
                enchants.set(holder, entry.getValue());
            } else {
                LOGGER.warn("[Modern Damage System] Preset enchantment holder not found: " + entry.getKey());
            }
        }
        book.set(DataComponents.STORED_ENCHANTMENTS, enchants.toImmutable());

        return book;
    }

    @SuppressWarnings("unchecked")
    public static net.minecraft.core.Holder<Enchantment> getEnchantmentHolder(Identifier id) {
        try {
            // 26.2中附魔是数据驱动的，不在BuiltInRegistries.REGISTRY中
            // 需要从客户端动态注册表获取
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc != null && mc.getConnection() != null) {
                net.minecraft.core.RegistryAccess registryAccess = mc.getConnection().registryAccess();
                java.util.Optional<? extends net.minecraft.core.HolderLookup<Enchantment>> lookup = registryAccess.lookup(Registries.ENCHANTMENT);
                if (lookup.isPresent()) {
                    ResourceKey<Enchantment> key = ResourceKey.create(Registries.ENCHANTMENT, id);
                    java.util.Optional<net.minecraft.core.Holder.Reference<Enchantment>> holder = lookup.get().get(key);
                    if (holder.isPresent()) {
                        return holder.get();
                    }
                }
            }
            LOGGER.warn("[Modern Damage System] 客户端动态注册表中未找到附魔: " + id);
            return null;
        } catch (Exception e) {
            LOGGER.warn("[Modern Damage System] Failed to get enchantment holder: " + id, e);
            return null;
        }
    }

    /**
     * 生成所有预设附魔书（仅创造模式获取）
     * 包含武器类17本 + 装备类12本 + 新手类8本 = 37本
     */
    public static java.util.List<ItemStack> generateAllPresetBooks() {
        java.util.List<ItemStack> presets = new java.util.ArrayList<>();
        Identifier mc = Identifier.fromNamespaceAndPath("minecraft", "");

        // ========== 武器类5星（传说）- 4本 ==========
        // 破晓
        presets.add(makePresetEnchantedBook("破晓", "§6", 5,
            java.util.Map.of(
                CriticalHitsEnchantments.CRITICAL_RATE_ID, 20,
                CriticalHitsEnchantments.CRITICAL_DAMAGE_ID, 20
            ),
            "一击破晓，暴击即斩杀",
            "满暴击爆发输出，适用于快速击杀低防御目标"));

        // 炎狱
        presets.add(makePresetEnchantedBook("炎狱", "§6", 5,
            java.util.Map.of(
                CriticalHitsEnchantments.PERCENTAGE_DAMAGE_ID, 20,
                CriticalHitsEnchantments.BONUS_DAMAGE_ID, 20
            ),
            "炎狱焚天，万物灰烬",
            "纯增伤输出，适用于稳定高伤害输出"));

        // 无极
        presets.add(makePresetEnchantedBook("无极", "§6", 5,
            java.util.Map.of(
                CriticalHitsEnchantments.CRITICAL_RATE_ID, 10,
                CriticalHitsEnchantments.CRITICAL_DAMAGE_ID, 10,
                CriticalHitsEnchantments.PERCENTAGE_DAMAGE_ID, 10,
                CriticalHitsEnchantments.ARMOR_PENETRATION_ID, 10,
                CriticalHitsEnchantments.LIFE_STEAL_ID, 5,
                Identifier.fromNamespaceAndPath("minecraft", "sharpness"), 2
            ),
            "无极之道，万法归一",
            "全属性均衡，适用于多种战斗场景灵活切换"));

        // 雷霆
        presets.add(makePresetEnchantedBook("雷霆", "§6", 5,
            java.util.Map.of(
                CriticalHitsEnchantments.CRITICAL_RATE_ID, 15,
                CriticalHitsEnchantments.CRITICAL_DAMAGE_ID, 15,
                CriticalHitsEnchantments.PERCENTAGE_DAMAGE_ID, 10
            ),
            "雷霆一击，天崩地裂",
            "高暴击高爆伤，适用于瞬间爆发击杀目标"));

        // ========== 武器类4星（史诗）- 7本 ==========
        // 狂潮
        presets.add(makePresetEnchantedBook("狂潮", "§d", 4,
            java.util.Map.of(
                CriticalHitsEnchantments.ATTACK_SPEED_ID, 15,
                CriticalHitsEnchantments.PERCENTAGE_DAMAGE_ID, 10,
                Identifier.fromNamespaceAndPath("minecraft", "sharpness"), 3
            ),
            "狂潮涌至，连击不息",
            "高攻速持续输出，适用于长时间消耗战"));

        // 破军
        presets.add(makePresetEnchantedBook("破军", "§d", 4,
            java.util.Map.of(
                CriticalHitsEnchantments.ARMOR_PENETRATION_ID, 20,
                CriticalHitsEnchantments.PERCENTAGE_DAMAGE_ID, 10
            ),
            "破甲破军，无视防御",
            "高护甲穿透，适用于对抗高护甲/高减伤目标"));

        // 噬血
        presets.add(makePresetEnchantedBook("噬血", "§d", 4,
            java.util.Map.of(
                CriticalHitsEnchantments.LIFE_STEAL_ID, 10,
                CriticalHitsEnchantments.PERCENTAGE_DAMAGE_ID, 10,
                Identifier.fromNamespaceAndPath("minecraft", "sharpness"), 3
            ),
            "噬血而生，越战越勇",
            "高生命吸取，适用于续航拉扯与持久战"));

        // 蛊毒
        presets.add(makePresetEnchantedBook("蛊毒", "§d", 4,
            java.util.Map.of(
                CriticalHitsEnchantments.VULNERABILITY_ID, 10,
                CriticalHitsEnchantments.CRITICAL_RATE_ID, 10,
                CriticalHitsEnchantments.CRITICAL_DAMAGE_ID, 10
            ),
            "下蛊施毒，万物皆殇",
            "易伤debuff施加，适用于团队辅助与目标压制"));

        // 影袭
        presets.add(makePresetEnchantedBook("影袭", "§d", 4,
            java.util.Map.of(
                CriticalHitsEnchantments.CRITICAL_RATE_ID, 15,
                CriticalHitsEnchantments.CRITICAL_DAMAGE_ID, 10,
                CriticalHitsEnchantments.ATTACK_SPEED_ID, 10,
                Identifier.fromNamespaceAndPath("minecraft", "sharpness"), 3
            ),
            "影袭瞬杀，快如闪电",
            "暴击与攻速结合，适用于灵活机动的爆发输出"));

        // 重刃
        presets.add(makePresetEnchantedBook("重刃", "§d", 4,
            java.util.Map.of(
                CriticalHitsEnchantments.ATTACK_SPEED_CURSE_ID, 10,
                CriticalHitsEnchantments.PERCENTAGE_DAMAGE_ID, 20,
                CriticalHitsEnchantments.CRITICAL_DAMAGE_ID, 10,
                Identifier.fromNamespaceAndPath("minecraft", "sharpness"), 4
            ),
            "重刃缓击，一击必杀",
            "负攻速高伤害，适用于慢速高爆发的重击流"));

        // 迟缓
        presets.add(makePresetEnchantedBook("迟缓", "§d", 4,
            java.util.Map.of(
                CriticalHitsEnchantments.ATTACK_SPEED_CURSE_ID, 5,
                CriticalHitsEnchantments.MOVEMENT_SPEED_CURSE_ID, 5,
                CriticalHitsEnchantments.PERCENTAGE_DAMAGE_ID, 15,
                CriticalHitsEnchantments.CRITICAL_DAMAGE_ID, 10,
                Identifier.fromNamespaceAndPath("minecraft", "sharpness"), 4
            ),
            "迟缓重击，以静制动",
            "双负属性高伤，适用于站桩稳定输出的战术"));

        // ========== 武器类3星（稀有）- 6本 ==========
        // 苍穹
        presets.add(makePresetEnchantedBook("苍穹", "§b", 3,
            java.util.Map.of(
                CriticalHitsEnchantments.ATTACK_RANGE_ID, 10,
                CriticalHitsEnchantments.ATTACK_SPEED_ID, 5,
                Identifier.fromNamespaceAndPath("minecraft", "sharpness"), 2
            ),
            "苍穹之下，皆为猎物",
            "长攻击范围，适用于远程拉扯与距离控制"));

        // 驱魔
        presets.add(makePresetEnchantedBook("驱魔", "§b", 3,
            java.util.Map.of(
                Identifier.fromNamespaceAndPath("minecraft", "smite"), 5,
                CriticalHitsEnchantments.PERCENTAGE_DAMAGE_ID, 15,
                CriticalHitsEnchantments.CRITICAL_RATE_ID, 10
            ),
            "驱魔斩魂，亡灵克星",
            "亡灵生物特化，适用于下界/要塞等亡灵密集区域"));

        // 灭虫
        presets.add(makePresetEnchantedBook("灭虫", "§b", 3,
            java.util.Map.of(
                Identifier.fromNamespaceAndPath("minecraft", "bane_of_arthropods"), 5,
                CriticalHitsEnchantments.PERCENTAGE_DAMAGE_ID, 15,
                CriticalHitsEnchantments.CRITICAL_RATE_ID, 10
            ),
            "灭虫除害，节肢克星",
            "节肢生物特化，适用于蜘蛛/洞穴蜘蛛等节肢生物战斗"));

        // 烈焰
        presets.add(makePresetEnchantedBook("烈焰", "§b", 3,
            java.util.Map.of(
                Identifier.fromNamespaceAndPath("minecraft", "fire_aspect"), 2,
                CriticalHitsEnchantments.PERCENTAGE_DAMAGE_ID, 15,
                CriticalHitsEnchantments.CRITICAL_RATE_ID, 10,
                Identifier.fromNamespaceAndPath("minecraft", "sharpness"), 2
            ),
            "烈焰焚身，持续灼烧",
            "火焰伤害流，适用于需要持续灼烧输出的场景"));

        // 震退
        presets.add(makePresetEnchantedBook("震退", "§b", 3,
            java.util.Map.of(
                Identifier.fromNamespaceAndPath("minecraft", "knockback"), 2,
                CriticalHitsEnchantments.ATTACK_RANGE_ID, 10,
                CriticalHitsEnchantments.PERCENTAGE_DAMAGE_ID, 10
            ),
            "震退千军，控场大师",
            "击退控制流，适用于群体战斗与距离控制"));

        // 短刃
        presets.add(makePresetEnchantedBook("短刃", "§b", 3,
            java.util.Map.of(
                CriticalHitsEnchantments.ATTACK_RANGE_CURSE_ID, 10,
                CriticalHitsEnchantments.ATTACK_SPEED_ID, 10,
                CriticalHitsEnchantments.CRITICAL_RATE_ID, 10,
                Identifier.fromNamespaceAndPath("minecraft", "sharpness"), 3
            ),
            "短刃疾刺，快如疾风",
            "负范围高攻速，适用于近身快速攻击的快攻流"));

        // ========== 装备类4星（史诗）- 2本 ==========
        // 磐石
        presets.add(makePresetEnchantedBook("磐石", "§d", 4,
            java.util.Map.of(
                CriticalHitsEnchantments.DAMAGE_REDUCTION_ID, 5,
                CriticalHitsEnchantments.ARMOR_DEFENSE_ID, 5,
                CriticalHitsEnchantments.CRITICAL_RESISTANCE_ID, 5,
                Identifier.fromNamespaceAndPath("minecraft", "protection"), 4
            ),
            "坚如磐石，不动如山",
            "高减伤高韧性，适用于正面承伤与防御反击"));

        // 永恒
        presets.add(makePresetEnchantedBook("永恒", "§d", 4,
            java.util.Map.of(
                Identifier.fromNamespaceAndPath("minecraft", "mending"), 1,
                Identifier.fromNamespaceAndPath("minecraft", "unbreaking"), 3,
                Identifier.fromNamespaceAndPath("minecraft", "protection"), 4,
                CriticalHitsEnchantments.DAMAGE_REDUCTION_ID, 3,
                CriticalHitsEnchantments.ARMOR_DEFENSE_ID, 2
            ),
            "永恒不朽，经久耐用",
            "生存长期使用，适用于日常探索/长期装备维护"));

        // ========== 装备类3星（稀有）- 7本 ==========
        // 不灭
        presets.add(makePresetEnchantedBook("不灭", "§b", 3,
            java.util.Map.of(
                CriticalHitsEnchantments.MAX_HEALTH_ID, 3,
                CriticalHitsEnchantments.DAMAGE_REDUCTION_ID, 3,
                CriticalHitsEnchantments.ARMOR_DEFENSE_ID, 3,
                Identifier.fromNamespaceAndPath("minecraft", "protection"), 4
            ),
            "不灭之躯，永生不死",
            "高生命值上限，适用于高血量生存与容错"));

        // 金刚
        presets.add(makePresetEnchantedBook("金刚", "§b", 3,
            java.util.Map.of(
                CriticalHitsEnchantments.DAMAGE_REDUCTION_ID, 5,
                CriticalHitsEnchantments.CRITICAL_RESISTANCE_ID, 5,
                Identifier.fromNamespaceAndPath("minecraft", "protection"), 4
            ),
            "金刚不坏，万法不侵",
            "高减伤高暴击减免，适用于对抗暴击爆发型输出"));

        // 涅槃
        presets.add(makePresetEnchantedBook("涅槃", "§b", 3,
            java.util.Map.of(
                CriticalHitsEnchantments.MAX_HEALTH_ID, 2,
                CriticalHitsEnchantments.DAMAGE_REDUCTION_ID, 4,
                CriticalHitsEnchantments.ARMOR_DEFENSE_ID, 3,
                Identifier.fromNamespaceAndPath("minecraft", "protection"), 4
            ),
            "浴火涅槃，死而复生",
            "血量与防御均衡，适用于综合生存能力提升"));

        // 玄甲
        presets.add(makePresetEnchantedBook("玄甲", "§b", 3,
            java.util.Map.of(
                CriticalHitsEnchantments.ARMOR_DEFENSE_ID, 5,
                CriticalHitsEnchantments.DAMAGE_REDUCTION_ID, 3,
                CriticalHitsEnchantments.CRITICAL_RESISTANCE_ID, 3,
                Identifier.fromNamespaceAndPath("minecraft", "protection"), 4
            ),
            "玄铁重甲，刀枪不入",
            "高护甲高韧性，适用于重甲防御与物理减伤"));

        // 深海
        presets.add(makePresetEnchantedBook("深海", "§b", 3,
            java.util.Map.of(
                Identifier.fromNamespaceAndPath("minecraft", "water_breathing"), 3,
                Identifier.fromNamespaceAndPath("minecraft", "depth_strider"), 3,
                Identifier.fromNamespaceAndPath("minecraft", "aqua_affinity"), 1,
                CriticalHitsEnchantments.DAMAGE_REDUCTION_ID, 2,
                Identifier.fromNamespaceAndPath("minecraft", "protection"), 4
            ),
            "深海畅游，如履平地",
            "水下探索特化，适用于海底遗迹/水下战斗场景"));

        // 荆棘
        presets.add(makePresetEnchantedBook("荆棘", "§b", 3,
            java.util.Map.of(
                Identifier.fromNamespaceAndPath("minecraft", "thorns"), 3,
                CriticalHitsEnchantments.DAMAGE_REDUCTION_ID, 3,
                CriticalHitsEnchantments.ARMOR_DEFENSE_ID, 3,
                Identifier.fromNamespaceAndPath("minecraft", "protection"), 4
            ),
            "荆棘缠身，反伤制敌",
            "反伤流，适用于近战对抗与防御反击"));

        // 重甲
        presets.add(makePresetEnchantedBook("重甲", "§b", 3,
            java.util.Map.of(
                CriticalHitsEnchantments.MOVEMENT_SPEED_CURSE_ID, 3,
                CriticalHitsEnchantments.DAMAGE_REDUCTION_ID, 5,
                CriticalHitsEnchantments.ARMOR_DEFENSE_ID, 5,
                Identifier.fromNamespaceAndPath("minecraft", "protection"), 4
            ),
            "重甲缓行，固若金汤",
            "负移速高防，适用于站桩防御与正面承伤"));

        // ========== 装备类2星（高级）- 3本 ==========
        // 疾风
        presets.add(makePresetEnchantedBook("疾风", "§a", 2,
            java.util.Map.of(
                CriticalHitsEnchantments.MOVEMENT_SPEED_ID, 3,
                CriticalHitsEnchantments.DAMAGE_REDUCTION_ID, 2,
                Identifier.fromNamespaceAndPath("minecraft", "protection"), 4
            ),
            "疾风掠影，来去如风",
            "高移动速度，适用于机动拉扯与风筝战术"));

        // 轻灵
        presets.add(makePresetEnchantedBook("轻灵", "§a", 2,
            java.util.Map.of(
                Identifier.fromNamespaceAndPath("minecraft", "feather_falling"), 4,
                CriticalHitsEnchantments.MOVEMENT_SPEED_ID, 2,
                CriticalHitsEnchantments.DAMAGE_REDUCTION_ID, 2,
                Identifier.fromNamespaceAndPath("minecraft", "protection"), 4
            ),
            "轻灵飘逸，登高履危",
            "探索跑酷特化，适用于高空作业/山地探索场景"));

        // 轻甲
        presets.add(makePresetEnchantedBook("轻甲", "§a", 2,
            java.util.Map.of(
                CriticalHitsEnchantments.MOVEMENT_SPEED_ID, 5,
                CriticalHitsEnchantments.DAMAGE_REDUCTION_ID, 2,
                Identifier.fromNamespaceAndPath("minecraft", "protection"), 4
            ),
            "轻甲疾行，来去自如",
            "高移速低防，适用于机动拉扯与风筝战术"));

        // ========== 新手类1星（普通）- 8本 ==========
        // 武器类新手1星 - 5本
        presets.add(makePresetEnchantedBook("初心·暴击", "§7", 1,
            java.util.Map.of(
                CriticalHitsEnchantments.CRITICAL_RATE_ID, 5
            ),
            "初心者之选，暴击入门",
            "基础暴击率提升，适用于新手体验暴击系统"));

        presets.add(makePresetEnchantedBook("初心·增伤", "§7", 1,
            java.util.Map.of(
                CriticalHitsEnchantments.PERCENTAGE_DAMAGE_ID, 5
            ),
            "初心者之选，增伤入门",
            "基础伤害加成，适用于新手体验增伤系统"));

        presets.add(makePresetEnchantedBook("初心·攻速", "§7", 1,
            java.util.Map.of(
                CriticalHitsEnchantments.ATTACK_SPEED_ID, 5
            ),
            "初心者之选，攻速入门",
            "基础攻击速度提升，适用于新手体验攻速系统"));

        presets.add(makePresetEnchantedBook("初心·吸血", "§7", 1,
            java.util.Map.of(
                CriticalHitsEnchantments.LIFE_STEAL_ID, 3
            ),
            "初心者之选，吸血入门",
            "基础生命吸取，适用于新手体验吸血系统"));

        presets.add(makePresetEnchantedBook("初心·锋利", "§7", 1,
            java.util.Map.of(
                Identifier.fromNamespaceAndPath("minecraft", "sharpness"), 2
            ),
            "初心者之选，锋利入门",
            "基础锋利附魔，适用于新手提升武器伤害"));

        // 装备类新手1星 - 3本
        presets.add(makePresetEnchantedBook("初心·防御", "§7", 1,
            java.util.Map.of(
                CriticalHitsEnchantments.DAMAGE_REDUCTION_ID, 3,
                Identifier.fromNamespaceAndPath("minecraft", "protection"), 2
            ),
            "初心者之选，防御入门",
            "基础伤害减免，适用于新手体验防御系统"));

        presets.add(makePresetEnchantedBook("初心·血量", "§7", 1,
            java.util.Map.of(
                CriticalHitsEnchantments.MAX_HEALTH_ID, 2
            ),
            "初心者之选，血量入门",
            "基础生命值提升，适用于新手体验血量系统"));

        presets.add(makePresetEnchantedBook("初心·移速", "§7", 1,
            java.util.Map.of(
                CriticalHitsEnchantments.MOVEMENT_SPEED_ID, 2
            ),
            "初心者之选，移速入门",
            "基础移动速度提升，适用于新手体验移速系统"));

        return presets;
    }

    /**
     * 从指定HolderLookup.Provider获取附魔Holder（服务器端/客户端通用）
     */
    public static net.minecraft.core.Holder<Enchantment> getEnchantmentHolder(Identifier id, net.minecraft.core.HolderLookup.Provider provider) {
        try {
            if (provider != null) {
                java.util.Optional<? extends net.minecraft.core.HolderLookup<Enchantment>> lookup = provider.lookup(Registries.ENCHANTMENT);
                if (lookup.isPresent()) {
                    ResourceKey<Enchantment> key = ResourceKey.create(Registries.ENCHANTMENT, id);
                    java.util.Optional<net.minecraft.core.Holder.Reference<Enchantment>> holder = lookup.get().get(key);
                    if (holder.isPresent()) {
                        return holder.get();
                    }
                }
            }
            return null;
        } catch (Exception e) {
            LOGGER.warn("[Modern Damage System] Failed to get enchantment holder from provider: " + id, e);
            return null;
        }
    }

    /**
     * 兼容性检查：检测可能冲突的mod，在日志中给出警告和解决方案。
     */
    private static void checkCompatibility() {
        // 检测 UniversalEnchants（通用附魔）- 会导致附魔可附在错误装备上
        if (net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("universalenchants")) {
            LOGGER.warn("[Modern Damage System] ========================================");
            LOGGER.warn("[Modern Damage System] ⚠️  检测到 UniversalEnchants（通用附魔）mod");
            LOGGER.warn("[Modern Damage System] 该mod会允许附魔附在不对应的装备上，可能导致游戏平衡性问题");
            LOGGER.warn("[Modern Damage System] 建议：在 UniversalEnchants 的配置中将本mod的附魔加入黑名单");
            LOGGER.warn("[Modern Damage System] 或者卸载 UniversalEnchants mod");
            LOGGER.warn("[Modern Damage System] ========================================");
        }

        // 检测 Better Combat（必选前置）
        if (!net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("bettercombat")) {
            LOGGER.error("[Modern Damage System] ========================================");
            LOGGER.error("[Modern Damage System] ❌ 未检测到 Better Combat mod！");
            LOGGER.error("[Modern Damage System] 攻击速度和攻击范围系统将无法正常工作");
            LOGGER.error("[Modern Damage System] 请安装 Better Combat mod 后再启动游戏");
            LOGGER.error("[Modern Damage System] ========================================");
        }

        // 检测 AttributeFix（必选前置）
        if (!net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("attributefix")) {
            LOGGER.warn("[Modern Damage System] ========================================");
            LOGGER.warn("[Modern Damage System] ⚠️  未检测到 AttributeFix mod");
            LOGGER.warn("[Modern Damage System] 最大生命值附魔（生命强化）可能无法正常工作");
            LOGGER.warn("[Modern Damage System] 建议安装 AttributeFix mod");
            LOGGER.warn("[Modern Damage System] ========================================");
        }

        LOGGER.info("[Modern Damage System] 兼容性检查完成");
    }

    @SuppressWarnings("unchecked")
    private static <T> void register(ResourceKey<Registry<T>> key, String name, T value) {
        Registry<?> reg = BuiltInRegistries.REGISTRY.get(key.identifier())
            .map(ref -> ref.value()).orElse(null);
        if (reg == null) {
            LOGGER.error("[Modern Damage System] 注册表不存在: " + key.identifier() + "，无法注册 " + name);
            return;
        }
        Registry<T> typedReg = (Registry<T>) reg;
        Registry.register(typedReg, Identifier.fromNamespaceAndPath(MOD_ID, name), value);
        LOGGER.info("[Modern Damage System] 注册成功: " + key.identifier() + " / " + name);
    }

    private void injectCriticalBooks(LootTable.Builder tableBuilder, net.minecraft.core.HolderLookup.Provider registries) {
        LootPool.Builder pool = LootPool.lootPool();
        int maxCount = CriticalHitsConfig.get().loot_enchantment_max_count;
        // 至少1次roll，避免箱子完全为空
        pool.setRolls(UniformGenerator.between(1, maxCount));

        boolean addedAny = false;

        // 遍历配置中的所有附魔
        for (java.util.Map.Entry<String, CriticalHitsConfig.LootEnchantmentConfig> entry :
                CriticalHitsConfig.get().loot_enchantments.entrySet()) {
            String enchantIdStr = entry.getKey();
            CriticalHitsConfig.LootEnchantmentConfig lootConfig = entry.getValue();

            // 检查是否启用
            if (!lootConfig.enabled) continue;

            Identifier type = Identifier.tryParse(enchantIdStr);
            if (type == null) continue;

            net.minecraft.core.Holder<Enchantment> holder = getEnchantmentHolder(type, registries);
            if (holder == null) {
                LOGGER.warn("[Modern Damage System] Enchantment not found, skipping loot injection: " + type);
                continue;
            }

            // 使用每个附魔单独的最高等级配置
            int maxLevel = lootConfig.max_level;
            int[] weights = lootConfig.weights;
            for (int level = 1; level <= maxLevel; level++) {
                int weight = (level - 1 < weights.length) ? weights[level - 1] : 1;
                pool.add(LootItem.lootTableItem(Items.ENCHANTED_BOOK)
                    .setWeight(weight)
                    .apply(new SetEnchantmentsFunction.Builder(false)
                        .withEnchantment(holder, ConstantValue.exactly(level))));
                addedAny = true;
            }
        }
        if (addedAny) {
            tableBuilder.withPool(pool);
        }
    }

    /**
     * 注入CC币碎片到战利品箱子。
     * 每箱最多配置数量的CC币碎片。
     */
    private void injectCoinShards(LootTable.Builder tableBuilder) {
        LootPool.Builder pool = LootPool.lootPool();
        int maxCount = CriticalHitsConfig.get().coin_shard_max_count;
        // 至少1次roll，避免箱子完全为空
        pool.setRolls(UniformGenerator.between(1, maxCount));
        pool.add(LootItem.lootTableItem(CRITICAL_COIN_SHARD)
            .setWeight(CriticalHitsConfig.get().coin_shard_weight));
        tableBuilder.withPool(pool);
    }
}
