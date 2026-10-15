# MDS (Modern Damage System) 项目上下文

> 此文件用于在不同 AI 助手之间转移项目进度。复制全部内容到新助手即可继续工作。
> 最近更新：2.1.0（附魔上限降至 10 级 + 每级效果翻倍 + 权重/配置/命令 3 个 bug 修复 + 配方书硬核说明分类）｜历史：2.0.7（视觉大改：HUD 双风格可切换 + 合成台 v2 + 图标/材料重做，双端真机验证通过）

## 〇、2.2.5 变更明细（相对 2.1.0）—— 成就系统

> 这一段跨越 2.1.1 ~ 2.2.5，主线是「**成就系统**」，另含等级上限与配置重构的收尾。

### 2.2.0 成就系统（40 个）

| 组成 | 说明 |
|---|---|
| **40 个主成就** | 三档难度（原版 `frame`）：简单 18 / 普通 9 / 困难 13；链式树结构 |
| **40 个手绘图标** | 每个成就一个 16×16 图标；物品 id `criticalhits:adv_icon_<成就id>`；**不进任何创造栏** |
| **26 个隐藏追踪成就** | `track_<附魔id>`，**JSON 里没有 display → 游戏内完全不显示**；玩家一获得该附魔就永久记录，用于统计"收集 10 种 / 26 种"（**不需要自己写玩家存档**，跟着存档走） |
| **森林风成就页背景** | `textures/gui/advancements/backgrounds/mds.png`（16×16 平铺） |
| **4 个触发器类型** | `mds:combat` / `mds:state` / `mds:event` / `mds:all_complete` —— 实为**统一用 `minecraft:impossible` + 代码发放**，无需注册自定义 CriterionTrigger |

**判定架构（重要设计决策）**：所有成就 JSON 的 criteria 统一写 `minecraft:impossible`，
由 `MdsAdvancements` 在满足条件时显式 `PlayerAdvancements#award(...)`。好处：
- 跨端零差异（不用写两套触发器注册）
- 条件全集中在 Java 里，改条件不用碰 JSON
- `mds:state` 类条件由**服务端每 20 tick 扫描**玩家（背包 + 护甲 + 副手 + 配置）

**战斗钩子**（`PlayerEntityAttackMixin` 内 4 处）：单次伤害 / 处决成功 / 溅射命中数 / 四重折磨；
另有 `EntityHurtMixin` 的反伤击杀与"被 MDS 附魔生物击杀"。

**双端 tick**：Fabric `ServerTickEvents.END_SERVER_TICK`，NeoForge `ServerTickEvent.Post`。

**奖励**：按难度发经验 + 材料模版（简单 15 / 普通 40 / 困难 100，终极 200/300）。
> ⚠️ **服务器端默认无效**（`achievement_rewards_enabled = false`），服主用 `/mds rewards true` 开启。
> 这一点**必须写进三个平台的项目介绍**（已写入 `store-listings-2.2.4/`）。

### 2.2.1 ~ 2.2.5 迭代（成就系统的坑与修法）

| 版本 | 内容 |
|---|---|
| 2.2.1 | 「爱读书」第一次尝试修复（在物品 `use()` 里发）→ **失败**，见下方教训 ② |
| 2.2.2 | 「爱读书」改为"**背包里拥有配方书**即发放"，同时把描述从"打开一次"改成"获得一本"；8 个隐藏成就改为**可见 + 描述 ???** |
| 2.2.3 | 秘密成就**名字恢复正常显示**，只保留描述为 `？？？` |
| 2.2.4 | 秘密成就**解锁瞬间在聊天栏揭示真实说明**（`Component.translatable`，客户端按自己语言渲染）；新增 **`/mds rewards <true\|false>`** 指令 |
| 2.2.5 | 补「四重折磨」的**第二条达成路径**（手持武器本身带齐 4 种 debuff 附魔）；新增 **`/mds` 自检**：启动时校验 66 个成就全部加载；清理 `advancement_backup` 残留目录 |

## ⚠️ 2.2.x 踩的两个大坑（务必记住，别再犯）

### ① 成就数据被 `build.gradle` 整体排除（最坑）

两个 `build.gradle` 里都有**前任作者留下的**排除规则：

```gradle
exclude("data/criticalhits/advancement/**")     // fabric 第 88 行 / neoforge 第 93 行
```

后果：**66 个成就 JSON 生成正确、但根本没打进 jar** → 游戏里进度页看不到、任何成就都触发不了。
已删除这两行（同时删了 `advancement_backup` 的排除）。

**教训**：新增任何 `data/` 或 `assets/` 资源后，**必须解压 jar 确认文件真的在里面**。

### ② 验证数据包必须"进世界/服务器"，不能只看主菜单日志

`advancement`、`loot_table` 等**数据包内容是在世界/服务器加载时才解析的**。
第一次验证时我只启动了客户端到主菜单、看日志没报错就以为没问题 → 实际数据根本没打包。
**正确做法**：跑 `:fabric:runServer`（或带 `MDS_SELFTEST=true`），看 `Loaded N advancements` 数字
与 `[MDS 自检] 成就加载 66/66`。

**参考数字**：本版本服务端应打印 `Loaded 1754 advancements`（其中 66 个是本模组的）。

### ③ 「爱读书」这类"纯客户端界面"服务端感知不到

MDS 配方书由客户端 `MouseHandlerMixin` **拦截右键后本地 `setScreen`** 打开，
**客户端不发右键包给服务端** → 物品的 `use()` 在服务端根本不会执行。
凡"打开某个客户端界面"类成就，服务端都无法检测；要么加自定义网络包，要么改成服务端可观察的条件（本版采用后者）。

### ④ 原版没有"解锁前 ???、解锁后真描述"的机制

反编译 `net/minecraft/client/gui/screens/advancements/AdvancementWidget.class` 确认：
类里**没有任何 `???` 字符串**，只有 `DisplayInfo.isHidden()` 与 `AdvancementProgress.isDone()` 两个判断。
即原版只有两种状态：**完全隐藏**（hidden && !done）或**显示 lang 真文字**。
`hidden: true` = 节点彻底消失（**不是**显示 ???）。
所以"???"必须**把 lang 文字直接写成 `？？？`**；
"解锁后看到说明"改用**解锁瞬间发聊天栏消息**实现。

## 〇、2.1.0 变更明细（相对 2.0.9）

| 项 | 内容 |
|---|---|
| **附魔上限 20/15 → 10** | 9 个附魔上限下调为 10 级：暴击、暴击伤害、暴击减免、伤害加成、护甲穿透、伤害减免、护甲防御、额外伤害（20→10），攻击速度（15→10）。其余 17 个（上限 ≤10）不变 |
| **每级效果翻倍（不取整）** | 设计目标：10 级 ≈ 原 20 级。暴击 +9.5%/级（10级 = 基础5% + 95% = **100%**）、暴击伤害 +19.6%（→246%）、暴击减免 +7.4%（→74%，硬顶80%）、伤害加成 +3.9%（→39%）、护甲穿透 +3.7%（→37%）、伤害减免 +1.9%（→19%）、护甲防御 +0.95（→9.5韧性）、额外伤害 +5.9%（→59%）、攻击速度 +11.4%（→114%） |
| **老存档兼容** | 新增 `MAX_ENCHANT_LEVEL = 10`，在 `getEnchantmentLevel()` 这个唯一入口统一钳制 → 老存档里已有的 11~20 级书效果按 10 级算，不会算出超纲数值 |
| **权重逻辑重写** | `syncLootWeight()` 按 5 条规则重写：①未开启掉落的附魔**完全不碰权重**；②开启后按固定比例分配；③**等级数量严格跟随玩家的「最高等级」设置**（去掉旧的"强制至少 5 级"），并夹到附魔真实上限；④**权重 0 = 真正不掉落**（旧代码 `max(1,0)` 反而变成"每级都掉"）；⑤内部 ×100 精度补偿（旧代码总权重偏低时 3%/1% 会被压平成 1，导致 4 级和 5 级一样常见） |
| **配置不再需要重启** | 新增 `registerSaveListener`：游戏内配置保存后立即 `syncToLegacyConfig()` + 落盘（旧代码只在 mod 启动时同步一次，玩家改完必须重启游戏才生效） |
| **`/mds loot setlevel` 修复** | 旧实现只写全局字段 `loot_enchantment_max_level`，但战利品注入读的是每个附魔自己的 `max_level` → 命令实际无效。现在两个都改，并按附魔真实上限自动夹紧 |
| **滑块上限 20 → 10** | 23 个 `loot_max_level_*` 滑块上限改为 10；其中 execute/shield 为 3、splash_damage 为 5（跟随各自附魔真实上限，防止掉落非法等级的书） |
| **文案全量同步** | 附魔书悬停 9 条 + 配方书悬停 9 条 + 暴击台说明 1 条 + 过时 JSON 指引 2 条 + 成就旧等级 2 条 = **23 条键 × 中英**；并修正"伤害减免代码 1% / 文案写 2%"的历史不一致 |
| **配方书新增「说明」分类** | 放在第一个分类（一打开就能看到），6 条短句（中英各 14 个新键）：硬核提示 / 默认掉落上限 / ①打开更高等级掉落 / ②提高出现概率 / 默认不掉落的附魔 / 这是一场硬核挑战 |
| 版本号 | 2.0.9 → **2.1.1** |

> 设计决定（用户确认）：**暴击台消耗曲线不改**（保持 4 本同级书 + 需 5 级以上的硬核设定），高级附魔书的获取途径是**战利品箱 + 玩家自行调高「最高等级」与「权重」**；暴击/护甲穿透**默认不掉落**（保持）。

## 〇、2.0.9 变更明细（相对 2.0.8）

| 项 | 内容 |
|---|---|
| **紫黑格子根因** | 26.2 的物品模型必须同时有 `assets/criticalhits/items/<id>.json`（物品模型定义，含 `{"model":{"type":"minecraft:model","model":"ns:item/<id>"}}`）**和** `models/item/<id>.json`。2.0.8 新建的 `attack_badge`/`defense_badge`/`preset_badge` **只建了后者** → 模型解析失败 → 游戏显示紫黑"缺失模型"格子。已补 3 个 `items/*.json` |
| HUD 花草补齐 | 按设计稿加：**顶部藤蔓 + 叶簇 + 左上/右上花丛**、左边缘双像素藤蔓 + 更多叶簇 + 小白花、底部藤蔓 + 叶 + 草丛、右侧叶片、分割线两端叶片（血战风格改为血溅点）、按钮上方「编辑模式」文字 |
| HUD 英文翻译 | HUD 标题、「切换风格」按钮、「编辑模式」、风格标签（森林/血战）此前**硬编码中文**，切英文不生效。已改为语言键 `modern_damage_system.hud.title_forest/title_blood/style_forest/style_blood/switch_style/edit_mode`，中英均补齐 |
| 版本清理 | 按用户要求删除 2.0.9 之前的全部 `modern-damage-system*` 归档（fabric/neoforge 的 release 目录 + build/libs 共 44 个文件），**仅保留 2.0.9**。MDS API（`mds-api-*`，独立 mod）保留不动 |
| 上架文案 | 新增 `store-listings/` 目录：2.0.9 更新说明（中英）、Modrinth/CurseForge 英文描述、MC百科 中文主条目、截图建议（含三平台规则差异） |
| 教训 | 以后新增**任何物品**，必须同时创建 `items/<id>.json` + `models/item/<id>.json` + `textures/item/<id>.png` 三件套 |

## 〇、2.0.8 变更明细（相对 2.0.7）

| 项 | 内容 |
|---|---|
| 分类图标补全 | 攻击/防御/预设三个标签页原来用**原版物品**（钻石剑/盾/附魔书）当图标，所以"看起来没改"。新增 3 个隐藏图标物品 `attack_badge`/`defense_badge`/`preset_badge`（各配 16×16 贴图 + 模型 JSON + 中英 lang），标签页图标全部换成自制图标。6 个标签页现在全部是 MDS 自制图标 |
| HUD 面板不透明 | `hud_background_alpha` 渲染时**下限设为 205**（用户反馈半透明导致背后木板透出来看不清） |
| HUD 花草常显 | 上一轮把花草/血溅/苦力怕画在 `if (editMode)` 里 → 常态看不到。现在**常态显示**：森林=左边缘藤蔓+3 组叶簇+花+底部藤蔓+花，血战=血溅点+苦力怕 |
| HUD 数据行排版 | 原来是"§c标签: §f值"整行左对齐。现在渲染时拆分：**色标方块 + 左标签 + 右对齐数值**（色标颜色从原 § 颜色码推导），贴近设计稿 |
| HUD 标题 | 标题左侧加金色**暴击星**徽记 |

## 〇、2.0.7 变更明细（相对 2.0.6）

| 项 | 内容 |
|---|---|
| HUD 双风格 | 新增配置 `hud_style`（`"forest"` 森林 / `"blood"` 血战），默认森林。编辑模式下 HUD 面板**旁**出现「切换风格」按钮（面板在右侧时按钮在左，反之在右），**可来回切换**并写入配置。森林：暗绿底 + 金色强调条 + 绿叶/小花；血战：暗红渐变 + 血红标题 + 血滴/血溅 + 苦力怕；标题栏右侧显示当前风格标签 |
| 材料 | 暴击币改用 **V2 血金徽币**（金环 + 血红珐琅 + 交叉剑），碎片同步 V2 风格 |
| 分类图标 | 暴击系列 → 剑 + 右下角暴击爆裂；属性系列 → 黑白肌肉小人；功能物品 → 新合成台方块（靠方块贴图）；攻击/防御/预设仍用原版剑/盾/书 |
| 合成台 GUI v2 | 血战铁框（血红内描边 + 四角血铆钉）+ 血红标题栏 + 左带 2 附魔书/钻石剑/铁斧 + 右带 4 种敌对生物（苦力怕/僵尸/骷髅/蜘蛛）+ 统一 18×18 槽框（旧版完全没有槽框） |
| 合成台槽位坐标 | 输入槽物品坐标 `(33,25)(51,25)(33,43)(51,43)`，输出槽 `(115,35)`；输入井 `(28,19)-(72,63)`、输出金槽 `(104,22)-(142,62)` 与之几何对齐 |
| 方块贴图 | 顶面金环+红宝石、侧面金线+交叉武器牌匾+红宝石、底面暗石铆钉 |
| 修复的贴图 bug | ①`mds_recipe_book.png` 由 32×32 改为 16×16（旧版被缩放发糊）②删除死文件 `item/critical_badge.png`（32×32 无任何模型引用）③删除死文件 `item/recipe_book.png`（与暴击币完全相同且 `recipe_book` 物品未注册；`RecipeBookItem.java` 是死类，暂留） |
| 配方书显示 | MDS 配方书用 `ItemStack` 渲染条目，换贴图后**自动同步**（已确认代码里没有任何硬编码 gui 贴图引用） |
| mod 图标 | **未改动**（按用户要求保留） |


## 〇、2.0.6 变更明细（相对 2.0.5）

| 项 | 内容 |
|---|---|
| 翻译缺失修复 | 补 `modern_damage_system.recipe.subcategory.critical`（配方书"附魔书展示"页首个分组标题，此前中英都缺 → 显示原始 key）与 `modern_damage_system.hud.percent`（中文缺 → HUD 百分比显示原始 key）；双端共用同一份 lang，改 common 即生效 |
| Fabric mod 描述 | `fabric.mod.json` description 精简为 1 段中文 + 1 段英文 + 前置 + 许可证；`contact` 提供 Modrinth / GitHub / Issues / 邮箱（ModMenu 里是可点击按钮） |
| NeoForge mod 描述 | `neoforge.mods.toml` description 精简；新增 `displayURL`（Modrinth）与 `logoFile`，`issueTrackerURL` 改为 GitHub issues；三者都是 NeoForge 模组列表里可点击/可见的字段 |
| NeoForge 附魔书等级数字 | 新增 `RegisterItemDecorationsEvent` + `IItemDecorator` 注册（`MdsClientHooks.decorateEnchantedBook` 改为返回 boolean 以适配 NeoForge 接口），与 Fabric 端同一份实现 |
| 版本号 | 2.0.5 → 2.0.6（2.0.5 已归档并验证，按"不覆盖历史版本"规矩另起版本） |


## 一、项目身份

| 项目 | 信息 |
|------|------|
| 主 mod MODID | `criticalhits` |
| 显示名 | Modern Damage System / 现代化伤害系统 |
| 作者 | Juzipi_AN |
| 邮箱 | ngdgsls151231@outlook.com |
| 主 mod 许可证 | 自定义源代码公开协议（ARR），禁止未经授权移植网易中国版 |
| API 许可证 | MIT |
| Modrinth | https://modrinth.com/mod/modern-damage-system |
| GitHub | https://github.com/JuzipiAN/modern-damage-system |

## 二、版本与路径

### 当前版本（2.2.5）
- Fabric 主 mod：**2.1.1**
- NeoForge 主 mod：**2.1.1**
- MDS API：**1.1.0**（独立仓库，双端各自维护，不并入本仓库）
- Minecraft：**26.2**（Fabric Loader 0.19.3 / Fabric API 0.158.0+26.2 / NeoForge 26.2.0.88）

### 关键路径
```
项目根目录：C:\Users\Administrator\Doubao\chats\2026-09-03\new-chat-2\

多模块项目（当前主力）：
  critical-hits-multiloader/
    ├── common/          # 平台无关代码（含客户端渲染/tooltip 逻辑）
    ├── fabric/          # Fabric 入口 + Fabric 专属客户端胶水
    ├── neoforge/        # NeoForge 入口 + NeoForge 客户端事件
    └── PROJECT_CONTEXT.md

发布目录（2.0.7 已归档，历史版本不覆盖）：
  fabric-release/     modern-damage-system-fabric-26.2-2.1.1-multiloader.jar / -sources.jar
  neoforge-release/   modern-damage-system-neoforge-26.2-2.1.1-multiloader.jar / -sources.jar

旧项目（保留不覆盖）：critical-hits/（Fabric 旧）、critical-hits-neoforge/（NeoForge 旧）

工具：
  JDK 25：C:\Users\Administrator\Doubao\chats\2026-09-03\new-chat\tools\jdk-25.0.4.1+1
  Gradle 9.5.1：C:\Users\Administrator\Doubao\chats\2026-09-03\new-chat\tools\gradle-9.5.1\bin\gradle.bat

游戏实例（PCL2，注意：该目录对非交互进程只读，安装 jar 要手动做）：
  Fabric：  C:\Users\Default\AppData\Roaming\.minecraft\versions\26.2-Fabric 0.19.3\mods
  NeoForge：C:\Users\Default\AppData\Roaming\.minecraft\versions\26.2-NeoForge_26.2.0.88\mods
```

## 三、构建命令

```powershell
$env:JAVA_HOME = "C:\Users\Administrator\Doubao\chats\2026-09-03\new-chat\tools\jdk-25.0.4.1+1"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
cd "C:\Users\Administrator\Doubao\chats\2026-09-03\new-chat-2\critical-hits-multiloader"

# 双端构建（产出 jar + sources jar）
& "C:\Users\Administrator\Doubao\chats\2026-09-03\new-chat\tools\gradle-9.5.1\bin\gradle.bat" :fabric:build :neoforge:build --console=plain
```

构建产物：
- Fabric：`fabric/build/libs/modern-damage-system-fabric-26.2-2.1.1.jar`（+ `-sources.jar`）
- NeoForge：`neoforge/build/libs/modern-damage-system-neoforge-26.2-2.1.1.jar`（+ `-sources.jar`）

版本号统一在 `gradle.properties` 的 `mod_version`；`neoforge.mods.toml` 用 `${version}` 由 processResources 注入，
`fabric.mod.json` 用 `${version}` 由 loom expand 注入。**不要再在 build.gradle 里硬编码版本号**。

### 开发环境启动测试（可选）
```powershell
$env:MDS_SELFTEST="true"; $env:MDS_SELFTEST_STOP="true"
& gradle.bat :fabric:runServer --console=plain          # Fabric 服务端自测
& gradle.bat :neoforge:runServer -PskipAssets --console=plain   # NeoForge（跳过客户端资源下载）
```
注意：
- 跑过一次 `runServer` 后如果有残留 java 进程，会锁住 `fabric/run/world/session.lock` 导致下次启动失败（`另一个程序已锁定文件的一部分`），需要先杀进程。
- `neoforge:downloadAssets` 固定要求 Java 21，已在 `gradle.properties` 里通过
  `org.gradle.java.installations.paths` 指向 Minecraft 自带的 `java-runtime-delta`（21.0.3）。
- maven.neoforged.net 有时连不上（SSL/超时），会导致 `prepareServerRun` 失败；编译本身依赖已缓存，不受影响。

## 四、26.2 API 关键变化（已踩坑，务必遵守）

| 旧 API / 旧写法 | 26.2 正确写法 | 说明 |
|--------|-------------|------|
| `slotClick(...)` 返回 ItemStack | `clicked(int,int,ContainerInput,Player)` 返回 **void** | `ClickType` → `ContainerInput`（PICKUP/QUICK_MOVE/SWAP/CLONE/THROW/QUICK_CRAFT/PICKUP_ALL） |
| `CraftingMenu.slotClick` | **`CraftingMenu` 不再覆写 `clicked`**，它声明在 `AbstractContainerMenu` | 注入必须打到**声明类** `AbstractContainerMenu` + `instanceof CraftingMenu` 守卫（见 `AbstractContainerMenuMixin`） |
| `Minecraft.setScreen` | `setScreenAndShow(Screen)`（**public，不需要反射**） | 已封装在 `MdsClientScreens.showScreen()` |
| `BlockEntityType.Builder` | **26.2 已删除**，直接用公开构造 `new BlockEntityType<>(supplier, Set.of(block))` | 旧反射 `Class.forName("...BlockEntityType$Builder")` 会 ClassNotFoundException |
| `ResourceLocation` / `ResourceKey.location()` | `Identifier` / `ResourceKey.identifier()` | 全局重命名 |
| `Minecraft.getInstance()` 取附魔注册表 | 服务端逻辑要用 `ServerLevel.registryAccess()` 或平台提供的 `platformRegistryAccess` | 专用服务器上没有客户端类，会 NoClassDefFoundError |
| `Component.translatable` 等 | 不变 | |
| NeoForge 静态注册 | **必须在 `RegisterEvent` 里创建并注册**（`new Item()`/`new Block()` 会 `createIntrusiveHolder`） | 否则 `IllegalStateException: Registry is already frozen` |
| NeoForge 自动发现 mixin | **不会自动发现**，必须在 `neoforge.mods.toml` 写 `[[mixins]] config=...` | 缺了不报错但所有 mixin 静默失效 |

## 五、当前进度（2.2.5）

### 已完成
1. ✅ 多模块（common + fabric + neoforge），双端编译通过
2. ✅ **Fabric 端可正常进游戏**（真实客户端验证：mod 列表出现 criticalhits 2.0.5 → OpenGL → Sound engine started）
3. ✅ **NeoForge 端可正常进游戏**（真实客户端验证同上；修复了注册表冻结崩溃）
4. ✅ **材料复制 bug 根治**（第六轮，找到真正根因，见第六节）
5. ✅ 自动化回归自测 `/mds selftest`（10 项断言，Fabric 服务端 10/10 通过）
6. ✅ NeoForge 端补 `[[mixins]]`，mixin 配置按平台拆分（common / common-client / fabric-client）
7. ✅ **NeoForge 客户端功能补齐**：界面注册、tooltip、HUD 渲染层、按键绑定、鼠标事件、配方书右键
8. ✅ 客户端逻辑（HUD 渲染 / tooltip / HudEditor）抽到 common，双端复用同一份
9. ✅ 双端版本号统一 2.0.6，产出 jar + sources jar 并归档（不覆盖历史）
10. ✅ 2.0.6：补齐 2 个缺失翻译键、双端 mod 描述精简 + 可点击链接、NeoForge 附魔书等级数字装饰
11. ✅ 2.0.7：HUD 森林/血战双风格（编辑模式可来回切换）+ 合成台 GUI v2（血战框/槽框/书·武器·怪物）+ 暴击币 V2 + 分类图标 v2 + 方块贴图 + 修复 3 个贴图 bug
12. ✅ 2.1.0：9 个附魔上限降至 10 级 + 每级效果翻倍（10级≈原20级）+ 老存档等级钳制 + 权重逻辑重写 + 配置保存回调 + `/mds loot setlevel` 修复 + 滑块上限调整 + 23 条文案×中英 + 配方书「说明」分类

### 成就系统（2.2.0 ~ 2.2.5）—— 已完成

- 40 个主成就（三档难度、链式树、40 个手绘图标、森林背景）
- 26 个隐藏追踪成就（"曾经获得过"的持久记录）
- 8 个秘密成就：名字正常 + 描述 `？？？`，解锁时聊天栏揭示真实说明
- 战斗钩子 4 处（`PlayerEntityAttackMixin`）+ 反伤/被击杀（`EntityHurtMixin`）
- 双端 20 tick 扫描 + 双端 tick 注册
- 奖励系统（默认关）+ `/mds rewards` 指令 + 启动自检（66/66）
- 上架文案包：`store-listings-2.2.4/`（7 个文件，含三平台正文与截图 alt 文本）

### 待完成 / 已知问题
1. ⚠️ **2.0.7 的视觉改动只做了"能进游戏"验证**：HUD 森林/血战、切换风格按钮、合成台 v2 界面都需要玩家在游戏里实际打开确认（自动化只能验证不崩）
2. ⚠️ **复制 bug 只做了自动化验证，还需要你在游戏里实点一次确认手感**（左键/右键/Shift/数字键/Q/拖拽）
2. ⚠️ 用户实例的 jar 需要手动替换（`C:\Users\Default\...` 对非交互进程只读）
3. ⚠️ Configured 在 Fabric 端会报 `ClassNotFoundException: configured.impl.jei.JeiConfigProvider`（Configured 自身问题，与 MDS 无关）
4. ⚠️ 附魔书右下角等级数字：用户已放弃，仅 Fabric 侧保留原调用，NeoForge 未移植
5. ⚠️ 消耗按 Item 匹配：冰霜/剧毒配方里的药水会被"任一瓶药水"满足（可能消耗错的那瓶）
6. ⚠️ 构建依赖仍有个别本机绝对路径（neoforge 的 mds-api jar、fabric 的 cloth-config jar）
7. ⚠️ 没有 git 仓库，建议尽快 `git init` 保护源码
8. ⚠️ NeoForge 编译用 26.2.0.85，运行时是 26.2.0.88（正常兼容，如需对齐要联网拉取 .88 产物）

## 六、材料复制 bug 修复历史（重要！避免重走弯路）

**问题**：工作台合成附魔书时，多放材料会导致材料复制（放 6 钻石合成需 2 钻石的配方，钻石不减反增）

**根因（第六轮才定位）**：附魔书不是原版配方产物，但结果槽用的是原版 `ResultSlot`。
原版 `ResultSlot.onTake` 逻辑：
```java
replacement = getRemainingItems(input, level);   // 没有匹配原版配方 → 回退 copyAllInputItems(input)（整格复制！）
for each 格子:
    if (!itemStack.isEmpty()) { craftSlots.removeItem(slot, 1); itemStack = getItem(slot); }   // 6 → 5
    if (!replacement.isEmpty()) { replacement.grow(itemStack.getCount()); setItem(slot, replacement); }  // 6.grow(5) = 11
```
→ 6 钻石变 11 钻石。**只要让原版 onTake 碰到这本附魔书，材料必翻倍。**

**六轮修复历程**：
1. ❌ Mixin 中加 static 字段 → InvalidMixinException
2. ❌ 迁移到普通类 CraftingRecipeState → 仍复制
3. ❌ ResultSlotMixin + ci.cancel() → 更糟糕
4. ❌ 从结果物品实时计算消耗 + ci.cancel() → 64 个材料直接翻倍
5. ⚠️ `CraftingMenu.clicked` HEAD 拦截 + 手动给物品 + 精确消耗 → **只覆盖了左键和 Shift**，右键/Q/数字键仍会走原版 onTake 复制
6. ✅ **当前方案**：
   - `@Mixin(AbstractContainerMenu.class)` + `instanceof CraftingMenu` 守卫，注入 `clicked` HEAD，**cancellable**
   - 只要点的是我们的附魔书结果槽 → **一律 `ci.cancel()`**，原版逻辑完全不执行
   - 自己实现 PICKUP(左右键)/QUICK_MOVE/SWAP/THROW/PICKUP_ALL/CLONE 的分发
   - 消耗用 `Slot.set()`（会触发 `slotsChanged`）而不是直接 `shrink()`，避免客户端不同步
   - 消耗完显式清空结果槽，再 `refreshResult()` 按剩余材料重算（6 钻石扣 2 剩 4 → 结果槽重新出现附魔书）
   - 材料不足时什么都不给（防数据不同步"白给书"）

**代码位置**：
- `common/.../mixin/AbstractContainerMenuMixin.java`（点击拦截，核心修复）
- `common/.../mixin/CraftingMenuMixin.java`（`slotChangedCraftingGrid` TAIL 注入结果槽 + 同步客户端）
- `common/.../CraftingRecipeState.java`（配方表 / 识别 / 精确消耗 / 结果构建，纯逻辑可复用）
- `common/.../MdsCraftingSelfTest.java`（自测）

## 七、自测（回归验证）

游戏内或服务端控制台执行：`/mds selftest`

覆盖 10 项断言：
1. 结果槽注入（6 钻石+书+币 → 结果槽出现 criticalhits:critical_damage 附魔书）
2. 左键取（6 钻石 → 剩 4，书和币各扣 1）
3. 左键取后结果槽清空
4. 右键取（旧实现会复制的路径）
5. Shift 快速移动
6. 数字键交换（旧实现会复制的路径）
7. Q 丢弃（旧实现会复制的路径）
8. 拖拽（不应消耗材料）
9. 材料不足不给书
10. 合成后结果槽刷新（剩 4 钻石+1 书+1 币 → 结果槽重新出现附魔书）

自动化：`MDS_SELFTEST=true`（服务器启动后自动跑）＋ `MDS_SELFTEST_STOP=true`（跑完自动关服）。

## 八、项目结构说明

### common 模块（平台无关）
- 核心逻辑：附魔常量、合成、怪物适配、战利品注入、配置
- 注册清单：`CriticalHitsMod.registerAll(Registrar)` —— **对象创建全部走幂等工厂**（`coinItem()`/`stationBlock()`/`vulnerabilityEffect()`/`potion(type, level)`…），
  由平台决定创建时机（Fabric: onInitialize 直接注册；NeoForge: RegisterEvent）
- 平台 helper：`platformIsModLoaded`、`platformRegistryAccess`
- 客户端通用：`MdsClientHooks`（HUD 渲染 + tooltip 处理）、`MdsClientScreens`（setScreenAndShow 封装）、`client/HudEditor`（HUD 编辑逻辑，按键由平台注册）
- Mixin：`criticalhits.mixins.json`（双端通用）、`criticalhits.client.mixins.json`（双端通用客户端）

### fabric 模块
- `CriticalHitsModFabric`：ModInitializer（注册对象 + 事件 + 自测钩子 + platformRegistryAccess）
- `CriticalHitsClient`：Fabric 客户端胶水（界面注册反射、Fabric 按键 API、tooltip/装饰事件转发）
- `mixin/GuiMixin`（HUD 渲染注入）、`mixin/client/MouseHandlerMixin`（鼠标事件 + 配方书右键）
- `criticalhits.fabric.client.mixins.json`（Fabric 专属客户端 mixin）

### neoforge 模块
- `CriticalHitsModNeoForge`：@Mod 入口（RegisterEvent 注册、创造标签、战利品、命令、自测钩子、platformRegistryAccess）
- `CriticalHitsModNeoForgeClient`：@Mod(dist=CLIENT) 客户端入口（界面注册、按键、HUD 层、tooltip、鼠标事件）
- `neoforge.mods.toml`：`loaderVersion="[11,)"` + **`[[mixins]]` 两段（必须保留）**
- build.gradle 只排除 Fabric 专属的 `CriticalHitsClient.java`

## 九、用户偏好与约束

1. **历史版本绝对不能覆盖**，文件名必须带 MC 版本号 + 加载器标识
2. **每次更新双平台同步**，均含源码 jar（-sources.jar）
3. **用户自行更新网页端项目描述**（CurseForge / Modrinth / MC百科 / GitHub），只需告知配方改动明细
4. **MDS API 保持双端独立更新**，不做双加载器，也不并入主 mod 仓库
5. **按需确认执行模式**：删除/覆盖/发布等有风险操作需用户确认
6. **附魔书右下角等级数字**：用户已放弃，不要再做
7. 用户机器上的游戏实例在 `C:\Users\Default\...`，命令行走不了写入，安装 jar 需用户手动操作

## 十、下一步建议

1. **先自己进游戏实测一次合成**（左键/右键/Shift/数字键/Q 各试一遍，看材料是否只减不增）
2. 确认无误后双端同步发布 2.0.5（jar 已在 `fabric-release/`、`neoforge-release/`）
3. 建议 `git init` 并把 2.0.5 作为第一个提交，之后每轮改动单独提交
4. 后续可做：把 26 个合成配方改成**真正的 CraftingRecipe**（自定义 RecipeSerializer + 数据包 JSON），
   彻底摆脱对原版 ResultSlot 的依赖，顺带获得配方书/JEI 兼容
5. 清理技术债：构建依赖的本机绝对路径、NeoForge 版本对齐到 26.2.0.88
