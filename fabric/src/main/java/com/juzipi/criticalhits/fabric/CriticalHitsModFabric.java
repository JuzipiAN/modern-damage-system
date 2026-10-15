package com.juzipi.criticalhits.fabric;

import com.juzipi.criticalhits.CriticalHitsMod;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Fabric 平台入口类。
 * 负责注册 Fabric 特定的事件，并调用 common 端的通用逻辑。
 */
public class CriticalHitsModFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        // 设置平台 helper
        CriticalHitsMod.platformIsModLoaded = id -> FabricLoader.getInstance().isModLoaded(id);

        // 调用通用初始化（配置、兼容性检查）
        CriticalHitsMod.init();

        // 注册所有游戏对象（Fabric 在 onInitialize 阶段注册表尚未冻结，可直接注册）
        CriticalHitsMod.registerAll(CriticalHitsMod::fabricRegister);

        // 注册创造模式标签页内容
        CreativeModeTabEvents.modifyOutputEvent(CriticalHitsMod.CRITICAL_TAB)
            .register(output -> CriticalHitsMod.fillCriticalTab(output));
        CreativeModeTabEvents.modifyOutputEvent(CriticalHitsMod.ATTACK_TAB)
            .register(output -> CriticalHitsMod.fillAttackTab(output));
        CreativeModeTabEvents.modifyOutputEvent(CriticalHitsMod.DEFENSE_TAB)
            .register(output -> CriticalHitsMod.fillDefenseTab(output));
        CreativeModeTabEvents.modifyOutputEvent(CriticalHitsMod.UTILITY_TAB)
            .register(output -> CriticalHitsMod.fillUtilityTab(output));
        CreativeModeTabEvents.modifyOutputEvent(CriticalHitsMod.ATTRIBUTE_TAB)
            .register(output -> CriticalHitsMod.fillAttributeTab(output));
        CreativeModeTabEvents.modifyOutputEvent(CriticalHitsMod.PRESET_TAB)
            .register(output -> CriticalHitsMod.fillPresetTab(output));

        // 注册战利品注入
        LootTableEvents.MODIFY.register((key, tableBuilder, source, registries) -> {
            if (CriticalHitsMod.shouldInjectLootTable(key)) {
                CriticalHitsMod.injectLootToTable(tableBuilder, registries);
                CriticalHitsMod.LOGGER.info("[MDS Loot] Injected: " + key);
            }
        });

        // 注册命令
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            CriticalHitsMod.registerCommands(dispatcher);
        });

        // 自测入口：仅在 -Dmds.selftest=true 或 MDS_SELFTEST=true 时执行（开发/回归验证用）
        // achievements: scan every 20 ticks
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(
            server -> com.juzipi.criticalhits.MdsAdvancements.tick(server));

        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STARTED.register(
            server -> {
                com.juzipi.criticalhits.MdsCraftingSelfTest.autoRun(server);
                com.juzipi.criticalhits.MdsAdvancements.selfTest(server);
            });

        // 提供当前注册表访问（服务端逻辑用，避免走客户端类）
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STARTING.register(
            server -> CriticalHitsMod.platformRegistryAccess = server::registryAccess);
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(
            server -> CriticalHitsMod.platformRegistryAccess = () -> null);

        CriticalHitsMod.LOGGER.info("[MDS] Fabric platform entrypoint initialized");
    }
}
