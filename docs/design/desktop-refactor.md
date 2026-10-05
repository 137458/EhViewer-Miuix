# 桌面端布局重构设计文档

> 状态：待实施
> 范围：`:desktop` 布局与导航重构，新增共享 UI 模块
> 参考：`E:\project\pixez-flutter-MIUIX`

## 1. 背景与目标

当前 `:desktop` 维护了一套与 `:app` 并行演化的独立界面实现，导致：

- 导航形态、库页排版、详情页信息密度与移动端大屏形态长期不一致；
- 断点、限宽、导航项等布局常量一度在桌面自持，与移动端双轨漂移；
- 桌面交互观感偏"复古"，与 Miuix/HyperOS 设计语言脱节。

**目标**：桌面端不再独立设计 UI，而是**移动端大屏形态的同源实现**——同一套 Compose 界面代码，按窗口尺寸自适应导航形态与内容布局。

## 2. 参考项目分析

参考项目 `pixez-flutter-MIUIX` 同时存在两种"桌面/移动关系"模型：

| 模型 | 载体 | 桌面判定 | 界面代码 | 导航 |
|---|---|---|---|---|
| 独立双树 | `archive/flutter-v1/lib`（Flutter 归档） | OS 平台（`Platform.isWindows\|\|Linux`） | 桌面 Fluent / 移动 Material 两套 | Fluent `NavigationPane` 侧栏 / Material 底栏 |
| 同源自适应 | `compose-miuix`（现役分支） | 窗口宽度断点 600dp | 单套 Compose 代码 | `NavigationRail` ↔ 底栏自动切换 |

**采纳现役 `compose-miuix` 模型（同源自适应）**，理由：与 `:app`/`:desktop` 已共享的 `core/ui` 自适应体系一致，避免维护两套界面。

## 3. 现状盘点

### 3.1 桌面端（`:desktop`）

- 入口：`desktop/src/desktopMain/kotlin/com/ehviewer/desktop/Main.kt`
- 导航模型：`DesktopPageStack`（纯模型栈：Library / GalleryDetail / Reader / Settings），单窗口逐页推入。
- 导航外壳：`DesktopNavRail`（8 项 `NavigationRail`）或 `DesktopFloatingNavBar`（液态玻璃悬浮底栏），由设置 `navBarStyle`（Rail / FloatingBottomBar / Auto）切换。
- 库页：`LibraryScreen.kt`（约 2250 行），顶栏为「Tab 标题 + 搜索框 + 排序/视图/刷新胶囊」，下方网格/列表；详情为**整页推入**（`GalleryDetailPageContent`）。
- 阅读器：`ReaderScreen.kt`；设置：`SettingsScreen.kt`。
- 原生菜单栏：`AppMenus`（文件 / 设置 / 帮助），即用户所指"复古 tab"。
- 打包：fat jar（`desktopFatJar`，系统 JRE）与 jpackage MSI/EXE 两条路径。

### 3.2 移动端（`:app`）

- 入口：`app/src/main/kotlin/com/hippo/ehviewer/ui/MainActivity.kt`，Compose-Destinations 导航。
- 导航外壳：`showNavigationRail = windowLayout.navigationChrome == NavigationChrome.Rail`；宽屏用 8 项 `NavigationRail`，窄屏用 6 项 `FloatingBottomBar`（主项策略见 `MainNavPolicy.kt`）。
- 库页：`ui/main/GalleryList.kt`（`FastScrollLazyVerticalGrid` / 瀑布流）+ `ui/main/SearchFilter.kt`（筛选 chips）。
- 详情：`ui/screen/GalleryDetailContent.kt` 已实现**双栏 master-detail**（宽屏 `isLargeLandscape` 启用，含可拖拽分隔条）。

### 3.3 共享层（`core/ui`）

- `util/AdaptiveLayout.kt`：`AdaptiveBreakpoints`（`RAIL_MIN_WIDTH_DP=600`、`RAIL_MIN_HEIGHT_DP=600`、`CONTENT_WIDE_WIDTH_MIN_DP=840`、`THUMB_MIN_COLUMN_WIDTH_DP=200` 等）、`WindowLayout`（`navigationChrome` 需宽高同时 ≥600 才为 Rail）、`LocalWindowLayout`、`Modifier.readableWidth()`。
- `component/`：`FloatingBottomBar` / `IosLiquidGlassNavigationBar`、`BlurredBar`、`CrystalCard`、`FastScrollLazyGrid` / `FastScrollLazyColumn`、`MutableSideSheet` 等。
- `theme/MiuixThemeController.kt`：`rememberMiuixThemeController`，两端同源配色（移动端另叠 M3 动态取色）。

**结论**：自适应策略与叶子组件已共享，割裂点在于**整页界面代码**（外壳、库页、详情）未共享。

## 4. 已确认决策

1. **关系模型**：同源自适应（单套代码），新建共享模块分步下沉。
2. **首步范围**：自适应外壳 + 库页 + 双栏详情；先只接 `:desktop`，`:app` 后续迁移。设置/阅读器随后。
3. **库页形态**：完全对齐移动端首页——`BlurredBar` 顶栏（标题 + 搜索 + 筛选 chips）+ 自适应列数网格/列表，排序/视图/刷新收进筛选栏。
4. **详情形态**：双栏 master-detail，内容与移动端一致（标签云 / 简介 / 操作区 / 相关推荐 / 评论）。
5. **导航**：移除原生菜单栏，入口内联到应用；导航项可自定义（显示哪些 + 顺序），入口为设置页新增的「导航项」页。
6. **阅读器**：工具栏/翻页控件 + 宽屏图片尺寸/留白（第二步）。
7. **DPI**：修 DPI 感知 + 设置页提供 UI 缩放系数兜底。
8. **桌面专属功能**：托盘驻留、拖拽打开、Ctrl 多选、键盘快捷键、会话恢复全部保留。

## 5. 目标架构

```
core/ui            （既有）自适应策略 + 叶子组件 + 主题
core/shell  （新） 自适应外壳 + 库页 + 双栏详情（commonMain，无状态）
   ├─ 依赖 core:ui / core:i18n / core:model
   ├─ :desktop 宿主：提供 state（DesktopHttp + Room）+ 桌面专属壳（窗口/托盘/拖拽/快捷键）
   └─ :app     宿主：后续将 ViewModel 输出映射为共享 state
```

**关键约束**：共享页面**无状态**，只接受 `state` + 回调，不直接持有数据层/Context。数据获取留在各端宿主。

- 新模块命名暂定 `:core:shell`（可调整）。
- 断点继续走 `core/ui` 的 `WindowLayout` / `AdaptiveBreakpoints`，不再新增桌面私有常量。
- 主题继续走共享 `rememberMiuixThemeController`。

## 6. 实施步骤

1. 建 `:core:shell` 骨架（KMP，`commonMain`；依赖 `core:ui` / `core:i18n` / `core:model`）。
2. 下沉自适应外壳：按 `WindowLayout.navigationChrome` 在 `NavigationRail` ↔ 悬浮底栏间切换，导航项来自可配置列表。
3. 下沉库页：顶栏（标题 + 搜索 + 筛选 chips）+ 自适应列数网格/列表。
4. 下沉双栏详情：从移动端 `GalleryDetailContent` 剥离 Android 耦合后复用。
5. `:desktop` 接入共享壳，删除 `Main.kt` / `DesktopGalleryItems.kt` 中的重复实现。
6. 设置页新增「导航项」页（勾选显示 + 拖拽排序）。
7. 移除原生菜单栏，`文件/关于/快捷键/退出` 入口内联。
8. DPI 修复 + 应用内缩放设置。
9. 阅读器改造（工具栏/翻页控件 + 宽屏图片尺寸）。

## 7. 风险与待定项

- **状态抽象是最大工作量**：`:app` 走 ViewModel，`:desktop` 为 ad-hoc state，需统一为无状态 `state + callbacks`。
- **双栏详情**的分隔条/预览网格需先从移动端 `GalleryDetailContent` 剥离 Android 耦合。
- **DPI 根因需实跑确认**：fat jar 走系统 JRE 时，`java.exe` 的 manifest 可能已将进程锁为 system-DPI-aware，导致 150% 缩放下被位图放大发虚；修法是在启动层确保 per-monitor DPI 感知，应用内缩放作为兜底。
- **导航项自定义**需定义可配置项集合与默认值（现桌面 8 项，移动端底栏 6 项主项）。

## 8. 验收标准

- `:desktop:compileKotlinDesktop`、`:desktop:desktopTest`、`:desktop:spotlessCheck` 三件套全绿。
- 桌面宽屏形态与移动端大屏形态在导航、库页、详情三处视觉与交互一致。
- 实机冒烟：DPI 清晰、导航项自定义生效、菜单栏入口内联可用。
