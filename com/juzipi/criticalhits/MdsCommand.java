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
            .executes(MdsCommand::showHelp)
        );
    }

    private static int showHelp(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSuccess(() -> Component.literal("§6=== 现代化伤害系统配置命令 ==="), false);
        context.getSource().sendSuccess(() -> Component.literal("§e/mds help §7- 显示此帮助"), false);
        context.getSource().sendSuccess(() -> Component.literal("§e/mds hud <属性> <on|off> §7- 配置HUD显示"), false);
        context.getSource().sendSuccess(() -> Component.literal("§e/mds loot <附魔> <on|off> §7- 配置战利品掉落"), false);
        context.getSource().sendSuccess(() -> Component.literal("§e/mds lootmaxlevel <1-5> §7- 战利品最高等级"), false);
        context.getSource().sendSuccess(() -> Component.literal("§e/mds lootmaxcount <1-5> §7- 每箱最大数量"), false);
        context.getSource().sendSuccess(() -> Component.literal("§e/mds reload §7- 重新加载配置"), false);
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
        CriticalHitsConfig.get().loot_enchantment_max_level = level;
        CriticalHitsConfig.save();
        context.getSource().sendSuccess(() -> Component.literal("§a战利品最高等级已设置为: " + level), false);
        return 1;
    }

    private static int setLootMaxCount(CommandContext<CommandSourceStack> context) {
        int count = IntegerArgumentType.getInteger(context, "count");
        CriticalHitsConfig.get().loot_enchantment_max_count = count;
        CriticalHitsConfig.save();
        context.getSource().sendSuccess(() -> Component.literal("§a每箱最大数量已设置为: " + count), false);
        return 1;
    }

    private static int reloadConfig(CommandContext<CommandSourceStack> context) {
        CriticalHitsConfig.load();
        context.getSource().sendSuccess(() -> Component.literal("§a配置已重新加载"), false);
        return 1;
    }
}
