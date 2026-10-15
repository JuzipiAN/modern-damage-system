package com.juzipi.criticalhits;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.rendering.v1.ExtractItemDecorationsCallback;

import net.minecraft.client.gui.screens.MenuScreens;

import java.lang.reflect.Method;

/**
 * Fabric 客户端初始化（2.0.5 起只保留 Fabric 专属胶水代码）。
 *
 * 平台无关的 HUD 渲染 / tooltip 逻辑已抽到 {@link MdsClientHooks}，
 * NeoForge 端复用同一份实现。
 */
public class CriticalHitsClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        CriticalHitsMod.LOGGER.info("[Modern Damage System] 客户端初始化 - 作者: {} | 代码指纹: {}",
            CriticalHitsMod.AUTHOR, CriticalHitsMod.CODE_FINGERPRINT);

        registerMenuScreens();
        registerHudEditor();

        // 物品 Tooltip：负等级名称红色、武器/装备信息、附魔折叠
        ItemTooltipCallback.EVENT.register((stack, context, flag, lines) ->
            MdsClientHooks.onItemTooltip(stack, lines));

        // 第二个 Tooltip 回调：删除附魔描述行（在本 mod 所有处理之后执行）
        ItemTooltipCallback.EVENT.register((stack, context, flag, lines) ->
            MdsClientHooks.stripEnchantDescriptions(stack, lines));

        // 物品装饰：附魔书右下角等级数字
        ExtractItemDecorationsCallback.EVENT.register((gfx, font, stack, x, y) ->
            MdsClientHooks.decorateEnchantedBook(gfx, font, stack, x, y));
    }

    /** 用反射注册 Screen（26.2 中 MenuScreens.register 是 private）。 */
    private void registerMenuScreens() {
        try {
            Class<?> screenConstructorClass = Class.forName("net.minecraft.client.gui.screens.MenuScreens$ScreenConstructor");
            Method registerMethod = MenuScreens.class.getDeclaredMethod("register",
                net.minecraft.world.inventory.MenuType.class, screenConstructorClass);
            registerMethod.setAccessible(true);

            Object stationConstructor = java.lang.reflect.Proxy.newProxyInstance(
                screenConstructorClass.getClassLoader(),
                new Class<?>[]{screenConstructorClass},
                (proxy, method, args) -> {
                    if (method.getName().equals("create")) {
                        return new CriticalStationScreen(
                            (CriticalStationMenu) args[0],
                            (net.minecraft.world.entity.player.Inventory) args[1],
                            (net.minecraft.network.chat.Component) args[2]);
                    }
                    if (method.isDefault()) {
                        return java.lang.reflect.InvocationHandler.invokeDefault(proxy, method, args);
                    }
                    return null;
                });
            registerMethod.invoke(null, CriticalHitsMod.CRITICAL_STATION_MENU, stationConstructor);
            CriticalHitsMod.LOGGER.info("[Modern Damage System] 暴击台界面注册成功");

            Object recipeBookConstructor = java.lang.reflect.Proxy.newProxyInstance(
                screenConstructorClass.getClassLoader(),
                new Class<?>[]{screenConstructorClass},
                (proxy, method, args) -> {
                    if (method.getName().equals("create")) {
                        return new RecipeBookScreen(
                            (RecipeBookMenu) args[0],
                            (net.minecraft.world.entity.player.Inventory) args[1],
                            (net.minecraft.network.chat.Component) args[2]);
                    }
                    if (method.isDefault()) {
                        return java.lang.reflect.InvocationHandler.invokeDefault(proxy, method, args);
                    }
                    return null;
                });
            registerMethod.invoke(null, CriticalHitsMod.RECIPE_BOOK_MENU, recipeBookConstructor);
            CriticalHitsMod.LOGGER.info("[Modern Damage System] 配方书界面注册成功");
        } catch (Exception e) {
            CriticalHitsMod.LOGGER.error("[Modern Damage System] 界面注册失败", e);
        }
    }

    /** 注册 HUD 编辑器按键与客户端 tick。 */
    private void registerHudEditor() {
        try {
            net.minecraft.client.KeyMapping toggle = net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper
                .registerKeyMapping(new net.minecraft.client.KeyMapping(
                    "key.criticalhits.toggle_hud", org.lwjgl.glfw.GLFW.GLFW_KEY_H,
                    com.juzipi.criticalhits.client.HudEditor.category()));
            net.minecraft.client.KeyMapping edit = net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper
                .registerKeyMapping(new net.minecraft.client.KeyMapping(
                    "key.criticalhits.edit_hud", org.lwjgl.glfw.GLFW.GLFW_KEY_C,
                    com.juzipi.criticalhits.client.HudEditor.category()));
            net.minecraft.client.KeyMapping config = net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper
                .registerKeyMapping(new net.minecraft.client.KeyMapping(
                    "key.criticalhits.open_config", org.lwjgl.glfw.GLFW.GLFW_KEY_K,
                    com.juzipi.criticalhits.client.HudEditor.category()));

            com.juzipi.criticalhits.client.HudEditor.init(toggle, edit, config);
            net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(
                com.juzipi.criticalhits.client.HudEditor::tick);
            CriticalHitsMod.LOGGER.info("[Modern Damage System] HUD编辑器注册成功");
        } catch (Exception e) {
            CriticalHitsMod.LOGGER.warn("[Modern Damage System] HUD编辑器注册失败", e);
        }
    }
}
