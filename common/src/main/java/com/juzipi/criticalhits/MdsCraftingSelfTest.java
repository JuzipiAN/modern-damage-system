package com.juzipi.criticalhits;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.UUID;

/**
 * 工作台合成附魔书的自测（回归测试）。
 *
 * 目的：把"材料复制 bug"变成一条可复现、可自动跑的断言，避免"改完进游戏才发现没修上"。
 *
 * 触发方式：
 * 1. 游戏内 / 服务端控制台执行 {@code /mds selftest}
 * 2. 启动 JVM 时加 {@code -Dmds.selftest=true}，服务器启动完成后自动执行
 *
 * 覆盖点：
 * A. 结果槽注入（CraftingMenuMixin）
 * B. 左键 / 右键 / Shift / 数字键 / Q 丢弃 / 拖拽 —— 每种点击都必须精确扣材料且不复制
 * C. 材料不足时不得白给附魔书（数据不同步保护）
 * D. 合成后剩余材料仍满足配方时，结果槽要自动刷新
 */
public final class MdsCraftingSelfTest {

    public static final String PROPERTY = "mds.selftest";
    /** 环境变量开关（便于开发环境启动测试，Gradle 会把它传给派生出的游戏进程）。 */
    public static final String ENV_VAR = "MDS_SELFTEST";

    private static final String TEST_ENCHANT = "criticalhits:critical_damage"; // 2 钻石

    private static int passed;
    private static int failed;

    private MdsCraftingSelfTest() {
    }

    /** 服务器启动后自动执行（仅在 -Dmds.selftest=true 或环境变量 MDS_SELFTEST=true 时）。 */
    public static void autoRun(MinecraftServer server) {
        boolean enabled = System.getProperty(PROPERTY) != null
            || "true".equalsIgnoreCase(System.getenv(ENV_VAR));
        if (!enabled) return;
        CriticalHitsMod.LOGGER.info("[MDS SelfTest] 检测到自测开关，开始自动自测");
        try {
            run(server);
        } catch (Throwable t) {
            CriticalHitsMod.LOGGER.error("[MDS SelfTest] 自测执行异常", t);
        }
        // 自动化跑测试时（MDS_SELFTEST_STOP=true）自测完就关服，避免测试进程常驻
        if ("true".equalsIgnoreCase(System.getenv("MDS_SELFTEST_STOP"))) {
            CriticalHitsMod.LOGGER.info("[MDS SelfTest] 自测结束，关闭服务器");
            server.halt(false);
        }
    }

    public static boolean run(MinecraftServer server) {
        passed = 0;
        failed = 0;
        ServerLevel level = server.overworld();
        ServerPlayer player = createTestPlayer(server, level);

        CriticalHitsMod.LOGGER.info("[MDS SelfTest] ================= 开始自测 =================");

        testResultInjection(player, level);
        testPickupLeft(player, level);
        testPickupRight(player, level);
        testQuickMove(player, level);
        testSwap(player, level);
        testThrow(player, level);
        testQuickCraft(player, level);
        testInsufficientMaterials(player, level);
        testResultRefresh(player, level);

        boolean ok = failed == 0;
        CriticalHitsMod.LOGGER.info("[MDS SelfTest] ===== 结果: 通过 {} / 失败 {} => {} =====",
            passed, failed, ok ? "SELFTEST PASS" : "SELFTEST FAIL");
        return ok;
    }

    // ==================== 用例 ====================

    /** A. 结果槽注入 */
    private static void testResultInjection(ServerPlayer player, ServerLevel level) {
        CraftingMenu menu = newMenu(player, level, true);
        fillGrid(menu, new ItemStack(Items.DIAMOND, 6), new ItemStack(Items.BOOK),
            new ItemStack(CriticalHitsMod.CRITICAL_COIN));

        ItemStack result = menu.getSlot(0).getItem();
        check("结果槽注入(6钻石+书+币)", TEST_ENCHANT.equals(CraftingRecipeState.getMdsEnchantmentId(result)),
            "结果槽=" + describe(result));
    }

    /** B1. 左键取 */
    private static void testPickupLeft(ServerPlayer player, ServerLevel level) {
        CraftingMenu menu = newMenu(player, level, false);
        fillGrid(menu, new ItemStack(Items.DIAMOND, 6), new ItemStack(Items.BOOK),
            new ItemStack(CriticalHitsMod.CRITICAL_COIN));
        setFakeResult(menu, level);

        menu.clicked(0, 0, ContainerInput.PICKUP, player);

        boolean bookGiven = TEST_ENCHANT.equals(CraftingRecipeState.getMdsEnchantmentId(menu.getCarried()));
        int left = count(menu, Items.DIAMOND);
        check("左键取(6钻石→应剩4)", bookGiven && left == 4 && count(menu, Items.BOOK) == 0,
            "光标=" + describe(menu.getCarried()) + " 钻石=" + left + " 书=" + count(menu, Items.BOOK));
        check("左键取-结果槽已清空", menu.getSlot(0).getItem().isEmpty(), "结果槽=" + describe(menu.getSlot(0).getItem()));
    }

    /** B2. 右键取（旧实现必复制） */
    private static void testPickupRight(ServerPlayer player, ServerLevel level) {
        CraftingMenu menu = newMenu(player, level, false);
        fillGrid(menu, new ItemStack(Items.DIAMOND, 6), new ItemStack(Items.BOOK),
            new ItemStack(CriticalHitsMod.CRITICAL_COIN));
        setFakeResult(menu, level);

        menu.clicked(0, 1, ContainerInput.PICKUP, player);

        boolean bookGiven = TEST_ENCHANT.equals(CraftingRecipeState.getMdsEnchantmentId(menu.getCarried()));
        int left = count(menu, Items.DIAMOND);
        check("右键取(6钻石→应剩4)", bookGiven && left == 4,
            "光标=" + describe(menu.getCarried()) + " 钻石=" + left);
    }

    /** B3. Shift 快速移动 */
    private static void testQuickMove(ServerPlayer player, ServerLevel level) {
        CraftingMenu menu = newMenu(player, level, false);
        fillGrid(menu, new ItemStack(Items.DIAMOND, 6), new ItemStack(Items.BOOK),
            new ItemStack(CriticalHitsMod.CRITICAL_COIN));
        setFakeResult(menu, level);

        menu.clicked(0, 0, ContainerInput.QUICK_MOVE, player);

        boolean inInventory = inventoryHas(player, TEST_ENCHANT);
        int left = count(menu, Items.DIAMOND);
        check("Shift取(6钻石→应剩4)", inInventory && left == 4,
            "背包有书=" + inInventory + " 钻石=" + left);
    }

    /** B4. 数字键交换（旧实现必复制） */
    private static void testSwap(ServerPlayer player, ServerLevel level) {
        CraftingMenu menu = newMenu(player, level, false);
        fillGrid(menu, new ItemStack(Items.DIAMOND, 6), new ItemStack(Items.BOOK),
            new ItemStack(CriticalHitsMod.CRITICAL_COIN));
        setFakeResult(menu, level);

        menu.clicked(0, 0, ContainerInput.SWAP, player);

        boolean inHotbar = TEST_ENCHANT.equals(
            CraftingRecipeState.getMdsEnchantmentId(player.getInventory().getItem(0)));
        int left = count(menu, Items.DIAMOND);
        check("数字键交换(6钻石→应剩4)", inHotbar && left == 4,
            "快捷栏0=" + describe(player.getInventory().getItem(0)) + " 钻石=" + left);
    }

    /** B5. Q 丢弃（旧实现必复制） */
    private static void testThrow(ServerPlayer player, ServerLevel level) {
        CraftingMenu menu = newMenu(player, level, false);
        fillGrid(menu, new ItemStack(Items.DIAMOND, 6), new ItemStack(Items.BOOK),
            new ItemStack(CriticalHitsMod.CRITICAL_COIN));
        setFakeResult(menu, level);

        menu.clicked(0, 0, ContainerInput.THROW, player);

        int left = count(menu, Items.DIAMOND);
        check("Q丢弃(6钻石→应剩4)", left == 4 && menu.getSlot(0).getItem().isEmpty(),
            "钻石=" + left + " 结果槽=" + describe(menu.getSlot(0).getItem()));
    }

    /** B6. 拖拽（QUICK_CRAFT）：不消耗、不复制、结果槽保持 */
    private static void testQuickCraft(ServerPlayer player, ServerLevel level) {
        CraftingMenu menu = newMenu(player, level, false);
        fillGrid(menu, new ItemStack(Items.DIAMOND, 6), new ItemStack(Items.BOOK),
            new ItemStack(CriticalHitsMod.CRITICAL_COIN));
        setFakeResult(menu, level);

        menu.clicked(0, 0, ContainerInput.QUICK_CRAFT, player);

        int left = count(menu, Items.DIAMOND);
        check("拖拽(不应消耗材料)", left == 6 && TEST_ENCHANT.equals(
            CraftingRecipeState.getMdsEnchantmentId(menu.getSlot(0).getItem())),
            "钻石=" + left + " 结果槽=" + describe(menu.getSlot(0).getItem()));
    }

    /** C. 材料不足时不得白给（数据不同步保护） */
    private static void testInsufficientMaterials(ServerPlayer player, ServerLevel level) {
        CraftingMenu menu = newMenu(player, level, false);
        fillGrid(menu, new ItemStack(Items.DIAMOND, 1), new ItemStack(Items.BOOK),
            new ItemStack(CriticalHitsMod.CRITICAL_COIN));
        setFakeResult(menu, level);

        menu.clicked(0, 0, ContainerInput.PICKUP, player);

        int left = count(menu, Items.DIAMOND);
        check("材料不足不给书", menu.getCarried().isEmpty() && left == 1,
            "光标=" + describe(menu.getCarried()) + " 钻石=" + left);
    }

    /** D. 合成后剩余材料仍满足配方 → 结果槽自动刷新 */
    private static void testResultRefresh(ServerPlayer player, ServerLevel level) {
        CraftingMenu menu = newMenu(player, level, true);
        // 2 份材料：合成一次后仍剩 1 书 1 币 4 钻石 → 结果槽应重新出现附魔书
        fillGrid(menu, new ItemStack(Items.DIAMOND, 6), new ItemStack(Items.BOOK, 2),
            new ItemStack(CriticalHitsMod.CRITICAL_COIN, 2));

        boolean injectedFirst = TEST_ENCHANT.equals(CraftingRecipeState.getMdsEnchantmentId(menu.getSlot(0).getItem()));

        // 真实数据包路径：合成格变化会触发原版 slotChangedCraftingGrid 与我们的注入
        safe(() -> menu.clicked(0, 0, ContainerInput.PICKUP, player));

        ItemStack result = menu.getSlot(0).getItem();
        int diamonds = count(menu, Items.DIAMOND);
        int books = count(menu, Items.BOOK);
        check("合成后结果槽刷新(剩4钻石+1书+1币仍可再合成)",
            injectedFirst && diamonds == 4 && books == 1
                && TEST_ENCHANT.equals(CraftingRecipeState.getMdsEnchantmentId(result)),
            "首次注入=" + injectedFirst + " 钻石=" + diamonds + " 书=" + books + " 结果槽=" + describe(result));
    }

    // ==================== 工具 ====================

    /**
     * 构造一个假玩家。
     *
     * 注意：原版 {@code CraftingMenu.slotChangedCraftingGrid} 结尾会往
     * {@code player.connection} 发包。如果 connection 为 null 会直接 NPE，
     * 导致我们挂在方法尾部的注入（TAIL）根本不会执行。这里挂一个"未建立"的
     * Connection —— {@code Connection.send} 在未连接时只是把包排队，不会 NPE，
     * 于是自测可以走完真实的服务端流程。
     */
    private static ServerPlayer createTestPlayer(MinecraftServer server, ServerLevel level) {
        GameProfile profile = new GameProfile(UUID.nameUUIDFromBytes("MDS_SelfTest".getBytes()), "MDS_SelfTest");
        ServerPlayer player = new ServerPlayer(server, level, profile, ClientInformation.createDefault());
        new ServerGamePacketListenerImpl(server, new Connection(PacketFlow.SERVERBOUND), player,
            CommonListenerCookie.createInitial(profile, false));
        return player;
    }

    /**
     * 新建工作台菜单。
     *
     * @param liveAccess true 时使用真实 ContainerLevelAccess —— 合成格变化会触发原版
     *                   {@code slotChangedCraftingGrid} 与我们的注入（贴近真实游戏）；
     *                   false 时使用 NULL 访问，合成格变化不会重算结果槽，
     *                   便于对点击逻辑做确定性断言。
     */
    private static CraftingMenu newMenu(ServerPlayer player, ServerLevel level, boolean liveAccess) {
        CraftingMenu menu = liveAccess
            ? new CraftingMenu(0, player.getInventory(), ContainerLevelAccess.create(level, BlockPos.ZERO))
            : new CraftingMenu(0, player.getInventory());
        player.containerMenu = menu;
        player.getInventory().clearContent();
        menu.setCarried(ItemStack.EMPTY);
        safe(() -> {
            for (int i = 1; i <= 9; i++) {
                menu.getSlot(i).set(ItemStack.EMPTY);
            }
        });
        // 结果槽不是 CraftingContainer 驱动，直接写不会触发 NPE
        menu.getSlot(0).set(ItemStack.EMPTY);
        return menu;
    }

    private static void fillGrid(CraftingMenu menu, ItemStack... items) {
        safe(() -> {
            for (int i = 0; i < items.length && i < 9; i++) {
                menu.getSlot(i + 1).set(items[i]);
            }
        });
    }

    /**
     * 假玩家没有网络连接，原版 {@code CraftingMenu.slotChangedCraftingGrid} 结尾会
     * {@code serverPlayer.connection.send(...)} 抛 NPE。该 NPE 发生在结果槽与 remote slot
     * 写入之后，属自测环境的预期行为，因此吞掉并继续断言。
     */
    private static void safe(Runnable action) {
        try {
            action.run();
        } catch (NullPointerException ignored) {
            // 预期：假玩家无连接
        }
    }

    /** 手动模拟"服务端把结果放进结果槽"（不触发原版重算）。 */
    private static void setFakeResult(CraftingMenu menu, ServerLevel level) {
        ItemStack result = CraftingRecipeState.buildResult(TEST_ENCHANT, level.registryAccess());
        menu.getSlot(0).set(result);
    }

    private static int count(CraftingMenu menu, Item item) {
        return CraftingRecipeState.countGridItems(menu).getOrDefault(item, 0);
    }

    private static boolean inventoryHas(ServerPlayer player, String enchantmentId) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (enchantmentId.equals(CraftingRecipeState.getMdsEnchantmentId(player.getInventory().getItem(i)))) {
                return true;
            }
        }
        return false;
    }

    private static String describe(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return "空";
        String ench = CraftingRecipeState.getMdsEnchantmentId(stack);
        return stack.getItem() + "x" + stack.getCount() + (ench == null ? "" : "[" + ench + "]");
    }

    private static void check(String name, boolean ok, String detail) {
        if (ok) {
            passed++;
            CriticalHitsMod.LOGGER.info("[MDS SelfTest] PASS - {} | {}", name, detail);
        } else {
            failed++;
            CriticalHitsMod.LOGGER.error("[MDS SelfTest] FAIL - {} | {}", name, detail);
        }
    }

    /** 供 /mds selftest 使用：返回可读结果文本。 */
    public static String runAndSummarize(MinecraftServer server) {
        boolean ok = run(server);
        return "§" + (ok ? "a自测通过" : "c自测失败") + " §7(通过 " + passed + " / 失败 " + failed + ")";
    }
}
