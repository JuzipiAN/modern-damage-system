package com.juzipi.criticalhits.neoforge;

import com.juzipi.criticalhits.CriticalHitsMod;
import com.juzipi.criticalhits.MdsCraftingSelfTest;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.LootTableLoadEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/**
 * NeoForge 平台入口类。
 *
 * 2.0.5 关键修复：注册必须在 {@link RegisterEvent} 中进行。
 * 旧实现把 {@code new Item()} / {@code new Block()} 放在 {@code CriticalHitsMod} 的静态初始化里，
 * 而 NeoForge 在 mod 构造阶段注册表已经 freeze，于是必然崩溃：
 *   IllegalStateException: Registry is already frozen
 *     at MappedRegistry.createIntrusiveHolder &lt;- Item.&lt;init&gt; &lt;- CriticalHitsMod.&lt;clinit&gt;
 * 现在对象创建时机由 RegisterEvent 决定（供应器在事件中执行），双端共用
 * {@link CriticalHitsMod#registerAll(CriticalHitsMod.Registrar)}。
 */
@Mod("criticalhits")
public class CriticalHitsModNeoForge {

    public CriticalHitsModNeoForge(IEventBus modEventBus) {
        // 设置平台 helper
        CriticalHitsMod.platformIsModLoaded = id -> net.neoforged.fml.ModList.get().getModFileById(id) != null;
        CriticalHitsMod.platformRegistryAccess = () -> {
            net.minecraft.server.MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            return server != null ? server.registryAccess() : null;
        };

        // 调用通用初始化（配置加载、兼容性检查；不再在这里创建/注册游戏对象）
        CriticalHitsMod.init();

        // 注册游戏对象（mod 总线，RegisterEvent 期间注册表允许写入）
        modEventBus.addListener(this::onRegister);
        modEventBus.addListener(this::onBuildCreativeTab);

        // 游戏总线事件
        NeoForge.EVENT_BUS.addListener(this::onLootTableLoad);
        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);
        NeoForge.EVENT_BUS.addListener(this::onServerStarted);
        NeoForge.EVENT_BUS.addListener(this::onServerStopped);
        NeoForge.EVENT_BUS.addListener(this::onServerTick);

        CriticalHitsMod.LOGGER.info("[MDS] NeoForge platform entrypoint initialized");
    }

    /** 把 common 的注册清单接到 NeoForge 的 RegisterEvent 上。 */
    private void onRegister(RegisterEvent event) {
        CriticalHitsMod.registerAll((registryKey, name, supplier) -> {
            // RegisterEvent 会对每个注册表分别触发，只处理与本次事件匹配的注册表
            if (!event.getRegistryKey().equals(registryKey)) return;
            registerToEvent(event, registryKey, name, supplier);
        });
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void registerToEvent(RegisterEvent event,
                                        net.minecraft.resources.ResourceKey<? extends net.minecraft.core.Registry<?>> registryKey,
                                        String name, java.util.function.Supplier<?> supplier) {
        Object value = supplier.get();
        if (value == null) {
            CriticalHitsMod.LOGGER.warn("[Modern Damage System] 注册对象不可用，已跳过: " + name);
            return;
        }
        event.register((net.minecraft.resources.ResourceKey) registryKey,
            Identifier.fromNamespaceAndPath(CriticalHitsMod.MOD_ID, name), () -> value);
        CriticalHitsMod.LOGGER.info("[Modern Damage System] 注册成功: "
            + registryKey.identifier() + " / " + name);
    }

    private void onServerStarted(ServerStartedEvent event) {
        MdsCraftingSelfTest.autoRun(event.getServer());
        com.juzipi.criticalhits.MdsAdvancements.selfTest(event.getServer());
    }

    private void onServerTick(net.neoforged.neoforge.event.tick.ServerTickEvent.Post event) {
        com.juzipi.criticalhits.MdsAdvancements.tick(event.getServer());
    }

    private void onServerStopped(ServerStoppedEvent event) {
        CriticalHitsMod.platformRegistryAccess = () -> null;
    }

    private void onBuildCreativeTab(BuildCreativeModeTabContentsEvent event) {
        var key = event.getTabKey();
        if (key.equals(CriticalHitsMod.CRITICAL_TAB)) CriticalHitsMod.fillCriticalTab(event);
        else if (key.equals(CriticalHitsMod.ATTACK_TAB)) CriticalHitsMod.fillAttackTab(event);
        else if (key.equals(CriticalHitsMod.DEFENSE_TAB)) CriticalHitsMod.fillDefenseTab(event);
        else if (key.equals(CriticalHitsMod.UTILITY_TAB)) CriticalHitsMod.fillUtilityTab(event);
        else if (key.equals(CriticalHitsMod.ATTRIBUTE_TAB)) CriticalHitsMod.fillAttributeTab(event);
        else if (key.equals(CriticalHitsMod.PRESET_TAB)) CriticalHitsMod.fillPresetTab(event);
    }

    private void onLootTableLoad(LootTableLoadEvent event) {
        var keyId = event.getName();
        var key = keyId != null ? net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE, keyId) : null;
        if (key != null && CriticalHitsMod.shouldInjectLootTable(key)) {
            try {
                var table = event.getTable();
                var builder = net.minecraft.world.level.storage.loot.LootTable.lootTable();
                // 复制原有 pools（用反射获取）
                var poolsField = net.minecraft.world.level.storage.loot.LootTable.class.getDeclaredField("pools");
                poolsField.setAccessible(true);
                @SuppressWarnings("unchecked")
                var pools = (java.util.List<?>) poolsField.get(table);
                for (var pool : pools) {
                    // LootPool 可能需要转换为 Builder
                    if (pool instanceof net.minecraft.world.level.storage.loot.LootPool lootPool) {
                        // 用反射调用 withPool
                        var withPoolMethod = builder.getClass().getMethod("withPool", net.minecraft.world.level.storage.loot.LootPool.class);
                        withPoolMethod.invoke(builder, lootPool);
                    }
                }
                // 注入 MDS 战利品
                CriticalHitsMod.injectLootToTable(builder, null);
                event.setTable(builder.build());
                CriticalHitsMod.LOGGER.info("[MDS Loot] Injected (NeoForge): " + key);
            } catch (Exception e) {
                CriticalHitsMod.LOGGER.warn("[MDS Loot] Failed to inject: " + key, e);
            }
        }
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        CriticalHitsMod.registerCommands(event.getDispatcher());
    }
}
