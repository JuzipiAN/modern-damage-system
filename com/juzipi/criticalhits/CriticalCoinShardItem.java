package com.juzipi.criticalhits;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

/**
 * 暴击交易币碎片（CC碎片）。
 * 可通过战利品获取，用于合成暴击交易币。
 */
public class CriticalCoinShardItem extends Item {

    public CriticalCoinShardItem() {
        super(new Item.Properties()
            .setId(ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(CriticalHitsEnchantments.MOD_ID, "critical_coin_shard")))
            .stacksTo(64));
    }
}
