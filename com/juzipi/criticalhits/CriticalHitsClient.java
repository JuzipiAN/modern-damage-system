package com.juzipi.criticalhits;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.rendering.v1.ExtractItemDecorationsCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.lang.reflect.Method;

/**
 * 客户端初始化。
 * 注册暴击台界面、物品Tooltip（暴击信息+附魔书描述）。
 */
public class CriticalHitsClient implements ClientModInitializer {


    /**
     * 翻译辅助方法：从语言文件中获取翻译后的字符串。
     * 支持国际化，切换游戏语言时自动切换。
     */
    private static String tr(String key) {
        try {
            return net.minecraft.network.chat.Component.translatable(key).getString();
        } catch (Exception e) {
            return key;
        }
    }

    @Override
    public void onInitializeClient() {
        CriticalHitsMod.LOGGER.info("[Modern Damage System] 客户端初始化 - 作者: {} | 代码指纹: {}", CriticalHitsMod.AUTHOR, CriticalHitsMod.CODE_FINGERPRINT);

        // 用反射注册Screen（26.2中MenuScreens.register是private）
        try {
            Class<?> screenConstructorClass = Class.forName("net.minecraft.client.gui.screens.MenuScreens$ScreenConstructor");
            Method registerMethod = MenuScreens.class.getDeclaredMethod("register",
                net.minecraft.world.inventory.MenuType.class, screenConstructorClass);
            registerMethod.setAccessible(true);

            // 用Proxy创建ScreenConstructor
            Object constructor = java.lang.reflect.Proxy.newProxyInstance(
                screenConstructorClass.getClassLoader(),
                new Class<?>[]{screenConstructorClass},
                (proxy, method, args) -> {
                    // 只处理create方法，fromPacket是default方法不需要拦截
                    if (method.getName().equals("create")) {
                        return new CriticalStationScreen(
                            (CriticalStationMenu) args[0],
                            (net.minecraft.world.entity.player.Inventory) args[1],
                            (net.minecraft.network.chat.Component) args[2]);
                    }
                    // 其他方法（如fromPacket、hashCode、equals、toString）调用默认实现或返回默认值
                    if (method.isDefault()) {
                        return java.lang.reflect.InvocationHandler.invokeDefault(proxy, method, args);
                    }
                    return null;
                });

            registerMethod.invoke(null, CriticalHitsMod.CRITICAL_STATION_MENU, constructor);
            CriticalHitsMod.LOGGER.info("[Modern Damage System] 暴击台界面注册成功");

            // 注册配方书界面
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
            CriticalHitsMod.LOGGER.error("[Modern Damage System] 暴击台界面注册失败", e);
        }

        // 注册HUD编辑器（按键绑定和编辑模式）
        try {
            com.juzipi.criticalhits.client.HudEditor.register();
            CriticalHitsMod.LOGGER.info("[Modern Damage System] HUD编辑器注册成功");
        } catch (Exception e) {
            CriticalHitsMod.LOGGER.warn("[Modern Damage System] HUD编辑器注册失败", e);
        }

        // 配方书右键打开GUI（通过物品的useOn方法实现，26.2中Fabric事件系统变化大）

        // 配方书右键使用事件（暂时注释，排查进食问题）
        /*
        try {
            // 获取UseItemCallback类
            Class<?> useItemCallbackClass = Class.forName("net.fabricmc.fabric.api.event.player.UseItemCallback");
            // 获取EVENT字段
            java.lang.reflect.Field eventField = useItemCallbackClass.getField("EVENT");
            Object event = eventField.get(null);
            // 获取register方法（26.2中参数类型是Object，不是具体的回调接口）
            java.lang.reflect.Method registerMethod = event.getClass().getMethod("register", Object.class);
            // 因为ArrayBackedEvent是包私有类，需要设置可访问
            registerMethod.setAccessible(true);
            // 创建代理对象
            Object handler = java.lang.reflect.Proxy.newProxyInstance(
                useItemCallbackClass.getClassLoader(),
                new Class<?>[]{useItemCallbackClass},
                (proxy, method, args) -> {
                    if (method.getName().equals("interact")) {
                        net.minecraft.world.entity.player.Player player = (net.minecraft.world.entity.player.Player) args[0];
                        net.minecraft.world.level.Level level = (net.minecraft.world.level.Level) args[1];
                        net.minecraft.world.InteractionHand hand = (net.minecraft.world.InteractionHand) args[2];
                        net.minecraft.world.item.ItemStack stack = player.getItemInHand(hand);
                        if (stack != null && stack.is(CriticalHitsMod.RECIPE_BOOK)) {
                            if (level.isClientSide()) {
                                // 在客户端直接打开Screen（使用虚拟Menu）
                                try {
                                    RecipeBookMenu dummyMenu = new RecipeBookMenu(0, player.getInventory());
                                    RecipeBookScreen screen = new RecipeBookScreen(dummyMenu, player.getInventory(), Component.translatable("modern_damage_system.recipe.title"));
                                    com.juzipi.criticalhits.api.MdsGuiApi.openScreen(screen);
                                } catch (Exception e) {
                                    CriticalHitsMod.LOGGER.warn("[Modern Damage System] 打开配方书失败", e);
                                }
                            }
                            // 26.2中TypedActionResult类找不到，返回null让事件继续传递
                            return null;
                        }
                        return null;
                    }
                    return null;
                });
            registerMethod.invoke(event, handler);
            CriticalHitsMod.LOGGER.info("[Modern Damage System] 配方书右键事件注册成功（反射方式）");
        } catch (Exception e) {
            CriticalHitsMod.LOGGER.warn("[Modern Damage System] 配方书右键事件注册失败", e);
        }
        */

        // 物品Tooltip事件 - 正常优先级：处理负等级名称红色、武器/装备信息等
        ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> {
            if (stack == null || stack.isEmpty()) return;

            try {
                // 负等级附魔书名称显示为红色
                if (stack.is(Items.ENCHANTED_BOOK)) {
                    ItemEnchantments storedEnchants = stack.get(DataComponents.STORED_ENCHANTMENTS);
                    if (storedEnchants != null && !storedEnchants.isEmpty()) {
                        boolean isCurse = false;
                        for (var entry : storedEnchants.entrySet()) {
                            var ench = entry.getKey();
                            if (ench.is(CriticalHitsEnchantments.ATTACK_SPEED_CURSE_ID)
                                || ench.is(CriticalHitsEnchantments.MOVEMENT_SPEED_CURSE_ID)
                                || ench.is(CriticalHitsEnchantments.ATTACK_RANGE_CURSE_ID)) {
                                isCurse = true;
                                break;
                            }
                        }
                        if (isCurse && !lines.isEmpty()) {
                            // 修改第一行（物品名称）为红色
                            Component originalName = lines.get(0);
                            lines.set(0, Component.literal("§c" + originalName.getString()));
                        }
                    }
                }

                // 武器/装备上的负等级附魔名称显示为红色
                if (isWeapon(stack) || isArmor(stack)) {
                    ItemEnchantments itemEnchants = stack.getEnchantments();
                    if (itemEnchants != null && !itemEnchants.isEmpty()) {
                        // 检查是否有负等级附魔
                        boolean hasAttackSpeedCurse = false;
                        boolean hasMovementSpeedCurse = false;
                        boolean hasAttackRangeCurse = false;
                        for (var entry : itemEnchants.entrySet()) {
                            var ench = entry.getKey();
                            if (ench == null) continue;
                            if (ench.is(CriticalHitsEnchantments.ATTACK_SPEED_CURSE_ID)) hasAttackSpeedCurse = true;
                            if (ench.is(CriticalHitsEnchantments.MOVEMENT_SPEED_CURSE_ID)) hasMovementSpeedCurse = true;
                            if (ench.is(CriticalHitsEnchantments.ATTACK_RANGE_CURSE_ID)) hasAttackRangeCurse = true;
                        }
                        // 遍历Tooltip行，将负等级附魔的名称改为红色
                        if (hasAttackSpeedCurse || hasMovementSpeedCurse || hasAttackRangeCurse) {
                            for (int i = 0; i < lines.size(); i++) {
                                Component line = lines.get(i);
                                if (line == null) continue;
                                String text = line.getString();
                                if (text == null || text.isEmpty()) continue;
                                // 检查是否包含负等级附魔的名称
                                if (hasAttackSpeedCurse && text.contains("攻击速度")) {
                                    lines.set(i, Component.literal("§c" + text));
                                } else if (hasMovementSpeedCurse && text.contains("移动速度")) {
                                    lines.set(i, Component.literal("§c" + text));
                                } else if (hasAttackRangeCurse && text.contains("攻击范围")) {
                                    lines.set(i, Component.literal("§c" + text));
                                }
                            }
                        }
                    }
                }

                if (stack.is(Items.ENCHANTED_BOOK)) {
                    // v2.0.0 附魔描述已移至MDS配方书的"附魔书展示"分类中
                    // addEnchantedBookDescription(stack, lines);
                }
                if (isWeapon(stack)) {
                    addWeaponCriticalInfo(stack, lines);
                }
                if (isArmor(stack)) {
                    addArmorCriticalInfo(stack, lines);
                }

                // v2.0.0 附魔信息折叠：超过4条附魔时，多余的附魔名称折叠
                // 注意：附魔描述已移至MDS配方书，这里只折叠名称
                if (isWeapon(stack) || isArmor(stack)) {
                    int enchantCountTotal = 0;
                    ItemEnchantments itemEnchants = stack.getEnchantments();
                    if (itemEnchants != null) enchantCountTotal = itemEnchants.size();

                    if (enchantCountTotal > 4) {
                        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                        com.mojang.blaze3d.platform.Window window = mc.getWindow();
                        boolean isShiftDown = com.mojang.blaze3d.platform.InputConstants.isKeyDown(window, org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT)
                            || com.mojang.blaze3d.platform.InputConstants.isKeyDown(window, org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_SHIFT);

                        if (!isShiftDown) {
                            java.util.List<Integer> indicesToRemove = new java.util.ArrayList<>();
                            int enchantCount = 0;

                            for (int i = 0; i < lines.size(); i++) {
                                Component line = lines.get(i);
                                if (line == null) continue;
                                if (line.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents translatable) {
                                    String key = translatable.getKey();
                                    if (key == null) continue;
                                    boolean isEnchantName = key.startsWith("enchantment.") && !key.endsWith(".desc") && !key.endsWith(".desc2");
                                    if (isEnchantName) {
                                        enchantCount++;
                                        if (enchantCount > 4) {
                                            indicesToRemove.add(i);
                                        }
                                    }
                                }
                            }

                            for (int i = indicesToRemove.size() - 1; i >= 0; i--) {
                                lines.remove((int) indicesToRemove.get(i));
                            }

                            if (!indicesToRemove.isEmpty()) {
                                lines.add(Component.literal("§8§o按住Shift查看全部附魔..."));
                            }
                        }
                    }
                }
            } catch (Exception e) {
                // 捕获所有异常，防止tooltip渲染崩溃
                CriticalHitsMod.LOGGER.warn("[Modern Damage System] 渲染物品Tooltip时发生异常: " + stack.getItem(), e);
            }
        });

        // v2.0.0 第二个Tooltip回调：删除附魔描述（在本mod所有处理之后执行）
        // 附魔描述统一移至MDS配方书的"附魔书展示"分类中
        ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> {
            if (stack == null || stack.isEmpty()) return;
            try {
                java.util.List<Integer> indicesToRemove = new java.util.ArrayList<>();
                for (int i = 0; i < lines.size(); i++) {
                    Component line = lines.get(i);
                    if (line == null) continue;
                    // 1. 删除以.desc/.desc2结尾的翻译键
                    if (line.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents translatable) {
                        String key = translatable.getKey();
                        if (key != null && (key.endsWith(".desc") || key.endsWith(".desc2"))) {
                            indicesToRemove.add(i);
                            continue;
                        }
                    }
                    // 2. 删除灰色斜体的描述行（附魔描述mod添加的）
                    String text = line.getString();
                    boolean isShiftHint = text.contains("按住Shift") || text.contains("hold Shift") || text.contains("查看全部") || text.contains("查看附魔");
                    if (i > 0 && !isShiftHint && text.contains("§o") && (text.startsWith("§7") || text.startsWith("§8"))) {
                        indicesToRemove.add(i);
                    }
                }
                for (int i = indicesToRemove.size() - 1; i >= 0; i--) {
                    lines.remove((int) indicesToRemove.get(i));
                }
            } catch (Exception e) {
                // 忽略异常
            }
        });

        // 物品装饰事件：在暴击附魔书右下角绘制等级数字
        ExtractItemDecorationsCallback.EVENT.register((gfx, font, stack, x, y) -> {
            try {
                if (stack == null || stack.isEmpty() || !stack.is(Items.ENCHANTED_BOOK)) return;

                ItemEnchantments enchants = stack.get(DataComponents.STORED_ENCHANTMENTS);
                if (enchants == null || enchants.isEmpty()) return;

                int level = -1;
                boolean isCurse = false;
                for (var entry : enchants.entrySet()) {
                    var ench = entry.getKey();
                    if (ench == null) continue;
                    // 正等级附魔
                    if (ench.is(CriticalHitsEnchantments.CRITICAL_RATE_ID)
                        || ench.is(CriticalHitsEnchantments.CRITICAL_DAMAGE_ID)
                        || ench.is(CriticalHitsEnchantments.CRITICAL_RESISTANCE_ID)
                        || ench.is(CriticalHitsEnchantments.PERCENTAGE_DAMAGE_ID)
                        || ench.is(CriticalHitsEnchantments.ARMOR_PENETRATION_ID)
                        || ench.is(CriticalHitsEnchantments.LIFE_STEAL_ID)
                        || ench.is(CriticalHitsEnchantments.VULNERABILITY_ID)
                        || ench.is(CriticalHitsEnchantments.DAMAGE_REDUCTION_ID)
                        || ench.is(CriticalHitsEnchantments.ARMOR_DEFENSE_ID)
                        || ench.is(CriticalHitsEnchantments.MAX_HEALTH_ID)
                        || ench.is(CriticalHitsEnchantments.ATTACK_SPEED_ID)
                        || ench.is(CriticalHitsEnchantments.MOVEMENT_SPEED_ID)
                        || ench.is(CriticalHitsEnchantments.ATTACK_RANGE_ID)
                        || ench.is(CriticalHitsEnchantments.BONUS_DAMAGE_ID)
                        || ench.is(CriticalHitsEnchantments.TRUE_DAMAGE_ID)
                        || ench.is(CriticalHitsEnchantments.SPLASH_DAMAGE_ID)
                        || ench.is(CriticalHitsEnchantments.EXECUTE_ID)
                        || ench.is(CriticalHitsEnchantments.FROST_ID)
                        || ench.is(CriticalHitsEnchantments.POISON_ID)
                        || ench.is(CriticalHitsEnchantments.ARMOR_SHATTER_ID)
                        || ench.is(CriticalHitsEnchantments.THORNS_REFLECT_ID)
                        || ench.is(CriticalHitsEnchantments.SHIELD_ID)
                        || ench.is(CriticalHitsEnchantments.KNOCKBACK_RESISTANCE_ID)) {
                        level = entry.getIntValue();
                        break;
                    }
                    // 负等级附魔（诅咒）
                    if (ench.is(CriticalHitsEnchantments.ATTACK_SPEED_CURSE_ID)
                        || ench.is(CriticalHitsEnchantments.MOVEMENT_SPEED_CURSE_ID)
                        || ench.is(CriticalHitsEnchantments.ATTACK_RANGE_CURSE_ID)) {
                        level = entry.getIntValue();
                        isCurse = true;
                        break;
                    }
                }

                if (level <= 0) return;

                // 在右下角绘制等级数字
                String levelStr = isCurse ? "-" + level : String.valueOf(level);
                int strWidth = font.width(levelStr);
                int textX = x + 16 - strWidth;
                int textY = y + 9;
                int textColor = isCurse ? 0xFFFF5555 : 0xFFFFFFFF; // 负等级红色，正等级白色
                // 黑色阴影
                gfx.text(font, levelStr, textX + 1, textY + 1, 0xFF000000);
                // 主文字
                gfx.text(font, levelStr, textX, textY, textColor);
            } catch (Exception e) {
                // 捕获所有异常，防止物品装饰渲染崩溃
                CriticalHitsMod.LOGGER.warn("[Modern Damage System] 渲染物品装饰时发生异常", e);
            }
        });
    }

    /**
     * HUD武器装备状态提示渲染。
     * 由GuiMixin调用。
     */
    public static void renderHud(GuiGraphicsExtractor gfx) {
        // 检查HUD是否启用
        if (!CriticalHitsConfig.get().hud_enabled) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        Player player = mc.player;
        Font font = mc.font;
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        // 收集武器信息
        ItemStack weapon = player.getMainHandItem();
        boolean isBow = weapon.is(Items.BOW) || weapon.is(Items.CROSSBOW);
        java.util.List<String> weaponLines = new java.util.ArrayList<>();
        java.util.Map<String, Boolean> visibleAttrs = CriticalHitsConfig.get().hud_visible_attributes;
        int critRate = isBow ? 0 : CriticalHitsEnchantments.getCriticalRateLevel(weapon);
        int critDmg = isBow ? 0 : CriticalHitsEnchantments.getCriticalDamageLevel(weapon);
        int pctDmg = CriticalHitsEnchantments.getPercentageDamageLevel(weapon);
        int armorPen = isBow ? 0 : CriticalHitsEnchantments.getArmorPenetrationLevel(weapon);
        int lifeSteal = isBow ? 0 : CriticalHitsEnchantments.getLifeStealLevel(weapon);
        int vulnerability = CriticalHitsEnchantments.getVulnerabilityEnchantLevel(weapon);
        int attackSpeed = isBow ? 0 : CriticalHitsEnchantments.getAttackSpeedLevel(weapon);
        int attackRange = isBow ? 0 : CriticalHitsEnchantments.getAttackRangeLevel(weapon);
        int bonusDamage = isBow ? 0 : CriticalHitsEnchantments.getBonusDamageLevel(weapon);
        // v2.0.0新增武器属性
        int trueDamage = isBow ? 0 : CriticalHitsEnchantments.getTrueDamageLevel(weapon);
        int splashDamage = isBow ? 0 : CriticalHitsEnchantments.getSplashDamageLevel(weapon);
        int execute = isBow ? 0 : CriticalHitsEnchantments.getExecuteLevel(weapon);
        int frost = isBow ? 0 : CriticalHitsEnchantments.getFrostLevel(weapon);
        int poison = isBow ? 0 : CriticalHitsEnchantments.getPoisonLevel(weapon);
        int armorShatter = isBow ? 0 : CriticalHitsEnchantments.getArmorShatterLevel(weapon);

        // 检测亡灵杀手和节肢杀手附魔
        int smiteLevel = 0;
        int baneLevel = 0;
        ItemEnchantments hudEnchants = weapon.getEnchantments();
        if (hudEnchants != null && !hudEnchants.isEmpty()) {
            for (var entry : hudEnchants.entrySet()) {
                if (entry.getKey().is(net.minecraft.resources.Identifier.fromNamespaceAndPath("minecraft", "smite"))) {
                    smiteLevel = entry.getIntValue();
                }
                if (entry.getKey().is(net.minecraft.resources.Identifier.fromNamespaceAndPath("minecraft", "bane_of_arthropods"))) {
                    baneLevel = entry.getIntValue();
                }
            }
        }

        if (critRate > 0 && visibleAttrs.getOrDefault("critical_rate", true)) weaponLines.add(tr("modern_damage_system.hud.critical_rate") + (int)(CriticalHitsEnchantments.calculateCriticalRate(critRate) * 100) + tr("modern_damage_system.hud.percent"));
        if (critDmg > 0 && visibleAttrs.getOrDefault("critical_damage", true)) weaponLines.add(tr("modern_damage_system.hud.critical_damage") + (int)(CriticalHitsEnchantments.calculateCriticalDamageMultiplier(critDmg) * 100) + tr("modern_damage_system.hud.percent"));
        if (pctDmg > 0 && visibleAttrs.getOrDefault("damage_bonus", true)) weaponLines.add(tr("modern_damage_system.hud.damage_bonus") + (int)(CriticalHitsEnchantments.calculatePercentageDamage(pctDmg) * 100) + tr("modern_damage_system.hud.percent"));
        if (bonusDamage > 0 && visibleAttrs.getOrDefault("bonus_damage", true)) weaponLines.add(tr("modern_damage_system.hud.bonus_damage") + (int)(CriticalHitsEnchantments.calculateBonusDamageMultiplier(bonusDamage) * 100 - 100) + tr("modern_damage_system.hud.percent"));
        if (armorPen > 0 && visibleAttrs.getOrDefault("armor_penetration", true)) weaponLines.add(tr("modern_damage_system.hud.armor_penetration") + (int)(CriticalHitsEnchantments.calculateArmorPenetration(armorPen) * 100) + tr("modern_damage_system.hud.percent"));
        if (lifeSteal > 0 && visibleAttrs.getOrDefault("life_steal", true)) weaponLines.add(tr("modern_damage_system.hud.life_steal") + (int)(CriticalHitsEnchantments.calculateLifeSteal(lifeSteal) * 100) + tr("modern_damage_system.hud.percent"));
        if ((attackSpeed != 0 || attackRange != 0) && visibleAttrs.getOrDefault("attack_speed", true)) {
            // 计算总攻速修正（攻击速度附魔 + 攻击范围带来的攻速惩罚）
            double speedMultiplier = CriticalHitsEnchantments.calculateAttackSpeedMultiplier(attackSpeed);
            double rangePenalty = CriticalHitsEnchantments.calculateAttackRangeSpeedPenalty(attackRange);
            double totalSpeed = speedMultiplier * rangePenalty;
            totalSpeed = Math.max(0.3, Math.min(3.0, totalSpeed)); // 上下限
            int speedPercent = (int)((totalSpeed - 1.0) * 100);
            String speedColor = speedPercent >= 0 ? "§a" : "§c";
            String speedSign = speedPercent >= 0 ? "+" : "";

            if (attackSpeed != 0) {
                String speedStr = attackSpeed > 0 ? "+" + attackSpeed : "" + attackSpeed;
                weaponLines.add(tr("modern_damage_system.hud.attack_speed") + speedStr + tr("modern_damage_system.hud.level") + " " + speedColor + "(" + speedSign + speedPercent + tr("modern_damage_system.hud.percent") + ")");
            } else {
                // 只有攻击范围影响攻速时，也显示在攻击速度行
                weaponLines.add(tr("modern_damage_system.hud.attack_speed") + speedColor + speedSign + speedPercent + tr("modern_damage_system.hud.percent"));
            }
        }
        if (attackRange != 0 && visibleAttrs.getOrDefault("attack_range", true)) {
            String rangeStr = attackRange > 0 ? "+" : "";
            weaponLines.add(tr("modern_damage_system.hud.attack_range") + rangeStr + String.format("%.1f", CriticalHitsEnchantments.calculateAttackRangeBonus(attackRange)) + tr("modern_damage_system.hud.block"));
        }
        // v2.0.0新增武器属性显示
        if (trueDamage > 0 && visibleAttrs.getOrDefault("true_damage", true)) {
            weaponLines.add(tr("modern_damage_system.hud.true_damage") + (int)(CriticalHitsEnchantments.calculateTrueDamage(trueDamage) * 100) + tr("modern_damage_system.hud.percent"));
        }
        if (splashDamage > 0 && visibleAttrs.getOrDefault("splash_damage", true)) {
            weaponLines.add(tr("modern_damage_system.hud.splash_damage") + (int)(CriticalHitsEnchantments.calculateSplashDamage(splashDamage) * 100) + tr("modern_damage_system.hud.percent"));
        }
        if (execute > 0 && visibleAttrs.getOrDefault("execute", true)) {
            weaponLines.add(tr("modern_damage_system.hud.execute") + execute + tr("modern_damage_system.hud.level"));
        }
        if (frost > 0 && visibleAttrs.getOrDefault("frost", true)) {
            weaponLines.add(tr("modern_damage_system.hud.frost") + (int)(CriticalHitsEnchantments.calculateFrostChance(frost) * 100) + tr("modern_damage_system.hud.percent"));
        }
        if (poison > 0 && visibleAttrs.getOrDefault("poison", true)) {
            weaponLines.add(tr("modern_damage_system.hud.poison") + (int)(CriticalHitsEnchantments.calculatePoisonChance(poison) * 100) + tr("modern_damage_system.hud.percent"));
        }
        if (armorShatter > 0 && visibleAttrs.getOrDefault("armor_shatter", true)) {
            weaponLines.add(tr("modern_damage_system.hud.armor_shatter") + (int)(CriticalHitsEnchantments.calculateArmorShatter(armorShatter) * 100) + tr("modern_damage_system.hud.percent"));
        }
        // 易伤移到debuff区域，显示等级
        if (vulnerability > 0 && visibleAttrs.getOrDefault("vulnerability", true)) {
            weaponLines.add(tr("modern_damage_system.hud.vulnerability") + vulnerability + tr("modern_damage_system.hud.level"));
        }
        // 显示亡灵杀手和节肢杀手的特殊附加伤害
        if (smiteLevel > 0) {
            float smiteDamage = smiteLevel * 2.5f;
            weaponLines.add(tr("modern_damage_system.hud.smite") + String.format("%.1f", smiteDamage) + tr("modern_damage_system.hud.vs_undead"));
        }
        if (baneLevel > 0) {
            float baneDamage = baneLevel * 2.5f;
            weaponLines.add(tr("modern_damage_system.hud.bane_of_arthropods") + String.format("%.1f", baneDamage) + tr("modern_damage_system.hud.vs_arthropods"));
        }

        // 收集护甲信息
        java.util.List<String> armorLines = new java.util.ArrayList<>();
        int dmgReduction = CriticalHitsEnchantments.getDamageReductionLevel(player);
        int armorDefense = CriticalHitsEnchantments.getArmorDefenseLevel(player);
        int maxHealth = CriticalHitsEnchantments.getMaxHealthLevel(player);
        int critResist = CriticalHitsEnchantments.getCriticalResistanceLevel(player);
        int movementSpeed = CriticalHitsEnchantments.getMovementSpeedLevel(player);
        // v2.0.0新增护甲属性
        int thornsReflect = CriticalHitsEnchantments.getThornsReflectLevel(player);
        int shieldLevel = CriticalHitsEnchantments.getShieldLevel(player);
        int knockbackResistance = CriticalHitsEnchantments.getKnockbackResistanceLevel(player);

        if (dmgReduction > 0 && visibleAttrs.getOrDefault("damage_reduction", true)) armorLines.add(tr("modern_damage_system.hud.damage_reduction") + (int)(CriticalHitsEnchantments.calculateDamageReduction(dmgReduction) * 100) + tr("modern_damage_system.hud.percent"));
        if (armorDefense > 0 && visibleAttrs.getOrDefault("armor_defense", true)) armorLines.add(tr("modern_damage_system.hud.armor_defense") + (int)CriticalHitsEnchantments.calculateArmorToughness(armorDefense) + tr("modern_damage_system.hud.toughness"));
        if (maxHealth > 0 && visibleAttrs.getOrDefault("max_health", true)) armorLines.add(tr("modern_damage_system.hud.max_health") + (int)CriticalHitsEnchantments.calculateMaxHealth(maxHealth) + tr("modern_damage_system.hud.health"));
        if (critResist > 0 && visibleAttrs.getOrDefault("critical_resistance", true)) armorLines.add(tr("modern_damage_system.hud.critical_resistance") + (int)(CriticalHitsEnchantments.calculateCriticalResistance(critResist) * 100) + tr("modern_damage_system.hud.percent"));
        if (movementSpeed != 0 && visibleAttrs.getOrDefault("movement_speed", true)) {
            String speedStr = movementSpeed > 0 ? "+" + movementSpeed : "" + movementSpeed;
            armorLines.add(tr("modern_damage_system.hud.movement_speed") + speedStr + tr("modern_damage_system.hud.level"));
        }
        // v2.0.0新增护甲属性显示
        if (thornsReflect > 0 && visibleAttrs.getOrDefault("thorns_reflect", true)) {
            armorLines.add(tr("modern_damage_system.hud.thorns_reflect") + (int)(CriticalHitsEnchantments.calculateThornsReflect(thornsReflect) * 100) + tr("modern_damage_system.hud.percent"));
        }
        if (shieldLevel > 0 && visibleAttrs.getOrDefault("shield", true)) {
            float shieldValue = CriticalHitsEnchantments.calculateShieldValue(shieldLevel, player.getMaxHealth());
            armorLines.add(tr("modern_damage_system.hud.shield") + String.format("%.1f", shieldValue) + tr("modern_damage_system.hud.health"));
        }
        if (knockbackResistance > 0 && visibleAttrs.getOrDefault("knockback_resistance", true)) {
            armorLines.add(tr("modern_damage_system.hud.knockback_resistance") + (int)(CriticalHitsEnchantments.calculateKnockbackResistance(knockbackResistance) * 100) + tr("modern_damage_system.hud.percent"));
        }

        if (weaponLines.isEmpty() && armorLines.isEmpty()) return;

        // 计算面板大小（应用缩放）
        float scale = CriticalHitsConfig.get().hud_scale;
        scale = Math.max(0.5f, Math.min(2.0f, scale));
        int panelWidth = (int)(120 * scale);
        int lineHeight = (int)(10 * scale);
        int padding = (int)(4 * scale);
        int totalLines = weaponLines.size() + armorLines.size() + (weaponLines.size() > 0 && armorLines.size() > 0 ? 1 : 0);
        int panelHeight = totalLines * lineHeight + padding * 2;

        // 面板位置：根据配置显示在左侧或右侧
        int xOffset = CriticalHitsConfig.get().hud_x_offset;
        int yOffset = CriticalHitsConfig.get().hud_y_offset;
        int x;
        if ("left".equalsIgnoreCase(CriticalHitsConfig.get().hud_side)) {
            x = 5 + xOffset;
        } else {
            x = screenWidth - panelWidth - 5 + xOffset;
        }
        int y = screenHeight - panelHeight - 50 + yOffset;

        // 背景透明度（从配置读取）
        int alpha = CriticalHitsConfig.get().hud_background_alpha;
        alpha = Math.max(0, Math.min(255, alpha));
        int bgColor = (alpha << 24) | 0x000000;

        // 编辑模式下用不同的边框颜色
        boolean editMode = com.juzipi.criticalhits.client.HudEditor.isEditMode();
        int borderColor = editMode ? 0xFFFFFF00 : 0xFF555555;

        // 绘制半透明背景
        gfx.fill(x, y, x + panelWidth, y + panelHeight, bgColor);
        // 绘制边框
        gfx.fill(x, y, x + panelWidth, y + 1, borderColor);
        gfx.fill(x, y + panelHeight - 1, x + panelWidth, y + panelHeight, borderColor);
        gfx.fill(x, y, x + 1, y + panelHeight, borderColor);
        gfx.fill(x + panelWidth - 1, y, x + panelWidth, y + panelHeight, borderColor);

        // 编辑模式下显示提示文字
        if (editMode) {
            String hint = tr("modern_damage_system.hud.edit_hint");
            int hintWidth = mc.font.width(hint);
            gfx.text(mc.font, hint, x + panelWidth / 2 - hintWidth / 2, y - 12, 0xFFFFFF00);
        }

        int curY = y + padding;
        for (String line : weaponLines) {
            gfx.text(font, line, x + padding, curY, 0xFFFFFFFF);
            curY += lineHeight;
        }
        if (weaponLines.size() > 0 && armorLines.size() > 0) {
            gfx.fill(x + padding, curY, x + panelWidth - padding, curY + 1, borderColor);
            curY += lineHeight;
        }
        for (String line : armorLines) {
            gfx.text(font, line, x + padding, curY, 0xFFFFFFFF);
            curY += lineHeight;
        }
    }

    private void addEnchantedBookDescription(ItemStack stack, java.util.List<Component> lines) {
        ItemEnchantments enchants = stack.get(DataComponents.STORED_ENCHANTMENTS);
        if (enchants == null || enchants.isEmpty()) return;

        // 检测Shift键，没按住就不显示描述
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        com.mojang.blaze3d.platform.Window window = mc.getWindow();
        boolean isShiftDown = com.mojang.blaze3d.platform.InputConstants.isKeyDown(window, org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT)
            || com.mojang.blaze3d.platform.InputConstants.isKeyDown(window, org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_SHIFT);

        // 收集已有的Tooltip文本，用于去重
        java.util.Set<String> existingTexts = new java.util.HashSet<>();
        for (Component line : lines) {
            if (line != null) {
                existingTexts.add(line.getString());
            }
        }

        boolean hasDescription = false;
        for (var entry : enchants.entrySet()) {
            var ench = entry.getKey();
            String descKey = null;
            String desc2Key = null;

            if (ench.is(CriticalHitsEnchantments.CRITICAL_RATE_ID)) {
                descKey = "enchantment.criticalhits.critical_rate.desc";
                desc2Key = "enchantment.criticalhits.critical_rate.desc2";
            } else if (ench.is(CriticalHitsEnchantments.CRITICAL_DAMAGE_ID)) {
                descKey = "enchantment.criticalhits.critical_damage.desc";
            } else if (ench.is(CriticalHitsEnchantments.CRITICAL_RESISTANCE_ID)) {
                descKey = "enchantment.criticalhits.critical_resistance.desc";
            } else if (ench.is(CriticalHitsEnchantments.PERCENTAGE_DAMAGE_ID)) {
                descKey = "enchantment.criticalhits.percentage_damage.desc";
            } else if (ench.is(CriticalHitsEnchantments.ARMOR_PENETRATION_ID)) {
                descKey = "enchantment.criticalhits.armor_penetration.desc";
                desc2Key = "enchantment.criticalhits.armor_penetration.desc2";
            } else if (ench.is(CriticalHitsEnchantments.LIFE_STEAL_ID)) {
                descKey = "enchantment.criticalhits.life_steal.desc";
            } else if (ench.is(CriticalHitsEnchantments.VULNERABILITY_ID)) {
                descKey = "enchantment.criticalhits.vulnerability.desc";
            } else if (ench.is(CriticalHitsEnchantments.DAMAGE_REDUCTION_ID)) {
                descKey = "enchantment.criticalhits.damage_reduction.desc";
            } else if (ench.is(CriticalHitsEnchantments.ARMOR_DEFENSE_ID)) {
                descKey = "enchantment.criticalhits.armor_defense.desc";
            } else if (ench.is(CriticalHitsEnchantments.MAX_HEALTH_ID)) {
                descKey = "enchantment.criticalhits.max_health.desc";
            } else if (ench.is(CriticalHitsEnchantments.ATTACK_SPEED_ID)) {
                descKey = "enchantment.criticalhits.attack_speed.desc";
            } else if (ench.is(CriticalHitsEnchantments.MOVEMENT_SPEED_ID)) {
                descKey = "enchantment.criticalhits.movement_speed.desc";
            } else if (ench.is(CriticalHitsEnchantments.ATTACK_RANGE_ID)) {
                descKey = "enchantment.criticalhits.attack_range.desc";
            } else if (ench.is(CriticalHitsEnchantments.BONUS_DAMAGE_ID)) {
                descKey = "enchantment.criticalhits.bonus_damage.desc";
            } else if (ench.is(CriticalHitsEnchantments.ATTACK_SPEED_CURSE_ID)) {
                descKey = "enchantment.criticalhits.attack_speed_curse.desc";
            } else if (ench.is(CriticalHitsEnchantments.MOVEMENT_SPEED_CURSE_ID)) {
                descKey = "enchantment.criticalhits.movement_speed_curse.desc";
            } else if (ench.is(CriticalHitsEnchantments.ATTACK_RANGE_CURSE_ID)) {
                descKey = "enchantment.criticalhits.attack_range_curse.desc";
            } else if (ench.is(CriticalHitsEnchantments.TRUE_DAMAGE_ID)) {
                descKey = "enchantment.criticalhits.true_damage.desc";
            } else if (ench.is(CriticalHitsEnchantments.SPLASH_DAMAGE_ID)) {
                descKey = "enchantment.criticalhits.splash_damage.desc";
            } else if (ench.is(CriticalHitsEnchantments.EXECUTE_ID)) {
                descKey = "enchantment.criticalhits.execute.desc";
            } else if (ench.is(CriticalHitsEnchantments.FROST_ID)) {
                descKey = "enchantment.criticalhits.frost.desc";
            } else if (ench.is(CriticalHitsEnchantments.POISON_ID)) {
                descKey = "enchantment.criticalhits.poison.desc";
            } else if (ench.is(CriticalHitsEnchantments.ARMOR_SHATTER_ID)) {
                descKey = "enchantment.criticalhits.armor_shatter.desc";
            } else if (ench.is(CriticalHitsEnchantments.THORNS_REFLECT_ID)) {
                descKey = "enchantment.criticalhits.thorns_reflect.desc";
            } else if (ench.is(CriticalHitsEnchantments.SHIELD_ID)) {
                descKey = "enchantment.criticalhits.shield.desc";
            } else if (ench.is(CriticalHitsEnchantments.KNOCKBACK_RESISTANCE_ID)) {
                descKey = "enchantment.criticalhits.knockback_resistance.desc";
            }

            if (descKey != null) {
                hasDescription = true;
                if (isShiftDown) {
                    // 去重检查：如果Tooltip中已经有相同的文本，就不再添加
                    Component descComponent = Component.translatable(descKey);
                    if (!existingTexts.contains(descComponent.getString())) {
                        lines.add(descComponent);
                        existingTexts.add(descComponent.getString());
                    }
                    if (desc2Key != null) {
                        Component desc2Component = Component.translatable(desc2Key);
                        if (!existingTexts.contains(desc2Component.getString())) {
                            lines.add(desc2Component);
                            existingTexts.add(desc2Component.getString());
                        }
                    }
                }
                return;
            }
        }

        // 没按住Shift且有描述时，添加提示文字
        if (!isShiftDown && hasDescription) {
            lines.add(Component.literal("§8§o按住Shift查看附魔描述..."));
        }
    }

    private void addWeaponCriticalInfo(ItemStack stack, java.util.List<Component> lines) {
        boolean isBow = stack.is(Items.BOW) || stack.is(Items.CROSSBOW);
        int rateLevel = isBow ? 0 : CriticalHitsEnchantments.getCriticalRateLevel(stack);
        int damageLevel = isBow ? 0 : CriticalHitsEnchantments.getCriticalDamageLevel(stack);
        int percentageLevel = CriticalHitsEnchantments.getPercentageDamageLevel(stack);
        int penetrationLevel = isBow ? 0 : CriticalHitsEnchantments.getArmorPenetrationLevel(stack);
        int lifeStealLevel = isBow ? 0 : CriticalHitsEnchantments.getLifeStealLevel(stack);
        int vulnerabilityLevel = CriticalHitsEnchantments.getVulnerabilityEnchantLevel(stack);
        int attackSpeedLevel = isBow ? 0 : CriticalHitsEnchantments.getAttackSpeedLevel(stack);
        int attackRangeLevel = isBow ? 0 : CriticalHitsEnchantments.getAttackRangeLevel(stack);
        int bonusDamageLevel = isBow ? 0 : CriticalHitsEnchantments.getBonusDamageLevel(stack);
        // v2.0.0 新附魔
        int trueDamageLevel = isBow ? 0 : CriticalHitsEnchantments.getEnchantmentLevel(CriticalHitsEnchantments.TRUE_DAMAGE_ID, stack);
        int splashDamageLevel = isBow ? 0 : CriticalHitsEnchantments.getEnchantmentLevel(CriticalHitsEnchantments.SPLASH_DAMAGE_ID, stack);
        int executeLevel = isBow ? 0 : CriticalHitsEnchantments.getEnchantmentLevel(CriticalHitsEnchantments.EXECUTE_ID, stack);
        int frostLevel = isBow ? 0 : CriticalHitsEnchantments.getEnchantmentLevel(CriticalHitsEnchantments.FROST_ID, stack);
        int poisonLevel = isBow ? 0 : CriticalHitsEnchantments.getEnchantmentLevel(CriticalHitsEnchantments.POISON_ID, stack);
        int armorShatterLevel = isBow ? 0 : CriticalHitsEnchantments.getEnchantmentLevel(CriticalHitsEnchantments.ARMOR_SHATTER_ID, stack);
        int thornsReflectLevel = isBow ? 0 : CriticalHitsEnchantments.getEnchantmentLevel(CriticalHitsEnchantments.THORNS_REFLECT_ID, stack);

        // 检查是否有锋利、亡灵杀手、节肢杀手附魔
        boolean hasSharpness = false;
        int smiteLevel = 0;
        int baneLevel = 0;
        ItemEnchantments weaponEnchants = stack.getEnchantments();
        if (weaponEnchants != null && !weaponEnchants.isEmpty()) {
            for (var entry : weaponEnchants.entrySet()) {
                if (entry.getKey().is(net.minecraft.resources.Identifier.fromNamespaceAndPath("minecraft", "sharpness"))) {
                    hasSharpness = true;
                }
                if (entry.getKey().is(net.minecraft.resources.Identifier.fromNamespaceAndPath("minecraft", "smite"))) {
                    smiteLevel = entry.getIntValue();
                }
                if (entry.getKey().is(net.minecraft.resources.Identifier.fromNamespaceAndPath("minecraft", "bane_of_arthropods"))) {
                    baneLevel = entry.getIntValue();
                }
            }
        }

        if (rateLevel == 0 && damageLevel == 0 && percentageLevel == 0
            && penetrationLevel == 0 && lifeStealLevel == 0 && vulnerabilityLevel == 0
            && attackSpeedLevel == 0 && attackRangeLevel == 0 && bonusDamageLevel == 0
            && trueDamageLevel == 0 && splashDamageLevel == 0 && executeLevel == 0
            && frostLevel == 0 && poisonLevel == 0 && armorShatterLevel == 0 && thornsReflectLevel == 0
            && !hasSharpness && smiteLevel == 0 && baneLevel == 0) return;

        double critRate = CriticalHitsEnchantments.calculateCriticalRate(rateLevel);
        double critDamage = CriticalHitsEnchantments.calculateCriticalDamageMultiplier(damageLevel);
        float percentageDamage = CriticalHitsEnchantments.calculatePercentageDamage(percentageLevel);
        float armorPenetration = CriticalHitsEnchantments.calculateArmorPenetration(penetrationLevel);
        float lifeSteal = CriticalHitsEnchantments.calculateLifeSteal(lifeStealLevel);
        double bonusDamageMultiplier = CriticalHitsEnchantments.calculateBonusDamageMultiplier(bonusDamageLevel);
        double attackSpeedDamageModifier = CriticalHitsEnchantments.calculateAttackSpeedDamageModifier(attackSpeedLevel);

        lines.add(Component.literal(""));
        if (rateLevel > 0) {
            lines.add(Component.literal(tr("modern_damage_system.tooltip.critical_rate") + (int)(critRate * 100) + tr("modern_damage_system.tooltip.percent")));
        }
        if (damageLevel > 0) {
            lines.add(Component.literal(tr("modern_damage_system.tooltip.critical_damage") + (int)(critDamage * 100) + tr("modern_damage_system.tooltip.percent")));
        }
        if (percentageLevel > 0) {
            lines.add(Component.literal(tr("modern_damage_system.tooltip.damage_bonus") + (int)(percentageDamage * 100) + tr("modern_damage_system.tooltip.percent")));
        }
        if (bonusDamageLevel > 0) {
            lines.add(Component.literal(tr("modern_damage_system.tooltip.bonus_damage") + (int)((bonusDamageMultiplier - 1) * 100) + tr("modern_damage_system.tooltip.percent")));
        }
        if (penetrationLevel > 0) {
            lines.add(Component.literal(tr("modern_damage_system.tooltip.armor_penetration") + (int)(armorPenetration * 100) + tr("modern_damage_system.tooltip.percent")));
        }
        if (lifeStealLevel > 0) {
            lines.add(Component.literal(tr("modern_damage_system.tooltip.life_steal") + (int)(lifeSteal * 100) + tr("modern_damage_system.tooltip.percent")));
        }
        if (vulnerabilityLevel > 0) {
            lines.add(Component.literal(tr("modern_damage_system.tooltip.vulnerability") + vulnerabilityLevel + tr("modern_damage_system.tooltip.level")));
        }
        if (attackSpeedLevel != 0) {
            String speedStr = attackSpeedLevel > 0 ? "+" + attackSpeedLevel : "" + attackSpeedLevel;
            lines.add(Component.literal(tr("modern_damage_system.tooltip.attack_speed") + speedStr + tr("modern_damage_system.tooltip.level")));
        }
        if (attackRangeLevel != 0) {
            String rangeStr = attackRangeLevel > 0 ? "+" : "";
            lines.add(Component.literal(tr("modern_damage_system.tooltip.attack_range") + rangeStr + String.format("%.1f", CriticalHitsEnchantments.calculateAttackRangeBonus(attackRangeLevel)) + tr("modern_damage_system.tooltip.block")));
        }
        // v2.0.0 新附魔显示
        if (trueDamageLevel > 0) {
            lines.add(Component.literal(tr("modern_damage_system.tooltip.true_damage") + trueDamageLevel + tr("modern_damage_system.tooltip.level")));
        }
        if (splashDamageLevel > 0) {
            lines.add(Component.literal(tr("modern_damage_system.tooltip.splash_damage") + splashDamageLevel + tr("modern_damage_system.tooltip.level")));
        }
        if (executeLevel > 0) {
            lines.add(Component.literal(tr("modern_damage_system.tooltip.execute") + executeLevel + tr("modern_damage_system.tooltip.level")));
        }
        if (frostLevel > 0) {
            lines.add(Component.literal(tr("modern_damage_system.tooltip.frost") + frostLevel + tr("modern_damage_system.tooltip.level")));
        }
        if (poisonLevel > 0) {
            lines.add(Component.literal(tr("modern_damage_system.tooltip.poison") + poisonLevel + tr("modern_damage_system.tooltip.level")));
        }
        if (armorShatterLevel > 0) {
            lines.add(Component.literal(tr("modern_damage_system.tooltip.armor_shatter") + armorShatterLevel + tr("modern_damage_system.tooltip.level")));
        }
        if (thornsReflectLevel > 0) {
            float reflectPercent = CriticalHitsEnchantments.calculateThornsReflect(thornsReflectLevel);
            lines.add(Component.literal(tr("modern_damage_system.tooltip.thorns_reflect") + (int)(reflectPercent * 100) + tr("modern_damage_system.tooltip.percent")));
        }

        float baseDamage = getWeaponBaseDamage(stack);
        if (baseDamage > 0) {
            // 计算含百分比伤害加成+攻击速度修正的基础伤害
            float damageWithPercentage = baseDamage * (1 + percentageDamage) * (float) attackSpeedDamageModifier;
            // 计算含额外伤害的伤害
            float damageWithBonus = damageWithPercentage * (float) bonusDamageMultiplier;
            // 计算暴击后伤害
            float critDamageValue = damageWithBonus * (1 + (float) critDamage);
            lines.add(Component.literal(tr("modern_damage_system.tooltip.attack_damage") + formatDamage(damageWithBonus)
                + tr("modern_damage_system.tooltip.crit_after") + formatDamage(critDamageValue) + tr("modern_damage_system.tooltip.close_paren")));

            // 显示亡灵杀手和节肢杀手的特殊附加伤害（不计算在动态伤害里，用青色显示）
            if (smiteLevel > 0) {
                float smiteDamage = smiteLevel * 2.5f;
                float smitePercent = (smiteDamage / baseDamage) * 100;
                lines.add(Component.literal(tr("modern_damage_system.tooltip.special_damage") + tr("modern_damage_system.tooltip.vs_undead") + formatDamage(smiteDamage)
                    + " §b(+" + (int)smitePercent + "%)"));
            }
            if (baneLevel > 0) {
                float baneDamage = baneLevel * 2.5f;
                float banePercent = (baneDamage / baseDamage) * 100;
                lines.add(Component.literal(tr("modern_damage_system.tooltip.special_damage") + tr("modern_damage_system.tooltip.vs_arthropods") + formatDamage(baneDamage)
                    + " §b(+" + (int)banePercent + "%)"));
            }
        }
    }

    private void addArmorCriticalInfo(ItemStack stack, java.util.List<Component> lines) {
        int critResistLevel = CriticalHitsEnchantments.getEnchantmentLevel(
            CriticalHitsEnchantments.CRITICAL_RESISTANCE_ID, stack);
        int damageReductionLevel = CriticalHitsEnchantments.getEnchantmentLevel(
            CriticalHitsEnchantments.DAMAGE_REDUCTION_ID, stack);
        int armorDefenseLevel = CriticalHitsEnchantments.getEnchantmentLevel(
            CriticalHitsEnchantments.ARMOR_DEFENSE_ID, stack);
        int maxHealthLevel = CriticalHitsEnchantments.getEnchantmentLevel(
            CriticalHitsEnchantments.MAX_HEALTH_ID, stack);
        int movementSpeedPositive = CriticalHitsEnchantments.getEnchantmentLevel(
            CriticalHitsEnchantments.MOVEMENT_SPEED_ID, stack);
        int movementSpeedNegative = CriticalHitsEnchantments.getEnchantmentLevel(
            CriticalHitsEnchantments.MOVEMENT_SPEED_CURSE_ID, stack);
        // v2.0.0 新防御附魔
        int thornsReflectLevel = CriticalHitsEnchantments.getEnchantmentLevel(
            CriticalHitsEnchantments.THORNS_REFLECT_ID, stack);
        int shieldLevel = CriticalHitsEnchantments.getEnchantmentLevel(
            CriticalHitsEnchantments.SHIELD_ID, stack);
        int knockbackResistLevel = CriticalHitsEnchantments.getEnchantmentLevel(
            CriticalHitsEnchantments.KNOCKBACK_RESISTANCE_ID, stack);

        if (critResistLevel == 0 && damageReductionLevel == 0
            && armorDefenseLevel == 0 && maxHealthLevel == 0
            && movementSpeedPositive == 0 && movementSpeedNegative == 0
            && thornsReflectLevel == 0 && shieldLevel == 0 && knockbackResistLevel == 0) return;

        lines.add(Component.literal(""));
        if (critResistLevel > 0) {
            double resist = CriticalHitsEnchantments.calculateCriticalResistance(critResistLevel);
            lines.add(Component.literal(tr("modern_damage_system.tooltip.critical_resistance") + (int)(resist * 100) + tr("modern_damage_system.tooltip.percent")));
        }
        if (damageReductionLevel > 0) {
            float reduction = CriticalHitsEnchantments.calculateDamageReduction(damageReductionLevel);
            lines.add(Component.literal(tr("modern_damage_system.tooltip.damage_reduction") + (int)(reduction * 100) + tr("modern_damage_system.tooltip.percent")));
        }
        if (armorDefenseLevel > 0) {
            float toughness = CriticalHitsEnchantments.calculateArmorToughness(armorDefenseLevel);
            lines.add(Component.literal(tr("modern_damage_system.tooltip.armor_defense") + (int)toughness + tr("modern_damage_system.tooltip.toughness")));
        }
        if (maxHealthLevel > 0) {
            float health = CriticalHitsEnchantments.calculateMaxHealth(maxHealthLevel);
            lines.add(Component.literal(tr("modern_damage_system.tooltip.max_health") + (int)health + tr("modern_damage_system.tooltip.health")));
        }
        if (movementSpeedPositive > 0) {
            lines.add(Component.literal(tr("modern_damage_system.tooltip.movement_speed_pos") + movementSpeedPositive + tr("modern_damage_system.tooltip.level")));
        }
        if (movementSpeedNegative > 0) {
            lines.add(Component.literal(tr("modern_damage_system.tooltip.movement_speed_neg") + movementSpeedNegative + tr("modern_damage_system.tooltip.level")));
        }
        // v2.0.0 新防御附魔显示
        if (thornsReflectLevel > 0) {
            float reflect = CriticalHitsEnchantments.calculateThornsReflect(thornsReflectLevel);
            lines.add(Component.literal(tr("modern_damage_system.tooltip.thorns_reflect") + (int)(reflect * 100) + tr("modern_damage_system.tooltip.percent")));
        }
        if (shieldLevel > 0) {
            float shield = CriticalHitsEnchantments.calculateShieldValue(shieldLevel, 20.0f);
            lines.add(Component.literal(tr("modern_damage_system.tooltip.shield") + (int)shield + tr("modern_damage_system.tooltip.health")));
        }
        if (knockbackResistLevel > 0) {
            float resist = CriticalHitsEnchantments.calculateKnockbackResistance(knockbackResistLevel);
            lines.add(Component.literal(tr("modern_damage_system.tooltip.knockback_resistance") + (int)(resist * 100) + tr("modern_damage_system.tooltip.percent")));
        }
    }

    private boolean isWeapon(ItemStack stack) {
        // 通用判断：检查物品是否有攻击伤害属性（支持Simply Swords等自定义武器mod）
        try {
            var attrComponent = stack.get(net.minecraft.core.component.DataComponents.ATTRIBUTE_MODIFIERS);
            if (attrComponent != null) {
                var modifiers = attrComponent.modifiers();
                if (modifiers != null) {
                    for (var entry : modifiers) {
                        if (entry.attribute().is(Attributes.ATTACK_DAMAGE)) {
                            return true;
                        }
                    }
                }
            }
        } catch (Exception e) {
            // 忽略异常，继续用原版武器兜底
        }
        // 原版武器兜底
        return stack.is(Items.DIAMOND_SWORD) || stack.is(Items.IRON_SWORD)
            || stack.is(Items.GOLDEN_SWORD) || stack.is(Items.STONE_SWORD)
            || stack.is(Items.WOODEN_SWORD) || stack.is(Items.NETHERITE_SWORD)
            || stack.is(Items.DIAMOND_AXE) || stack.is(Items.IRON_AXE)
            || stack.is(Items.GOLDEN_AXE) || stack.is(Items.STONE_AXE)
            || stack.is(Items.WOODEN_AXE) || stack.is(Items.NETHERITE_AXE)
            || stack.is(Items.TRIDENT) || stack.is(Items.MACE)
            || stack.is(Items.BOW) || stack.is(Items.CROSSBOW);
    }

    private boolean isArmor(ItemStack stack) {
        return stack.is(Items.DIAMOND_HELMET) || stack.is(Items.DIAMOND_CHESTPLATE)
            || stack.is(Items.DIAMOND_LEGGINGS) || stack.is(Items.DIAMOND_BOOTS)
            || stack.is(Items.IRON_HELMET) || stack.is(Items.IRON_CHESTPLATE)
            || stack.is(Items.IRON_LEGGINGS) || stack.is(Items.IRON_BOOTS)
            || stack.is(Items.GOLDEN_HELMET) || stack.is(Items.GOLDEN_CHESTPLATE)
            || stack.is(Items.GOLDEN_LEGGINGS) || stack.is(Items.GOLDEN_BOOTS)
            || stack.is(Items.NETHERITE_HELMET) || stack.is(Items.NETHERITE_CHESTPLATE)
            || stack.is(Items.NETHERITE_LEGGINGS) || stack.is(Items.NETHERITE_BOOTS)
            || stack.is(Items.CHAINMAIL_HELMET) || stack.is(Items.CHAINMAIL_CHESTPLATE)
            || stack.is(Items.CHAINMAIL_LEGGINGS) || stack.is(Items.CHAINMAIL_BOOTS)
            || stack.is(Items.LEATHER_HELMET) || stack.is(Items.LEATHER_CHESTPLATE)
            || stack.is(Items.LEATHER_LEGGINGS) || stack.is(Items.LEATHER_BOOTS)
            || stack.is(Items.TURTLE_HELMET) || stack.is(Items.ELYTRA);
    }

    private float getWeaponBaseDamage(ItemStack stack) {
        // 通用判断：从属性修饰符组件中获取攻击伤害（支持Simply Swords等自定义武器mod）
        try {
            var attrComponent = stack.get(net.minecraft.core.component.DataComponents.ATTRIBUTE_MODIFIERS);
            if (attrComponent != null) {
                var modifiers = attrComponent.modifiers();
                if (modifiers != null) {
                    for (var entry : modifiers) {
                        if (entry.attribute().is(Attributes.ATTACK_DAMAGE)) {
                            var modifier = entry.modifier();
                            if (modifier.operation() == net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE) {
                                float base = (float) modifier.amount();
                                // 加上锋利附魔加成
                                ItemEnchantments enchants = stack.getEnchantments();
                                if (enchants != null && !enchants.isEmpty()) {
                                    for (var e : enchants.entrySet()) {
                                        if (e.getKey().is(net.minecraft.resources.Identifier.fromNamespaceAndPath("minecraft", "sharpness"))) {
                                            int level = e.getIntValue();
                                            base += 1.0f + (level - 1) * 0.5f;
                                            break;
                                        }
                                    }
                                }
                                return base;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // 忽略异常，继续用原版武器兜底
        }

        // 原版武器兜底
        float base = 0;
        if (stack.is(Items.DIAMOND_SWORD) || stack.is(Items.DIAMOND_AXE)) base = 7;
        else if (stack.is(Items.IRON_SWORD) || stack.is(Items.IRON_AXE)) base = 6;
        else if (stack.is(Items.GOLDEN_SWORD) || stack.is(Items.GOLDEN_AXE)) base = 4;
        else if (stack.is(Items.STONE_SWORD) || stack.is(Items.STONE_AXE)) base = 5;
        else if (stack.is(Items.WOODEN_SWORD) || stack.is(Items.WOODEN_AXE)) base = 4;
        else if (stack.is(Items.NETHERITE_SWORD) || stack.is(Items.NETHERITE_AXE)) base = 8;
        else if (stack.is(Items.TRIDENT)) base = 8;
        else if (stack.is(Items.MACE)) base = 6;
        else if (stack.is(Items.BOW)) base = 9;
        else if (stack.is(Items.CROSSBOW)) base = 9;

        // 加上锋利附魔加成（1 + (等级-1)*0.5）
        ItemEnchantments enchants = stack.getEnchantments();
        if (enchants != null && !enchants.isEmpty()) {
            for (var entry : enchants.entrySet()) {
                if (entry.getKey().is(net.minecraft.resources.Identifier.fromNamespaceAndPath("minecraft", "sharpness"))) {
                    int level = entry.getIntValue();
                    base += 1.0f + (level - 1) * 0.5f;
                    break;
                }
            }
        }
        return base;
    }

    private String formatDamage(float damage) {
        if (damage == (int) damage) return String.valueOf((int) damage);
        return String.format("%.1f", damage);
    }
}
