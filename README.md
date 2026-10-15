# Modern Damage System

A combat overhaul for Minecraft 26.2, built for both Fabric and NeoForge.

MDS adds 26 damage enchantments, a station that upgrades enchantment books, a combat HUD you can
rearrange, an in-game config for loot and scaling, and 40 achievements.

## Enchantments

26 enchantments across six creative tabs.

- **Critical** — Critical Rate, Critical Damage, Critical Resistance, Percentage Damage
- **Attack** — Armor Penetration, Life Steal, Vulnerability, Bonus Damage, True Damage, Splash Damage, Execute
- **Defense** — Damage Reduction, Armor Defense, Max Health, Thorns Reflect, Shield, Knockback Resistance
- **Attribute** — Attack Speed, Movement Speed, Attack Range
- **Curses** — Attack Speed Curse, Movement Speed Curse, Attack Range Curse
- **Preset** — ready-made books for creative mode and testing

Every enchantment stops at level 10. The per-level gains are not round numbers, so a fully maxed build
lands on clean totals:

| Enchantment | Per level | At level 10 |
|---|---|---|
| Critical Rate | +9.5% | 100% crit chance |
| Critical Damage | +19.6% | 246% |
| Critical Resistance | +7.4% | 74%, capped at 80% |
| Percentage Damage | +3.9% | 39% |
| Armor Penetration | +3.7% | 37% |
| Damage Reduction | +1.9% | 19% |
| Armor Defense | +0.95 | 9.5 toughness |
| Bonus Damage | +5.9% | 59% |
| Attack Speed | +11.4% | 114% |

## Crafting Station

Four books of the same enchantment and level combine into one book of the next level. Each craft costs
material templates, and the books you feed in have to be level 5 or higher. Reaching level 10 takes a while.

## Combat HUD

The HUD comes in two styles, Forest and Blood. In edit mode you can drag it anywhere, scroll to resize it,
hide the stats you don't care about, and swap styles with the button beside the panel.

## Config

The config screen handles an enchantment's max level, its loot weight, and whether it drops at all.
Global Weight Control scales every weight at once and shows the resulting multiplier while you drag it.

## Loot, and why high-level books are rare

By default, loot chests only drop books up to level 5. Levels 6 to 10 come from raising both the max level
and the loot weight in the config, or from upgrading books one step at a time at the station.

Critical Rate and Armor Penetration don't drop from loot out of the box. They're strong enough to warp early
progression, so they're opt-in. The recipe book's Hardcore Notes category in game walks through all of this.

## Achievements

40 achievements across three difficulties, each with a hand-drawn icon. Eight of them show `？？？` instead of
a description, so you have to work them out from the picture. The explanation shows up in chat the moment you
unlock one.

Rewards are 15, 40 or 100 XP depending on difficulty, plus material templates. Servers have rewards turned off
by default, and an operator can enable them with `/mds rewards true`.

## Requirements

| | |
|---|---|
| Minecraft | 26.2 |
| Fabric | Loader 0.19.3+ and Fabric API 0.158.0+26.2 |
| NeoForge | 26.2.0.88+ |
| Java | 25 |

Install the jar that matches your loader. The two are alternatives, not companions.

## Building

```bash
./gradlew :fabric:build :neoforge:build
```

Jars land in `fabric/build/libs` and `neoforge/build/libs`.

`common` holds the platform-independent code and gets compiled into both loader modules. It has no Minecraft
dependency of its own, so it can't be built on its own.

To run a development client or server:

```bash
./gradlew :fabric:runClient
./gradlew :neoforge:runClient
./gradlew :fabric:runServer
```

Start a dev server with `-Dmds.selftest=true`, or set the `MDS_SELFTEST` environment variable, to check that
crafting behaves and that all 66 advancements loaded. A healthy run logs:

```
[MDS 自检] 成就加载 66/66
```

## Commands

| Command | What it does |
|---|---|
| `/mds help` | List every MDS command |
| `/mds hud ...` | Toggle and configure HUD stats |
| `/mds lootmaxlevel ...` | Set an enchantment's loot max level |
| `/mds lootmaxcount ...` | Set how many enchantments one loot roll can pick |
| `/mds rewards <true\|false>` | Turn achievement rewards on or off |
| `/mds reload` | Reload the config |
| `/mds selftest` | Run the crafting and achievement self-test |

## Project layout

```
common/     platform-independent code: enchantments, UI, config, achievements, mixins
fabric/     Fabric entrypoint and Fabric-only mixins
neoforge/   NeoForge entrypoint and NeoForge-only mixins
```

## License

All rights reserved. Modpacks are welcome and credit is appreciated. Please don't reupload the mod or pass
it off as your own. See [LICENSE](LICENSE) for the full text.

---

## 中文说明

面向 Minecraft 26.2 的战斗改造模组，Fabric 和 NeoForge 都能用。

里面包含 26 个伤害附魔、一台升级附魔书的合成台、可以自由摆放的战斗面板、管战利品和数值的配置界面，
以及 40 个成就。

### 附魔

26 个附魔，分在六个创造栏里。

- **暴击** — 暴击率、暴击伤害、暴击减免、伤害加成
- **攻击** — 护甲穿透、生命吸取、易伤、额外伤害、真实伤害、溅射伤害、处决
- **防御** — 伤害减免、护甲防御、生命强化、反伤、护盾、击退抗性
- **属性** — 攻击速度、移动速度、攻击范围
- **诅咒** — 攻速诅咒、移速诅咒、范围诅咒
- **预设** — 现成的满级附魔书，方便创造模式和测试

所有附魔都在 10 级封顶。每级收益刻意不取整，满级刚好落在好记的数字上：

| 附魔 | 每级 | 10 级 |
|---|---|---|
| 暴击率 | +9.5% | 100% 暴击率 |
| 暴击伤害 | +19.6% | 246% |
| 暴击减免 | +7.4% | 74%，上限 80% |
| 伤害加成 | +3.9% | 39% |
| 护甲穿透 | +3.7% | 37% |
| 伤害减免 | +1.9% | 19% |
| 护甲防御 | +0.95 | 9.5 韧性 |
| 额外伤害 | +5.9% | 59% |
| 攻击速度 | +11.4% | 114% |

### 合成台

四本同附魔同等级的书合成一本高一级的书。每次合成要消耗材料模版，而且喂进去的书至少要有 5 级。
想练到 10 级是要花时间的。

### 战斗面板

有森林和血战两种风格。进入编辑模式后可以随便拖位置、滚轮改大小、隐藏不想看的属性，
点面板旁边的按钮就能在两种风格之间切换。

### 配置

配置界面管的是每个附魔的最高等级、战利品权重，以及它到底掉不掉。全局权重总控会把所有权重一起缩放，
拖动的同时显示最终倍率。

### 关于战利品，以及高级附魔书为什么稀有

默认情况下战利品箱最多只掉 5 级的书。6 到 10 级要么去配置里同时调高最高等级和权重，
要么在合成台一级一级往上合。

暴击率和护甲穿透默认不参与掉落。它们强到足以打乱前期节奏，所以做成了需要手动开启。
游戏内配方书的硬核说明分类把这些都讲了一遍。

### 成就

40 个成就，分三档难度，每个都有手绘图标。其中 8 个把描述藏成 `？？？`，只能看图标猜，
解锁的瞬间会在聊天栏告诉你答案。

奖励按难度给 15、40 或 100 点经验，外加材料模版。服务器默认关闭奖励，管理员用 `/mds rewards true` 打开。

### 前置

| | |
|---|---|
| 游戏 | Minecraft 26.2 |
| Fabric | Loader 0.19.3+ 以及 Fabric API 0.158.0+26.2 |
| NeoForge | 26.2.0.88+ |
| Java | 25 |

装与你加载器对应的那一个 jar 就行，两个是二选一的关系。

### 构建

```bash
./gradlew :fabric:build :neoforge:build
```

产物在 `fabric/build/libs` 和 `neoforge/build/libs`。

`common` 放的是与平台无关的代码，会被编译进两个加载器模块。它自己不带 Minecraft 依赖，所以没法单独构建。

### 授权

保留所有权利。欢迎放进整合包，注明出处就好。请不要二次上传，也不要当成自己的作品。
