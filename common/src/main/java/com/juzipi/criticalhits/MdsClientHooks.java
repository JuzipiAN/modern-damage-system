package com.juzipi.criticalhits;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/**
 * 平台无关的客户端逻辑：HUD 渲染、物品 tooltip 处理、附魔书等级装饰。
 *
 * 从 fabric 模块的 CriticalHitsClient 抽出（2.0.5），
 * Fabric 与 NeoForge 各自在自己的客户端事件里调用这里，避免两端重复实现。
 */
public final class MdsClientHooks {

    private MdsClientHooks() {
    }
    private static String tr(String key) {
        try {
            return net.minecraft.network.chat.Component.translatable(key).getString();
        } catch (Exception e) {
            return key;
        }
    }

    /** 物品 tooltip：负等级名称红色 + 武器/装备信息 + 附魔折叠。 */
    public static void onItemTooltip(ItemStack stack, java.util.List<Component> lines) {
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
            CriticalHitsMod.LOGGER.warn("[Modern Damage System] 渲染物品Tooltip时发生异常", e);
        }
    }

    /** 删除附魔描述行（在本 mod 所有处理之后执行）。 */
    public static void stripEnchantDescriptions(ItemStack stack, java.util.List<Component> lines) {
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
    }

    /**
     * 附魔书右下角等级数字。
     * Fabric 通过 ExtractItemDecorationsCallback 调用；NeoForge 通过
     * RegisterItemDecorationsEvent + IItemDecorator 调用，两者签名一致。
     *
     * @return true 表示确实绘制了等级数字（NeoForge 的 IItemDecorator 需要该返回值）
     */
    public static boolean decorateEnchantedBook(GuiGraphicsExtractor gfx, Font font, ItemStack stack, int x, int y) {
        try {
                if (stack == null || stack.isEmpty() || !stack.is(Items.ENCHANTED_BOOK)) return false;

                ItemEnchantments enchants = stack.get(DataComponents.STORED_ENCHANTMENTS);
                if (enchants == null || enchants.isEmpty()) return false;

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

                if (level <= 0) return false;

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
                return true;
        } catch (Exception e) {
            CriticalHitsMod.LOGGER.warn("[Modern Damage System] 渲染物品装饰时发生异常", e);
            return false;
        }
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

        // ---- 面板几何（含标题栏，应用缩放）----
        float scale = CriticalHitsConfig.get().hud_scale;
        scale = Math.max(0.5f, Math.min(2.0f, scale));
        int panelWidth = (int)(150 * scale);
        int lineHeight = (int)(10 * scale);
        int padding = (int)(5 * scale);
        int headerH = (int)(16 * scale);
        int totalLines = weaponLines.size() + armorLines.size() + (weaponLines.size() > 0 && armorLines.size() > 0 ? 1 : 0);
        int panelHeight = headerH + totalLines * lineHeight + padding * 2;

        int xOffset = CriticalHitsConfig.get().hud_x_offset;
        int yOffset = CriticalHitsConfig.get().hud_y_offset;
        boolean leftSide = "left".equalsIgnoreCase(CriticalHitsConfig.get().hud_side);
        int x = leftSide ? 5 + xOffset : screenWidth - panelWidth - 5 + xOffset;
        int y = screenHeight - panelHeight - 50 + yOffset;

        // 面板必须足够不透明，否则文字会被背后的木板/天空干扰（2026-10 用户反馈）
        int alpha = Math.max(205, Math.min(255, CriticalHitsConfig.get().hud_background_alpha));
        boolean editMode = com.juzipi.criticalhits.client.HudEditor.isEditMode();
        boolean forest = !"blood".equalsIgnoreCase(CriticalHitsConfig.get().hud_style);

        // 记录几何供鼠标命中"切换风格"按钮
        lastPanelX = x; lastPanelY = y; lastPanelWidth = panelWidth; lastPanelHeight = panelHeight;

        if (forest) {
            drawForestChrome(gfx, x, y, panelWidth, panelHeight, headerH, alpha, editMode);
        } else {
            drawBloodChrome(gfx, x, y, panelWidth, panelHeight, headerH, alpha, editMode);
        }

        // 标题 + 风格标签
        drawCritStar(gfx, x + 5, y + (int)(4 * scale));
        gfx.text(font, forest ? tr("modern_damage_system.hud.title_forest") : tr("modern_damage_system.hud.title_blood"),
            x + padding + (int)(12 * scale), y + (int)(4 * scale),
            forest ? 0xFFFFD97A : 0xFFFFE6E6);
        String tag = forest ? tr("modern_damage_system.hud.style_forest") : tr("modern_damage_system.hud.style_blood");
        int tagColor = forest ? 0xFF8AD46A : 0xFFFF968C;
        int tagW = font.width(tag) + 8;
        int tagX = x + panelWidth - padding - tagW;
        gfx.fill(tagX, y + (int)(3 * scale), tagX + tagW, y + (int)(13 * scale),
            forest ? 0xB0182A18 : 0xB02A0A0E);
        gfx.text(font, tag, tagX + 4, y + (int)(4 * scale), tagColor);

        // 数据行：色标 + 左标签 + 右对齐数值
        int curY = y + headerH + padding;
        for (String line : weaponLines) {
            drawStatRow(gfx, font, x, curY, panelWidth, padding, line, forest);
            curY += lineHeight;
        }
        if (!weaponLines.isEmpty() && !armorLines.isEmpty()) {
            gfx.fill(x + padding, curY, x + panelWidth - padding, curY + 1,
                forest ? 0xFF3A5630 : 0xFF5A2A2A);
            if (forest) {
                drawLeaf(gfx, x + panelWidth - padding - 7, curY, true);
                drawLeaf(gfx, x + panelWidth - padding - 12, curY + 1, false);
            } else {
                dot(gfx, x + panelWidth - padding - 6, curY, 0xFF8A1A1E);
                dot(gfx, x + panelWidth - padding - 10, curY + 1, 0xFF6A1214);
            }
            curY += lineHeight;
        }
        for (String line : armorLines) {
            drawStatRow(gfx, font, x, curY, panelWidth, padding, line, forest);
            curY += lineHeight;
        }

        // 编辑模式：切换风格按钮 + 提示
        if (editMode) {
            drawStyleButton(gfx, x, y, panelWidth, panelHeight, scale, leftSide);
            String hint = tr("modern_damage_system.hud.edit_hint");
            int hintWidth = mc.font.width(hint);
            gfx.text(mc.font, hint, x + panelWidth / 2 - hintWidth / 2, y - 12, 0xFFFFFF00);
        }
    }

    // ==================== HUD 风格装饰 ====================

    private static int lastPanelX, lastPanelY, lastPanelWidth, lastPanelHeight;
    private static int styleBtnX = -1, styleBtnY, styleBtnW, styleBtnH;

    private static void dot(GuiGraphicsExtractor gfx, int x, int y, int color) {
        gfx.fill(x, y, x + 1, y + 1, color);
    }

    /** 标题栏的小暴击星。 */
    private static void drawCritStar(GuiGraphicsExtractor gfx, int x, int y) {
        gfx.fill(x + 2, y, x + 3, y + 6, 0xFFFFD97A);
        gfx.fill(x, y + 2, x + 6, y + 3, 0xFFFFD97A);
        gfx.fill(x + 2, y + 2, x + 3, y + 3, 0xFFFFF0C0);
    }

    /**
     * 旧式 "§c标签: §f数值" 行 → 色标方块 + 左标签 + 右对齐数值。
     * 保留原有收集逻辑，只在渲染时拆分，避免改动大量取数代码。
     */
    private static void drawStatRow(GuiGraphicsExtractor gfx, Font font, int x, int y,
                                    int width, int pad, String raw, boolean forest) {
        String plain = raw.replaceAll("§.", "").trim();
        int idx = plain.indexOf(':');
        String label = idx > 0 ? plain.substring(0, idx).trim() : plain;
        String value = idx > 0 ? plain.substring(idx + 1).trim() : "";
        int bullet = firstColorCode(raw, forest ? 0xFF8AC66A : 0xFFFF968C);
        gfx.fill(x + pad, y + 3, x + pad + 4, y + 7, bullet);
        gfx.fill(x + pad, y + 3, x + pad + 4, y + 4, 0x50FFFFFF);
        gfx.text(font, label, x + pad + 7, y, forest ? 0xFFD6DAE4 : 0xFFE6E0E0);
        if (!value.isEmpty()) {
            int vw = font.width(value);
            gfx.text(font, value, x + width - pad - vw, y, forest ? 0xFFFFE0A0 : 0xFFFFB4AC);
        }
    }

    /** 取原字符串里第一个 § 颜色码对应的颜色，用作色标。 */
    private static int firstColorCode(String s, int fallback) {
        for (int i = 0; i + 1 < s.length(); i++) {
            if (s.charAt(i) == '§') {
                switch (Character.toLowerCase(s.charAt(i + 1))) {
                    case 'c': return 0xFFD9534F;
                    case 'e': return 0xFFE8B14A;
                    case 'a': return 0xFF8AC66A;
                    case 'b': return 0xFF6AC6E8;
                    case 'd': return 0xFFE888D8;
                    case '5': return 0xFFB06AD4;
                    case '6': return 0xFFD4A24A;
                    case '9': return 0xFF7A8AD4;
                    case 'f': return 0xFFD6DAE4;
                    default: break;
                }
            }
        }
        return fallback;
    }

    /** 森林风：暗绿底 + 金色强调条 + 绿叶/小花装饰。 */
    private static void drawForestChrome(GuiGraphicsExtractor gfx, int x, int y, int w, int h,
                                         int headerH, int alpha, boolean editMode) {
        gfx.fill(x, y, x + w, y + h, (alpha << 24) | 0x0C140C);
        int border = editMode ? 0xFF7AC44A : 0xFF3A5630;
        gfx.fill(x, y, x + w, y + 1, border);
        gfx.fill(x, y + h - 1, x + w, y + h, border);
        gfx.fill(x, y, x + 1, y + h, border);
        gfx.fill(x + w - 1, y, x + w, y + h, border);
        // 左侧金色强调条（三段近似渐变）
        gfx.fill(x + 1, y + 1, x + 3, y + headerH, 0xFF8A6414);
        gfx.fill(x + 1, y + headerH, x + 3, y + h / 2, 0xFFB8860B);
        gfx.fill(x + 1, y + h / 2, x + 3, y + h - 1, 0xFFDAA520);
        // 标题栏底色 + 下划线
        gfx.fill(x + 3, y + 1, x + w - 1, y + headerH, 0x60182A18);
        gfx.fill(x + 3, y + headerH, x + w - 1, y + headerH + 1, 0xFF8A6414);
        // ---- 花草装饰（常态显示，对齐设计稿）----
        // 顶部藤蔓（双像素，带波浪）
        for (int i = 0; i < w; i++) {
            int yy = y - 3 + ((i / 9) % 2);
            dot(gfx, x + i, yy, (i % 4 < 2) ? 0xFF8AC66A : 0xFF5E8C4A);
            if (i % 5 == 0) dot(gfx, x + i, yy + 1, 0xFF5E8C4A);
        }
        for (int i = 8; i < w - 6; i += 22) {
            drawLeaf(gfx, x + i, y - 7, false);
            drawLeaf(gfx, x + i + 5, y - 5, true);
        }
        // 左边缘连续藤蔓 + 叶簇 + 小白花
        for (int i = 0; i < h - 6; i++) {
            dot(gfx, x - 3, y + 3 + i, (i % 3 < 2) ? 0xFF8AC66A : 0xFF5E8C4A);
            if (i % 6 < 3) dot(gfx, x - 2, y + 3 + i, 0xFF5E8C4A);
        }
        for (int i = 10; i < h - 12; i += 22) {
            drawLeaf(gfx, x - 2, y + i, false);
            drawLeaf(gfx, x - 2, y + i + 6, true);
        }
        for (int i = 22; i < h - 10; i += 44) {
            dot(gfx, x - 6, y + i, 0xFFF6F6EC);
            dot(gfx, x - 6, y + i + 1, 0xFFE2E2C8);
        }
        // 底部藤蔓 + 叶 + 草丛
        for (int i = 1; i < w - 1; i++) {
            dot(gfx, x + i, y + h + 1, (i % 5 < 3) ? 0xFF5E8C4A : 0xFF8AC66A);
            dot(gfx, x + i, y + h + 2, (i % 3 < 2) ? 0xFF8AC66A : 0xFF5E8C4A);
        }
        for (int i = 10; i < w - 12; i += 26) {
            drawLeaf(gfx, x + i, y + h + 3, false);
        }
        for (int i = 22; i < w - 10; i += 40) {
            dot(gfx, x + i, y + h + 4, 0xFF8AC66A);
            dot(gfx, x + i + 2, y + h + 5, 0xFF5E8C4A);
            dot(gfx, x + i + 4, y + h + 4, 0xFF8AC66A);
        }
        // 右侧叶片
        for (int i = 14; i < h - 12; i += 30) {
            drawLeaf(gfx, x + w + 2, y + i, true);
        }
        // 花丛
        drawFlower(gfx, x - 8, y - 6, 0xFFEE9EBE, 0xFFF6DC84);
        drawFlower(gfx, x + w - 17, y - 7, 0xFFEE9EBE, 0xFFF6DC84);
        drawFlower(gfx, x + w - 8, y - 5, 0xFFF6DC84, 0xFFEE9EBE);
        drawFlower(gfx, x - 7, y + headerH + 16, 0xFFEE9EBE, 0xFFF6DC84);
        drawFlower(gfx, x - 7, y + headerH + 40, 0xFFF6DC84, 0xFFEE9EBE);
        drawFlower(gfx, x + 4, y + h + 4, 0xFFEE9EBE, 0xFFF6DC84);
        drawFlower(gfx, x + w - 10, y + h + 4, 0xFFF6DC84, 0xFFEE9EBE);
    }

    /** 血战风：暗红渐变 + 血红标题 + 血滴/血溅 + 苦力怕。 */
    private static void drawBloodChrome(GuiGraphicsExtractor gfx, int x, int y, int w, int h,
                                        int headerH, int alpha, boolean editMode) {
        int a = alpha;
        gfx.fill(x, y, x + w, y + h / 3, (a << 24) | 0x3A0E12);
        gfx.fill(x, y + h / 3, x + w, y + 2 * h / 3, (a << 24) | 0x260A0C);
        gfx.fill(x, y + 2 * h / 3, x + w, y + h, (a << 24) | 0x160607);
        int border = editMode ? 0xFF7AC44A : 0xFF7A262A;
        gfx.fill(x, y, x + w, y + 1, border);
        gfx.fill(x, y + h - 1, x + w, y + h, border);
        gfx.fill(x, y, x + 1, y + h, border);
        gfx.fill(x + w - 1, y, x + w, y + h, border);
        // 标题栏
        gfx.fill(x + 1, y + 1, x + w - 1, y + headerH, 0xC8A82028);
        gfx.fill(x + 1, y + headerH - 2, x + w - 1, y + headerH - 1, 0xFF58120E);
        for (int i = 0; i < w; i += 11) {
            dot(gfx, x + i, y + headerH, 0xFF8A1A1E);
            dot(gfx, x + i, y + headerH + 2, 0xFF8A1A1E);
        }
        // 血溅 + 敌对生物（常态显示）
        dot(gfx, x + w - 10, y + headerH + 10, 0xFF8A1A1E);
        dot(gfx, x + w - 14, y + headerH + 14, 0xFF6A1214);
        dot(gfx, x + w - 8, y + headerH + 20, 0xFF6A1214);
        dot(gfx, x + w - 16, y + headerH + 24, 0xFF8A1A1E);
        dot(gfx, x + 8, y + h - 8, 0xFF6A1214);
        dot(gfx, x + 14, y + h - 14, 0xFF8A1A1E);
        drawCreeper(gfx, x + 3, y + h - 12);
    }

    /** 编辑模式：面板旁的"切换风格"按钮。 */
    private static void drawStyleButton(GuiGraphicsExtractor gfx, int x, int y, int w, int h,
                                        float scale, boolean leftSide) {
        int bw = (int)(62 * scale);
        int bh = (int)(16 * scale);
        int bx = leftSide ? x + w + 6 : x - bw - 6;
        int by = y + 4;
        styleBtnX = bx; styleBtnY = by; styleBtnW = bw; styleBtnH = bh;
        gfx.fill(bx + 2, by + 2, bx + bw + 2, by + bh + 2, 0x50000000);
        gfx.fill(bx, by, bx + bw, by + bh, 0xE0221830);
        gfx.fill(bx, by, bx + bw, by + 1, 0xFFFFD97A);
        gfx.fill(bx, by + bh - 1, bx + bw, by + bh, 0xFF8A6414);
        gfx.fill(bx, by, bx + 1, by + bh, 0xFFFFD97A);
        gfx.fill(bx + bw - 1, by, bx + bw, by + bh, 0xFF8A6414);
        gfx.text(Minecraft.getInstance().font, tr("modern_damage_system.hud.switch_style"), bx + 5, by + 4, 0xFFFFE8C8);
        gfx.text(Minecraft.getInstance().font, tr("modern_damage_system.hud.edit_mode"), bx - 2, by - 15, 0xFFFFD97A);
        for (int i = bx - 4; i < bx + bw + 4; i += 4) {
            dot(gfx, i, by - 6, 0xFFFFD97A);
            dot(gfx, i, by + bh + 5, 0xFFFFD97A);
        }
    }

    /** 是否点中"切换风格"按钮（编辑模式下由鼠标事件调用）。 */
    public static boolean isStyleButtonHit(int mouseX, int mouseY) {
        return styleBtnX >= 0 && mouseX >= styleBtnX && mouseX < styleBtnX + styleBtnW
            && mouseY >= styleBtnY && mouseY < styleBtnY + styleBtnH;
    }

    private static void drawLeaf(GuiGraphicsExtractor gfx, int x, int y, boolean flip) {
        int dx = flip ? -1 : 1;
        dot(gfx, x + dx, y, 0xFF5E8C4A);
        dot(gfx, x + 2 * dx, y, 0xFF8AC66A);
        dot(gfx, x, y + 1, 0xFF5E8C4A);
        dot(gfx, x + dx, y + 1, 0xFF8AC66A);
        dot(gfx, x + 2 * dx, y + 1, 0xFF8AC66A);
        dot(gfx, x + dx, y + 2, 0xFF5E8C4A);
    }

    private static void drawFlower(GuiGraphicsExtractor gfx, int x, int y, int c1, int c2) {
        dot(gfx, x + 1, y, c1);
        dot(gfx, x, y + 1, c2);
        dot(gfx, x + 1, y + 1, 0xFFFFF6C8);
        dot(gfx, x + 2, y + 1, c2);
        dot(gfx, x + 1, y + 2, c1);
    }

    private static void drawCreeper(GuiGraphicsExtractor gfx, int x, int y) {
        int green = 0xFF4E8E4C;
        int[][] face = {
            {1, 1}, {2, 1}, {5, 1}, {6, 1},
            {1, 2}, {2, 2}, {5, 2}, {6, 2},
            {3, 3}, {4, 3},
            {2, 4}, {3, 4}, {4, 4}, {5, 4},
            {2, 5}, {3, 5}, {4, 5}, {5, 5},
            {2, 6}, {5, 6},
        };
        for (int[] p : face) {
            gfx.fill(x + p[0], y + p[1], x + p[0] + 1, y + p[1] + 1, green);
        }
    }

    private static void addEnchantedBookDescription(ItemStack stack, java.util.List<Component> lines) {
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

    private static void addWeaponCriticalInfo(ItemStack stack, java.util.List<Component> lines) {
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

    private static void addArmorCriticalInfo(ItemStack stack, java.util.List<Component> lines) {
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

    private static boolean isWeapon(ItemStack stack) {
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

    private static boolean isArmor(ItemStack stack) {
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

    private static float getWeaponBaseDamage(ItemStack stack) {
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

    private static String formatDamage(float damage) {
        if (damage == (int) damage) return String.valueOf((int) damage);
        return String.format("%.1f", damage);
    }
}
