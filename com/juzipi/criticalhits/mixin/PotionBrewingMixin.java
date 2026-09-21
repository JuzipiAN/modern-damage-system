package com.juzipi.criticalhits.mixin;

import com.juzipi.criticalhits.CriticalHitsMod;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.alchemy.Potions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 酿造配方Mixin。
 * 注入到PotionBrewing.addVanillaMixes，添加易伤药水酿造配方。
 * 粗制药水 + 蜘蛛眼 = 易伤药水
 */
@Mixin(PotionBrewing.class)
public class PotionBrewingMixin {

    @Inject(method = "addVanillaMixes", at = @At("TAIL"))
    private static void criticalhits_addCustomMixes(PotionBrewing.Builder builder, CallbackInfo ci) {
        try {
            // 粗制药水 + 蜘蛛眼 = 易伤药水1级
            builder.addMix(
                Potions.AWKWARD,
                Items.SPIDER_EYE,
                BuiltInRegistries.POTION.wrapAsHolder(CriticalHitsMod.VULNERABILITY_POTION)
            );
            // 易伤药水1级 + 萤石粉 = 易伤药水2级
            builder.addMix(
                BuiltInRegistries.POTION.wrapAsHolder(CriticalHitsMod.VULNERABILITY_POTION),
                Items.GLOWSTONE_DUST,
                BuiltInRegistries.POTION.wrapAsHolder(CriticalHitsMod.VULNERABILITY_POTION_2)
            );
            // 易伤药水2级 + 萤石粉 = 易伤药水3级
            builder.addMix(
                BuiltInRegistries.POTION.wrapAsHolder(CriticalHitsMod.VULNERABILITY_POTION_2),
                Items.GLOWSTONE_DUST,
                BuiltInRegistries.POTION.wrapAsHolder(CriticalHitsMod.VULNERABILITY_POTION_3)
            );
            // 易伤药水3级 + 萤石粉 = 易伤药水3级（最高级，不再升级）
            // 冰霜药水酿造配方
            if (CriticalHitsMod.FROST_POTION != null) {
                // 粗制药水 + 蓝冰 = 冰霜药水1级
                builder.addMix(
                    Potions.AWKWARD,
                    Items.BLUE_ICE,
                    BuiltInRegistries.POTION.wrapAsHolder(CriticalHitsMod.FROST_POTION)
                );
                // 冰霜药水1级 + 萤石粉 = 冰霜药水2级
                builder.addMix(
                    BuiltInRegistries.POTION.wrapAsHolder(CriticalHitsMod.FROST_POTION),
                    Items.GLOWSTONE_DUST,
                    BuiltInRegistries.POTION.wrapAsHolder(CriticalHitsMod.FROST_POTION_2)
                );
                // 冰霜药水2级 + 萤石粉 = 冰霜药水3级
                builder.addMix(
                    BuiltInRegistries.POTION.wrapAsHolder(CriticalHitsMod.FROST_POTION_2),
                    Items.GLOWSTONE_DUST,
                    BuiltInRegistries.POTION.wrapAsHolder(CriticalHitsMod.FROST_POTION_3)
                );
                CriticalHitsMod.LOGGER.info("[Modern Damage System] 冰霜药水酿造配方注册成功（1/2/3级）");
            }

            // 剧毒药水酿造配方
            if (CriticalHitsMod.POISON_PLUS_POTION != null) {
                // 粗制药水 + 蜘蛛眼 + 发酵蛛眼 = 剧毒药水1级（需要先加蜘蛛眼，再加发酵蛛眼）
                // 由于Minecraft酿造台一次只能加一种材料，这里用：剧毒药水 = 中毒药水 + 发酵蛛眼
                builder.addMix(
                    Potions.POISON,
                    Items.FERMENTED_SPIDER_EYE,
                    BuiltInRegistries.POTION.wrapAsHolder(CriticalHitsMod.POISON_PLUS_POTION)
                );
                // 剧毒药水1级 + 萤石粉 = 剧毒药水2级
                builder.addMix(
                    BuiltInRegistries.POTION.wrapAsHolder(CriticalHitsMod.POISON_PLUS_POTION),
                    Items.GLOWSTONE_DUST,
                    BuiltInRegistries.POTION.wrapAsHolder(CriticalHitsMod.POISON_PLUS_POTION_2)
                );
                // 剧毒药水2级 + 萤石粉 = 剧毒药水3级
                builder.addMix(
                    BuiltInRegistries.POTION.wrapAsHolder(CriticalHitsMod.POISON_PLUS_POTION_2),
                    Items.GLOWSTONE_DUST,
                    BuiltInRegistries.POTION.wrapAsHolder(CriticalHitsMod.POISON_PLUS_POTION_3)
                );
                CriticalHitsMod.LOGGER.info("[Modern Damage System] 剧毒药水酿造配方注册成功（1/2/3级）");
            }

            // 破甲药水酿造配方
            if (CriticalHitsMod.ARMOR_BREAK_POTION != null) {
                // 粗制药水 + 铁锭 = 破甲药水1级
                builder.addMix(
                    Potions.AWKWARD,
                    Items.IRON_INGOT,
                    BuiltInRegistries.POTION.wrapAsHolder(CriticalHitsMod.ARMOR_BREAK_POTION)
                );
                // 破甲药水1级 + 萤石粉 = 破甲药水2级
                builder.addMix(
                    BuiltInRegistries.POTION.wrapAsHolder(CriticalHitsMod.ARMOR_BREAK_POTION),
                    Items.GLOWSTONE_DUST,
                    BuiltInRegistries.POTION.wrapAsHolder(CriticalHitsMod.ARMOR_BREAK_POTION_2)
                );
                // 破甲药水2级 + 萤石粉 = 破甲药水3级
                builder.addMix(
                    BuiltInRegistries.POTION.wrapAsHolder(CriticalHitsMod.ARMOR_BREAK_POTION_2),
                    Items.GLOWSTONE_DUST,
                    BuiltInRegistries.POTION.wrapAsHolder(CriticalHitsMod.ARMOR_BREAK_POTION_3)
                );
                CriticalHitsMod.LOGGER.info("[Modern Damage System] 破甲药水酿造配方注册成功（1/2/3级）");
            }

            CriticalHitsMod.LOGGER.info("[Modern Damage System] 易伤药水酿造配方注册成功（1/2/3级）");
        } catch (Exception e) {
            CriticalHitsMod.LOGGER.warn("[Modern Damage System] 易伤药水酿造配方注册失败", e);
        }
    }
}
