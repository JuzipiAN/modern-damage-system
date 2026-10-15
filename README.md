# Modern Damage System (MDS)

**26 damage enchantments · a crafting station · a combat HUD · a deep config · 40 achievements**

A multi-loader Minecraft mod for **26.2** targeting **Fabric** and **NeoForge**.

[中文说明见下方](#中文说明)

---

## Features

* **26 custom damage enchantments** across 6 creative tabs — Critical, Attack, Defense, Attribute,
  Curses and Preset. Maximum level is **10**, with deliberately non-round per-level values
  (Critical Rate +9.5%/level → exactly 100% at level 10).
* **MDS Crafting Station** — combine 4 same-level books into 1 book of the next level.
  Costs material templates; higher tiers require level 5+ source books.
* **Combat HUD** — two switchable styles (🌲 Forest / 🩸 Blood), draggable, scalable,
  per-attribute visibility, live stat readout.
* **Config screen (Cloth Config)** — per-enchantment max level, loot weight and drop toggles,
  plus a **Global Weight Control** master slider with a live multiplier readout.
* **40 achievements** with hand-drawn icons, including 8 secret ones whose descriptions read `？？？`.
* **Custom mob effects** — Vulnerability and Armor Break.

## Important balance facts

* Loot chests only drop books **up to level 5 by default**. Levels 6–10 require raising both the
  **loot max level** and the **loot weight**, or upgrading step by step at the station.
* **Critical Rate and Armor Penetration do not drop from loot by default** — enable them in the config.
* **Achievement rewards are OFF by default on servers.** Enable with `/mds rewards true`.
* The in-game recipe book's **"⚠ Hardcore Notes"** category explains all of the above.

## Requirements

| | |
|---|---|
| Minecraft | 26.2 |
| Fabric | Fabric Loader 0.19.3+ **and Fabric API 0.158.0+26.2** |
| NeoForge | NeoForge 26.2.0.88+ |
| Java | 25 |

## Building from source

This is a multi-loader project: `common` holds platform-independent code and is compiled into the
two loader modules (it has no Minecraft dependency of its own, so it **cannot** be compiled standalone).

```bash
# Build both loaders (produces jar + sources jar for each)
./gradlew :fabric:build :neoforge:build

# Optional: run a dev client / server
./gradlew :fabric:runClient
./gradlew :neoforge:runClient
./gradlew :fabric:runServer
```

Outputs land in `fabric/build/libs/` and `neoforge/build/libs/`.

### Development self-test

Start a dev server with `-Dmds.selftest=true` (or the `MDS_SELFTEST=true` environment variable) to
run the crafting regression test and the achievement loader check. A healthy run logs:

```
[MDS 自检] 成就加载 66/66
```

## Project structure

```
common/     platform-independent code (enchantments, UI, config, achievements, mixins)
fabric/     Fabric entrypoint + Fabric-specific mixins
neoforge/   NeoForge entrypoint + NeoForge-specific mixins
```

## Commands

`/mds help`, `/mds hud ...`, `/mds lootmaxlevel ...`, `/mds lootmaxcount ...`,
`/mds rewards <true|false>`, `/mds reload`, `/mds selftest`

## License

**All Rights Reserved.** You may include this mod in modpacks (with credit).
You may **not** reupload, redistribute or claim the mod files as your own.
See [LICENSE](LICENSE).

---

## 中文说明

**26 个伤害附魔 · 合成台 · 战斗面板 · 深度配置 · 40 个成就**，适配 Minecraft **26.2**，支持 **Fabric 与 NeoForge**。

### 主要功能

- **26 个附魔**，分 6 个创造栏（暴击 / 攻击 / 防御 / 属性 / 诅咒 / 预设），最高 **10 级**，
  每级收益带小数浮动（暴击率每级 +9.5%，10 级正好 100%）
- **MDS 合成台**：4 本同附魔同等级的书 → 1 本高一级的书；消耗材料模版；5 级以上才能继续合
- **战斗面板**：🌲 森林 / 🩸 血战两种风格，编辑模式里可一键切换；可拖动、可缩放、可逐条隐藏
- **配置界面**：每个附魔的最高等级 / 战利品权重 / 是否掉落；**全局权重总控**拖动实时显示倍率
- **40 个成就**，含 8 个秘密成就（描述显示 `？？？`，达成时聊天栏揭示真实说明）
- **自定义状态效果**：易伤、破甲

### 四个关键事实

1. **战利品箱默认只掉 5 级及以下**的书；6~10 级要同时调「最高等级」与「权重」，或用合成台合成
2. **暴击率 / 护甲穿透默认不从战利品掉落**，需在配置里开启
3. **成就奖励服务器端默认无效**，服主用 `/mds rewards true` 开启
4. 配方书的 **「⚠ 硬核说明」** 分类里讲清了上面这些

### 前置

| | |
|---|---|
| 游戏 | Minecraft 26.2 |
| Fabric | Fabric Loader 0.19.3+ 与 **Fabric API 0.158.0+26.2** |
| NeoForge | NeoForge 26.2.0.88+ |
| Java | 25 |

### 构建

```bash
./gradlew :fabric:build :neoforge:build
```

（`common` 模块没有 Minecraft 依赖，**无法单独编译**，必须通过两个加载器模块一起构建。）

### 授权

**保留所有权利（ARR）**：允许放入整合包（请注明出处），**不允许**二次上传或重新分发模组文件。
