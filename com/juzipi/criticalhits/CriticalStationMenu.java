package com.juzipi.criticalhits;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/**
 * 暴击台菜单。
 * 4本同类型同等级暴击附魔书 → 1本高一级书，不消耗经验。
 */
public class CriticalStationMenu extends AbstractContainerMenu {

    private final Container inputSlots = new SimpleContainer(4) {
        @Override
        public void setChanged() {
            super.setChanged();
            CriticalStationMenu.this.slotsChanged(this);
        }
    };
    private final ResultContainer resultSlots = new ResultContainer();
    private final ContainerLevelAccess access;
    private final Player player;

    // 输入槽位置（2x2排列，在深灰色背景内居中）
    private static final int[][] INPUT_POS = {
        {31, 24}, {49, 24}, {31, 42}, {49, 42}
    };
    private static final int RESULT_X = 119;
    private static final int RESULT_Y = 34;

    public CriticalStationMenu(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, (CriticalStationBlockEntity) null);
    }

    public CriticalStationMenu(int syncId, Inventory playerInventory, CriticalStationBlockEntity blockEntity) {
        super(CriticalHitsMod.CRITICAL_STATION_MENU, syncId);
        if (blockEntity != null) {
            this.access = ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos());
        } else {
            this.access = ContainerLevelAccess.NULL;
        }
        this.player = playerInventory.player;

        // 4个输入槽
        for (int i = 0; i < 4; i++) {
            this.addSlot(new Slot(this.inputSlots, i, INPUT_POS[i][0], INPUT_POS[i][1]) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return isCriticalEnchantedBook(stack);
                }
            });
        }

        // 输出槽
        this.addSlot(new Slot(this.resultSlots, 0, RESULT_X, RESULT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public void onTake(Player player, ItemStack stack) {
                // 取走输出时消耗4本输入书
                for (int i = 0; i < 4; i++) {
                    inputSlots.getItem(i).shrink(1);
                }
                // 播放附魔台附魔成功声音
                access.execute((level, pos) ->
                    level.playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE,
                        SoundSource.BLOCKS, 1.0F, level.getRandom().nextFloat() * 0.4F + 1.2F));
                super.onTake(player, stack);
            }
        });

        // 玩家背包
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        // 快捷栏
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
    }

    @Override
    public void slotsChanged(Container container) {
        super.slotsChanged(container);
        if (container == this.inputSlots) {
            this.updateResult();
        }
    }

    /**
     * 更新输出槽：检查4本输入书是否满足合成条件。
     */
    private void updateResult() {
        ItemStack result = ItemStack.EMPTY;

        try {
            // 检查4个输入槽是否都有物品
            boolean allFilled = true;
            for (int i = 0; i < 4; i++) {
                if (this.inputSlots.getItem(i).isEmpty()) {
                    allFilled = false;
                    break;
                }
            }

            if (allFilled) {
                // 获取第一本书的暴击附魔类型和等级
                Identifier firstType = null;
                int firstLevel = -1;

                for (int i = 0; i < 4; i++) {
                    ItemStack stack = this.inputSlots.getItem(i);
                    Identifier type = getCriticalEnchantmentType(stack);
                    int level = getCriticalEnchantmentLevel(stack);
                    if (type == null || level <= 0) {
                        allFilled = false;
                        break;
                    }
                    if (firstType == null) {
                        firstType = type;
                        firstLevel = level;
                    } else if (!type.equals(firstType) || level != firstLevel) {
                        allFilled = false;
                        break;
                    }
                }

                if (allFilled && firstType != null && firstLevel >= 5 && firstLevel < getMaxLevel(firstType)) {
                    // 获取附魔Holder，确保不为null
                    Holder<Enchantment> holder = getEnchantmentHolder(firstType);
                    if (holder != null) {
                        // 合成高一级的附魔书
                        result = new ItemStack(Items.ENCHANTED_BOOK);
                        ItemEnchantments.Mutable enchants = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
                        enchants.set(holder, firstLevel + 1);
                        result.set(DataComponents.STORED_ENCHANTMENTS, enchants.toImmutable());
                    }
                }
            }
        } catch (Exception e) {
            // 捕获所有异常，防止崩溃
            CriticalHitsMod.LOGGER.warn("[Modern Damage System] 暴击台更新输出时发生异常", e);
            result = ItemStack.EMPTY;
        }

        this.resultSlots.setItem(0, result);
        this.broadcastChanges();
    }

    /**
     * 检查物品是否是本mod的附魔书（所有10个系列）。
     */
    public static boolean isCriticalEnchantedBook(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.is(Items.ENCHANTED_BOOK)) {
            return false;
        }
        return getCriticalEnchantmentType(stack) != null;
    }

    /**
     * 检查附魔是否是本mod的附魔（所有10个系列）。
     */
    private static boolean isModEnchantment(Holder<Enchantment> ench) {
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
     * 获取附魔书上的本mod附魔类型（如果有多个则返回null）。
     */
    public static Identifier getCriticalEnchantmentType(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        // 只处理附魔书，武器等物品的附魔存在ENCHANTMENTS组件中，直接返回null
        if (!stack.is(Items.ENCHANTED_BOOK)) return null;
        ItemEnchantments enchants = stack.get(DataComponents.STORED_ENCHANTMENTS);
        if (enchants == null || enchants.isEmpty()) return null;

        Identifier type = null;
        for (Object2IntMap.Entry<Holder<Enchantment>> entry : enchants.entrySet()) {
            Holder<Enchantment> ench = entry.getKey();
            if (ench == null) continue;
            if (isModEnchantment(ench)) {
                if (type != null) return null; // 多本mod附魔，不允许
                type = getEnchantmentId(ench);
            } else {
                return null; // 含非mod附魔，不允许
            }
        }
        return type;
    }

    /**
     * 获取附魔书上的本mod附魔等级。
     */
    public static int getCriticalEnchantmentLevel(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0;
        // 只处理附魔书
        if (!stack.is(Items.ENCHANTED_BOOK)) return 0;
        ItemEnchantments enchants = stack.get(DataComponents.STORED_ENCHANTMENTS);
        if (enchants == null || enchants.isEmpty()) return 0;
        for (Object2IntMap.Entry<Holder<Enchantment>> entry : enchants.entrySet()) {
            Holder<Enchantment> ench = entry.getKey();
            if (ench == null) continue;
            if (isModEnchantment(ench)) {
                return entry.getIntValue();
            }
        }
        return 0;
    }

    private static Identifier getEnchantmentId(Holder<Enchantment> holder) {
        return holder.unwrapKey().map(key -> key.identifier()).orElse(null);
    }

    /**
     * 获取附魔的最高等级。
     * 生命吸取、易伤、最大生命值最高10级，其他最高20级。
     */
    private static int getMaxLevel(Identifier enchantmentId) {
        if (enchantmentId.equals(CriticalHitsEnchantments.LIFE_STEAL_ID)
            || enchantmentId.equals(CriticalHitsEnchantments.VULNERABILITY_ID)
            || enchantmentId.equals(CriticalHitsEnchantments.MAX_HEALTH_ID)
            || enchantmentId.equals(CriticalHitsEnchantments.MOVEMENT_SPEED_ID)
            || enchantmentId.equals(CriticalHitsEnchantments.ATTACK_RANGE_ID)
            || enchantmentId.equals(CriticalHitsEnchantments.ATTACK_SPEED_CURSE_ID)
            || enchantmentId.equals(CriticalHitsEnchantments.MOVEMENT_SPEED_CURSE_ID)
            || enchantmentId.equals(CriticalHitsEnchantments.ATTACK_RANGE_CURSE_ID)) {
            return 10;
        }
        if (enchantmentId.equals(CriticalHitsEnchantments.ATTACK_SPEED_ID)) {
            return 15;
        }
        return 20;
    }

    private static Holder<Enchantment> getEnchantmentHolder(Identifier id) {
        return CriticalHitsMod.getEnchantmentHolder(id);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            boolean moved = false;

            if (index == 4) {
                // 输出槽：移到玩家背包
                if (!this.moveItemStackTo(stack, 5, 41, true)) {
                    return ItemStack.EMPTY;
                }
                slot.onTake(player, stack);
                moved = true;
            } else if (index < 4) {
                // 输入槽：移到玩家背包
                if (this.moveItemStackTo(stack, 5, 41, false)) {
                    moved = true;
                }
            } else {
                // 玩家背包：如果是暴击附魔书移到输入槽
                if (isCriticalEnchantedBook(stack)) {
                    if (this.moveItemStackTo(stack, 0, 4, false)) {
                        moved = true;
                    }
                }
                // 非暴击附魔书不移动，moved保持false
            }

            if (moved) {
                if (stack.isEmpty()) {
                    slot.set(ItemStack.EMPTY);
                } else {
                    slot.setChanged();
                }
            } else {
                // 物品没有被移动，返回EMPTY（防止客户端和服务端不同步导致死循环）
                return ItemStack.EMPTY;
            }
        }
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.access.evaluate((level, pos) ->
            level.getBlockState(pos).is(CriticalHitsMod.CRITICAL_STATION_BLOCK)
                && player.distanceToSqr((double) pos.getX() + 0.5, (double) pos.getY() + 0.5, (double) pos.getZ() + 0.5) <= 64.0,
            true);
    }
}
