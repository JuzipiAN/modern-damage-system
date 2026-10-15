package com.juzipi.criticalhits;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/**
 * 暴击台界面（v2 血战风格）。
 *
 * 修复旧版问题：
 * 1. 所有槽位现在都有明确的 18×18 槽框。
 * 2. 输入井 / 输出金槽与真实槽位几何对齐。
 * 3. 统一血战铁框 + 血红标题 + 附魔书/武器/敌对生物装饰。
 */
public class CriticalStationScreen extends AbstractContainerScreen<CriticalStationMenu> {

    private final List<Particle> particles = new ArrayList<>();
    private final List<BloodDrop> bloodDrops = new ArrayList<>();
    private final RandomSource random = RandomSource.create();
    private int tickCount = 0;

    private static final String[] CREEPER = {
        "........", ".XX..XX.", ".XX..XX.", "...XX...",
        "..XXXX..", "..XXXX..", "..X..X..", "........",
    };
    private static final String[] ZOMBIE = {
        "........", ".XXXXXX.", ".XXXXXX.", ".X.XX.X.",
        ".X.XX.X.", ".XXXXXX.", ".X.XX.X.", ".XX..XX.",
    };
    private static final String[] SKELETON = {
        "..XXXX..", ".XXXXXX.", "XXXXXXXX", "X.XXXX.X",
        "X.XXXX.X", "XXXXXXXX", ".XXXXXX.", "..X..X..",
    };
    private static final String[] SPIDER = {
        "........", ".o....o.", ".XXXXXX.", ".XXXXXX.",
        "..XXXX..", "..XXXX..", "...XX...", "........",
    };

    public CriticalStationScreen(CriticalStationMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 166);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor e, int mouseX, int mouseY, float partialTick) {
        int x = this.leftPos;
        int y = this.topPos;
        int w = this.imageWidth;
        int h = this.imageHeight;

        // ---- 血战铁框 ----
        e.fill(x - 3, y - 3, x + w + 3, y + h + 3, 0xFF0A080A);
        e.fill(x - 3, y - 3, x + w + 3, y - 1, 0xFF967672);
        e.fill(x - 3, y - 3, x - 1, y + h + 3, 0xFF967672);
        e.fill(x - 3, y + h + 1, x + w + 3, y + h + 3, 0xFF2E1214);
        e.fill(x + w + 1, y - 3, x + w + 3, y + h + 3, 0xFF2E1214);
        e.fill(x - 1, y - 1, x + w + 1, y, 0xFF701E22);
        e.fill(x - 1, y + h, x + w + 1, y + h + 1, 0xFF701E22);
        e.fill(x - 1, y - 1, x, y + h + 1, 0xFF701E22);
        e.fill(x + w, y - 1, x + w + 1, y + h + 1, 0xFF701E22);

        // ---- 主背景 ----
        e.fill(x, y, x + w, y + h, 0xFF1A1C21);

        // ---- 合成区 (19..70) ----
        e.fill(x + 5, y + 19, x + w - 5, y + 70, 0xFF20232B);
        drawSlotBorder(e, x + 5, y + 19, w - 10, 51, 0xFF60282A);

        // 左带装饰：2 本附魔书 + 剑 + 斧
        drawDecoratedItem(e, new ItemStack(Items.ENCHANTED_BOOK), x + 5, y + 20, 0.5f, -8f);
        drawDecoratedItem(e, new ItemStack(Items.ENCHANTED_BOOK), x + 5, y + 31, 0.5f, 6f);
        drawDecoratedItem(e, new ItemStack(Items.DIAMOND_SWORD), x + 5, y + 42, 0.45f, -25f);
        drawDecoratedItem(e, new ItemStack(Items.IRON_AXE), x + 6, y + 53, 0.45f, 16f);

        // 输入井 (28,19)-(72,63)
        e.fill(x + 28, y + 19, x + 72, y + 63, 0xFF101116);
        drawSlotBorder(e, x + 28, y + 19, 44, 44, 0xFF0C0D11);

        // 输出金槽 (104,22)-(142,62)
        e.fill(x + 104, y + 22, x + 142, y + 62, 0xFF281A10);
        drawSlotBorder(e, x + 104, y + 22, 38, 40, 0xFFE8B14A);
        e.fill(x + 105, y + 23, x + 141, y + 61, 0xFF14161C);

        // 右带装饰：4 种敌对生物
        drawPixelArt(e, x + 148, y + 20, CREEPER, 0xFF4E8E4C, 0);
        drawPixelArt(e, x + 148, y + 32, ZOMBIE, 0xFF5A8050, 0);
        drawPixelArt(e, x + 148, y + 44, SKELETON, 0xFFE2E2D6, 0);
        drawPixelArt(e, x + 150, y + 56, SPIDER, 0xFF30221E, 0xFFE23C34);

        // ---- 玩家背包区 (72..162) ----
        e.fill(x + 5, y + 72, x + w - 5, y + 162, 0xFF1E2026);
        drawSlotBorder(e, x + 5, y + 72, w - 10, 90, 0xFF60282A);

        // 所有槽位统一绘制 18×18 槽框（在物品之下）
        for (Slot slot : this.menu.slots) {
            drawSlotFrame(e, x + slot.x - 1, y + slot.y - 1);
        }

        super.extractRenderState(e, mouseX, mouseY, partialTick);
    }

    @Override
    public void extractContents(GuiGraphicsExtractor e, int mouseX, int mouseY, float partialTick) {
        super.extractContents(e, mouseX, mouseY, partialTick);
        int x = this.leftPos;
        int y = this.topPos;
        int w = this.imageWidth;

        // 覆盖原标题区域 + 血红标题栏
        e.fill(x + 4, y + 4, x + w - 4, y + 18, 0xFF1A1C21);
        for (int i = 0; i < 12; i++) {
            e.fill(x + 5, y + 5 + i, x + w - 5, y + 6 + i, mixColor(0xFFA82028, 0xFF58120E, i / 11.0f));
        }
        e.fill(x + 5, y + 17, x + w - 5, y + 18, 0xFF8A6414);
        e.centeredText(this.font, this.title.getString(), x + w / 2, y + 7, 0xFFFFE6E6);

        // 覆盖原物品栏文字区域，重画标签
        e.fill(x + 5, y + 70, x + w - 5, y + 82, 0xFF1E2026);
        String inventoryText = this.playerInventoryTitle.getString();
        int textWidth = this.font.width(inventoryText);
        e.centeredText(this.font, inventoryText, x + 8 + textWidth / 2, y + 74, 0xFFDED8D8);

        // 像素箭头 + 提示
        drawArrow(e, x + 78, y + 40);
        e.text(this.font, "x4", x + 86, y + 56, 0xFFB0A890);
        e.text(this.font, "等级+1", x + 108, y + 56, 0xFFFFD97A);

        // 粒子
        for (Particle p : particles) {
            int px = x + (int) p.x;
            int py = y + (int) p.y;
            if (px >= x && px <= x + this.imageWidth && py >= y && py <= y + this.imageHeight) {
                e.fill(px, py, px + 1, py + 1, p.color);
            }
        }
        // 血滴
        for (BloodDrop d : bloodDrops) {
            int dx = x + (int) d.x;
            int dy = y + (int) d.y;
            if (dx >= x && dx <= x + this.imageWidth && dy >= y && dy <= y + this.imageHeight) {
                e.fill(dx, dy, dx + 1, dy + 2, 0xCC8B0000);
                e.fill(dx - 1, dy + 1, dx + 2, dy + 3, 0xCC8B0000);
                e.fill(dx, dy + 3, dx + 1, dy + 4, 0x99660000);
            }
        }

        tickCount++;
        if (tickCount % 2 == 0) {
            updateParticles();
            updateBloodDrops();
            spawnParticles();
            spawnBloodDrops();
        }
    }

    // ==================== 绘制工具 ====================

    private void drawSlotFrame(GuiGraphicsExtractor e, int sx, int sy) {
        e.fill(sx, sy, sx + 18, sy + 18, 0xFF14161C);
        e.fill(sx, sy, sx + 18, sy + 1, 0xFF08090C);
        e.fill(sx, sy, sx + 1, sy + 18, 0xFF08090C);
        e.fill(sx, sy + 17, sx + 18, sy + 18, 0xFF4A5060);
        e.fill(sx + 17, sy, sx + 18, sy + 18, 0xFF4A5060);
    }

    private void drawSlotBorder(GuiGraphicsExtractor e, int sx, int sy, int sw, int sh, int color) {
        e.fill(sx, sy, sx + sw, sy + 1, color);
        e.fill(sx, sy + sh - 1, sx + sw, sy + sh, color);
        e.fill(sx, sy, sx + 1, sy + sh, color);
        e.fill(sx + sw - 1, sy, sx + sw, sy + sh, color);
    }

    private void drawDecoratedItem(GuiGraphicsExtractor e, ItemStack stack, int sx, int sy, float scale, float rotation) {
        var pose = e.pose();
        pose.pushMatrix();
        pose.translate(sx + 8, sy + 8);
        if (rotation != 0) pose.rotate((float) Math.toRadians(rotation));
        if (scale != 1.0f) pose.scale(scale, scale);
        pose.translate(-8, -8);
        e.item(stack, 0, 0);
        pose.popMatrix();
    }

    private void drawArrow(GuiGraphicsExtractor e, int sx, int sy) {
        int gold = 0xFFE8B14A, goldLt = 0xFFFFD97A;
        for (int i = 0; i < 16; i++) {
            e.fill(sx + i, sy, sx + i + 1, sy + 2, gold);
        }
        for (int i = 0; i < 6; i++) {
            e.fill(sx + 16 + i, sy - i + 2, sx + 16 + i + 1, sy + 2 + i + 1, goldLt);
        }
    }

    private void drawPixelArt(GuiGraphicsExtractor e, int sx, int sy, String[] art, int body, int eye) {
        for (int j = 0; j < art.length; j++) {
            String row = art[j];
            for (int i = 0; i < row.length(); i++) {
                char c = row.charAt(i);
                if (c == 'X') e.fill(sx + i, sy + j, sx + i + 1, sy + j + 1, body);
                else if (c == 'o') e.fill(sx + i, sy + j, sx + i + 1, sy + j + 1, eye);
            }
        }
    }

    private static int mixColor(int a, int b, float t) {
        int ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        int r = (int) (ar + (br - ar) * t);
        int g = (int) (ag + (bg - ag) * t);
        int bl = (int) (ab + (bb - ab) * t);
        return 0xFF000000 | (r << 16) | (g << 8) | bl;
    }

    // ==================== 粒子 / 血滴动画 ====================

    private void updateParticles() {
        particles.removeIf(p -> {
            p.x += p.vx;
            p.y += p.vy;
            p.vy += 0.02;
            p.life--;
            return p.life <= 0;
        });
    }

    private void updateBloodDrops() {
        bloodDrops.removeIf(d -> {
            d.y += d.vy;
            d.vy += 0.05;
            return d.y > this.imageHeight + 5;
        });
    }

    private void spawnParticles() {
        if (tickCount % 6 == 0 && particles.size() < 20) {
            int[][] areas = {{33, 25, 16, 16}, {51, 25, 16, 16}, {33, 43, 16, 16}, {51, 43, 16, 16}, {115, 35, 16, 16}};
            int[] a = areas[random.nextInt(areas.length)];
            int color = random.nextBoolean() ? 0xFFFFD700 : (random.nextBoolean() ? 0xFFFF6600 : 0xFFFFFFFF);
            particles.add(new Particle(
                a[0] + random.nextDouble() * a[2],
                a[1] + random.nextDouble() * a[3],
                (random.nextDouble() - 0.5) * 0.8,
                (random.nextDouble() - 0.5) * 0.8 - 0.3,
                color, 20 + random.nextInt(30)));
        }
    }

    private void spawnBloodDrops() {
        if (tickCount % 16 == 0 && bloodDrops.size() < 8) {
            bloodDrops.add(new BloodDrop(
                10 + random.nextDouble() * (this.imageWidth - 20),
                -2, 0.3 + random.nextDouble() * 0.5));
        }
    }

    private static class Particle {
        double x, y, vx, vy;
        int color, life;
        Particle(double x, double y, double vx, double vy, int color, int life) {
            this.x = x; this.y = y; this.vx = vx; this.vy = vy; this.color = color; this.life = life;
        }
    }

    private static class BloodDrop {
        double x, y, vy;
        BloodDrop(double x, double y, double vy) { this.x = x; this.y = y; this.vy = vy; }
    }
}
