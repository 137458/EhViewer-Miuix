# EhViewer-Miuix 组件升级与 Miuix 组件库采用度调研报告

- **调研目标**：为架构重构方向提供「依赖组件升级路径」与「Miuix 组件库特性使用率」两轴的事实底座与行动排序
- **源码基线**：`E:\project\eh` 工作区（main 分支，2026-10-23），版本目录 `gradle/libs.versions.toml`
- **主技术栈**：Kotlin 2.4.20 / Compose Multiplatform 1.12.1（Android + Desktop JVM）/ Miuix `top.yukonga.miuix.kmp` 0.9.4 / Rust 原生下沉
- **调研方法**：本地全仓 grep 统计 + 本地 Gradle 缓存构件 API 枚举（`D:\DevCache\gradle`，miuix-ui-desktop-0.9.4.jar 等）+ Maven Central / Google Maven 元数据 + GitHub API。所有版本号均有一手信源，未运行编译。

---

## 1. 执行摘要（TL;DR）

本仓依赖栈整体新鲜：约三分之二关键依赖已处最新稳定版，剩余差距集中在补丁级（Compose Multiplatform 1.12.1→1.12.2、Kotlin 2.4.20→2.4.21 等），不存在大规模落后的升级债务。真正的结构性风险与机会各有一个：**compose-destinations 上游已停摆整整一年**（最后 push 2025-10-12），而 Miuix 官方恰在 0.9.4（本仓在用版本）新发布了自包含导航运行时 `miuix-nav`，可零版本成本引入并统一移动端 27 个 Screen 的路由与桌面端自绘 `DesktopPageStack`；同时上游 FooIbar/EhViewer 已于 2026-10-05 被 GitHub DMCA 下架，fork 从此进入自持演进阶段。组件库采用度方面，Miuix 已深度使用（115 文件、含 blur 高级特性全家桶），但库内还有约 20 项未使用能力，其中 `pagerGestureOverride`（阅读器手势冲突）、`Tooltip`、`Badge`、`NavigationBar`、`ScrollBar` 与自绘组件存在直接收敛关系。

---

## 2. 外部环境两个战略性事实

### 2.1 上游 FooIbar/EhViewer 已被 DMCA 下架

GitHub API 对 `repos/FooIbar/EhViewer` 返回（2026-10-23 查询）：

```json
{ "message": "Repository access blocked",
  "block": { "reason": "dmca", "created_at": "2026-10-05T15:19:44Z",
             "html_url": "https://github.com/github/dmca/blob/master/2026/03/2026-03-23-fakku.md" } }
```

影响：`AGENTS.md` 中「上游同步 FooIbar/EhViewer」的既有策略失效，raw.githubusercontent 的上游 toml 对照（本次调研原计划项）不可达。次级参照 `xiaojieonly/Ehviewer_CN_SXJ` 仍活跃（pushed_at 2026-10-21）。本仓 `libehviewer.so` 入库 + `checkNativeLibs` 兜底的既有决策（见 `ehviewer-release-process` 约定）被证明是对该类上游消失风险的正确对冲——Rust/C 源与产物自持，不受 DMCA 影响。

### 2.2 Miuix 上游活跃且与 CMP 版本节奏对齐

- 仓库：`github.com/compose-miuix-ui/miuix`（由 miuix-ui-0.9.4.pom 的 scm 字段锚定），1486 stars，pushed_at 2026-10-22（调研前一日）。
- Maven Central 全工件（miuix-ui/preference/icons/blur/core/shader/squircle/nav）`<release>` 均为 **0.9.4**（2026-09-20 发布）——本仓已是最新，无版本升级可做，只有「特性采用」空间。
- 0.9.4-rc01 说明确认其构建基线为 Compose Multiplatform 1.12.0，与本仓 CMP 1.12.x 完全同代。

---

## 3. 依赖版本对照（本仓 → 最新稳定）

信源：repo1.maven.org 与 dl.google.com 的 maven-metadata.xml（2026-10-23 抓取），GitHub Releases API。

### 3.1 已是最新稳定（无需动作）

| 依赖 | 版本 | 备注 |
|---|---|---|
| miuix 全系 | 0.9.4 | 上游最新即本仓在用 |
| coil-bom | 3.6.3 | |
| ktor | 3.6.0 | |
| kotlinx-coroutines | 1.11.0 | |
| kotlinx-io / datetime | 0.9.1 / 0.8.0 | |
| arrow-stack | 2.2.3 | 2.3.0 仍处 alpha（2.3.0-alpha.8），不追 |
| JB material3 | 1.9.0 | **1.10+ 全为 alpha/beta，1.9.0 就是最新稳定**，catalog 硬编码 pin 是正确值 |
| compose-destinations | 2.3.0 | 版本上是最新，但项目停摆（见 5.2） |
| telephoto | 0.19.0 | GitHub 最新 release（2026-04-02）；Central 另有 1.0.0-alpha02 预览线 |
| lifecycle / activity / appcompat / browser | 2.11.0 / 1.13.0 / 1.8.0 / 1.10.0 | |
| reorderable / xmlutil / okhttp 线内 | 3.1.0 / 1.0.2(.1) / — | xmlutil 本仓 1.0.2.1 热修版较 Central `<release>` 1.0.2 更新 |
| biometric | 1.4.0-alpha07 | 稳定线停在 1.1.0，alpha 是该库唯一活跃线，属被迫依赖 |
| compose-material-icons-core | 1.7.3 | JetBrains icons 工件冻结线（末版），31 文件消费，无升级对象 |

### 3.2 可执行的补丁/小版本升级（低风险，逐项独立回滚）

| 依赖 | 本仓 | 最新稳定 | 升级要点 |
|---|---|---|---|
| compose-multiplatform | 1.12.1 | **1.12.2**（2026-10-15） | 修复非矩形 `graphicsLayer` 阴影动画闪烁（compose-multiplatform-core#3465）——与 SquircleShape/LiquidGlass 的异形阴影直接相关 |
| kotlin | 2.4.20 | **2.4.21** | 补丁版 |
| ksp | 2.3.9 | **2.3.12** | 与 Kotlin 2.4.21 对齐 |
| agp | 9.3.1 | **9.4.1**（或保守 9.3.3） | 需过 build-logic convention 全量验证；Gradle wrapper 9.7.1 待兼容性复核 |
| room | 2.8.4 | **2.8.5** | 补丁版 |
| okhttp-bom | 5.5.0 | **5.5.1** | 已升级并实测：okhttp-jvm-5.5.1.jar 仍不含 AsyncDns 类（仅有新回调式 Dns API），vendored shim 继续保留 |
| kotlinx-serialization | 1.11.0 | **1.12.0** | 小版本 |
| compose-bom-alpha | 2026.08.00 | **不可再升**（2026-10-23 实测） | 2026.09.00 起全部 BOM 把 foundation 拉到 1.13.0-alpha03、2026.10.x 到 beta02，要求 compileSdk 37.1（本仓 37）直接构建失败；2026.08.00 实为 1.13.0-alpha01（未带 SDK 门槛故可编译）。BOM 升级须与 compileSdk 37.1 迁移捆绑 |
| datastore | 1.2.1 | **1.3.0** | 小版本，消费方 `:app`/`:core:data` |
| moko-resources | 0.26.4 | **0.27.1** | 涉及 `:core:i18n` 五 locale 生成链，升级后跑全量资源任务 |
| webkit / work / paging | 1.17.0 / 2.11.2 / 3.5.1 | **1.18.0 / 2.12.0 / 3.5.3** | androidx 常规小版本 |

### 3.3 预览线（不建议现在追）

Kotlin 2.5.0-Beta2、CMP 1.13.0-beta01（2026-10-23 当日发布）、JB material3 1.13.0-beta01、JB adaptive 1.4.0-alpha01、telephoto 1.0.0-alpha02。追入条件：miuix 发布针对 CMP 1.13 的版本、且 compose-destinations 届时已被移除（否则其 KSP 编译器插件是首个断点）。

---

## 4. Miuix 组件库采用度审计

### 4.1 总体采用面

`top.yukonga.miuix.kmp` 消费文件共 **115**（`:app` 71 / `:core:ui` 25 / `:core:shell` 8 / `:desktop` 11），覆盖 8 个包：theme、basic、icon（extended 约 30 枚图标）、blur、preference、window、menu/popup、squircle。依赖声明集中在 `core/ui/build.gradle.kts:17-20`（commonMain 对四工件 api 暴露）。

高频组件（import 次数）：MiuixTheme 85、Text 69、Icon 50、MiuixIcons 43、IconButton 26、Card 23、Scaffold 17、InfiniteProgressIndicator 14、Button 14、TopAppBar 13、MiuixScrollBehavior 12、SmallTitle 11。

### 4.2 已用满的高级特性（不是「只用皮毛」的状态）

- **blur 全家桶**（`miuix-blur`）：`Backdrop`/`LayerBackdrop`/`rememberLayerBackdrop`、`ProgressiveBlur` 与 `progressiveTextureBlur`（0.9.4 新特性，本仓 3 文件即采用）、`highlight.Highlight/BloomStroke/LightSource/LightPosition` 高光描边、`rememberDeviceTilt` 设备倾斜传感器联动、`runtimeShaderEffect`/`colorControls`、`isRuntimeShaderSupported` 分支（8 文件）。
- **squircle**：`SquircleShape.kt` 基于 `addSquircleRect` 构建。
- **NavigationRail 可展开态**（0.9.3 新 API）：`rememberNavigationRailState` 已在 `MainActivity.kt` 使用。
- **window 体系**：`WindowDialog`（6 文件）/ `WindowBottomSheet`（4 文件）/ `WindowIconDropdownMenu` / `WindowDropdownPopup`。
- **preference 体系**：设置页以 `PreferenceWidgets.kt` 为薄封装，底层直接用 miuix-preference；已用 ArrowPreference/SwitchPreference/OverlayDropdownPreference/WindowDropdownPreference/SliderPreference 共 5 类。

### 4.3 库提供但未使用的能力（与自绘代码的对照）

依据：miuix-ui-desktop-0.9.4.jar 与 miuix-preference-desktop-0.9.4.jar 的类清单枚举，叠加 0.9.3/0.9.4 release notes；「0 文件」均经全仓 grep 验证。

| miuix 能力 | 状态 | 本仓对应自绘/现状 | 收敛建议与优先级 |
|---|---|---|---|
| **miuix-nav**（独立工件，0.9.4 新发布） | 未引（0 文件） | 移动端 compose-destinations 2.3.0（27 个 `@Destination`、38 文件）；桌面端自绘 `DesktopPageStack.kt`（含测试） | **最高价值**。自包含 CMP 导航运行时：NavDisplay/NavController/NavBackStack、连续栈深动画（animatedTop）、slide/modal 转场、预测性返回、opt-in 滑动返回、entry 生命周期与 ViewModel/SavedStateHandle 集成，零 androidx.navigation3 依赖。统一双端导航的核心抓手（P1） |
| `basic.ScrollBar` | 0 文件 | `core/ui/.../androidx/compose/foundation/scrollbar/` 下自维护 3 文件（Scrollbar.kt、ScrollbarAdapter.kt、LazyStaggeredGridScrollbarAdapter.kt，androidx 包名 fork） | 对比迁移可削掉一层 androidx fork；需先核对 LazyStaggeredGrid 适配能力是否等价（P2） |
| `pagerGestureOverride` / `springAnimateToPage`（0.9.4，utils/PagerGestureUtilsKt） | 0 文件 | 阅读器 `PagerViewer.kt:102` 裸用 `HorizontalPager`，手势冲突靠自身处理；`core/ui` 另 fork 了 3 个 gestures 文件（AnchoredHorizontalDraggable 等） | 官方为「列表 fling 中横滑翻页」提供的解法，与阅读器 双轴手势场景直接对口，可做小步试点（P2） |
| `basic.NavigationBar`（含 item badge slot） | 0 文件 | `core/ui/component/FloatingBottomBar*.kt` 3 文件（PixEz-MIUIX 移植的液态玻璃底栏）+ `core/shell/nav/` 壳 | miuix 原生底栏无液态玻璃质感，属风格取舍而非重复造轮子；保持自绘合理，仅建议对齐 `badge` 槽语义（P3） |
| `basic.Switch` | 0 文件 | 自绘 `LabeledCheckbox.kt` + `CheckIndicator.kt` | 语义不同（复选 vs 开关），保留自绘；新代码可优先库件（P3） |
| `basic.Tooltip`（0.9.3 新） | 0 文件 | 无 | 桌面端 hover 提示场景（Reader 工具栏、设置项）直接可用（P2，desktop 侧） |
| `basic.Badge` / `badge` 槽（0.9.3 新） | 仅 BadgedBox 1 处 | 下载页角标等散点自绘 | 低成本补齐（P3） |
| `basic.ColorPicker` / `ColorPalette` | 0 文件 | `ColorFilterSetting.kt` 自绘滑条调色 | 阅读器滤镜取色可换库件，交互一致化（P3） |
| `basic.NumberPicker`、`basic.BreadcrumbBar`（0.9.4 新）、`basic.FloatingToolbar`、`ListPopup`/`Cascading*`（Morph 级联菜单）、`OverlayDialog`/`OverlayBottomSheet`/`OverlayListPopup`、`MiuixEasing`、`MiuixIndication`、`Overscroll`、`Pressable`、`ScrollEndHaptic`、`theme.DismissState` | 0 文件 | — | 无现成收敛点；`DismissState` 对应能力目前走 material3 fork 的 SwipeToDismissBox（见 5.1） |
| preference 未用件：`CheckboxPreference`/`RadioButtonPreference`/`OverlaySpinnerPreference`/`WindowSpinnerPreference` | 0 文件 | 部分场景用自绘 BasicComponent 组合 | 按需替换即可（P3） |

结论：自绘组件中真正与库能力**重复**的只有 scrollbar 一处半（ScrollBar、半个 ColorPicker）；`FloatingBottomBar`/`LiquidGlass`/`MutableSideSheet`/`RollingNumber`/`RatingWidget` 属库缺口，是本 fork 的差异化资产，不应收敛。

### 4.4 修正旧报告一处结论

`docs/research/architecture-refactor-review.md`（2026-10-11）2.2 节记载「`:core:shell` 未被 app 消费」。当前 `app/build.gradle.kts:181` 已有 `implementation(projects.core.shell)`，实际消费 2 文件（`ui/main/SearchFilter.kt`、`ui/screen/GalleryListScreen.kt`）——shell 同构迁移（R08/R29）已开始合流但仍处中间态，桌面与移动双侧收敛应继续沿 M0-M7 推进。

---

## 5. Compose 双体系现状与升级约束

### 5.1 三层体系实测规模

- **miuix 层**（设计系统主体）：115 文件，见第 4 章。
- **JB material3 层**（`org.jetbrains.compose.material3:material3:1.9.0`，catalog 硬编码）：直接 import 仅 **8 文件**（`:app` 7 + `:core:ui` 1），用途高度特化：`SwipeToDismissBox` 族（含 `core/ui/.../material3/fork/SwipeToDismissBox.kt`、`DraggableAnchors.kt` 两个 fork 文件，8 文件消费）、DatePicker/TimePicker、动态取色 `dynamicLight/DarkColorScheme`、`MotionScheme`、`MaterialTheme` 桥接。这是与 MiuixTheme 并存的第二主题树（架构报告 2.5.3 已载）。
- **androidx foundation 层**：143 文件（`:app` 75 / `:core:ui` 50 / `:desktop` 11 / `:core:shell` 7）消费 `androidx.compose.foundation`，属基础布局原语，非设计系统混用。
- material3-adaptive（1.2.0）仅 2 文件消费（`currentWindowAdaptiveInfoV2` 等），断点逻辑另有自绘 `AdaptiveLayout.kt`/`WindowSizeClass.kt`。

### 5.2 compose-destinations：结构性风险（本次调研最重要发现之一）

- 上游 `raamcosta/compose-destinations`：**最后 push 2025-10-12（距今整 1 年）、最新 release 2.3.0（2025-10-12）、55 个 open issues**。停摆确认。
- 本仓耦合面：27 个 `@Destination`、38 个 import 文件、KSP 编译器插件（`app/build.gradle.kts:186-187`）、`PreferenceWidgets.kt` 的 `DirectionDestinationSpec/DestinationsNavigator` 贯穿全部设置页导航。
- 风险机理：destinations 以 KSP 编译器插件形态生成代码，对 Kotlin/CMP 版本前进敏感；当前在 Kotlin 2.4.20/CMP 1.12.1 可用纯属兼容窗口，Kotlin 2.5 / CMP 1.13 升级时大概率成为首个断点，且无人修复。
- 对策即 miuix-nav 迁移（4.3 首行）：同版本（0.9.4）、同设计语言、双端同一运行时，还能顺带消掉桌面端自绘 `DesktopPageStack` 的第三套导航语义。

### 5.3 vendored/fork 清单（升级时需逐项复核的锚点）

| 文件 | 存在原因 | 升级触发器 |
|---|---|---|
| `app/src/main/kotlin/okhttp3/AsyncDns.kt` + `AndroidAsyncDns.kt` | okhttp-jvm 5.5.0 工件不含 AsyncDns（本地缓存 jar 实测 0 命中），shim 为 API 29+ 双栈 DNS 填坑（唯一消费点 `ktor/OkhttpExtensions.kt`） | 每次 okhttp 小版本升级后 `unzip -l` 复检；官方补回即删 |
| `core/ui/.../androidx/compose/foundation/gestures/` 3 文件 | fork 自 androidx，支撑横拖手势 | miuix `pagerGestureOverride` 试点成功后可削减 |
| `core/ui/.../androidx/compose/material3/fork/` 2 文件 | SwipeToDismissBox + DraggableAnchors fork | material3 1.9.0 已含稳定 DraggableAnchors，fork 或可瘦身（需 diff 验证动机） |
| `core/ui/.../androidx/compose/foundation/scrollbar/` 3 文件 | 列表滚动条（上游 EhViewer 血统） | miuix ScrollBar 能力对齐后迁移 |
| `desktop/build.gradle.kts:41` 注释 | coil3 依赖 Dispatchers.Main，JVM 侧靠 `kotlinx-coroutines-swing` ServiceLoader 提供 | 既有事实，非债务 |

---

## 6. 与既有重构路线的衔接（行动排序）

叠加在架构报告六阶段路线图与 `:core:shell` M0-M7 之上，组件升级轴建议如下排序（每步独立可回滚，验证标准 = `assembleDefaultRelease` + `:desktop:compileKotlin` + `:desktop:desktopTest` 三件套）：

1. **第 0 批（本周，零代码改动的版本对齐）**：CMP 1.12.2（顺带收获异形阴影闪烁修复）→ Kotlin 2.4.21 + KSP 2.3.12 → Room 2.8.5 / okhttp 5.5.1（升后复检 AsyncDns shim）/ serialization 1.12.0 / bom-alpha 2026.10.01 / webkit / work / paging。datastore 1.3.0 与 moko 0.27.1 涉及 `:core:i18n` 资源生成与偏好层，单独一批并跑五 locale 校验。
2. **第 1 批（AGP 线，独立窗口）**：AGP 9.3.1 → 9.4.1，全量过 build-logic convention 与 baselineprofile 插件；失败则退守 9.3.3 补丁。
3. **第 2 批（miuix-nav 试点，桌面先行）**：`:desktop` 引入 `top.yukonga.miuix.kmp:miuix-nav:0.9.4`，以 `DesktopPageStack`（已是纯模型栈）映射 NavController/NavBackStack，验证转场与返回栈语义；桌面单窗口约束（用户硬性要求）不受影响。
4. **第 3 批（移动端 destinations 退场）**：27 个 Screen 逐片迁 miuix-nav，先设置页（`PreferenceWidgets.kt` 单点改造即可覆盖全部子页入口）后业务页；完成后删除 destinations 依赖与 KSP 插件，**解除 Kotlin 2.5/CMP 1.13 升级的最大阻塞**。该批与 shell 迁移（M0-M7）天然合流：`core/shell/nav/AppShell.kt` 正是双端导航壳的落点。
5. **第 4 批（自绘收敛试点，机会型）**：阅读器 `pagerGestureOverride`、桌面 Tooltip、滚动条迁移评估、ColorFilterSetting 换 ColorPicker。逐项独立，不做大爆炸替换。
6. **长期**：预览线（Kotlin 2.5 / CMP 1.13 / material3 1.13）在 miuix 发布对应兼容版且 destinations 已移除后再评估进入。

### 不变量与风险

- 上游 DMCA 后，安全与协议维护责任完全自持：任何「跟上游拿补丁」的假设不再成立，Rust 解析器与 libarchive 链路升级需自建跟踪（可参照 SXJ 镜像与 EhWiki 协议变更）。
- miuix-nav 发布仅月余（2026-09-20），API 尚新：迁移试点期双轨并存（destinations 与 miuix-nav 在不同 Screen 共存是可接受的中间态），以 Screen 为单位切换。
- miuix 单点依赖加深（nav 迁移后达 5 工件）：对冲是上游活跃（pushed 2026-10-22）+ 版本节奏与 CMP 对齐；若需极端解耦，`core/ui` 的 api 暴露层已是正确隔离面。

---

## 7. 信源清单

- FooIbar/EhViewer DMCA：`api.github.com/repos/FooIbar/EhViewer`（2026-10-23），block.html_url 指向 github/dmca/2026/03/2026-03-23-fakku
- miuix 发布与仓库：`api.github.com/repos/compose-miuix-ui/miuix`（releases v0.9.4/v0.9.4-rc01/v0.9.3，repo pushed_at 2026-10-22）；`repo1.maven.org/maven2/top/yukonga/miuix/kmp/*/maven-metadata.xml`（全工件 latest=0.9.4，miuix-nav lastUpdated 2026-09-20）；miuix-ui-0.9.4.pom scm 字段
- 版本元数据：repo1.maven.org（kotlin/jb-material3/adaptive/ksp/coil/ktor/okhttp/coroutines/serialization/io/datetime/destinations/telephoto/moko/arrow/xmlutil/reorderable）与 dl.google.com（AGP/room/datastore/lifecycle/activity/appcompat/biometric/webkit/work/paging/browser/compose-bom-alpha），2026-10-23
- CMP releases：`api.github.com/repos/JetBrains/compose-multiplatform/releases`（v1.12.2 2026-10-15，v1.13.0-beta01 2026-10-23）
- compose-destinations 状态：`api.github.com/repos/raamcosta/compose-destinations`（pushed_at 2025-10-12，55 open issues）
- 本地构件：`D:\DevCache\gradle\caches\modules-2\files-2.1\top.yukonga.miuix.kmp\*`（miuix-ui/preference-desktop-0.9.4.jar 类清单、miuix-nav.aar、okhttp-jvm-5.5.0.jar AsyncDns 复检）
- 本仓统计：git grep（app 71 / core:ui 25 / core:shell 8 / desktop 11 的 miuix 消费计数；@Destination 27、destinations import 38、material3 import 8、foundation 143、icons 31），`core/ui/build.gradle.kts:17-29`、`app/build.gradle.kts:178-230`、`desktop/build.gradle.kts:40-105`
