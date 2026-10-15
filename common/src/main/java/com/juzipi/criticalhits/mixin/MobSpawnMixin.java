package com.juzipi.criticalhits.mixin;

import com.juzipi.criticalhits.CriticalHitsConfig;
import com.juzipi.criticalhits.CriticalHitsEnchantments;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Random;

/**
 * 敌对生物生成Mixin。
 * 根据难度给生物的武器和护甲添加现代化伤害系统附魔。
 * 简单难度：不添加
 * 普通难度：1-10级，低等级权重高
 * 困难难度：10-20级，低等级权重高
 */
@Mixin(Mob.class)
public class MobSpawnMixin {
    private static final Random RANDOM = new Random();

    @Inject(method = "finalizeSpawn", at = @At("TAIL"))
    private void criticalhits_onFinalizeSpawn(CallbackInfoReturnable<?> ci) {
        Mob mob = (Mob) (Object) this;
        Difficulty difficulty = mob.level().getDifficulty();

        // 简单难度不添加附魔
        if (difficulty == Difficulty.PEACEFUL || difficulty == Difficulty.EASY) return;

        // 只对敌对生物生效，排除村民、商人等友好生物
        if (!(mob instanceof net.minecraft.world.entity.monster.Enemy)) return;

        // 按配置概率添加附魔
        if (RANDOM.nextFloat() > CriticalHitsConfig.get().mob_enchant_chance) return;

        boolean isHard = difficulty == Difficulty.HARD;

        // v2.0.0 如果怪物被选中要附魔但主手没武器，强制给一把随机近战武器
        ItemStack weapon = mob.getItemBySlot(EquipmentSlot.MAINHAND);
        if (weapon.isEmpty() && RANDOM.nextFloat() < 0.6f) {
            weapon = getRandomWeapon(isHard);
            mob.setItemSlot(EquipmentSlot.MAINHAND, weapon);
            mob.setDropChance(EquipmentSlot.MAINHAND, 0.085f); // 小概率掉落
        }

        // v2.0.0 如果怪物被选中要附魔但没穿盔甲，强制给随机盔甲
        if (!hasAnyArmor(mob) && RANDOM.nextFloat() < 0.6f) {
            equipRandomArmor(mob, isHard);
        }

        // 给主手武器添加攻击类附魔
        if (!weapon.isEmpty() && isWeapon(weapon)) {
            addRandomAttackEnchantment(mob, weapon, isHard);
        }

        // 给护甲添加防御类附魔
        EquipmentSlot[] armorSlots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        for (EquipmentSlot slot : armorSlots) {
            ItemStack armor = mob.getItemBySlot(slot);
            if (!armor.isEmpty()) {
                addRandomDefenseEnchantment(mob, armor, isHard);
            }
        }

        // 穿护甲的生物按防御提升生命值（困难难度）
        if (isHard && hasAnyArmor(mob)) {
            float bonusHealth = CriticalHitsConfig.get().mob_hard_bonus_health;
            mob.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH)
                .addTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                    net.minecraft.resources.Identifier.fromNamespaceAndPath("criticalhits", "mob_health_bonus"),
                    bonusHealth,
                    net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE
                ));
            mob.setHealth(mob.getMaxHealth());
        }
    }

    private boolean isWeapon(ItemStack stack) {
        return stack.is(net.minecraft.world.item.Items.WOODEN_SWORD) || stack.is(net.minecraft.world.item.Items.STONE_SWORD)
            || stack.is(net.minecraft.world.item.Items.IRON_SWORD) || stack.is(net.minecraft.world.item.Items.GOLDEN_SWORD)
            || stack.is(net.minecraft.world.item.Items.DIAMOND_SWORD) || stack.is(net.minecraft.world.item.Items.NETHERITE_SWORD)
            || stack.is(net.minecraft.world.item.Items.WOODEN_AXE) || stack.is(net.minecraft.world.item.Items.STONE_AXE)
            || stack.is(net.minecraft.world.item.Items.IRON_AXE) || stack.is(net.minecraft.world.item.Items.GOLDEN_AXE)
            || stack.is(net.minecraft.world.item.Items.DIAMOND_AXE) || stack.is(net.minecraft.world.item.Items.NETHERITE_AXE);
    }

    /**
     * v2.0.0 根据难度获取随机近战武器。
     * 普通难度：木剑、石剑、铁剑（低品质权重高）
     * 困难难度：石剑、铁剑、钻石剑（高品质权重高）
     */
    private ItemStack getRandomWeapon(boolean isHard) {
        net.minecraft.world.item.Item[] weapons;
        if (isHard) {
            // 困难模式：石剑30%、铁剑40%、钻石剑25%、下界合金剑5%
            float r = RANDOM.nextFloat();
            if (r < 0.30f) return new ItemStack(net.minecraft.world.item.Items.STONE_SWORD);
            if (r < 0.70f) return new ItemStack(net.minecraft.world.item.Items.IRON_SWORD);
            if (r < 0.95f) return new ItemStack(net.minecraft.world.item.Items.DIAMOND_SWORD);
            return new ItemStack(net.minecraft.world.item.Items.NETHERITE_SWORD);
        } else {
            // 普通模式：木剑40%、石剑35%、铁剑20%、金剑5%
            float r = RANDOM.nextFloat();
            if (r < 0.40f) return new ItemStack(net.minecraft.world.item.Items.WOODEN_SWORD);
            if (r < 0.75f) return new ItemStack(net.minecraft.world.item.Items.STONE_SWORD);
            if (r < 0.95f) return new ItemStack(net.minecraft.world.item.Items.IRON_SWORD);
            return new ItemStack(net.minecraft.world.item.Items.GOLDEN_SWORD);
        }
    }

    /**
     * v2.0.0 给怪物装备随机盔甲。
     * 随机给2-4个槽位装备盔甲，根据难度调整品质。
     */
    private void equipRandomArmor(Mob mob, boolean isHard) {
        EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        int armorCount = 2 + RANDOM.nextInt(3); // 2-4件
        int equipped = 0;
        for (EquipmentSlot slot : slots) {
            if (equipped >= armorCount) break;
            if (RANDOM.nextFloat() < 0.8f) {
                ItemStack armor = getRandomArmorPiece(slot, isHard);
                if (!armor.isEmpty()) {
                    mob.setItemSlot(slot, armor);
                    mob.setDropChance(slot, 0.085f);
                    equipped++;
                }
            }
        }
    }

    private ItemStack getRandomArmorPiece(EquipmentSlot slot, boolean isHard) {
        String material;
        if (isHard) {
            float r = RANDOM.nextFloat();
            if (r < 0.20f) material = "chainmail";
            else if (r < 0.60f) material = "iron";
            else if (r < 0.90f) material = "diamond";
            else material = "netherite";
        } else {
            float r = RANDOM.nextFloat();
            if (r < 0.40f) material = "leather";
            else if (r < 0.70f) material = "chainmail";
            else if (r < 0.95f) material = "iron";
            else material = "golden";
        }
        net.minecraft.world.item.Item[] set = getArmorSet(material);
        int index = switch (slot) {
            case HEAD -> 0;
            case CHEST -> 1;
            case LEGS -> 2;
            case FEET -> 3;
            default -> 0;
        };
        if (index < set.length && set[index] != null) {
            return new ItemStack(set[index]);
        }
        return ItemStack.EMPTY;
    }

    private net.minecraft.world.item.Item[] getArmorSet(String material) {
        return switch (material) {
            case "leather" -> new net.minecraft.world.item.Item[]{
                net.minecraft.world.item.Items.LEATHER_HELMET,
                net.minecraft.world.item.Items.LEATHER_CHESTPLATE,
                net.minecraft.world.item.Items.LEATHER_LEGGINGS,
                net.minecraft.world.item.Items.LEATHER_BOOTS
            };
            case "chainmail" -> new net.minecraft.world.item.Item[]{
                net.minecraft.world.item.Items.CHAINMAIL_HELMET,
                net.minecraft.world.item.Items.CHAINMAIL_CHESTPLATE,
                net.minecraft.world.item.Items.CHAINMAIL_LEGGINGS,
                net.minecraft.world.item.Items.CHAINMAIL_BOOTS
            };
            case "iron" -> new net.minecraft.world.item.Item[]{
                net.minecraft.world.item.Items.IRON_HELMET,
                net.minecraft.world.item.Items.IRON_CHESTPLATE,
                net.minecraft.world.item.Items.IRON_LEGGINGS,
                net.minecraft.world.item.Items.IRON_BOOTS
            };
            case "golden" -> new net.minecraft.world.item.Item[]{
                net.minecraft.world.item.Items.GOLDEN_HELMET,
                net.minecraft.world.item.Items.GOLDEN_CHESTPLATE,
                net.minecraft.world.item.Items.GOLDEN_LEGGINGS,
                net.minecraft.world.item.Items.GOLDEN_BOOTS
            };
            case "diamond" -> new net.minecraft.world.item.Item[]{
                net.minecraft.world.item.Items.DIAMOND_HELMET,
                net.minecraft.world.item.Items.DIAMOND_CHESTPLATE,
                net.minecraft.world.item.Items.DIAMOND_LEGGINGS,
                net.minecraft.world.item.Items.DIAMOND_BOOTS
            };
            case "netherite" -> new net.minecraft.world.item.Item[]{
                net.minecraft.world.item.Items.NETHERITE_HELMET,
                net.minecraft.world.item.Items.NETHERITE_CHESTPLATE,
                net.minecraft.world.item.Items.NETHERITE_LEGGINGS,
                net.minecraft.world.item.Items.NETHERITE_BOOTS
            };
            default -> new net.minecraft.world.item.Item[4];
        };
    }

    private boolean hasAnyArmor(LivingEntity entity) {
        return !entity.getItemBySlot(EquipmentSlot.HEAD).isEmpty()
            || !entity.getItemBySlot(EquipmentSlot.CHEST).isEmpty()
            || !entity.getItemBySlot(EquipmentSlot.LEGS).isEmpty()
            || !entity.getItemBySlot(EquipmentSlot.FEET).isEmpty();
    }

    /**
     * 给武器添加随机攻击类附魔。
     * 普通难度：1-10级，低等级权重高
     * 困难难度：10-20级，低等级权重高
     */
    private void addRandomAttackEnchantment(Mob mob, ItemStack stack, boolean isHard) {
        // 随机选择一种攻击附魔（包含v2.0.0新附魔）
        net.minecraft.resources.Identifier[] attackEnchants = {
            CriticalHitsEnchantments.CRITICAL_RATE_ID,
            CriticalHitsEnchantments.CRITICAL_DAMAGE_ID,
            CriticalHitsEnchantments.PERCENTAGE_DAMAGE_ID,
            CriticalHitsEnchantments.ARMOR_PENETRATION_ID,
            CriticalHitsEnchantments.LIFE_STEAL_ID,
            CriticalHitsEnchantments.VULNERABILITY_ID,
            CriticalHitsEnchantments.TRUE_DAMAGE_ID,
            CriticalHitsEnchantments.SPLASH_DAMAGE_ID,
            CriticalHitsEnchantments.EXECUTE_ID,
            CriticalHitsEnchantments.FROST_ID,
            CriticalHitsEnchantments.POISON_ID,
            CriticalHitsEnchantments.ARMOR_SHATTER_ID
        };
        net.minecraft.resources.Identifier enchantId = attackEnchants[RANDOM.nextInt(attackEnchants.length)];

        // 低等级权重高的随机等级
        int level = getWeightedRandomLevel(isHard);
        if (level <= 0) return;

        // 各附魔最高等级限制
        if (enchantId == CriticalHitsEnchantments.LIFE_STEAL_ID
            || enchantId == CriticalHitsEnchantments.VULNERABILITY_ID
            || enchantId == CriticalHitsEnchantments.TRUE_DAMAGE_ID
            || enchantId == CriticalHitsEnchantments.FROST_ID
            || enchantId == CriticalHitsEnchantments.POISON_ID
            || enchantId == CriticalHitsEnchantments.ARMOR_SHATTER_ID) {
            level = Math.min(level, 10);
        }
        if (enchantId == CriticalHitsEnchantments.SPLASH_DAMAGE_ID) {
            level = Math.min(level, 5);
        }
        if (enchantId == CriticalHitsEnchantments.EXECUTE_ID) {
            level = Math.min(level, 3);
        }

        addEnchantment(mob, stack, enchantId, level);
    }

    /**
     * 给护甲添加随机防御类附魔。
     */
    private void addRandomDefenseEnchantment(Mob mob, ItemStack stack, boolean isHard) {
        net.minecraft.resources.Identifier[] defenseEnchants = {
            CriticalHitsEnchantments.CRITICAL_RESISTANCE_ID,
            CriticalHitsEnchantments.DAMAGE_REDUCTION_ID,
            CriticalHitsEnchantments.ARMOR_DEFENSE_ID,
            CriticalHitsEnchantments.MAX_HEALTH_ID,
            CriticalHitsEnchantments.THORNS_REFLECT_ID,
            CriticalHitsEnchantments.SHIELD_ID,
            CriticalHitsEnchantments.KNOCKBACK_RESISTANCE_ID
        };
        net.minecraft.resources.Identifier enchantId = defenseEnchants[RANDOM.nextInt(defenseEnchants.length)];

        int level = getWeightedRandomLevel(isHard);
        if (level <= 0) return;

        // 各附魔最高等级限制
        if (enchantId == CriticalHitsEnchantments.MAX_HEALTH_ID
            || enchantId == CriticalHitsEnchantments.THORNS_REFLECT_ID
            || enchantId == CriticalHitsEnchantments.KNOCKBACK_RESISTANCE_ID) {
            level = Math.min(level, 10);
        }
        if (enchantId == CriticalHitsEnchantments.SHIELD_ID) {
            level = Math.min(level, 3);
        }

        addEnchantment(mob, stack, enchantId, level);
    }

    /**
     * 低等级权重高的随机等级。
     * 普通：1-10级，困难：10-20级
     */
    private int getWeightedRandomLevel(boolean isHard) {
        int min = isHard ? 10 : 1;
        int max = isHard ? 20 : 10;
        int range = max - min + 1;

        // 低等级权重高：用平方分布
        double r = RANDOM.nextDouble();
        int level = min + (int)(range * (1 - Math.sqrt(r)));
        return Math.max(min, Math.min(max, level));
    }

    /**
     * 给物品添加附魔。
     */
    private void addEnchantment(Mob mob, ItemStack stack, net.minecraft.resources.Identifier enchantId, int level) {
        try {
            // 从动态注册表获取附魔Holder
            var registryAccess = mob.level().registryAccess();
            var enchantmentOpt = registryAccess.lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
                .get(enchantId);
            if (enchantmentOpt.isEmpty()) return;

            ItemEnchantments enchants = stack.get(net.minecraft.core.component.DataComponents.ENCHANTMENTS);
            if (enchants == null) {
                enchants = ItemEnchantments.EMPTY;
            }
            ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(enchants);
            mutable.set(enchantmentOpt.get(), level);
            stack.set(net.minecraft.core.component.DataComponents.ENCHANTMENTS, mutable.toImmutable());
        } catch (Exception e) {
            // 忽略附魔添加失败
        }
    }
}
