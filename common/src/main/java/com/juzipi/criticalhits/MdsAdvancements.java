package com.juzipi.criticalhits;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 成就系统（服务端逻辑）。
 *
 * <h3>设计</h3>
 * 所有成就 JSON 的判定准则统一用 {@code minecraft:impossible}，由本类在满足条件时
 * 显式调用 {@code PlayerAdvancements#award(...)} 发放。这样：
 * <ul>
 *   <li>不需要注册自定义 CriterionTrigger（跨端零差异）；</li>
 *   <li>所有条件集中在 Java 里，改条件不用碰 JSON；</li>
 *   <li>每 20 tick（1 秒）扫描一次玩家，覆盖背包/装备/配置类条件。</li>
 * </ul>
 *
 * <h3>26 个隐藏追踪成就</h3>
 * 每个附魔一个 {@code track_<id>} 技术性成就（JSON 里没有 display，游戏内完全不显示）。
 * 玩家一旦获得该附魔就永久记录 → "收集 10 种 / 26 种"直接数这些成就的完成数，
 * 天然持久（跟着存档走），不需要自己写玩家存档。
 */
public final class MdsAdvancements {

    private static final String NS = CriticalHitsEnchantments.MOD_ID;
    private static final String CRITERION = "trigger";

    /** 40 个成就的图形物品 id（注册用）。 */
    public static final String[] ICON_IDS = {
        "root", "first_critical_book", "max_critical", "damage_master", "forty_meter_blade",
        "high_damage", "true_damage", "execute_master", "dragonslayer", "splash_master",
        "clean_sweep", "shield_enchant", "armor_defense", "max_health", "damage_reduction",
        "unkillable", "reflect_kill", "craft_mds_station", "bookworm", "collector",
        "collect_all", "rich", "hardcore_mode", "tuner", "attack_speed",
        "movement_speed", "attack_range", "all_attributes", "frost", "poison",
        "armor_shatter", "vulnerability", "all_debuffs", "quadruple_torment", "new_look",
        "mixologist", "ultimate_master", "graduated", "self_inflicted", "bigger_is_better",
    };

    /** 26 种附魔（追踪成就 + 收集统计）。 */
    public static final String[] ENCH_IDS = {
        "critical_rate", "critical_damage", "critical_resistance", "percentage_damage",
        "armor_penetration", "life_steal", "vulnerability", "damage_reduction",
        "armor_defense", "max_health", "attack_speed", "movement_speed", "attack_range",
        "bonus_damage", "attack_speed_curse", "movement_speed_curse", "attack_range_curse",
        "true_damage", "splash_damage", "execute", "frost", "poison", "armor_shatter",
        "thorns_reflect", "shield", "knockback_resistance",
    };

    /** 成就 id -> 奖励经验。 */
    private static final Map<String, Integer> XP = new HashMap<>();
    /** 成就 id -> 奖励材料模版数量。 */
    private static final Map<String, Integer> COINS = new HashMap<>();

    static {
        Object[][] R = {
            {"root", 15, 0}, {"first_critical_book", 15, 0}, {"max_critical", 100, 8},
            {"damage_master", 100, 16}, {"forty_meter_blade", 100, 0}, {"high_damage", 40, 0},
            {"true_damage", 40, 0}, {"execute_master", 40, 0}, {"dragonslayer", 40, 4},
            {"splash_master", 40, 0}, {"clean_sweep", 100, 0}, {"shield_enchant", 15, 0},
            {"armor_defense", 15, 0}, {"max_health", 15, 0}, {"damage_reduction", 15, 0},
            {"unkillable", 100, 8}, {"reflect_kill", 100, 0}, {"craft_mds_station", 15, 2},
            {"bookworm", 15, 0}, {"collector", 40, 5}, {"collect_all", 100, 32},
            {"rich", 40, 10}, {"hardcore_mode", 100, 8}, {"tuner", 40, 0},
            {"attack_speed", 15, 0}, {"movement_speed", 15, 0}, {"attack_range", 15, 0},
            {"all_attributes", 100, 8}, {"frost", 15, 0}, {"poison", 15, 0},
            {"armor_shatter", 15, 0}, {"vulnerability", 15, 0}, {"all_debuffs", 100, 8},
            {"quadruple_torment", 100, 0}, {"new_look", 15, 0}, {"mixologist", 40, 4},
            {"ultimate_master", 200, 32}, {"graduated", 300, 64}, {"self_inflicted", 15, 1},
            {"bigger_is_better", 15, 0},
        };
        for (Object[] r : R) {
            XP.put((String) r[0], (Integer) r[1]);
            COINS.put((String) r[0], (Integer) r[2]);
        }
    }

    /** 需要"同时拥有多件附魔"的成就：id -> 附魔列表。 */
    private static final Map<String, String[]> HAS_ALL = new HashMap<>();
    /** 需要"全部满级"的成就：id -> 附魔列表。 */
    private static final Map<String, String[]> ALL_MAX = new HashMap<>();

    static {
        HAS_ALL.put("unkillable", new String[]{"shield", "damage_reduction", "armor_defense", "max_health"});
        HAS_ALL.put("all_attributes", new String[]{"attack_speed", "movement_speed", "attack_range"});
        HAS_ALL.put("all_debuffs", new String[]{"frost", "poison", "armor_shatter", "vulnerability"});
        ALL_MAX.put("damage_master", new String[]{"critical_rate", "critical_damage", "percentage_damage", "true_damage"});
        // 还有谁！：所有武器附魔 + 所有 debuff 附魔全满级
        ALL_MAX.put("ultimate_master", new String[]{
            "critical_rate", "critical_damage", "percentage_damage", "bonus_damage",
            "armor_penetration", "life_steal", "attack_speed", "attack_range",
            "true_damage", "splash_damage", "execute", "frost", "poison",
            "armor_shatter", "vulnerability"});
    }

    /** 护甲类附魔（"任意一件护甲满配"判定用）。 */
    private static final String[] ARMOR_ENCH = {
        "critical_resistance", "damage_reduction", "armor_defense", "max_health",
        "movement_speed", "thorns_reflect", "shield", "knockback_resistance"};

    /** 解锁时需要"揭示真实说明"的秘密成就（描述在游戏里显示为 ？？？）。 */
    private static final Set<String> SECRET = new HashSet<>(java.util.Arrays.asList(
        "bigger_is_better", "new_look", "self_inflicted", "hardcore_mode",
        "tuner", "mixologist", "reflect_kill", "quadruple_torment"));

    private static int tickCounter = 0;

    private MdsAdvancements() {
    }

    // ==================== 每 20 tick 扫描 ====================

    public static void tick(MinecraftServer server) {
        if (server == null) return;
        if (++tickCounter % 20 != 0) return;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            try {
                scan(p);
            } catch (Throwable t) {
                CriticalHitsMod.LOGGER.warn("[Modern Damage System] 成就扫描异常", t);
            }
        }
    }

    private static void scan(ServerPlayer p) {
        // ---- 26 个追踪成就 ----
        for (String e : ENCH_IDS) {
            if (maxLevelOf(p, e) > 0) {
                award(p, "track_" + e);
            }
        }

        // ---- 单一附魔类 ----
        if (maxLevelOf(p, "critical_rate") > 0) award(p, "first_critical_book");
        for (String e : new String[]{"true_damage", "shield_enchant", "armor_defense", "max_health",
            "damage_reduction", "attack_speed", "movement_speed", "attack_range",
            "frost", "poison", "armor_shatter", "vulnerability"}) {
            if (maxLevelOf(p, e) > 0) award(p, e);
        }

        // ---- 根成就：任意 MDS 附魔 ----
        for (String e : ENCH_IDS) {
            if (maxLevelOf(p, e) > 0) {
                award(p, "root");
                break;
            }
        }

        // ---- 满级类 ----
        if (maxLevelOf(p, "critical_rate") >= 10 || maxLevelOf(p, "critical_damage") >= 10) {
            award(p, "max_critical");
        }
        for (Map.Entry<String, String[]> e : ALL_MAX.entrySet()) {
            if (e.getKey().equals("ultimate_master")) continue;
            if (allAtMax(p, e.getValue())) award(p, e.getKey());
        }
        if (allAtMax(p, ALL_MAX.get("ultimate_master")) || anyArmorPieceFullyMaxed(p)) {
            award(p, "ultimate_master");
        }

        // ---- 同时拥有类 ----
        for (Map.Entry<String, String[]> e : HAS_ALL.entrySet()) {
            if (hasAll(p, e.getValue())) award(p, e.getKey());
        }

        // ---- 四重折磨的第二条路径：手持武器本身就带齐 4 种 debuff 附魔 ----
        if (hasAllOnHeld(p, new String[]{"frost", "poison", "armor_shatter", "vulnerability"})) {
            award(p, "quadruple_torment");
        }

        // ---- 合成台 / 配方书 ----
        if (countItem(p, () -> CriticalHitsMod.stationBlockItem()) > 0) award(p, "craft_mds_station");
        // 2.2.2：MDS 配方书是纯客户端界面（右键被客户端拦截，不发包给服务端），
        // 所以「爱读书」改成"背包里拥有配方书"即发放，语义见 lang 文案。
        if (countItem(p, () -> CriticalHitsMod.recipeBookItem()) > 0) award(p, "bookworm");

        // ---- 收集类（数隐藏追踪成就）----
        int kinds = 0;
        for (String e : ENCH_IDS) {
            if (isDone(p, "track_" + e)) kinds++;
        }
        if (kinds >= 10) award(p, "collector");
        if (kinds >= 26) award(p, "collect_all");

        // ---- 材料数量 ----
        if (countItem(p, () -> CriticalHitsMod.coinItem()) >= 100) award(p, "rich");

        // ---- 药水 ----
        if (countMdsPotions(p) >= 4) award(p, "mixologist");

        // ---- 元成就（配置类）----
        CriticalHitsConfig.ConfigData cfg = CriticalHitsConfig.get();
        for (CriticalHitsConfig.LootEnchantmentConfig lc : cfg.loot_enchantments.values()) {
            if (lc.max_level >= 10) {
                award(p, "hardcore_mode");
                break;
            }
        }
        if (cfg.master_weight_enabled && cfg.master_weight >= 100) award(p, "tuner");
        if ("blood".equalsIgnoreCase(cfg.hud_style)) award(p, "new_look");
        if (cfg.hud_scale >= 2.0f) award(p, "bigger_is_better");

        // ---- 毕业 ----
        if (allMainDone(p)) award(p, "graduated");
    }

    // ==================== 战斗事件（由 mixin 调用）====================

    /** 单次攻击伤害。 */
    public static void onDamageDealt(ServerPlayer p, float damage) {
        if (damage >= 16f) award(p, "high_damage");
        if (damage >= 40f) award(p, "forty_meter_blade");
    }

    /** 成功处决（target 为被处决目标）。 */
    public static void onExecute(ServerPlayer p, net.minecraft.world.entity.LivingEntity target) {
        award(p, "execute_master");
        if (target != null && target.getMaxHealth() >= 100f) award(p, "dragonslayer");
    }

    /** 溅射命中数量。 */
    public static void onSplash(ServerPlayer p, int hitCount) {
        if (hitCount >= 3) award(p, "splash_master");
        if (hitCount >= 5) award(p, "clean_sweep");
    }

    /** 反伤击杀。 */
    public static void onReflectKill(ServerPlayer p) {
        award(p, "reflect_kill");
    }

    /** 同一目标身上同时存在 4 种 debuff。 */
    public static void onQuadrupleDebuff(ServerPlayer p) {
        award(p, "quadruple_torment");
    }

    /** 自食其果：被带 MDS 附魔的生物击杀。 */
    public static void onKilledByMdsMob(ServerPlayer p) {
        award(p, "self_inflicted");
    }

    /** 该生物身上（手持+护甲）是否带有任意 MDS 附魔（「自食其果」判定用）。 */
    public static boolean hasAnyMdsEnchantment(net.minecraft.world.entity.LivingEntity e) {
        if (e == null) return false;
        try {
            for (net.minecraft.world.entity.EquipmentSlot slot
                    : net.minecraft.world.entity.EquipmentSlot.values()) {
                ItemStack s = e.getItemBySlot(slot);
                for (String id : ENCH_IDS) {
                    if (CriticalHitsEnchantments.getEnchantmentLevel(
                            Identifier.fromNamespaceAndPath(NS, id), s) > 0) {
                        return true;
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    /** 同一目标身上同时存在 冰霜(减速) + 剧毒 + 易伤 + 破甲 四种 debuff。 */
    public static void onAttackEnd(ServerPlayer p, net.minecraft.world.entity.LivingEntity target) {
        if (target == null) return;
        try {
            boolean slow = false, poison = false, vuln = false, shatter = false;
            for (var inst : target.getActiveEffects()) {
                var key = inst.getEffect().unwrapKey();
                if (key.isEmpty()) continue;
                Identifier id = key.get().identifier();
                if (id.getNamespace().equals("minecraft") && id.getPath().equals("slowness")) slow = true;
                else if (id.getNamespace().equals("minecraft") && id.getPath().equals("poison")) poison = true;
                else if (id.getNamespace().equals(NS) && id.getPath().equals("vulnerability")) vuln = true;
                else if (id.getNamespace().equals(NS) && id.getPath().equals("armor_break")) shatter = true;
            }
            if (slow && poison && vuln && shatter) award(p, "quadruple_torment");
        } catch (Throwable ignored) {
        }
    }

    // ==================== 发放 ====================

    /** 发放成就（幂等）。 */
    public static boolean award(ServerPlayer p, String id) {
        try {
            AdvancementHolder h = holder(p, id);
            if (h == null) return false;
            if (p.getAdvancements().getOrStartProgress(h).isDone()) return false;
            p.getAdvancements().award(h, CRITERION);
            grantReward(p, id);
            revealIfSecret(p, id);
            return true;
        } catch (Throwable t) {
            CriticalHitsMod.LOGGER.warn("[Modern Damage System] 发放成就失败: " + id, t);
            return false;
        }
    }

    /**
     * 秘密成就的解锁揭示。
     * 原版 AdvancementWidget 只判断 isHidden/isDone，没有任何 ??? 渲染逻辑，
     * 所以"解锁前???、解锁后显示真描述"在原版做不到；这里改为解锁瞬间用聊天栏
     * 把真实说明发给玩家（Component.translatable 由客户端按自己的语言渲染）。
     */
    private static void revealIfSecret(ServerPlayer p, String id) {
        if (!SECRET.contains(id)) return;
        try {
            p.sendSystemMessage(Component.literal("§6[MDS] §e")
                .append(Component.translatable("advancement.criticalhits." + id + ".title"))
                .append(Component.literal(" §6达成！§7"))
                .append(Component.translatable("advancement.criticalhits." + id + ".reveal")));
        } catch (Throwable ignored) {
        }
    }

    private static AdvancementHolder holder(ServerPlayer p, String id) {
        MinecraftServer server = p.level().getServer();
        if (server == null) return null;
        return server.getAdvancements().get(Identifier.fromNamespaceAndPath(NS, id));
    }

    private static boolean isDone(ServerPlayer p, String id) {
        AdvancementHolder h = holder(p, id);
        if (h == null) return false;
        return p.getAdvancements().getOrStartProgress(h).isDone();
    }

    private static void grantReward(ServerPlayer p, String id) {
        // 服务器端默认不发放奖励，服主用 /mds rewards true 开启
        if (!CriticalHitsConfig.get().achievement_rewards_enabled) return;
        Integer xp = XP.get(id);
        if (xp != null && xp > 0) {
            p.giveExperiencePoints(xp);
        }
        Integer coins = COINS.get(id);
        if (coins != null && coins > 0) {
            ItemStack stack = new ItemStack(CriticalHitsMod.coinItem(), Math.min(coins, 64));
            int times = (coins + 63) / 64;
            for (int i = 0; i < times; i++) {
                ItemStack s = new ItemStack(CriticalHitsMod.coinItem(), Math.min(64, coins - i * 64));
                if (!p.getInventory().add(s)) p.drop(s, false);
            }
        }
        p.sendSystemMessage(Component.literal("§6[MDS] 成就奖励已发放"));
    }

    /** 供 /mds 命令与自检使用：当前玩家已完成的 MDS 成就数。 */
    public static int doneCount(ServerPlayer p, boolean includeTrack) {
        int n = 0;
        for (String id : ICON_IDS) {
            if (isDone(p, id)) n++;
        }
        if (includeTrack) {
            for (String e : ENCH_IDS) {
                if (isDone(p, "track_" + e)) n++;
            }
        }
        return n;
    }

    private static boolean allMainDone(ServerPlayer p) {
        for (String id : ICON_IDS) {
            if (id.equals("graduated")) continue;
            if (!isDone(p, id)) return false;
        }
        return true;
    }

    // ==================== 背包工具 ====================

    /** 玩家身上（背包+护甲+副手）该附魔的最高等级；附魔书也算。 */
    public static int maxLevelOf(ServerPlayer p, String enchantId) {
        int best = 0;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            best = Math.max(best, levelOn(p.getInventory().getItem(i), enchantId));
        }
        return best;
    }

    private static int levelOn(ItemStack stack, String enchantId) {
        if (stack == null || stack.isEmpty()) return 0;
        int best = levelFrom(stack.getEnchantments(), enchantId);
        ItemEnchantments stored = stack.get(DataComponents.STORED_ENCHANTMENTS);
        if (stored != null) best = Math.max(best, levelFrom(stored, enchantId));
        return best;
    }

    private static int levelFrom(ItemEnchantments ench, String enchantId) {
        if (ench == null || ench.isEmpty()) return 0;
        int best = 0;
        for (var entry : ench.entrySet()) {
            var key = entry.getKey().unwrapKey();
            if (key.isEmpty()) continue;
            if (key.get().identifier().getNamespace().equals(NS)
                && key.get().identifier().getPath().equals(enchantId)) {
                best = Math.max(best, entry.getIntValue());
            }
        }
        return best;
    }

    /** 手持物品上是否带齐指定附魔。 */
    private static boolean hasAllOnHeld(ServerPlayer p, String[] ids) {
        ItemStack held = p.getMainHandItem();
        for (String id : ids) {
            if (levelOn(held, id) <= 0) return false;
        }
        return true;
    }

    /** 自检：确认 66 个成就都已被服务端成功加载（能解析到 AdvancementHolder）。 */
    public static void selfTest(MinecraftServer server) {
        if (server == null) return;
        int ok = 0;
        java.util.List<String> missing = new java.util.ArrayList<>();
        for (String id : ICON_IDS) {
            if (server.getAdvancements().get(Identifier.fromNamespaceAndPath(NS, id)) != null) ok++;
            else missing.add(id);
        }
        for (String e : ENCH_IDS) {
            if (server.getAdvancements().get(Identifier.fromNamespaceAndPath(NS, "track_" + e)) != null) ok++;
            else missing.add("track_" + e);
        }
        if (missing.isEmpty()) {
            CriticalHitsMod.LOGGER.info("[MDS 自检] 成就加载 " + ok + "/66 ✅");
        } else {
            CriticalHitsMod.LOGGER.warn("[MDS 自检] 成就只加载了 " + ok + "/66，缺失: " + missing);
        }
    }

    private static boolean hasAll(ServerPlayer p, String[] ids) {
        for (String id : ids) {
            if (maxLevelOf(p, id) <= 0) return false;
        }
        return true;
    }

    private static boolean allAtMax(ServerPlayer p, String[] ids) {
        for (String id : ids) {
            if (maxLevelOf(p, id) < maxLevelOfEnchant(id)) return false;
        }
        return true;
    }

    /** 该附魔在定义里的上限（失败时回退 10）。 */
    private static int maxLevelOfEnchant(String id) {
        try {
            var holder = CriticalHitsMod.getEnchantmentHolder(Identifier.fromNamespaceAndPath(NS, id));
            if (holder != null) return holder.value().getMaxLevel();
        } catch (Throwable ignored) {
        }
        return CriticalHitsEnchantments.MAX_ENCHANT_LEVEL;
    }

    /** 任意一件护甲上 8 种护甲附魔全部满级。 */
    private static boolean anyArmorPieceFullyMaxed(ServerPlayer p) {
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            ItemStack s = p.getInventory().getItem(i);
            if (s == null || s.isEmpty()) continue;
            boolean all = true;
            for (String e : ARMOR_ENCH) {
                if (levelOn(s, e) < maxLevelOfEnchant(e)) {
                    all = false;
                    break;
                }
            }
            if (all) return true;
        }
        return false;
    }

    private static int countItem(ServerPlayer p, java.util.function.Supplier<net.minecraft.world.item.Item> item) {
        int n = 0;
        try {
            net.minecraft.world.item.Item target = item.get();
            for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
                ItemStack s = p.getInventory().getItem(i);
                if (!s.isEmpty() && s.is(target)) n += s.getCount();
            }
        } catch (Throwable ignored) {
        }
        return n;
    }

    private static int countMdsPotions(ServerPlayer p) {
        Set<String> types = new HashSet<>();
        try {
            for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
                ItemStack s = p.getInventory().getItem(i);
                if (s.isEmpty()) continue;
                if (!s.is(Items.POTION) && !s.is(Items.SPLASH_POTION) && !s.is(Items.LINGERING_POTION)) continue;
                var contents = s.get(DataComponents.POTION_CONTENTS);
                if (contents == null || contents.potion().isEmpty()) continue;
                String str = contents.potion().get().toString();
                if (str.contains(NS)) types.add(str);
            }
        } catch (Throwable ignored) {
        }
        return types.size();
    }
}
