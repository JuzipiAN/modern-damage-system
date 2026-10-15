package com.juzipi.criticalhits;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 工作台合成配方的定义与执行逻辑（普通类，非 Mixin）。
 *
 * 设计要点（2.0.5 重写）：
 * 1. 附魔书不是原版配方，结果由 {@code CraftingMenuMixin.slotChangedCraftingGrid} 注入到结果槽。
 * 2. 原版 {@code ResultSlot.onTake} 在"没有匹配原版配方"时会走
 *    {@code copyAllInputItems} 回退分支，把整格材料复制回去（6 钻石 → 11 钻石），
 *    这是材料复制 bug 的根因。因此任何会让原版 ResultSlot.onTake 接触到我们这本
 *    附魔书的点击都必须被提前拦截（见 {@code AbstractContainerMenuMixin}）。
 * 3. 本类提供纯逻辑：配方识别、结果构建、材料校验与精确消耗，便于复用与自测。
 */
public class CraftingRecipeState {

    /** 工作台结果槽索引 */
    public static final int RESULT_SLOT = 0;
    /** 工作台合成格槽位区间 [GRID_SLOT_START, GRID_SLOT_END] */
    public static final int GRID_SLOT_START = 1;
    public static final int GRID_SLOT_END = 9;

    /**
     * 根据附魔ID返回配方需要消耗的材料数量（包括模板和书）。
     * 此方法不依赖任何外部状态，可在任意时刻安全调用。
     */
    public static Map<Item, Integer> getRecipeCost(String enchantmentId) {
        Map<Item, Integer> cost = new HashMap<>();
        cost.put(CriticalHitsMod.CRITICAL_COIN, 1); // 模板
        cost.put(Items.BOOK, 1); // 书

        switch (enchantmentId) {
            case "criticalhits:critical_rate":
                cost.put(Items.NETHERITE_INGOT, 1); cost.put(Items.DIAMOND, 1); break;
            case "criticalhits:critical_damage":
                cost.put(Items.DIAMOND, 2); break;
            case "criticalhits:critical_resistance":
                cost.put(Items.IRON_INGOT, 5); break;
            case "criticalhits:percentage_damage":
                cost.put(Items.DIAMOND, 2); cost.put(Items.EMERALD, 1); break;
            case "criticalhits:armor_penetration":
                cost.put(Items.NETHERITE_INGOT, 1); break;
            case "criticalhits:life_steal":
                cost.put(Items.GOLD_BLOCK, 1); break;
            case "criticalhits:vulnerability":
                cost.put(Items.GUNPOWDER, 2); cost.put(Items.DIAMOND, 1); break;
            case "criticalhits:damage_reduction":
                cost.put(Items.EMERALD_BLOCK, 1); break;
            case "criticalhits:armor_defense":
                cost.put(Items.IRON_BLOCK, 3); break;
            case "criticalhits:max_health":
                cost.put(Items.GOLD_BLOCK, 1); cost.put(Items.AMETHYST_SHARD, 2); break;
            case "criticalhits:attack_speed":
                cost.put(Items.WIND_CHARGE, 1); cost.put(Items.EMERALD, 1); break;
            case "criticalhits:movement_speed":
                cost.put(Items.RABBIT_FOOT, 2); cost.put(Items.EMERALD, 1); break;
            case "criticalhits:attack_range":
                cost.put(Items.LAPIS_BLOCK, 1); break;
            case "criticalhits:bonus_damage":
                cost.put(Items.GOLD_BLOCK, 2); cost.put(Items.EMERALD, 1); break;
            case "criticalhits:true_damage":
                cost.put(Items.NETHERITE_INGOT, 1); cost.put(Items.DRAGON_BREATH, 5); break;
            case "criticalhits:splash_damage":
                cost.put(Items.GUNPOWDER, 5); cost.put(Items.BLAZE_POWDER, 2); cost.put(Items.REDSTONE, 2); break;
            case "criticalhits:execute":
                cost.put(Items.NETHER_STAR, 1); break;
            case "criticalhits:frost":
                cost.put(Items.BLUE_ICE, 2); cost.put(Items.LAPIS_LAZULI, 1); cost.put(Items.POTION, 1); break;
            case "criticalhits:poison":
                cost.put(Items.SPIDER_EYE, 2); cost.put(Items.FERMENTED_SPIDER_EYE, 1); cost.put(Items.POTION, 1); break;
            case "criticalhits:armor_shatter":
                cost.put(Items.NETHERITE_INGOT, 1); cost.put(Items.ANVIL, 1); break;
            case "criticalhits:thorns_reflect":
                cost.put(Items.CACTUS, 4); cost.put(Items.EMERALD, 1); cost.put(Items.FLINT, 1); break;
            case "criticalhits:shield":
                cost.put(Items.GOLD_BLOCK, 1); cost.put(Items.AMETHYST_SHARD, 2); cost.put(Items.TURTLE_HELMET, 3); break;
            case "criticalhits:knockback_resistance":
                cost.put(Items.IRON_BLOCK, 3); cost.put(Items.SLIME_BALL, 1); break;
            case "criticalhits:attack_speed_curse":
                cost.put(Items.WIND_CHARGE, 1); cost.put(Items.EMERALD, 1); cost.put(Items.COPPER_INGOT, 1); break;
            case "criticalhits:movement_speed_curse":
                cost.put(Items.RABBIT_FOOT, 2); cost.put(Items.EMERALD, 1); cost.put(Items.COPPER_INGOT, 1); break;
            case "criticalhits:attack_range_curse":
                cost.put(Items.LAPIS_BLOCK, 1); cost.put(Items.COPPER_INGOT, 1); break;
            default:
                return null;
        }
        return cost;
    }

    // ==================== 配方识别 ====================

    /** 统计工作台合成格（slot 1..9）中的物品数量。 */
    public static Map<Item, Integer> countGridItems(AbstractContainerMenu menu) {
        Map<Item, Integer> counts = new HashMap<>();
        collectGridItems(menu, counts);
        return counts;
    }

    /** 统计合成格物品，排除模板（暴击币）与书本身。 */
    public static Map<Item, Integer> countIngredients(AbstractContainerMenu menu) {
        Map<Item, Integer> all = countGridItems(menu);
        Map<Item, Integer> result = new HashMap<>();
        for (Map.Entry<Item, Integer> entry : all.entrySet()) {
            Item item = entry.getKey();
            if (item != CriticalHitsMod.CRITICAL_COIN && item != Items.BOOK) {
                result.put(item, entry.getValue());
            }
        }
        return result;
    }

    private static void collectGridItems(AbstractContainerMenu menu, Map<Item, Integer> counts) {
        for (int i = GRID_SLOT_START; i <= GRID_SLOT_END && i < menu.slots.size(); i++) {
            ItemStack stack = menu.getSlot(i).getItem();
            if (!stack.isEmpty()) {
                counts.merge(stack.getItem(), stack.getCount(), Integer::sum);
            }
        }
    }

    /** 判断合成格物品是否满足配方的材料需求。 */
    public static boolean canConsume(Map<Item, Integer> gridCounts, Map<Item, Integer> cost) {
        if (cost == null) return false;
        for (Map.Entry<Item, Integer> entry : cost.entrySet()) {
            if (gridCounts.getOrDefault(entry.getKey(), 0) < entry.getValue()) return false;
        }
        return true;
    }

    /**
     * 精确消耗材料。使用 {@link Slot#set(ItemStack)} 而不是直接 shrink，
     * 保证容器收到 setChanged/slotsChanged 通知，服务端与客户端不会不同步。
     */
    public static void consume(AbstractContainerMenu menu, Map<Item, Integer> cost) {
        if (cost == null) return;
        Map<Item, Integer> remaining = new HashMap<>(cost);
        for (int i = GRID_SLOT_START; i <= GRID_SLOT_END && i < menu.slots.size(); i++) {
            Slot slot = menu.getSlot(i);
            ItemStack stack = slot.getItem();
            if (stack.isEmpty()) continue;
            Integer need = remaining.get(stack.getItem());
            if (need == null || need <= 0) continue;
            int take = Math.min(stack.getCount(), need);
            int left = stack.getCount() - take;
            slot.set(left <= 0 ? ItemStack.EMPTY : stack.copyWithCount(left));
            remaining.put(stack.getItem(), need - take);
        }
    }

    /** 手动触发结果槽重算（材料变化后由原版逻辑刷新结果槽）。 */
    public static void refreshResult(AbstractContainerMenu menu) {
        if (menu.slots.size() <= GRID_SLOT_START) return;
        Container grid = menu.getSlot(GRID_SLOT_START).container;
        if (grid instanceof CraftingContainer) {
            menu.slotsChanged(grid);
        }
    }

    // ==================== 结果构建 ====================

    /** 从结果物品中取出 MDS 附魔ID，非本模组的书返回 null。 */
    public static String getMdsEnchantmentId(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        ItemEnchantments enchantments = stack.get(DataComponents.STORED_ENCHANTMENTS);
        if (enchantments == null || enchantments.isEmpty()) return null;
        for (Map.Entry<Holder<Enchantment>, Integer> entry : enchantments.entrySet()) {
            Optional<ResourceKey<Enchantment>> key = entry.getKey().unwrapKey();
            if (key.isPresent() && key.get().identifier().getNamespace().equals(CriticalHitsEnchantments.MOD_ID)) {
                return key.get().identifier().toString();
            }
        }
        return null;
    }

    /** 构建等级1的 MDS 附魔书；附魔查不到时返回空物品。 */
    public static ItemStack buildResult(String enchantmentId, HolderLookup.Provider registries) {
        Identifier id = Identifier.tryParse(enchantmentId);
        if (id == null) return ItemStack.EMPTY;
        Holder<Enchantment> holder = CriticalHitsMod.getEnchantmentHolder(id, registries);
        if (holder == null) return ItemStack.EMPTY;
        ItemStack result = new ItemStack(Items.ENCHANTED_BOOK);
        ItemEnchantments.Mutable enchantments = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        enchantments.set(holder, 1);
        result.set(DataComponents.STORED_ENCHANTMENTS, enchantments.toImmutable());
        return result;
    }

    // ==================== 配方表 ====================

    /** 配方识别逻辑（原 CraftingMenuMixin 内联，2.0.5 移出以便复用与自测）。 */
    public static String detectEnchantment(Map<Item, Integer> m, CraftingContainer craftingContainer) {
        // 暴击率 = 1下界合金锭 + 1钻石
        if (m.size() == 2 && m.getOrDefault(Items.NETHERITE_INGOT, 0) >= 1 && m.getOrDefault(Items.DIAMOND, 0) >= 1)
            return "criticalhits:critical_rate";
        // 伤害加成 = 2钻石 + 1绿宝石
        if (m.size() == 2 && m.getOrDefault(Items.DIAMOND, 0) >= 2 && m.getOrDefault(Items.EMERALD, 0) >= 1)
            return "criticalhits:percentage_damage";
        // 暴击伤害 = 2钻石
        if (m.size() == 1 && m.getOrDefault(Items.DIAMOND, 0) >= 2)
            return "criticalhits:critical_damage";
        // 暴击减免 = 5铁锭
        if (m.size() == 1 && m.getOrDefault(Items.IRON_INGOT, 0) >= 5)
            return "criticalhits:critical_resistance";
        // 护甲穿透 = 1下界合金锭
        if (m.size() == 1 && m.getOrDefault(Items.NETHERITE_INGOT, 0) >= 1)
            return "criticalhits:armor_penetration";
        // 额外伤害 = 2金块 + 1绿宝石
        if (m.size() == 2 && m.getOrDefault(Items.GOLD_BLOCK, 0) >= 2 && m.getOrDefault(Items.EMERALD, 0) >= 1)
            return "criticalhits:bonus_damage";
        // 生命吸取 = 1金块
        if (m.size() == 1 && m.getOrDefault(Items.GOLD_BLOCK, 0) >= 1)
            return "criticalhits:life_steal";
        // 易伤 = 2火药 + 1钻石
        if (m.size() == 2 && m.getOrDefault(Items.GUNPOWDER, 0) >= 2 && m.getOrDefault(Items.DIAMOND, 0) >= 1)
            return "criticalhits:vulnerability";
        // 伤害减免 = 1绿宝石块
        if (m.size() == 1 && m.getOrDefault(Items.EMERALD_BLOCK, 0) >= 1)
            return "criticalhits:damage_reduction";
        // 护甲防御 = 3铁块
        if (m.size() == 1 && m.getOrDefault(Items.IRON_BLOCK, 0) >= 3)
            return "criticalhits:armor_defense";
        // 生命强化 = 1金块 + 2紫水晶碎片
        if (m.size() == 2 && m.getOrDefault(Items.GOLD_BLOCK, 0) >= 1 && m.getOrDefault(Items.AMETHYST_SHARD, 0) >= 2)
            return "criticalhits:max_health";
        // 攻击速度 = 1风弹 + 1绿宝石
        if (m.size() == 2 && m.getOrDefault(Items.WIND_CHARGE, 0) >= 1 && m.getOrDefault(Items.EMERALD, 0) >= 1)
            return "criticalhits:attack_speed";
        // 移动速度 = 2兔子腿 + 1绿宝石
        if (m.size() == 2 && m.getOrDefault(Items.RABBIT_FOOT, 0) >= 2 && m.getOrDefault(Items.EMERALD, 0) >= 1)
            return "criticalhits:movement_speed";
        // 攻击范围 = 1青金石块
        if (m.size() == 1 && m.getOrDefault(Items.LAPIS_BLOCK, 0) >= 1)
            return "criticalhits:attack_range";
        // 负等级配方
        if (m.size() == 3 && m.getOrDefault(Items.WIND_CHARGE, 0) >= 1 && m.getOrDefault(Items.EMERALD, 0) >= 1 && m.getOrDefault(Items.COPPER_INGOT, 0) >= 1)
            return "criticalhits:attack_speed_curse";
        if (m.size() == 3 && m.getOrDefault(Items.RABBIT_FOOT, 0) >= 2 && m.getOrDefault(Items.EMERALD, 0) >= 1 && m.getOrDefault(Items.COPPER_INGOT, 0) >= 1)
            return "criticalhits:movement_speed_curse";
        if (m.size() == 2 && m.getOrDefault(Items.LAPIS_BLOCK, 0) >= 1 && m.getOrDefault(Items.COPPER_INGOT, 0) >= 1)
            return "criticalhits:attack_range_curse";
        // 新增附魔
        if (m.size() == 2 && m.getOrDefault(Items.NETHERITE_INGOT, 0) >= 1 && m.getOrDefault(Items.DRAGON_BREATH, 0) >= 5)
            return "criticalhits:true_damage";
        if (m.size() == 3 && m.getOrDefault(Items.GUNPOWDER, 0) >= 5 && m.getOrDefault(Items.BLAZE_POWDER, 0) >= 2 && m.getOrDefault(Items.REDSTONE, 0) >= 2)
            return "criticalhits:splash_damage";
        if (m.size() == 1 && m.getOrDefault(Items.NETHER_STAR, 0) >= 1)
            return "criticalhits:execute";
        if (m.size() == 3 && m.getOrDefault(Items.BLUE_ICE, 0) >= 2 && m.getOrDefault(Items.LAPIS_LAZULI, 0) >= 1 && hasPotionOfType(craftingContainer, "slowness"))
            return "criticalhits:frost";
        if (m.size() == 3 && m.getOrDefault(Items.SPIDER_EYE, 0) >= 2 && m.getOrDefault(Items.FERMENTED_SPIDER_EYE, 0) >= 1 && hasPotionOfType(craftingContainer, "poison"))
            return "criticalhits:poison";
        if (m.size() == 2 && m.getOrDefault(Items.NETHERITE_INGOT, 0) >= 1 && m.getOrDefault(Items.ANVIL, 0) >= 1)
            return "criticalhits:armor_shatter";
        if (m.size() == 3 && m.getOrDefault(Items.CACTUS, 0) >= 4 && m.getOrDefault(Items.EMERALD, 0) >= 1 && m.getOrDefault(Items.FLINT, 0) >= 1)
            return "criticalhits:thorns_reflect";
        if (m.size() == 3 && m.getOrDefault(Items.GOLD_BLOCK, 0) >= 1 && m.getOrDefault(Items.AMETHYST_SHARD, 0) >= 2 && m.getOrDefault(Items.TURTLE_HELMET, 0) >= 3)
            return "criticalhits:shield";
        if (m.size() == 2 && m.getOrDefault(Items.IRON_BLOCK, 0) >= 3 && m.getOrDefault(Items.SLIME_BALL, 0) >= 1)
            return "criticalhits:knockback_resistance";
        return null;
    }

    public static boolean hasPotionOfType(CraftingContainer craftingContainer, String effectName) {
        for (int i = 0; i < craftingContainer.getContainerSize(); i++) {
            ItemStack stack = craftingContainer.getItem(i);
            if (stack.isEmpty()) continue;
            Item item = stack.getItem();
            if (item != Items.POTION && item != Items.SPLASH_POTION && item != Items.LINGERING_POTION) continue;
            net.minecraft.world.item.alchemy.PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
            if (contents != null && contents.potion().isPresent()) {
                String holderStr = contents.potion().get().toString();
                if (holderStr != null && holderStr.toLowerCase().contains(effectName)) {
                    return true;
                }
            }
        }
        return false;
    }
}
