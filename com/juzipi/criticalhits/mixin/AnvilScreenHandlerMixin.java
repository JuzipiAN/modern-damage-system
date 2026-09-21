package com.juzipi.criticalhits.mixin;

import com.juzipi.criticalhits.CriticalHitsEnchantments;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 铁砧界面 Mixin。
 * <p>
 * 职责：
 * 1. 暴击系列附魔书合并时经验消耗减半
 * 2. 5级及以上暴击附魔书禁止在铁砧合并（仅能通过暴击台合成）
 */
@Mixin(AnvilMenu.class)
public abstract class AnvilScreenHandlerMixin {

    @Shadow
    private DataSlot cost;

    /**
     * 在铁砧更新合成结果后，修改经验消耗或禁止高等级合并。
     */
    @Inject(method = "createResult()V", at = @At("TAIL"))
    private void criticalhits$modifyAnvilResult(CallbackInfo ci) {
        AnvilMenu self = (AnvilMenu) (Object) this;
        ItemStack left = self.getSlot(0).getItem();
        ItemStack right = self.getSlot(1).getItem();

        // 阻止攻击速度附魔在护甲上（即使有通用附魔mod也不行）
        if (isArmor(left) && isAttackSpeedBook(right)) {
            self.getSlot(2).set(ItemStack.EMPTY);
            this.cost.set(0);
            return;
        }

        // 阻止正负等级同时附魔（攻击速度、移动速度、攻击范围）
        if (containsOppositeEnchantments(left, right)) {
            self.getSlot(2).set(ItemStack.EMPTY);
            this.cost.set(0);
            return;
        }

        // 检查是否是两本5级及以上的本mod附魔书合并（所有系列都禁止）
        int leftLevel = getModBookLevel(left);
        int rightLevel = getModBookLevel(right);

        if (leftLevel >= 5 && rightLevel >= 5 && isModBook(left) && isModBook(right)) {
            // 禁止合并：清空结果槽，成本设为0
            self.getSlot(2).set(ItemStack.EMPTY);
            this.cost.set(0);
            return;
        }

        // 暴击系列附魔经验减半
        if (containsCriticalEnchantment(left) || containsCriticalEnchantment(right)) {
            int originalCost = this.cost.get();
            if (originalCost > 0) {
                int halvedCost = Math.max(1, (originalCost + 1) / 2);
                this.cost.set(halvedCost);
            }
        }
    }

    /**
     * 检查是否是本mod的附魔书（所有10个系列），并返回等级（不是则返回0）。
     */
    private int getModBookLevel(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.is(Items.ENCHANTED_BOOK)) return 0;
        ItemEnchantments enchants = stack.get(DataComponents.STORED_ENCHANTMENTS);
        if (enchants == null || enchants.isEmpty()) return 0;
        for (Object2IntMap.Entry<Holder<Enchantment>> entry : enchants.entrySet()) {
            Holder<Enchantment> ench = entry.getKey();
            if (isModEnchantment(ench)) {
                return entry.getIntValue();
            }
        }
        return 0;
    }

    private boolean isModBook(ItemStack stack) {
        return getModBookLevel(stack) > 0;
    }

    /**
     * 检查附魔是否是本mod的附魔（所有17个系列）。
     */
    private boolean isModEnchantment(Holder<Enchantment> ench) {
        return ench.is(CriticalHitsEnchantments.CRITICAL_RATE_ID)
            || ench.is(CriticalHitsEnchantments.CRITICAL_DAMAGE_ID)
            || ench.is(CriticalHitsEnchantments.CRITICAL_RESISTANCE_ID)
            || ench.is(CriticalHitsEnchantments.PERCENTAGE_DAMAGE_ID)
            || ench.is(CriticalHitsEnchantments.ARMOR_PENETRATION_ID)
            || ench.is(CriticalHitsEnchantments.LIFE_STEAL_ID)
            || ench.is(CriticalHitsEnchantments.VULNERABILITY_ID)
            || ench.is(CriticalHitsEnchantments.DAMAGE_REDUCTION_ID)
            || ench.is(CriticalHitsEnchantments.ARMOR_DEFENSE_ID)
            || ench.is(CriticalHitsEnchantments.MAX_HEALTH_ID)
            || ench.is(CriticalHitsEnchantments.ATTACK_SPEED_ID)
            || ench.is(CriticalHitsEnchantments.MOVEMENT_SPEED_ID)
            || ench.is(CriticalHitsEnchantments.ATTACK_RANGE_ID)
            || ench.is(CriticalHitsEnchantments.BONUS_DAMAGE_ID)
            || ench.is(CriticalHitsEnchantments.ATTACK_SPEED_CURSE_ID)
            || ench.is(CriticalHitsEnchantments.MOVEMENT_SPEED_CURSE_ID)
            || ench.is(CriticalHitsEnchantments.ATTACK_RANGE_CURSE_ID);
    }

    /**
     * 检查是否有正负等级同时附魔的情况（攻击速度、移动速度、攻击范围）。
     */
    private boolean containsOppositeEnchantments(ItemStack left, ItemStack right) {
        if (left == null || left.isEmpty()) return false;

        // 定义正负等级对
        net.minecraft.resources.Identifier[][] pairs = {
            {CriticalHitsEnchantments.ATTACK_SPEED_ID, CriticalHitsEnchantments.ATTACK_SPEED_CURSE_ID},
            {CriticalHitsEnchantments.MOVEMENT_SPEED_ID, CriticalHitsEnchantments.MOVEMENT_SPEED_CURSE_ID},
            {CriticalHitsEnchantments.ATTACK_RANGE_ID, CriticalHitsEnchantments.ATTACK_RANGE_CURSE_ID},
        };

        // 检查左侧物品是否已经同时有正负等级
        ItemEnchantments leftEnchants = left.getEnchantments();
        if (leftEnchants != null && !leftEnchants.isEmpty()) {
            for (var pair : pairs) {
                boolean hasPositive = false;
                boolean hasNegative = false;
                for (Object2IntMap.Entry<Holder<Enchantment>> entry : leftEnchants.entrySet()) {
                    Holder<Enchantment> ench = entry.getKey();
                    if (ench.is(pair[0])) hasPositive = true;
                    if (ench.is(pair[1])) hasNegative = true;
                }
                if (hasPositive && hasNegative) return true;
            }
        }

        // 检查右侧附魔书是否与左侧物品的正负等级冲突
        if (right != null && right.is(Items.ENCHANTED_BOOK)) {
            ItemEnchantments rightEnchants = right.get(DataComponents.STORED_ENCHANTMENTS);
            if (rightEnchants != null && !rightEnchants.isEmpty()) {
                for (var pair : pairs) {
                    boolean leftHasPositive = false;
                    boolean leftHasNegative = false;
                    boolean rightHasPositive = false;
                    boolean rightHasNegative = false;

                    if (leftEnchants != null && !leftEnchants.isEmpty()) {
                        for (Object2IntMap.Entry<Holder<Enchantment>> entry : leftEnchants.entrySet()) {
                            Holder<Enchantment> ench = entry.getKey();
                            if (ench.is(pair[0])) leftHasPositive = true;
                            if (ench.is(pair[1])) leftHasNegative = true;
                        }
                    }
                    for (Object2IntMap.Entry<Holder<Enchantment>> entry : rightEnchants.entrySet()) {
                        Holder<Enchantment> ench = entry.getKey();
                        if (ench.is(pair[0])) rightHasPositive = true;
                        if (ench.is(pair[1])) rightHasNegative = true;
                    }

                    // 左侧有正等级，右侧有负等级
                    if (leftHasPositive && rightHasNegative) return true;
                    // 左侧有负等级，右侧有正等级
                    if (leftHasNegative && rightHasPositive) return true;
                }
            }
        }

        return false;
    }

    /**
     * 检查物品是否是护甲。
     */
    private boolean isArmor(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        return stack.is(Items.DIAMOND_HELMET) || stack.is(Items.DIAMOND_CHESTPLATE)
            || stack.is(Items.DIAMOND_LEGGINGS) || stack.is(Items.DIAMOND_BOOTS)
            || stack.is(Items.IRON_HELMET) || stack.is(Items.IRON_CHESTPLATE)
            || stack.is(Items.IRON_LEGGINGS) || stack.is(Items.IRON_BOOTS)
            || stack.is(Items.GOLDEN_HELMET) || stack.is(Items.GOLDEN_CHESTPLATE)
            || stack.is(Items.GOLDEN_LEGGINGS) || stack.is(Items.GOLDEN_BOOTS)
            || stack.is(Items.CHAINMAIL_HELMET) || stack.is(Items.CHAINMAIL_CHESTPLATE)
            || stack.is(Items.CHAINMAIL_LEGGINGS) || stack.is(Items.CHAINMAIL_BOOTS)
            || stack.is(Items.LEATHER_HELMET) || stack.is(Items.LEATHER_CHESTPLATE)
            || stack.is(Items.LEATHER_LEGGINGS) || stack.is(Items.LEATHER_BOOTS)
            || stack.is(Items.NETHERITE_HELMET) || stack.is(Items.NETHERITE_CHESTPLATE)
            || stack.is(Items.NETHERITE_LEGGINGS) || stack.is(Items.NETHERITE_BOOTS)
            || stack.is(Items.TURTLE_HELMET) || stack.is(Items.WOLF_ARMOR);
    }

    /**
     * 检查物品是否是攻击速度附魔书（正等级或负等级）。
     */
    private boolean isAttackSpeedBook(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.is(Items.ENCHANTED_BOOK)) return false;
        ItemEnchantments enchants = stack.get(DataComponents.STORED_ENCHANTMENTS);
        if (enchants == null || enchants.isEmpty()) return false;
        for (Object2IntMap.Entry<Holder<Enchantment>> entry : enchants.entrySet()) {
            Holder<Enchantment> ench = entry.getKey();
            if (ench.is(CriticalHitsEnchantments.ATTACK_SPEED_ID)
                || ench.is(CriticalHitsEnchantments.ATTACK_SPEED_CURSE_ID)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查物品是否包含暴击系列附魔（武器/护甲上的附魔）。
     */
    private boolean containsCriticalEnchantment(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        ItemEnchantments enchantments = stack.getEnchantments();
        if (enchantments.isEmpty()) return false;
        for (Object2IntMap.Entry<Holder<Enchantment>> entry : enchantments.entrySet()) {
            Holder<Enchantment> ench = entry.getKey();
            if (ench.is(CriticalHitsEnchantments.CRITICAL_RATE_ID)
                || ench.is(CriticalHitsEnchantments.CRITICAL_DAMAGE_ID)
                || ench.is(CriticalHitsEnchantments.CRITICAL_RESISTANCE_ID)) {
                return true;
            }
        }
        return false;
    }
}
