package com.juzipi.criticalhits;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/**
 * 现代化伤害系统配置命令。
 * 由于26.2 GUI API变化较大，暂时使用命令系统配置，后续版本完善GUI界面。
 */
public class MdsCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("mds")
            .then(Commands.literal("help")
                .executes(MdsCommand::showHelp))
            .then(Commands.literal("hud")
                .then(Commands.argument("attribute", StringArgumentType.string())
                    .suggests((context, builder) -> {
                        builder.suggest("critical_rate");
                        builder.suggest("critical_damage");
                        builder.suggest("damage_bonus");
                        builder.suggest("armor_penetration");
                        builder.suggest("life_steal");
                        builder.suggest("vulnerability");
                        builder.suggest("attack_speed");
                        builder.suggest("attack_range");
                        builder.suggest("bonus_damage");
                        builder.suggest("damage_reduction");
                        builder.suggest("armor_defense");
                        builder.suggest("max_health");
                        builder.suggest("critical_resistance");
                        builder.suggest("movement_speed");
                        builder.suggest("all");
                        return builder.buildFuture();
                    })
                    .then(Commands.argument("state", StringArgumentType.string())
                        .suggests((context, builder) -> {
                            builder.suggest("on");
                            builder.suggest("off");
                            return builder.buildFuture();
                        })
                        .executes(MdsCommand::setHudAttribute))))
            .then(Commands.literal("loot")
                .then(Commands.argument("enchantment", StringArgumentType.string())
                    .suggests((context, builder) -> {
                        builder.suggest("critical_rate");
                        builder.suggest("critical_damage");
                        builder.suggest("critical_resistance");
                        builder.suggest("percentage_damage");
                        builder.suggest("armor_penetration");
                        builder.suggest("life_steal");
                        builder.suggest("vulnerability");
                        builder.suggest("damage_reduction");
                        builder.suggest("armor_defense");
                        builder.suggest("max_health");
                        builder.suggest("bonus_damage");
                        builder.suggest("attack_range");
                        builder.suggest("attack_speed");
                        builder.suggest("movement_speed");
                        builder.suggest("true_damage");
                        builder.suggest("splash_damage");
                        builder.suggest("execute");
                        builder.suggest("frost");
                        builder.suggest("poison");
                        builder.suggest("armor_shatter");
                        builder.suggest("thorns_reflect");
                        builder.suggest("shield");
                        builder.suggest("knockback_resistance");
                        builder.suggest("all");
                        return builder.buildFuture();
                    })
                    .then(Commands.argument("state", StringArgumentType.string())
                        .suggests((context, builder) -> {
                            builder.suggest("on");
                            builder.suggest("off");
                            return builder.buildFuture();
                        })
                        .executes(MdsCommand::setLootEnchantment))))
            .then(Commands.literal("lootmaxlevel")
                .then(Commands.argument("level", IntegerArgumentType.integer(1, 5))
                    .executes(MdsCommand::setLootMaxLevel)))
            .then(Commands.literal("lootmaxcount")
                .then(Commands.argument("count", IntegerArgumentType.integer(1, 5))
                    .executes(MdsCommand::setLootMaxCount)))
            .then(Commands.literal("reload")
                .executes(MdsCommand::reloadConfig))
            .then(Commands.literal("selftest")
                .executes(MdsCommand::runSelfTest))
            .then(Commands.literal("rewards")
                .then(Commands.argument("enabled", com.mojang.brigadier.arguments.BoolArgumentType.bool())
                    .executes(MdsCommand::setRewards)))
            .executes(MdsCommand::showHelp)
        );
    }

    private static int runSelfTest(CommandContext<CommandSourceStack> context) {
        net.minecraft.server.MinecraftServer server = context.getSource().getServer();
        String summary = MdsCraftingSelfTest.runAndSummarize(server);
        context.getSource().sendSuccess(() -> Component.literal(summary), false);
        return 1;
    }

    private static int showHelp(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSuccess(() -> Component.literal("§6=== 现代化伤害系统配置命令 ==="), false);
        context.getSource().sendSuccess(() -> Component.literal("§e/mds help §7- 显示此帮助"), false);
        context.getSource().sendSuccess(() -> Component.literal("§e/mds hud <属性> <on|off> §7- 配置HUD显示"), false);
        context.getSource().sendSuccess(() -> Component.literal("§e/mds loot <附魔> <on|off> §7- 配置战利品掉落"), false);
        context.getSource().sendSuccess(() -> Component.literal("§e/mds lootmaxlevel <1-5> §7- 战利品最高等级"), false);
        context.getSource().sendSuccess(() -> Component.literal("§e/mds lootmaxcount <1-5> §7- 每箱最大数量"), false);
        context.getSource().sendSuccess(() -> Component.literal("§e/mds reload §7- 重新加载配置"), false);
        context.getSource().sendSuccess(() -> Component.literal("§e/mds selftest §7- 运行合成系统自测（回归测试）"), false);
        return 1;
    }

    private static int setHudAttribute(CommandContext<CommandSourceStack> context) {
        String attribute = StringArgumentType.getString(context, "attribute");
        String state = StringArgumentType.getString(context, "state");
        boolean enabled = state.equalsIgnoreCase("on");

        CriticalHitsConfig.ConfigData config = CriticalHitsConfig.get();
        if (config.hud_visible_attributes == null) {
            config.hud_visible_attributes = new java.util.HashMap<>();
        }

        if (attribute.equalsIgnoreCase("all")) {
            String[] allAttrs = {"critical_rate", "critical_damage", "damage_bonus", "armor_penetration",
                "life_steal", "vulnerability", "attack_speed", "attack_range", "bonus_damage",
                "damage_reduction", "armor_defense", "max_health", "critical_resistance", "movement_speed"};
            for (String attr : allAttrs) {
                config.hud_visible_attributes.put(attr, enabled);
            }
            context.getSource().sendSuccess(() -> Component.literal("§a所有HUD属性已" + (enabled ? "开启" : "关闭")), false);
        } else {
            config.hud_visible_attributes.put(attribute, enabled);
            context.getSource().sendSuccess(() -> Component.literal("§aHUD属性 " + attribute + " 已" + (enabled ? "开启" : "关闭")), false);
        }

        CriticalHitsConfig.save();
        return 1;
    }

    private static int setLootEnchantment(CommandContext<CommandSourceStack> context) {
        String enchantment = StringArgumentType.getString(context, "enchantment");
        String state = StringArgumentType.getString(context, "state");
        boolean enabled = state.equalsIgnoreCase("on");

        CriticalHitsConfig.ConfigData config = CriticalHitsConfig.get();
        String fullId = "criticalhits:" + enchantment;

        if (enchantment.equalsIgnoreCase("all")) {
            for (java.util.Map.Entry<String, CriticalHitsConfig.LootEnchantmentConfig> entry : config.loot_enchantments.entrySet()) {
                entry.getValue().enabled = enabled;
            }
            context.getSource().sendSuccess(() -> Component.literal("§a所有战利品附魔已" + (enabled ? "开启" : "关闭")), false);
        } else {
            CriticalHitsConfig.LootEnchantmentConfig lootConfig = config.loot_enchantments.get(fullId);
            if (lootConfig != null) {
                lootConfig.enabled = enabled;
                context.getSource().sendSuccess(() -> Component.literal("§a战利品附魔 " + enchantment + " 已" + (enabled ? "开启" : "关闭")), false);
            } else {
                context.getSource().sendFailure(Component.literal("§c未找到附魔: " + enchantment));
                return 0;
            }
        }

        CriticalHitsConfig.save();
        return 1;
    }

    private static int setLootMaxLevel(CommandContext<CommandSourceStack> context) {
        int level = IntegerArgumentType.getInteger(context, "level");
        // 2.1.0：旧实现只写 loot_enchantment_max_level 这个全局字段，但战利品注入读的是
        // 每个附魔自己的 max_level，所以命令实际无效。现在两个都改。
        net.minecraft.core.HolderLookup.Provider access = context.getSource().getServer().registryAccess();
        int affected = 0;
        for (java.util.Map.Entry<String, CriticalHitsConfig.LootEnchantmentConfig> entry
                : CriticalHitsConfig.get().loot_enchantments.entrySet()) {
            int realMax = level;
            net.minecraft.core.Holder<net.minecraft.world.item.enchantment.Enchantment> holder =
                CriticalHitsMod.getEnchantmentHolder(net.minecraft.resources.Identifier.tryParse(entry.getKey()), access);
            if (holder != null) {
                realMax = holder.value().getMaxLevel();
            }
            entry.getValue().max_level = Math.max(1, Math.min(level, realMax));
            affected++;
        }
        CriticalHitsConfig.save();
        final int applied = affected;
        context.getSource().sendSuccess(() -> Component.literal(
            "§a战利品最高等级已设置为: " + level + "（已应用到 " + applied + " 个附魔，超出各自上限的会自动夹紧；需 /reload 或重进世界生效）"), false);
        return 1;
    }

    private static int setLootMaxCount(CommandContext<CommandSourceStack> context) {
        int count = IntegerArgumentType.getInteger(context, "count");
        CriticalHitsConfig.get().loot_enchantment_max_count = count;
        CriticalHitsConfig.save();
        context.getSource().sendSuccess(() -> Component.literal("§a每箱最大数量已设置为: " + count), false);
        return 1;
    }

    /** /mds rewards <true|false>：开启/关闭成就奖励（服务器端默认关闭）。 */
    private static int setRewards(CommandContext<CommandSourceStack> context) {
        boolean on = com.mojang.brigadier.arguments.BoolArgumentType.getBool(context, "enabled");
        CriticalHitsConfig.get().achievement_rewards_enabled = on;
        CriticalHitsConfig.save();
        context.getSource().sendSuccess(() -> Component.literal(
            "§a成就奖励已" + (on ? "开启" : "关闭") + "（只影响之后解锁的成就；单人存档默认关闭，可在配置界面或此指令开启）"), true);
        return 1;
    }

    private static int reloadConfig(CommandContext<CommandSourceStack> context) {
        CriticalHitsConfig.load();
        context.getSource().sendSuccess(() -> Component.literal("§a配置已重新加载"), false);
        return 1;
    }
}
