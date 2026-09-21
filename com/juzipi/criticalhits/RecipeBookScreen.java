package com.juzipi.criticalhits;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/**
 * 现代化伤害系统配方书界面。
 * 继承AbstractContainerScreen，自动处理鼠标点击和滚轮滚动事件。
 */
public class RecipeBookScreen extends AbstractContainerScreen<RecipeBookMenu> {

    private static final int GUI_WIDTH = 300;
    private static final int GUI_HEIGHT = 220;
    private static final int CATEGORY_WIDTH = 80;
    private static final int RECIPE_HEIGHT = 55;
    private static final int MDS_STATION_HEIGHT = 80; // MDS合成台卡片更高，显示3x3布局
    private static final int SCROLL_BAR_WIDTH = 6;

    private int selectedCategory = 0;
    private double scrollOffset = 0;
    private String pendingTooltip = null; // 待渲染的tooltip文本，在最后统一渲染

    private final List<RecipeBookData.RecipeCategory> categories;

    public RecipeBookScreen(RecipeBookMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, GUI_WIDTH, GUI_HEIGHT);
        this.categories = RecipeBookData.getAllCategories();
    }

    @Override
    protected void init() {
        super.init();
        // 创建分类按钮
        int categoryY = this.topPos + 22;
        for (int i = 0; i < categories.size(); i++) {
            final int categoryIndex = i;
            RecipeBookData.RecipeCategory category = categories.get(i);
            int buttonWidth = CATEGORY_WIDTH - 4;
            int buttonHeight = 20;
            int buttonX = this.leftPos + 4;
            int buttonY = categoryY + 1;

            // 创建按钮（点击切换分类，tooltip由renderCategories手动实现）
            net.minecraft.client.gui.components.Button button = net.minecraft.client.gui.components.Button.builder(
                Component.literal(""), // 按钮文字为空，由renderCategories渲染
                btn -> {
                    selectedCategory = categoryIndex;
                    scrollOffset = 0;
                }
            ).bounds(buttonX, buttonY, buttonWidth, buttonHeight).build();

            this.addRenderableWidget(button);
            categoryY += 22 + 2;
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        pendingTooltip = null; // 清空待渲染的tooltip
        // 渲染背景（不渲染原版物品栏背景）
        renderCustomBackground(extractor);

        // 渲染分类标签页背景（点击事件由按钮处理）
        renderCategories(extractor, mouseX, mouseY);

        // 渲染配方列表
        renderRecipes(extractor, mouseX, mouseY);

        // 渲染滚动条
        renderScrollBar(extractor);

        // 在最后统一渲染tooltip（避免被其他元素遮挡）
        if (pendingTooltip != null) {
            renderMultiLineTooltip(extractor, pendingTooltip, mouseX, mouseY);
        }
    }

    private void renderCustomBackground(GuiGraphicsExtractor extractor) {
        int x = this.leftPos;
        int y = this.topPos;

        // 主背景
        extractor.fill(x, y, x + GUI_WIDTH, y + GUI_HEIGHT, 0xFF0A0A0A);

        // 边框
        extractor.fill(x, y, x + GUI_WIDTH, y + 1, 0xFFDAA520);
        extractor.fill(x, y + GUI_HEIGHT - 1, x + GUI_WIDTH, y + GUI_HEIGHT, 0xFFDAA520);
        extractor.fill(x, y, x + 1, y + GUI_HEIGHT, 0xFFDAA520);
        extractor.fill(x + GUI_WIDTH - 1, y, x + GUI_WIDTH, y + GUI_HEIGHT, 0xFFDAA520);

        // 标题栏
        extractor.fill(x + 1, y + 1, x + GUI_WIDTH - 1, y + 18, 0xFF1A1A2E);
        extractor.centeredText(this.font, Component.translatable("modern_damage_system.recipe.title"),
            x + GUI_WIDTH / 2, y + 5, 0xFFFFD700);

        // 分类区域和配方区域的分隔线
        extractor.fill(x + CATEGORY_WIDTH + 1, y + 19, x + CATEGORY_WIDTH + 2, y + GUI_HEIGHT - 1, 0xFF3A3A3A);
    }

    private void renderCategories(GuiGraphicsExtractor extractor, int mouseX, int mouseY) {
        int categoryY = this.topPos + 22;
        for (int i = 0; i < categories.size(); i++) {
            RecipeBookData.RecipeCategory category = categories.get(i);
            int categoryHeight = 22;
            int categoryX = this.leftPos + 2;
            int categoryWidth = CATEGORY_WIDTH;

            // 选中的分类高亮
            if (i == selectedCategory) {
                extractor.fill(categoryX, categoryY, categoryX + categoryWidth, categoryY + categoryHeight, 0xFF2A2A4A);
                extractor.fill(categoryX, categoryY, categoryX + 2, categoryY + categoryHeight, 0xFFFFD700);
            }

            // 分类名称（自动截断）
            Component categoryName = Component.translatable(category.titleKey);
            String catText = categoryName.getString();
            int maxCatWidth = categoryWidth - 12;
            catText = truncateText(catText, maxCatWidth);
            extractor.text(this.font, Component.literal(catText), this.leftPos + 8, categoryY + 7,
                i == selectedCategory ? 0xFFFFD700 : 0xFFAAAAAA);

            // 手动检测鼠标悬停，显示完整分类名称tooltip
            if (mouseX >= categoryX && mouseX <= categoryX + categoryWidth &&
                mouseY >= categoryY && mouseY <= categoryY + categoryHeight) {
                // 只有当名称被截断时才显示tooltip
                String fullText = categoryName.getString();
                if (this.font.width(fullText) > maxCatWidth) {
                    pendingTooltip = fullText;
                }
            }

            categoryY += categoryHeight + 2;
        }
    }

    private void renderRecipes(GuiGraphicsExtractor extractor, int mouseX, int mouseY) {
        if (selectedCategory >= categories.size()) return;

        RecipeBookData.RecipeCategory category = categories.get(selectedCategory);
        List<RecipeBookData.RecipeEntry> recipes = category.recipes;

        int contentX = this.leftPos + CATEGORY_WIDTH + 8;
        int contentWidth = GUI_WIDTH - CATEGORY_WIDTH - SCROLL_BAR_WIDTH - 20;
        int contentY = this.topPos + 22;
        int contentHeight = GUI_HEIGHT - 30;

        // 计算总高度
        int totalHeight = 0;
        String lastSubCategory = "";
        for (RecipeBookData.RecipeEntry recipe : recipes) {
            if (!recipe.subCategory.equals(lastSubCategory)) {
                totalHeight += 18;
                lastSubCategory = recipe.subCategory;
            }
            int cardHeight = recipe.nameKey.equals("modern_damage_system.recipe.mds_station") ? MDS_STATION_HEIGHT : RECIPE_HEIGHT;
            totalHeight += RECIPE_HEIGHT + 4;
        }

        // 限制滚动范围
        int maxScroll = Math.max(0, totalHeight - contentHeight);
        if (scrollOffset > maxScroll) scrollOffset = maxScroll;
        if (scrollOffset < 0) scrollOffset = 0;

        int currentY = contentY - (int) scrollOffset;
        lastSubCategory = "";

        for (RecipeBookData.RecipeEntry recipe : recipes) {
            // 二级标题
            if (!recipe.subCategory.equals(lastSubCategory)) {
                lastSubCategory = recipe.subCategory;
                if (currentY + 18 > contentY && currentY < contentY + contentHeight) {
                    extractor.fill(contentX, currentY, contentX + contentWidth, currentY + 18, 0xFF1A1A2E);
                    extractor.fill(contentX, currentY, contentX + contentWidth, currentY + 1, 0xFF3A3A5A);
                    extractor.text(this.font, Component.translatable(recipe.subCategory).copy().withStyle(net.minecraft.network.chat.Style.EMPTY.applyFormat(net.minecraft.ChatFormatting.GOLD).applyFormat(net.minecraft.ChatFormatting.BOLD)),
                        contentX + 6, currentY + 5, 0xFFFFD700);
                }
                currentY += 18;
            }

            // 配方卡片（只有在可视区域内才渲染）
            int cardHeight = recipe.nameKey.equals("modern_damage_system.recipe.mds_station") ? MDS_STATION_HEIGHT : RECIPE_HEIGHT;
            if (currentY + cardHeight > contentY && currentY < contentY + contentHeight) {
                renderRecipeCard(extractor, recipe, contentX, currentY, contentWidth, mouseX, mouseY, contentY, contentY + contentHeight, cardHeight);
            }
            currentY += cardHeight + 4;
        }
    }

    private void renderRecipeCard(GuiGraphicsExtractor extractor, RecipeBookData.RecipeEntry recipe,
                                   int x, int y, int width, int mouseX, int mouseY,
                                   int viewportTop, int viewportBottom, int cardHeight) {
        // 计算实际渲染的背景区域（只渲染可视区域内的部分）
        int bgTop = Math.max(y, viewportTop);
        int bgBottom = Math.min(y + cardHeight, viewportBottom);
        if (bgTop >= bgBottom) return; // 完全不在可视区域内，不渲染

        // 卡片背景（只渲染可视区域内的部分）
        extractor.fill(x, bgTop, x + width, bgBottom, 0xFF151525);
        // 卡片顶部边框（如果顶部在可视区域内）
        if (y >= viewportTop) {
            extractor.fill(x, y, x + width, y + 1, 0xFF3A3A5A);
        }
        // 卡片左右边框（只渲染可视区域内的部分）
        extractor.fill(x, bgTop, x + 1, bgBottom, 0xFF3A3A5A);
        extractor.fill(x + width - 1, bgTop, x + width, bgBottom, 0xFF3A3A5A);
        // 卡片底部边框（如果底部在可视区域内）
        if (y + cardHeight <= viewportBottom) {
            extractor.fill(x, y + cardHeight - 1, x + width, y + cardHeight, 0xFF3A3A5A);
        }

        // 输出物品图标（只有在可视区域内才渲染）
        if (y + 4 + 16 > viewportTop && y + 4 < viewportBottom) {
            extractor.item(recipe.output, x + 4, y + 4);
        }

        // 配方名称（只有在可视区域内才渲染）
        if (y + 4 + 10 > viewportTop && y + 4 < viewportBottom) {
            Component nameComp = Component.translatable(recipe.nameKey);
            String nameText = nameComp.getString();
            int maxNameWidth = width - 32 - 8;
            nameText = truncateText(nameText, maxNameWidth);
            extractor.text(this.font, Component.literal(nameText), x + 28, y + 4, 0xFFFFD700);
        }

        // 配方描述（只有在可视区域内才渲染）
        if (y + 16 + 10 > viewportTop && y + 16 < viewportBottom) {
            Component desc = Component.translatable(recipe.descriptionKey);
            String descText = desc.getString();
            int maxDescWidth = width - 32 - 8;
            descText = truncateText(descText, maxDescWidth);
            extractor.text(this.font, Component.literal("§7" + descText), x + 28, y + 16, 0xFFAAAAAA);
        }

        // 材料物品图标区域（只有在可视区域内才渲染）
        if (y + 30 + 16 > viewportTop && y + 30 < viewportBottom) {
            int materialX = x + 4;
            int materialY = y + 30;
            int maxMaterials = 0;

            // 特殊处理：MDS合成台显示3x3合成布局
            boolean isMdsStation = recipe.nameKey.equals("modern_damage_system.recipe.mds_station");
            // 特殊处理：现代化伤害材料模板显示所有材料选项
            boolean isMaterialTemplate = recipe.nameKey.equals("modern_damage_system.recipe.material_template");

            if (isMdsStation) {
                // 显示3x3合成布局
                int gridSize = 3;
                int slotSize = 16;
                int slotSpacing = 2;
                int gridWidth = gridSize * slotSize + (gridSize - 1) * slotSpacing;
                int gridX = x + (width - gridWidth) / 2;
                int gridY = y + 28;

                // 3x3格子的物品：角落=材料模板，边中=金块，中心=红石块
                ItemStack[][] grid = {
                    {new ItemStack(CriticalHitsMod.CRITICAL_COIN), new ItemStack(Items.GOLD_BLOCK), new ItemStack(CriticalHitsMod.CRITICAL_COIN)},
                    {new ItemStack(Items.GOLD_BLOCK), new ItemStack(Items.REDSTONE_BLOCK), new ItemStack(Items.GOLD_BLOCK)},
                    {new ItemStack(CriticalHitsMod.CRITICAL_COIN), new ItemStack(Items.GOLD_BLOCK), new ItemStack(CriticalHitsMod.CRITICAL_COIN)}
                };

                for (int row = 0; row < gridSize; row++) {
                    for (int col = 0; col < gridSize; col++) {
                        int slotX = gridX + col * (slotSize + slotSpacing);
                        int slotY = gridY + row * (slotSize + slotSpacing);
                        // 格子背景
                        extractor.fill(slotX - 1, slotY - 1, slotX + slotSize + 1, slotY + slotSize + 1, 0xFF3A3A5A);
                        extractor.fill(slotX, slotY, slotX + slotSize, slotY + slotSize, 0xFF1A1A2E);
                        // 物品图标
                        extractor.item(grid[row][col], slotX, slotY);
                        // 鼠标悬停显示物品名称
                        if (mouseX >= slotX && mouseX <= slotX + slotSize &&
                            mouseY >= slotY && mouseY <= slotY + slotSize) {
                            pendingTooltip = grid[row][col].getHoverName().getString();
                        }
                    }
                }
                // 箭头指向输出
                int arrowX = gridX + gridWidth + 4;
                extractor.text(this.font, Component.literal("§6→"), arrowX, gridY + slotSize / 2, 0xFFFFD700);
                // 输出物品
                extractor.item(recipe.output, arrowX + 12, gridY);
            } else if (isMaterialTemplate) {
                maxMaterials = 4;
                // 显示4种材料选项：腐肉、骨头、蜘蛛眼、火药，每种8个+1碎片
                ItemStack[] materials = {
                    new ItemStack(Items.ROTTEN_FLESH, 8),
                    new ItemStack(Items.BONE, 8),
                    new ItemStack(Items.SPIDER_EYE, 8),
                    new ItemStack(Items.GUNPOWDER, 8)
                };
                int iconSpacing = 18;
                for (int i = 0; i < materials.length; i++) {
                    ItemStack material = materials[i];
                    if (materialX + 16 > x + width - 4) break;
                    extractor.item(material, materialX, materialY);
                    String countStr = String.valueOf(material.getCount());
                    int countWidth = this.font.width(countStr);
                    extractor.text(this.font, countStr, materialX + 16 - countWidth, materialY + 8, 0xFFFFFFFF);
                    if (mouseX >= materialX && mouseX <= materialX + 16 &&
                        mouseY >= materialY && mouseY <= materialY + 16) {
                        pendingTooltip = material.getHoverName().getString();
                    }
                    materialX += iconSpacing;
                    if (i < materials.length - 1 && materialX + 4 < x + width - 4) {
                        extractor.text(this.font, Component.literal("§7/"), materialX - 2, materialY + 4, 0xFFAAAAAA);
                    }
                }
                if (materialX + 32 < x + width - 4) {
                    extractor.text(this.font, Component.literal("§7+1"), materialX, materialY + 4, 0xFFAAAAAA);
                    materialX += 14;
                    extractor.item(new ItemStack(CriticalHitsMod.CRITICAL_COIN_SHARD), materialX, materialY);
                    if (mouseX >= materialX && mouseX <= materialX + 16 &&
                        mouseY >= materialY && mouseY <= materialY + 16) {
                        pendingTooltip = "现代化伤害材料碎片";
                    }
                }
            } else {
                maxMaterials = Math.min(recipe.materials.size(), 5);
                int iconSpacing = 20;
                for (int i = 0; i < maxMaterials; i++) {
                    ItemStack material = recipe.materials.get(i);
                    if (materialX + 16 > x + width - 4) break;
                    extractor.item(material, materialX, materialY);
                    if (material.getCount() > 1) {
                        String countStr = String.valueOf(material.getCount());
                        int countWidth = this.font.width(countStr);
                        extractor.text(this.font, countStr, materialX + 16 - countWidth, materialY + 8, 0xFFFFFFFF);
                    }
                    if (mouseX >= materialX && mouseX <= materialX + 16 &&
                        mouseY >= materialY && mouseY <= materialY + 16) {
                        pendingTooltip = material.getHoverName().getString();
                    }
                    materialX += iconSpacing;
                }

                // 箭头符号（普通配方才显示）
                if (!recipe.materials.isEmpty()) {
                    int arrowX = x + 4 + 20 * maxMaterials + 2;
                    if (arrowX + 10 < x + width - 4) {
                        extractor.text(this.font, Component.literal("§6→"), arrowX, y + 34, 0xFFFFD700);
                    }
                }
            }
        }

        // 检测鼠标悬停在输出物品上，显示物品名称
        if (mouseX >= x + 4 && mouseX <= x + 20 &&
            mouseY >= y + 4 && mouseY <= y + 20) {
            pendingTooltip = recipe.output.getHoverName().getString();
        }

        // 检测鼠标悬停在描述区域，显示完整描述
        if (mouseX >= x + 28 && mouseX <= x + width - 4 &&
            mouseY >= y + 16 && mouseY <= y + 28) {
            Component descFull = Component.translatable(recipe.descriptionKey);
            pendingTooltip = descFull.getString();
        }
    }

    /**
     * 渲染多行tooltip，带边框，自动换行，避免被遮挡。
     */
    private void renderMultiLineTooltip(GuiGraphicsExtractor extractor, String text, int mouseX, int mouseY) {
        // 自动换行（每行最多40个字符）
        List<String> lines = new ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();
        int maxLineWidth = 180;

        for (String word : words) {
            if (currentLine.length() == 0) {
                currentLine.append(word);
            } else {
                String testLine = currentLine + " " + word;
                if (this.font.width(testLine) > maxLineWidth) {
                    lines.add(currentLine.toString());
                    currentLine = new StringBuilder(word);
                } else {
                    currentLine = new StringBuilder(testLine);
                }
            }
        }
        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }

        // 计算tooltip尺寸
        int tooltipWidth = 0;
        for (String line : lines) {
            tooltipWidth = Math.max(tooltipWidth, this.font.width(line));
        }
        tooltipWidth += 8;
        int tooltipHeight = lines.size() * 10 + 6;

        // 计算tooltip位置（避免超出屏幕和被遮挡）
        int tooltipX = mouseX + 12;
        int tooltipY = mouseY - tooltipHeight - 12;

        // 如果上方空间不够，显示在下方
        if (tooltipY < this.topPos) {
            tooltipY = mouseY + 12;
        }
        // 如果右侧超出，显示在左侧
        if (tooltipX + tooltipWidth > this.leftPos + GUI_WIDTH) {
            tooltipX = mouseX - tooltipWidth - 12;
        }
        // 确保不超出左边界
        if (tooltipX < this.leftPos) {
            tooltipX = this.leftPos + 4;
        }

        // 渲染tooltip背景和边框
        extractor.fill(tooltipX, tooltipY, tooltipX + tooltipWidth, tooltipY + tooltipHeight, 0xF0100010);
        extractor.fill(tooltipX + 1, tooltipY + 1, tooltipX + tooltipWidth - 1, tooltipY + tooltipHeight - 1, 0xF0100010);
        // 边框
        extractor.fill(tooltipX, tooltipY, tooltipX + tooltipWidth, tooltipY + 1, 0xFF5050FF);
        extractor.fill(tooltipX, tooltipY + tooltipHeight - 1, tooltipX + tooltipWidth, tooltipY + tooltipHeight, 0xFF5050FF);
        extractor.fill(tooltipX, tooltipY, tooltipX + 1, tooltipY + tooltipHeight, 0xFF5050FF);
        extractor.fill(tooltipX + tooltipWidth - 1, tooltipY, tooltipX + tooltipWidth, tooltipY + tooltipHeight, 0xFF5050FF);

        // 渲染文字
        int textY = tooltipY + 4;
        for (String line : lines) {
            extractor.text(this.font, line, tooltipX + 4, textY, 0xFFFFFFFF);
            textY += 10;
        }
    }

    /**
     * 根据最大宽度截断文本，添加省略号。
     */
    private String truncateText(String text, int maxWidth) {
        if (this.font.width(text) <= maxWidth) {
            return text;
        }
        String ellipsis = "...";
        int ellipsisWidth = this.font.width(ellipsis);
        int availableWidth = maxWidth - ellipsisWidth;
        if (availableWidth <= 0) {
            return ellipsis;
        }
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            String test = result.toString() + text.charAt(i);
            if (this.font.width(test) > availableWidth) {
                break;
            }
            result.append(text.charAt(i));
        }
        return result.toString() + ellipsis;
    }

    private void renderScrollBar(GuiGraphicsExtractor extractor) {
        if (selectedCategory >= categories.size()) return;

        RecipeBookData.RecipeCategory category = categories.get(selectedCategory);
        List<RecipeBookData.RecipeEntry> recipes = category.recipes;

        // 计算总高度
        int totalHeight = 0;
        String lastSubCategory = "";
        for (RecipeBookData.RecipeEntry recipe : recipes) {
            if (!recipe.subCategory.equals(lastSubCategory)) {
                totalHeight += 18;
                lastSubCategory = recipe.subCategory;
            }
            int cardHeight = recipe.nameKey.equals("modern_damage_system.recipe.mds_station") ? MDS_STATION_HEIGHT : RECIPE_HEIGHT;
            totalHeight += RECIPE_HEIGHT + 4;
        }

        int contentHeight = GUI_HEIGHT - 30;
        if (totalHeight <= contentHeight) return;

        int scrollBarX = this.leftPos + GUI_WIDTH - SCROLL_BAR_WIDTH - 4;
        int scrollBarY = this.topPos + 22;
        int scrollBarHeight = contentHeight;

        // 滚动条背景
        extractor.fill(scrollBarX, scrollBarY, scrollBarX + SCROLL_BAR_WIDTH, scrollBarY + scrollBarHeight, 0xFF2A2A2A);

        // 滚动条滑块
        int thumbHeight = (int) ((float) contentHeight / totalHeight * scrollBarHeight);
        thumbHeight = Math.max(20, thumbHeight);
        int thumbY = scrollBarY + (int) (scrollOffset / (totalHeight - contentHeight) * (scrollBarHeight - thumbHeight));

        extractor.fill(scrollBarX, thumbY, scrollBarX + SCROLL_BAR_WIDTH, thumbY + thumbHeight, 0xFF8B6914);
    }

    // 鼠标点击处理（由Fabric API的ScreenMouseClickEvent调用）
    public boolean handleMouseClick(double mouseX, double mouseY, int button) {
        CriticalHitsMod.LOGGER.info("[RecipeBook] handleMouseClick called: x={}, y={}, button={}", mouseX, mouseY, button);
        if (button == 0) {
            // 检查分类标签页点击
            int categoryY = this.topPos + 22;
            for (int i = 0; i < categories.size(); i++) {
                int categoryHeight = 22;
                if (mouseX >= this.leftPos + 2 && mouseX <= this.leftPos + CATEGORY_WIDTH &&
                    mouseY >= categoryY && mouseY <= categoryY + categoryHeight) {
                    selectedCategory = i;
                    scrollOffset = 0;
                    CriticalHitsMod.LOGGER.info("[RecipeBook] Selected category: {}", i);
                    return true;
                }
                categoryY += categoryHeight + 2;
            }
        }
        return false;
    }

    // 鼠标点击处理（保留，以防被调用）
    public boolean mouseClicked(double mouseX, double mouseY, double button) {
        return handleMouseClick(mouseX, mouseY, (int) button);
    }

    // 滚轮滚动处理
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        // 检查鼠标是否在配方区域内
        int contentX = this.leftPos + CATEGORY_WIDTH + 8;
        int contentWidth = GUI_WIDTH - CATEGORY_WIDTH - SCROLL_BAR_WIDTH - 16;
        int contentY = this.topPos + 22;
        int contentHeight = GUI_HEIGHT - 30;

        if (mouseX >= contentX && mouseX <= contentX + contentWidth &&
            mouseY >= contentY && mouseY <= contentY + contentHeight) {
            scrollOffset -= verticalAmount * 20;
            return true;
        }
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
