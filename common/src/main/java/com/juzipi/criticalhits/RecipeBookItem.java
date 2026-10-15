package com.juzipi.criticalhits;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

/**
 * 现代化伤害系统配方书。
 * 查看所有附魔和物品的合成配方。
 * 右键打开配方书GUI（通过Mixin拦截右键实现）。
 */
public class RecipeBookItem extends Item {

    public RecipeBookItem() {
        super(new Item.Properties()
            .setId(ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(CriticalHitsMod.MOD_ID, "recipe_book")))
            .stacksTo(64));
    }
}
