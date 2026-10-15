package com.juzipi.criticalhits;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.AutoConfigClient;
import me.shedaniel.autoconfig.ConfigHolder;
import me.shedaniel.autoconfig.ConfigManager;
import me.shedaniel.autoconfig.gui.ConfigScreenProvider;
import me.shedaniel.autoconfig.gui.DefaultGuiProviders;
import me.shedaniel.autoconfig.gui.registry.GuiRegistry;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.lang.reflect.Field;
import java.util.List;

/**
 * MDS 配置界面（在 AutoConfig 默认界面上做定制）。
 *
 * <h3>全局权重总控</h3>
 * <ul>
 *   <li>总控<b>关闭</b>：「全局权重倍率」滑块置灰不可点；</li>
 *   <li>总控<b>开启</b>：单附魔权重滑块全部置灰，实际权重 = 各自权重 x 倍率（保持相对强弱）；</li>
 *   <li><b>拖动即时刷新</b>：textGetter 每帧都会被调用，在里面把当前值实时写回配置对象，
 *       其它条目下一帧就能读到新值并立刻重算显示（Cloth Config 本来只在保存时才写回）。</li>
 * </ul>
 *
 * <h3>掉落开关联动</h3>
 * 某个附魔的「战利品开关」关闭时，它的<b>权重滑块</b>与<b>最高等级滑块</b>都会置灰不可点，
 * 并在文本上标出"[战利品已关闭]"，避免误以为这些值还在起作用。
 */
public final class MdsConfigGui {

    /** 倍率基准：20 = 1.00x。 */
    private static final int MASTER_REFERENCE = 20;

    private MdsConfigGui() {
    }

    public static Screen create(Screen parent) {
        try {
            ConfigHolder<MdsClothConfig> holder = AutoConfig.getConfigHolder(MdsClothConfig.class);
            if (!(holder instanceof ConfigManager)) {
                return AutoConfigClient.getConfigScreen(MdsClothConfig.class, parent).get();
            }
            @SuppressWarnings("unchecked")
            ConfigManager<MdsClothConfig> manager = (ConfigManager<MdsClothConfig>) holder;
            MdsClothConfig cfg = manager.getConfig();

            GuiRegistry registry = DefaultGuiProviders.apply(new GuiRegistry());

            // ---- 单个附魔权重滑块 ----
            registry.registerPredicateTransformer(
                (entries, i13n, field, config, defaults, gui) -> {
                    String suffix = field.getName().substring("loot_weight_".length());
                    boolean lootOn = isLootEnabled(cfg, suffix);
                    int value = readInt(field, config, 0);
                    AbstractConfigListEntry<Integer> entry = ConfigEntryBuilder.create()
                        .startIntSlider(Component.translatable(i13n), value, 0, 100)
                        .setDefaultValue(0)
                        .setTextGetter(v -> {
                            if (!lootOn) {
                                return Component.literal(v + "   [战利品已关闭]");
                            }
                            if (!cfg.master_weight_enabled) {
                                return Component.literal(String.valueOf(v));
                            }
                            int eff = (int) Math.round(v * (cfg.master_weight / (double) MASTER_REFERENCE));
                            return Component.literal(v + "  \u2192  " + eff);
                        })
                        .setSaveConsumer(v -> writeInt(field, config, v))
                        .setTooltip(Component.literal("该附魔的掉落权重（0 = 不掉落）"),
                            Component.literal("需先在上面的「战利品附魔开关」里开启该附魔"),
                            Component.literal("开启「全局权重总控」后显示：原值 \u2192 实际生效值"))
                        .build();
                    entry.setEditable(lootOn && !cfg.master_weight_enabled);
                    return List.of(entry);
                },
                field -> field.getName().startsWith("loot_weight_")
                    && !field.getName().equals("master_weight"));

            // ---- 「全局权重倍率」滑块 ----
            registry.registerPredicateTransformer(
                (entries, i13n, field, config, defaults, gui) -> {
                    int value = readInt(field, config, MASTER_REFERENCE);
                    AbstractConfigListEntry<Integer> entry = ConfigEntryBuilder.create()
                        .startIntSlider(Component.translatable(i13n), value, 1, 100)
                        .setDefaultValue(MASTER_REFERENCE)
                        .setTextGetter(v -> {
                            // 拖动即时写回 -> 其它条目下一帧即可读到
                            cfg.master_weight = v;
                            if (!cfg.master_weight_enabled) {
                                return Component.literal(v + "   [总控未开启，不生效]");
                            }
                            return Component.literal(v + "   (" + String.format("%.2f", v / (double) MASTER_REFERENCE) + "x)");
                        })
                        .setSaveConsumer(v -> cfg.master_weight = v)
                        .setTooltip(Component.literal("开启「全局权重总控」后，这里统一放大或缩小所有附魔的掉落权重"),
                            Component.literal("基准 20 = 1.00x；单附魔权重会按原比例自动缩放，保持相对强弱"))
                        .build();
                    entry.setEditable(cfg.master_weight_enabled);
                    return List.of(entry);
                },
                field -> field.getName().equals("master_weight"));

            // ---- 最高等级滑块：掉落开关关闭时置灰 ----
            registry.registerPredicateTransformer(
                (entries, i13n, field, config, defaults, gui) -> {
                    String suffix = field.getName().substring("loot_max_level_".length());
                    boolean lootOn = isLootEnabled(cfg, suffix);
                    for (AbstractConfigListEntry<?> e : entries) {
                        e.setEditable(lootOn);
                    }
                    return entries;
                },
                field -> field.getName().startsWith("loot_max_level_"));

            return new ConfigScreenProvider<>(manager, registry, parent).get();
        } catch (Throwable t) {
            CriticalHitsMod.LOGGER.warn("[Modern Damage System] 自定义配置界面创建失败，退回默认界面", t);
            try {
                return AutoConfigClient.getConfigScreen(MdsClothConfig.class, parent).get();
            } catch (Throwable t2) {
                CriticalHitsMod.LOGGER.error("[Modern Damage System] 默认配置界面也创建失败", t2);
                return null;
            }
        }
    }

    /**
     * 读取某个附魔的"战利品开关"（字段名规则：loot_weight_X / loot_max_level_X -> loot_X）。
     * 读不到时按"开启"处理，避免误置灰。
     */
    private static boolean isLootEnabled(MdsClothConfig cfg, String suffix) {
        try {
            Field f = MdsClothConfig.class.getField("loot_" + suffix);
            return f.getBoolean(cfg);
        } catch (Throwable t) {
            return true;
        }
    }

    private static int readInt(Field field, Object config, int fallback) {
        try {
            field.setAccessible(true);
            return field.getInt(config);
        } catch (Throwable t) {
            return fallback;
        }
    }

    private static void writeInt(Field field, Object config, int value) {
        try {
            field.setAccessible(true);
            field.setInt(config, value);
        } catch (Throwable t) {
            CriticalHitsMod.LOGGER.warn("[Modern Damage System] 写入配置字段失败: " + field.getName(), t);
        }
    }

    /** 供调试/命令查询当前生效的全局权重说明。 */
    public static String describeMaster() {
        try {
            MdsClothConfig cfg = AutoConfig.getConfigHolder(MdsClothConfig.class).getConfig();
            if (!cfg.master_weight_enabled) {
                return "全局权重总控：关闭（各附魔使用各自权重）";
            }
            return "全局权重总控：开启，倍率 " + String.format("%.2f", cfg.master_weight / (double) MASTER_REFERENCE)
                + "x（基准值 " + MASTER_REFERENCE + " = 1.00x）";
        } catch (Throwable t) {
            return "全局权重总控：无法读取";
        }
    }
}
