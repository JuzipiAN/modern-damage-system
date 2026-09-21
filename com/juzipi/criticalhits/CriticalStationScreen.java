package com.juzipi.criticalhits;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/**
 * 暴击台界面。
 * 自定义暴击主题背景：暗红色调、随机火花粒子、顶部血滴下落动画。
 */
public class CriticalStationScreen extends AbstractContainerScreen<CriticalStationMenu> {

    private final List<Particle> particles = new ArrayList<>();
    private final List<BloodDrop> bloodDrops = new ArrayList<>();
    private final RandomSource random = RandomSource.create();
    private int tickCount = 0;
    private boolean decorationsDrawn = false;

    public CriticalStationScreen(CriticalStationMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 166);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        int x = this.leftPos;
        int y = this.topPos;

        // 装饰性外边框：最外层深色背景（扩大到6像素，给细节留空间）
        extractor.fill(x - 6, y - 6, x + this.imageWidth + 6, y + this.imageHeight + 6, 0xFF050505);

        // 外层边框：深色带金色调
        extractor.fill(x - 6, y - 6, x + this.imageWidth + 6, y - 4, 0xFF3A2A10); // 上边
        extractor.fill(x - 6, y + this.imageHeight + 4, x + this.imageWidth + 6, y + this.imageHeight + 6, 0xFF3A2A10); // 下边
        extractor.fill(x - 6, y - 6, x - 4, y + this.imageHeight + 6, 0xFF3A2A10); // 左边
        extractor.fill(x + this.imageWidth + 4, y - 6, x + this.imageWidth + 6, y + this.imageHeight + 6, 0xFF3A2A10); // 右边

        // 中层边框：中金色
        extractor.fill(x - 4, y - 4, x + this.imageWidth + 4, y - 3, 0xFF8B6914); // 上边
        extractor.fill(x - 4, y + this.imageHeight + 3, x + this.imageWidth + 4, y + this.imageHeight + 4, 0xFF8B6914); // 下边
        extractor.fill(x - 4, y - 4, x - 3, y + this.imageHeight + 4, 0xFF8B6914); // 左边
        extractor.fill(x + this.imageWidth + 3, y - 4, x + this.imageWidth + 4, y + this.imageHeight + 4, 0xFF8B6914); // 右边

        // 内层边框：浅金色细线
        extractor.fill(x - 3, y - 3, x + this.imageWidth + 3, y - 2, 0xFFDAA520); // 上边
        extractor.fill(x - 3, y + this.imageHeight + 2, x + this.imageWidth + 3, y + this.imageHeight + 3, 0xFFDAA520); // 下边
        extractor.fill(x - 3, y - 3, x - 2, y + this.imageHeight + 3, 0xFFDAA520); // 左边
        extractor.fill(x + this.imageWidth + 2, y - 3, x + this.imageWidth + 3, y + this.imageHeight + 3, 0xFFDAA520); // 右边

        // 边框装饰铆钉：上边（每隔20像素一个金色小点）
        for (int i = x + 10; i < x + this.imageWidth - 10; i += 20) {
            extractor.fill(i, y - 5, i + 2, y - 3, 0xFFFFD700);
        }
        // 下边
        for (int i = x + 10; i < x + this.imageWidth - 10; i += 20) {
            extractor.fill(i, y + this.imageHeight + 3, i + 2, y + this.imageHeight + 5, 0xFFFFD700);
        }
        // 左边
        for (int i = y + 10; i < y + this.imageHeight - 10; i += 20) {
            extractor.fill(x - 5, i, x - 3, i + 2, 0xFFFFD700);
        }
        // 右边
        for (int i = y + 10; i < y + this.imageHeight - 10; i += 20) {
            extractor.fill(x + this.imageWidth + 3, i, x + this.imageWidth + 5, i + 2, 0xFFFFD700);
        }

        // 四个角的装饰角块（金色华丽风格）
        drawDecorativeCorner(extractor, x - 6, y - 6, true, true);   // 左上角
        drawDecorativeCorner(extractor, x + this.imageWidth, y - 6, false, true);  // 右上角
        drawDecorativeCorner(extractor, x - 6, y + this.imageHeight, true, false);  // 左下角
        drawDecorativeCorner(extractor, x + this.imageWidth, y + this.imageHeight, false, false); // 右下角

        // 边框和内部界面之间的过渡色（深色渐变）
        extractor.fill(x - 2, y - 2, x + this.imageWidth + 2, y, 0xFF151515); // 上边过渡
        extractor.fill(x - 2, y + this.imageHeight, x + this.imageWidth + 2, y + this.imageHeight + 2, 0xFF151515); // 下边过渡
        extractor.fill(x - 2, y, x, y + this.imageHeight, 0xFF151515); // 左边过渡
        extractor.fill(x + this.imageWidth, y, x + this.imageWidth + 2, y + this.imageHeight, 0xFF151515); // 右边过渡

        // 主背景：深灰色Minecraft GUI风格（加深，增加对比度）
        extractor.fill(x, y, x + this.imageWidth, y + this.imageHeight, 0xFF1A1A1A);

        // 外边框：浅灰色像素边框（Minecraft GUI风格）
        extractor.fill(x, y, x + this.imageWidth, y + 1, 0xFF8B8B8B);
        extractor.fill(x, y + this.imageHeight - 1, x + this.imageWidth, y + this.imageHeight, 0xFF8B8B8B);
        extractor.fill(x, y, x + 1, y + this.imageHeight, 0xFF8B8B8B);
        extractor.fill(x + this.imageWidth - 1, y, x + this.imageWidth, y + this.imageHeight, 0xFF8B8B8B);

        // 顶部标题区域（稍浅的灰色，用不透明矩形覆盖原标题）
        extractor.fill(x + 4, y + 4, x + this.imageWidth - 4, y + 18, 0xFF252525);
        drawSlotBorder(extractor, x + 4, y + 4, this.imageWidth - 8, 14, 0xFF555555);

        // 合成区域背景（深灰色）
        extractor.fill(x + 4, y + 20, x + this.imageWidth - 4, y + 62, 0xFF202020);
        drawSlotBorder(extractor, x + 4, y + 20, this.imageWidth - 8, 42, 0xFF555555);

        // 输入槽背景（2x2区域，更深的灰色，浅灰色边框）
        extractor.fill(x + 22, y + 24, x + 74, y + 58, 0xFF0F0F0F);
        drawSlotBorder(extractor, x + 22, y + 24, 52, 34, 0xFF555555);

        // 输出槽背景（更大，带金色高亮边框）
        extractor.fill(x + 110, y + 26, x + 144, y + 58, 0xFF0F0F0F);
        drawSlotBorder(extractor, x + 110, y + 26, 34, 32, 0xFFFFD700);

        // 玩家背包区域（深灰色，用不透明矩形覆盖原物品栏文字）
        extractor.fill(x + 4, y + 66, x + this.imageWidth - 4, y + 134, 0xFF202020);
        drawSlotBorder(extractor, x + 4, y + 66, this.imageWidth - 8, 68, 0xFF555555);

        // 快捷栏区域（深灰色）
        extractor.fill(x + 4, y + 138, x + this.imageWidth - 4, y + 160, 0xFF202020);
        drawSlotBorder(extractor, x + 4, y + 138, this.imageWidth - 8, 22, 0xFF555555);

        // 背景装饰：合成区域两侧小物品（降低渲染频率）
        if (tickCount % 10 == 0 || !decorationsDrawn) {
            decorationsDrawn = true;
        }
        drawDecoratedItem(extractor, new ItemStack(Items.DIAMOND_SWORD), x + 6, y + 32, 0.35f, -20f);
        drawDecoratedItem(extractor, new ItemStack(Items.NETHERITE_CHESTPLATE), x + this.imageWidth - 18, y + 32, 0.3f, 15f);

        // 再调用父类渲染槽位（在背景之上）
        super.extractRenderState(extractor, mouseX, mouseY, partialTick);
    }

    private void drawDecoratedItem(GuiGraphicsExtractor extractor, ItemStack stack, int x, int y, float scale, float rotation) {
        var pose = extractor.pose();
        pose.pushMatrix();
        pose.translate(x + 8, y + 8);
        if (rotation != 0) pose.rotate((float) Math.toRadians(rotation));
        if (scale != 1.0f) pose.scale(scale, scale);
        pose.translate(-8, -8);
        extractor.item(stack, 0, 0);
        pose.popMatrix();
    }

    private void drawSlotBorder(GuiGraphicsExtractor extractor, int x, int y, int w, int h, int color) {
        extractor.fill(x, y, x + w, y + 1, color);
        extractor.fill(x, y + h - 1, x + w, y + h, color);
        extractor.fill(x, y, x + 1, y + h, color);
        extractor.fill(x + w - 1, y, x + w, y + h, color);
    }

    @Override
    public void extractContents(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        super.extractContents(extractor, mouseX, mouseY, partialTick);
        int x = this.leftPos;
        int y = this.topPos;

        // 先用不透明矩形覆盖原浅灰色标题区域（防止标题重复渲染）
        extractor.fill(x + 4, y + 4, x + this.imageWidth - 4, y + 18, 0xFF252525);
        drawSlotBorder(extractor, x + 4, y + 4, this.imageWidth - 8, 14, 0xFF555555);

        // 手动渲染标题（居中，白色）
        extractor.centeredText(this.font, this.title.getString(), x + this.imageWidth / 2, y + 7, 0xFFFFFFFF);

        // 先用不透明矩形覆盖原浅灰色物品栏文字区域
        extractor.fill(x + 4, y + 66, x + this.imageWidth - 4, y + 76, 0xFF202020);

        // 手动渲染物品栏文字（白色，左对齐，用centeredText模拟）
        String inventoryText = this.playerInventoryTitle.getString();
        int textWidth = this.font.width(inventoryText);
        extractor.centeredText(this.font, inventoryText, x + 8 + textWidth / 2, y + 69, 0xFFFFFFFF);

        // 箭头（在2x2输入槽和输出槽之间，完美居中）
        extractor.centeredText(this.font, "→", x + 92, y + 41, 0xFFCC8800);

        // 绘制粒子
        for (Particle p : particles) {
            int px = x + (int) p.x;
            int py = y + (int) p.y;
            if (px >= x && px <= x + this.imageWidth && py >= y && py <= y + this.imageHeight) {
                extractor.fill(px, py, px + 1, py + 1, p.color);
            }
        }

        // 绘制血滴
        for (BloodDrop d : bloodDrops) {
            int dx = x + (int) d.x;
            int dy = y + (int) d.y;
            if (dx >= x && dx <= x + this.imageWidth && dy >= y && dy <= y + this.imageHeight) {
                extractor.fill(dx, dy, dx + 1, dy + 2, 0xCC8B0000);
                extractor.fill(dx - 1, dy + 1, dx + 2, dy + 3, 0xCC8B0000);
                extractor.fill(dx, dy + 3, dx + 1, dy + 4, 0x99660000);
            }
        }

        // 更新动画（每隔2帧更新一次，性能优化）
        tickCount++;
        if (tickCount % 2 == 0) {
            updateParticles();
            updateBloodDrops();
            spawnParticles();
            spawnBloodDrops();
        }
    }

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
            // 2x2输入槽位置 + 输出槽位置（新坐标）
            int[][] areas = {{31, 24, 16, 16}, {49, 24, 16, 16}, {31, 42, 16, 16}, {49, 42, 16, 16}, {119, 34, 16, 16}};
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

    /**
     * 绘制装饰性角块（金色华丽风格，多层渐变+装饰细节）
     * @param extractor 绘图器
     * @param cornerX 角点X坐标
     * @param cornerY 角点Y坐标
     * @param isLeft 是否是左侧角（左上角/左下角）
     * @param isTop 是否是顶部角（左上角/右上角）
     */
    private void drawDecorativeCorner(GuiGraphicsExtractor extractor, int cornerX, int cornerY, boolean isLeft, boolean isTop) {
        int size = 12;
        int dx = isLeft ? 1 : -1;
        int dy = isTop ? 1 : -1;

        // 第1层：最深金色（金属暗部）
        extractor.fill(cornerX, cornerY, cornerX + dx * size, cornerY + dy * 2, 0xFF6B4914);
        extractor.fill(cornerX, cornerY, cornerX + dx * 2, cornerY + dy * size, 0xFF6B4914);

        // 第2层：深金色
        extractor.fill(cornerX + dx * 2, cornerY + dy * 2, cornerX + dx * (size - 2), cornerY + dy * 3, 0xFF8B6914);
        extractor.fill(cornerX + dx * 2, cornerY + dy * 2, cornerX + dx * 3, cornerY + dy * (size - 2), 0xFF8B6914);

        // 第3层：中金色
        extractor.fill(cornerX + dx * 3, cornerY + dy * 3, cornerX + dx * (size - 3), cornerY + dy * 4, 0xFFB8860B);
        extractor.fill(cornerX + dx * 3, cornerY + dy * 3, cornerX + dx * 4, cornerY + dy * (size - 3), 0xFFB8860B);

        // 第4层：浅金色
        extractor.fill(cornerX + dx * 4, cornerY + dy * 4, cornerX + dx * (size - 4), cornerY + dy * 5, 0xFFDAA520);
        extractor.fill(cornerX + dx * 4, cornerY + dy * 4, cornerX + dx * 5, cornerY + dy * (size - 4), 0xFFDAA520);

        // 第5层：亮金色
        extractor.fill(cornerX + dx * 5, cornerY + dy * 5, cornerX + dx * (size - 5), cornerY + dy * 6, 0xFFFFD700);
        extractor.fill(cornerX + dx * 5, cornerY + dy * 5, cornerX + dx * 6, cornerY + dy * (size - 5), 0xFFFFD700);

        // 角点核心：最亮的金色高光
        extractor.fill(cornerX + dx * 6, cornerY + dy * 6, cornerX + dx * 8, cornerY + dy * 8, 0xFFFFE44D);

        // 最亮核心点
        extractor.fill(cornerX + dx * 7, cornerY + dy * 7, cornerX + dx * 8, cornerY + dy * 8, 0xFFFFFFE0);

        // 装饰细节：角块外侧的小装饰点
        extractor.fill(cornerX + dx * (size - 1), cornerY + dy * 3, cornerX + dx * size, cornerY + dy * 4, 0xFFFFD700);
        extractor.fill(cornerX + dx * 3, cornerY + dy * (size - 1), cornerX + dx * 4, cornerY + dy * size, 0xFFFFD700);
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
