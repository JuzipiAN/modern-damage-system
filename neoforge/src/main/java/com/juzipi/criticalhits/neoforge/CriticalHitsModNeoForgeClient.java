package com.juzipi.criticalhits.neoforge;

import com.juzipi.criticalhits.CriticalHitsMod;
import com.juzipi.criticalhits.CriticalStationScreen;
import com.juzipi.criticalhits.MdsClientHooks;
import com.juzipi.criticalhits.MdsClientScreens;
import com.juzipi.criticalhits.RecipeBookMenu;
import com.juzipi.criticalhits.RecipeBookScreen;
import com.juzipi.criticalhits.client.HudEditor;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterItemDecorationsEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import org.lwjgl.glfw.GLFW;

/**
 * NeoForge 客户端入口（2.0.5 新增）。
 *
 * 旧版本 NeoForge 端完全没有客户端功能：HUD、tooltip、按键、鼠标事件、
 * 甚至 MenuScreen 注册都缺失（打开暴击台/配方书界面会崩）。
 * 这里用 NeoForge 事件系统补齐，并复用 common 里的 {@link MdsClientHooks} 与
 * {@link HudEditor}，与 Fabric 端行为保持一致。
 */
@Mod(value = "criticalhits", dist = Dist.CLIENT)
public class CriticalHitsModNeoForgeClient {

    private static final Identifier HUD_LAYER_ID =
        Identifier.fromNamespaceAndPath(CriticalHitsMod.MOD_ID, "hud");

    public CriticalHitsModNeoForgeClient(IEventBus modEventBus,
                                        net.neoforged.fml.ModContainer modContainer) {
        // mod 总线
        modEventBus.addListener(this::onRegisterMenuScreens);
        modEventBus.addListener(this::onRegisterKeyMappings);
        modEventBus.addListener(this::onRegisterGuiLayers);
        modEventBus.addListener(this::onRegisterItemDecorations);

        // 游戏总线
        NeoForge.EVENT_BUS.addListener(this::onItemTooltip);
        NeoForge.EVENT_BUS.addListener(this::onClientTick);
        NeoForge.EVENT_BUS.addListener(this::onMouseButton);
        NeoForge.EVENT_BUS.addListener(this::onMouseScroll);

        // 模组列表里的 Config 按钮：NeoForge 自带的配置界面扩展点，
        // 玩家不需要再依赖 Configured 就能打开 MDS 配置。
        try {
            modContainer.registerExtensionPoint(
                net.neoforged.neoforge.client.gui.IConfigScreenFactory.class,
                (container, parent) -> com.juzipi.criticalhits.MdsConfigGui.create(parent));
            CriticalHitsMod.LOGGER.info("[MDS] 已注册 NeoForge 配置界面入口");
        } catch (Throwable e) {
            CriticalHitsMod.LOGGER.warn("[MDS] 注册配置界面入口失败（不影响游戏）", e);
        }

        CriticalHitsMod.LOGGER.info("[MDS] NeoForge 客户端入口初始化完成");
    }

    /** 界面注册（旧版缺失 → 打开界面必崩）。 */
    private void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        // 26.3：MenuScreens.ScreenConstructor 变成包内私有，模组无法直接引用该类型，
        // 因此用动态代理 + 反射调用 register，绕开编译期的访问检查。
        registerScreen(event, CriticalHitsMod.stationMenu(), CriticalStationScreen.class);
        registerScreen(event, CriticalHitsMod.recipeBookMenu(), RecipeBookScreen.class);
        CriticalHitsMod.LOGGER.info("[Modern Damage System] 暴击台/配方书界面注册成功 (NeoForge, 反射方式)");
    }

    /** 用动态代理实现不可访问的 ScreenConstructor 接口，再反射调用 event.register。 */
    private void registerScreen(RegisterMenuScreensEvent event,
                                net.minecraft.world.inventory.MenuType<?> menuType,
                                final Class<?> screenClass) {
        try {
            Class<?> sc = Class.forName("net.minecraft.client.gui.screens.MenuScreens$ScreenConstructor");
            Object proxy = java.lang.reflect.Proxy.newProxyInstance(
                screenClass.getClassLoader(), new Class<?>[]{sc},
                (p, method, args) -> {
                    if ("create".equals(method.getName()) && args != null && args.length == 3) {
                        return screenClass.getConstructor(
                                net.minecraft.world.inventory.AbstractContainerMenu.class,
                                net.minecraft.world.entity.player.Inventory.class,
                                net.minecraft.network.chat.Component.class)
                            .newInstance(args[0], args[1], args[2]);
                    }
                    return null;
                });
            java.lang.reflect.Method reg = event.getClass().getMethod(
                "register", net.minecraft.world.inventory.MenuType.class, sc);
            reg.invoke(event, menuType, proxy);
        } catch (Throwable e) {
            CriticalHitsMod.LOGGER.error("[Modern Damage System] 注册界面失败: " + screenClass.getSimpleName(), e);
        }
    }

    /** 按键绑定 + HUD 编辑器。 */
    private void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.registerCategory(HudEditor.category());

        KeyMapping toggle = new KeyMapping("key.criticalhits.toggle_hud", GLFW.GLFW_KEY_H, HudEditor.category());
        KeyMapping edit = new KeyMapping("key.criticalhits.edit_hud", GLFW.GLFW_KEY_C, HudEditor.category());
        KeyMapping config = new KeyMapping("key.criticalhits.open_config", GLFW.GLFW_KEY_K, HudEditor.category());
        event.register(toggle);
        event.register(edit);
        event.register(config);

        HudEditor.init(toggle, edit, config);
    }

    /** HUD 渲染层。 */
    private void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(HUD_LAYER_ID, (guiGraphics, deltaTracker) -> MdsClientHooks.renderHud(guiGraphics));
    }

    /**
     * 附魔书右下角等级数字（与 Fabric 端同一份实现）。
     * NeoForge 的 IItemDecorator 与 Fabric 的 ExtractItemDecorationsCallback 签名一致。
     */
    private void onRegisterItemDecorations(RegisterItemDecorationsEvent event) {
        event.register(net.minecraft.world.item.Items.ENCHANTED_BOOK,
            (guiGraphics, font, stack, xOffset, yOffset) ->
                MdsClientHooks.decorateEnchantedBook(guiGraphics, font, stack, xOffset, yOffset));
        CriticalHitsMod.LOGGER.info("[Modern Damage System] 附魔书等级数字装饰注册成功 (NeoForge)");
    }

    /** 物品 tooltip（与 Fabric 端同一份逻辑）。 */
    private void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        MdsClientHooks.onItemTooltip(stack, event.getToolTip());
        MdsClientHooks.stripEnchantDescriptions(stack, event.getToolTip());
    }

    /** 客户端 tick：HUD 编辑器按键 + 编辑模式拖动。 */
    private void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;
        HudEditor.tick(mc);

        // NeoForge 没有鼠标移动事件，编辑模式下的拖动在 tick 里按当前鼠标位置更新
        if (HudEditor.isEditMode() && HudEditor.isDragging() && mc.getWindow() != null && mc.mouseHandler != null) {
            double mouseX = MouseHandler.getScaledXPos(mc.getWindow(), mc.mouseHandler.xpos());
            double mouseY = MouseHandler.getScaledYPos(mc.getWindow(), mc.mouseHandler.ypos());
            HudEditor.updateDrag((int) mouseX, (int) mouseY);
        }
    }

    /** 鼠标按键：右键配方书打开界面；HUD 编辑模式下拦截点击。 */
    private void onMouseButton(InputEvent.MouseButton.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null) return;

        // 右键配方书 → 打开配方书界面
        if (!HudEditor.isEditMode() && event.getAction() == 1 && event.getButton() == 1) {
            ItemStack mainHand = mc.player.getMainHandItem();
            ItemStack offHand = mc.player.getOffhandItem();
            boolean isRecipeBook = (!mainHand.isEmpty() && mainHand.is(CriticalHitsMod.recipeBookItem()))
                || (!offHand.isEmpty() && offHand.is(CriticalHitsMod.recipeBookItem()));
            if (isRecipeBook) {
                event.setCanceled(true);
                try {
                    RecipeBookMenu dummyMenu = new RecipeBookMenu(0, mc.player.getInventory());
                    MdsClientScreens.showScreen(new RecipeBookScreen(dummyMenu, mc.player.getInventory(),
                        Component.translatable("modern_damage_system.recipe.title")));
                } catch (Exception e) {
                    CriticalHitsMod.LOGGER.error("[MDS] 配方书右键处理异常", e);
                }
                return;
            }
        }

        if (!HudEditor.isEditMode()) return;
        // 编辑模式：拦截点击，避免误触攻击/使用物品
        event.setCanceled(true);
        if (mc.getWindow() == null || mc.mouseHandler == null) return;

        double mouseX = MouseHandler.getScaledXPos(mc.getWindow(), mc.mouseHandler.xpos());
        double mouseY = MouseHandler.getScaledYPos(mc.getWindow(), mc.mouseHandler.ypos());
        if (event.getAction() == 1 && event.getButton() == 0) {
            if (MdsClientHooks.isStyleButtonHit((int) mouseX, (int) mouseY)) {
                HudEditor.toggleStyle();
                return;
            }
            HudEditor.startDrag((int) mouseX, (int) mouseY);
        } else if (event.getAction() == 0 && event.getButton() == 0) {
            HudEditor.endDrag();
        }
    }

    /** 滚轮：HUD 编辑模式下调整缩放并拦截物品栏切换。 */
    private void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        if (!HudEditor.isEditMode()) return;
        event.setCanceled(true);
        HudEditor.onScroll(event.getScrollDeltaY());
    }
}
