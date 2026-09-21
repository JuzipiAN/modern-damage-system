package com.juzipi.criticalhits.mixin;

import com.juzipi.criticalhits.CriticalHitsMod;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Map;

/**
 * 注入到CraftingMenu.slotChangedCraftingGrid，实现附魔书合成。
 * CC币 + 书 + 特定材料 = 对应附魔书（1级）
 */
@Mixin(CraftingMenu.class)
public class CraftingMenuMixin {

    @Inject(method = "slotChangedCraftingGrid", at = @At("TAIL"))
    private static void onSlotChangedCraftingGrid(
            AbstractContainerMenu menu,
            ServerLevel level,
            Player player,
            CraftingContainer craftingContainer,
            ResultContainer resultContainer,
            RecipeHolder<?> recipeHolder,
            CallbackInfo ci) {

        // 收集输入物品
        Map<Item, Integer> itemCounts = new HashMap<>();
        for (int i = 0; i < craftingContainer.getContainerSize(); i++) {
            ItemStack stack = craftingContainer.getItem(i);
            if (!stack.isEmpty()) {
                itemCounts.merge(stack.getItem(), stack.getCount(), Integer::sum);
            }
        }

        // 检查是否包含CC币和书（各至少1个）
        int coinCount = itemCounts.getOrDefault(CriticalHitsMod.CRITICAL_COIN, 0);
        int bookCount = itemCounts.getOrDefault(Items.BOOK, 0);
        if (coinCount < 1 || bookCount < 1) return;

        // 收集其他材料（非CC币、非书）
        Map<Item, Integer> m = new HashMap<>();
        for (Map.Entry<Item, Integer> entry : itemCounts.entrySet()) {
            Item item = entry.getKey();
            if (item != CriticalHitsMod.CRITICAL_COIN && item != Items.BOOK) {
                m.put(item, entry.getValue());
            }
        }

        // 判断合成哪种附魔书（材料种类精确匹配，每种材料数量至少匹配，允许多放）
        String enchantmentId = null;

        // 暴击率 = 1下界合金锭 + 1钻石
        if (m.size() == 2
            && m.getOrDefault(Items.NETHERITE_INGOT, 0) >= 1
            && m.getOrDefault(Items.DIAMOND, 0) >= 1) {
            enchantmentId = "criticalhits:critical_rate";
        }
        // 伤害加成 = 3钻石（必须在暴击伤害前面判断）
        else if (m.size() == 1 && m.getOrDefault(Items.DIAMOND, 0) >= 3) {
            enchantmentId = "criticalhits:percentage_damage";
        }
        // 暴击伤害 = 2钻石
        else if (m.size() == 1 && m.getOrDefault(Items.DIAMOND, 0) >= 2) {
            enchantmentId = "criticalhits:critical_damage";
        }
        // 暴击减免 = 5铁锭
        else if (m.size() == 1 && m.getOrDefault(Items.IRON_INGOT, 0) >= 5) {
            enchantmentId = "criticalhits:critical_resistance";
        }
        // 护甲穿透 = 1下界合金锭
        else if (m.size() == 1 && m.getOrDefault(Items.NETHERITE_INGOT, 0) >= 1) {
            enchantmentId = "criticalhits:armor_penetration";
        }
        // 额外伤害 = 2金块（必须在生命吸取=1金块前面判断）
        else if (m.size() == 1 && m.getOrDefault(Items.GOLD_BLOCK, 0) >= 2) {
            enchantmentId = "criticalhits:bonus_damage";
        }
        // 生命吸取 = 1金块
        else if (m.size() == 1 && m.getOrDefault(Items.GOLD_BLOCK, 0) >= 1) {
            enchantmentId = "criticalhits:life_steal";
        }
        // 易伤 = 2火药 + 1钻石
        else if (m.size() == 2
            && m.getOrDefault(Items.GUNPOWDER, 0) >= 2
            && m.getOrDefault(Items.DIAMOND, 0) >= 1) {
            enchantmentId = "criticalhits:vulnerability";
        }
        // 伤害减免 = 1绿宝石块
        else if (m.size() == 1 && m.getOrDefault(Items.EMERALD_BLOCK, 0) >= 1) {
            enchantmentId = "criticalhits:damage_reduction";
        }
        // 护甲防御 = 3铁块
        else if (m.size() == 1 && m.getOrDefault(Items.IRON_BLOCK, 0) >= 3) {
            enchantmentId = "criticalhits:armor_defense";
        }
        // 生命强化 = 1金块 + 2紫水晶碎片
        else if (m.size() == 2
            && m.getOrDefault(Items.GOLD_BLOCK, 0) >= 1
            && m.getOrDefault(Items.AMETHYST_SHARD, 0) >= 2) {
            enchantmentId = "criticalhits:max_health";
        }
        // 攻击速度 = 1风弹 + 1绿宝石
        else if (m.size() == 2
            && m.getOrDefault(Items.WIND_CHARGE, 0) >= 1
            && m.getOrDefault(Items.EMERALD, 0) >= 1) {
            enchantmentId = "criticalhits:attack_speed";
        }
        // 移动速度 = 2兔子腿 + 1绿宝石
        else if (m.size() == 2
            && m.getOrDefault(Items.RABBIT_FOOT, 0) >= 2
            && m.getOrDefault(Items.EMERALD, 0) >= 1) {
            enchantmentId = "criticalhits:movement_speed";
        }
        // 攻击范围 = 1青金石块
        else if (m.size() == 1 && m.getOrDefault(Items.LAPIS_BLOCK, 0) >= 1) {
            enchantmentId = "criticalhits:attack_range";
        }
        // ===== 负等级配方（加入铜锭） =====
        // 攻击速度负等级 = 1风弹 + 1绿宝石 + 1铜锭
        else if (m.size() == 3
            && m.getOrDefault(Items.WIND_CHARGE, 0) >= 1
            && m.getOrDefault(Items.EMERALD, 0) >= 1
            && m.getOrDefault(Items.COPPER_INGOT, 0) >= 1) {
            enchantmentId = "criticalhits:attack_speed_curse";
        }
        // 移动速度负等级 = 2兔子腿 + 1绿宝石 + 1铜锭
        else if (m.size() == 3
            && m.getOrDefault(Items.RABBIT_FOOT, 0) >= 2
            && m.getOrDefault(Items.EMERALD, 0) >= 1
            && m.getOrDefault(Items.COPPER_INGOT, 0) >= 1) {
            enchantmentId = "criticalhits:movement_speed_curse";
        }
        // 攻击范围负等级 = 1青金石块 + 1铜锭
        else if (m.size() == 2
            && m.getOrDefault(Items.LAPIS_BLOCK, 0) >= 1
            && m.getOrDefault(Items.COPPER_INGOT, 0) >= 1) {
            enchantmentId = "criticalhits:attack_range_curse";
        }
        // ===== 新增附魔配方 =====
        // 真实伤害 = 1下界合金锭 + 5龙息
        else if (m.size() == 2
            && m.getOrDefault(Items.NETHERITE_INGOT, 0) >= 1
            && m.getOrDefault(Items.DRAGON_BREATH, 0) >= 5) {
            enchantmentId = "criticalhits:true_damage";
        }
        // 溅射伤害 = 5火药 + 2烈焰粉 + 2红石（必须在易伤=2火药+1钻石前面判断）
        else if (m.size() == 3
            && m.getOrDefault(Items.GUNPOWDER, 0) >= 5
            && m.getOrDefault(Items.BLAZE_POWDER, 0) >= 2
            && m.getOrDefault(Items.REDSTONE, 0) >= 2) {
            enchantmentId = "criticalhits:splash_damage";
        }
        // 处决 = 1下界之星
        else if (m.size() == 1 && m.getOrDefault(Items.NETHER_STAR, 0) >= 1) {
            enchantmentId = "criticalhits:execute";
        }
        // 冰霜 = 2蓝冰 + 1青金石 + 任意减速药水
        else if (m.size() == 3
            && m.getOrDefault(Items.BLUE_ICE, 0) >= 2
            && m.getOrDefault(Items.LAPIS_LAZULI, 0) >= 1
            && hasPotionOfType(craftingContainer, "slowness")) {
            enchantmentId = "criticalhits:frost";
        }
        // 剧毒 = 2蜘蛛眼 + 1发酵蛛眼 + 任意中毒药水
        else if (m.size() == 3
            && m.getOrDefault(Items.SPIDER_EYE, 0) >= 2
            && m.getOrDefault(Items.FERMENTED_SPIDER_EYE, 0) >= 1
            && hasPotionOfType(craftingContainer, "poison")) {
            enchantmentId = "criticalhits:poison";
        }
        // 破甲 = 1下界合金锭 + 1铁砧
        else if (m.size() == 2
            && m.getOrDefault(Items.NETHERITE_INGOT, 0) >= 1
            && m.getOrDefault(Items.ANVIL, 0) >= 1) {
            enchantmentId = "criticalhits:armor_shatter";
        }
        // 荆棘反伤 = 4仙人掌 + 1绿宝石 + 1燧石
        else if (m.size() == 3
            && m.getOrDefault(Items.CACTUS, 0) >= 4
            && m.getOrDefault(Items.EMERALD, 0) >= 1
            && m.getOrDefault(Items.FLINT, 0) >= 1) {
            enchantmentId = "criticalhits:thorns_reflect";
        }
        // 护盾 = 1金块 + 2紫水晶碎片 + 3海龟壳（必须在生命强化前面判断，因为材料种类更多）
        else if (m.size() == 3
            && m.getOrDefault(Items.GOLD_BLOCK, 0) >= 1
            && m.getOrDefault(Items.AMETHYST_SHARD, 0) >= 2
            && m.getOrDefault(Items.TURTLE_HELMET, 0) >= 3) {
            enchantmentId = "criticalhits:shield";
        }
        // 击退抗性 = 3铁块 + 1粘液球（必须在护甲防御前面判断，因为材料种类更多）
        else if (m.size() == 2
            && m.getOrDefault(Items.IRON_BLOCK, 0) >= 3
            && m.getOrDefault(Items.SLIME_BALL, 0) >= 1) {
            enchantmentId = "criticalhits:knockback_resistance";
        }

        if (enchantmentId == null) return;

        // 获取附魔Holder
        net.minecraft.resources.Identifier id = net.minecraft.resources.Identifier.tryParse(enchantmentId);
        Holder<Enchantment> holder = (id != null) ? CriticalHitsMod.getEnchantmentHolder(id) : null;
        if (holder == null) return;

        // 创建输出附魔书（1级）
        ItemStack result = new ItemStack(Items.ENCHANTED_BOOK);
        ItemEnchantments.Mutable enchants = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        enchants.set(holder, 1);
        result.set(DataComponents.STORED_ENCHANTMENTS, enchants.toImmutable());

        resultContainer.setItem(0, result);
    }

    /**
     * 检查输入物品中是否包含特定效果的药水（普通/喷溅/滞留）。
     * @param craftingContainer 合成容器
     * @param effectName 药水效果名称（如"slowness"、"poison"）
     * @return 是否包含该效果的药水
     */
    private static boolean hasPotionOfType(CraftingContainer craftingContainer, String effectName) {
        for (int i = 0; i < craftingContainer.getContainerSize(); i++) {
            ItemStack stack = craftingContainer.getItem(i);
            if (stack.isEmpty()) continue;
            Item item = stack.getItem();
            // 只检查药水物品（普通/喷溅/滞留）
            if (item != Items.POTION && item != Items.SPLASH_POTION && item != Items.LINGERING_POTION) continue;
            // 读取药水DataComponents中的Potion效果
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
