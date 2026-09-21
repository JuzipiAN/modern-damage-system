package com.juzipi.criticalhits;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

/**
 * 暴击交易币（CC）。
 * 暴击师村民的专用货币，仅通过村民收购腐肉/骨头获得，不可合成、不可战利品获取。
 */
public class CriticalCoinItem extends Item {

    public CriticalCoinItem() {
        this("critical_coin");
    }

    public CriticalCoinItem(String id) {
        super(new Item.Properties()
            .setId(ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(CriticalHitsEnchantments.MOD_ID, id)))
            .stacksTo(64));
    }
}
