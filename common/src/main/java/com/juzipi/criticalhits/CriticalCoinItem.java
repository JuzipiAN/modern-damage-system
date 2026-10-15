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

    /** 使用该物品时要发放的成就 id（可为 null）。 */
    private final String achievementOnUse;

    public CriticalCoinItem(String id) {
        this(id, null);
    }

    public CriticalCoinItem(String id, String achievementOnUse) {
        super(new Item.Properties()
            .setId(ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(CriticalHitsEnchantments.MOD_ID, id)))
            .stacksTo(64));
        this.achievementOnUse = achievementOnUse;
    }

    /**
     * 2.2.0：MDS 配方书是<b>纯客户端界面</b>（用假菜单打开），服务端感知不到，
     * 所以「爱读书」改在这里发放：只要服务端收到一次使用（右键），就算打开过。
     */
    @Override
    public net.minecraft.world.InteractionResult use(
            net.minecraft.world.level.Level level,
            net.minecraft.world.entity.player.Player player,
            net.minecraft.world.InteractionHand hand) {
        if (achievementOnUse != null && !level.isClientSide()
                && player instanceof net.minecraft.server.level.ServerPlayer sp) {
            MdsAdvancements.award(sp, achievementOnUse);
        }
        return net.minecraft.world.InteractionResult.PASS;
    }
}
