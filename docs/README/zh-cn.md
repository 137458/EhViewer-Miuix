<p align="right">
  <a href="/README.md">
  简体中文
  </a>
  <span> | </span>
  <a href="/docs/README/en.md">
  English
  </a>
  <span> | </span>
  <a href="/docs/README/zh-tw.md">
  正體中文
  </a>
  <span> | </span>
  <a href="/docs/README/ja.md">
  日本語
  </a>
</p>

<h1 align="center">
  <img src="https://github.com/FooIbar/EhViewer-art/blob/master/launcher_icon-web.svg" width="160" alt="EhViewer MIUIX">
  <br>EhViewer MIUIX<br>
</h1>

<p align="center">
  <a href="https://github.com/137458/ehviewer-miuix/releases">
    <img src="https://img.shields.io/github/v/release/137458/ehviewer-miuix" alt="Release">
  </a>
  <a href="/LICENSE">
    <img src="https://img.shields.io/badge/License-GPLv3-blue.svg" alt="LICENSE">
  </a>
  <a href="https://github.com/137458/ehviewer-miuix/issues">
    <img src="https://img.shields.io/github/issues/137458/ehviewer-miuix" alt="Issues">
  </a>
  <img src="https://img.shields.io/badge/Platform-Android%208.0%2B-green.svg" alt="Android">
  <img src="https://img.shields.io/badge/Design-MIUIX%20%2F%20HyperOS-FF6900.svg" alt="Design">
  <img src="https://img.shields.io/badge/Language-Kotlin%202.4-7F52FF.svg" alt="Kotlin">
</p>

<div align="center">
  <h3>
    <a href="#项目定位">项目定位</a>
    <span> | </span>
    <a href="#核心特性">核心特性</a>
    <span> | </span>
    <a href="#下载与安装">下载安装</a>
    <span> | </span>
    <a href="#系统要求">系统要求</a>
    <span> | </span>
    <a href="#常见问题解答-faq">常见问题</a>
    <span> | </span>
    <a href="#历史渊源与致谢">致谢名单</a>
    <span> | </span>
    <a href="#许可证">许可证</a>
  </h3>
</div>

---

## 重要声明与防伪警示

> [!IMPORTANT]
> **官方唯一发布渠道**：
> 本应用为纯粹的技术交流与兴趣开源项目，**有且仅在 GitHub Releases 页面发布更新**。
> 互联网上所有自称“EhViewer 官网”、兜售 VIP 会员、付费购买应用、或在第三方下载站上架的所谓“破解版 / 去广告版”均属虚假欺诈行为，可能存在恶意后门注入与账号盗取风险。请广大用户认准官方开源仓库，切勿在非官方渠道下载任何安装包。

---

## 项目定位

**EhViewer MIUIX** 致力于为 Xiaomi HyperOS / MIUI 生态及现代 Android 平台提供极致视觉表现与丝滑交互体验的高性能 EhViewer 客户端。

本项目以 Kotlin Multiplatform 与 Jetpack Compose 为架构基础，深度融合 `top.yukonga.miuix.kmp` 组件生态与底层物理着色器渲染管线，重构了全应用视觉语言与交互组件，呈现纯粹的 HyperOS 设计美学。

---

## 核心特性

- **MIUIX 设计体系全面重构**：全量接入原生 MIUIX 设计规范，深度重构顶栏、卡片容器、分组设置项、弹窗对话框与底部抽屉面板，与 HyperOS 系统风格完美融为一体。
- **液态玻璃悬浮底栏 (Floating Bottom Bar)**：
  - **SDF 物理透镜折射**：基于距离场方程计算物理透镜屈光与边缘折射率。
  - **色散彩虹光晕**：模拟真实光学色散（Chromatic Dispersion）与高光反射边缘。
  - **双重背景实时采样**：多图层 backdrop 实时采样与高斯模糊合成。
  - **物理阻尼手感**：内置微交互回弹力学模型，支持阻尼滑动切换与重力感应高光倾斜。
- **沉浸式渐进模糊顶栏 (BlurredBar)**：
  - 采用 Xiaomi HyperOS 标准渐进式纹理模糊算法，状态栏满额模糊向下自然衰减至像素级清晰，杜绝生硬切割线条。
  - 协同滚动驱动大标题与小标题平滑转换，滚动位移自适应淡入淡出。
- **平滑超椭圆曲率 (Squircle Shape)**：
  - 全局卡片、图片缩略图与控件统一采用平滑曲率超椭圆几何算法，消除传统圆角的折角生硬感。
- **响应式大屏与折叠屏适配**：
  - 针对平板、折叠屏大屏设备提供自适应响应式布局。
  - 在横屏与宽屏场景下，底栏自动流转切换至左侧垂直导航栏（NavigationRail），画廊与列表自适应切换为多列流式排版。
- **硬件着色器兼容层 (RuntimeShaderCompat)**：
  - 在 Android 13+ (API 33+) 自动启用硬件级 AGSL 着色器执行物理折射与复杂模糊。
  - 在低版本系统环境自动平滑降级至高性能原生备用管线，兼顾前沿视觉冲击与极宽的设备兼容性。
- **完整的画廊与阅读体验**：
  - 支持多线程异步预加载与平滑缩放。
  - 包含垂直滚动、从左到右、从右到左、条漫模式以及双页阅读等多种模式。
  - 支持高级标签多维搜索、分类筛选、以图搜图与快速历史归档。
- **稳定健全的下载与导出系统**：
  - 支持多任务并发下载、断点续传、下载任务智能重试、自定义存储目录与 ZIP / CBZ 归档导出。

---

## 下载与安装

请前往 [GitHub Releases](https://github.com/137458/ehviewer-miuix/releases) 页面下载最新构建安装包：

| 变种版本 (Flavor) | 最低 Android 版本 | 架构说明 | 特性支持 |
|:---|:---|:---|:---|
| **Default** | Android 8.0 (API 26) | arm64-v8a / Universal | 完整支持 MIUIX 设计系统与物理视觉管线（推荐） |
| **Marshmallow** | Android 6.0 (API 23) | 通用架构 | 历史设备兼容版本，部分高级动效自动平滑降级 |

### 安装与配置建议

1. 下载对应变体的 `.apk` 安装文件并在设备上完成安装。
2. **后台下载保活建议（HyperOS / MIUI 设备）**：
   - 前往系统「设置」-「应用设置」-「应用管理」-「EhViewer」。
   - 将「省电策略」从「智能限制」修改为「无限制」，并开启「自启动」权限，避免系统在锁屏后休眠网络导致批量下载中断。
3. **视觉效果建议**：
   - 建议在 Android 13 及以上系统使用，即可开启完整的实时物理透镜折射与渐进式模糊效果。

---

## 系统要求

- **最低运行要求**：Android 8.0 (API 26) 及以上。
- **推荐运行环境**：
  - Android 13 (API 33) 或更高版本。
  - Xiaomi HyperOS / MIUI 运行环境。
  - 支持 RuntimeShader 硬件级图形渲染的现代移动芯片。

---

## 常见问题解答 (FAQ)

### 1. 为什么提示“没有权限”、“Sad Panda”或无法正常进入画廊？
- 请检查是否已正确登录账号。
- 部分画廊与里站（ExHentai）资源需要账号满足相应访问权限条件。可通过应用内的 WebView 网页登录，或手动导入具备有效身份验证凭证的 Cookie。

### 2. 为什么浏览时出现 HTTP 509 错误？
- HTTP 509 代表图像配额耗尽（Image limit exceeded）。
- 这是服务端对单个 IP 或账号在固定时间周期内的配额限制，并非客户端缺陷。可通过更换网络节点、重置网络分配、或稍作等待以恢复每日配额。

### 3. 网络无法正常连接或超时如何处理？
- 请检查系统代理或网络分流规则，确保 EhViewer 未被代理软件误拦截或设置错误。
- 若处于特定网络限制环境中，请使用合规的网络代理接入，并在应用设置中确认内置 Hosts 与网络解析配置。

### 4. 存储与下载画廊读取失败？
- 在 Android 11 及以上系统，因系统存储沙盒限制，请进入系统设置授予应用必要的文件访问管理权限。
- 若需修改下载保存路径，建议使用系统文件选择器指定公共 Documents 或 Download 目录。

---

## 历史渊源与致谢

EhViewer 的发展凝结了开源社区众多先驱与贡献者的心血，特此向以下项目与开发者致以崇高的敬意：

### 项目先驱与分支贡献
- [seven332 (Hippo)](https://github.com/seven332) — EhViewer 项目奠基人与最初缔造者。
- [NekoInverter](https://github.com/NekoInverter) — 早期架构现代化维护与演进贡献。
- [Tarsin Norbin](https://github.com/TarsinNorbin) — 现代 EhViewer 演进关键维护者。
- [FooIbar](https://github.com/FooIbar/EhViewer) — 高性能 Compose 架构重塑与演进分支。
- [xiaojieonly (SXJ)](https://github.com/xiaojieonly/Ehviewer_CN_SXJ) — 国内分支长线维护与丰富功能特性贡献。

### UI 框架与物理着色器核心项目
- [top.yukonga.miuix.kmp](https://github.com/miuix-kotlin-multiplatform/miuix) — 卓越的 Kotlin Multiplatform MIUIX / HyperOS 设计规范组件库。
- [Kyant0/AndroidLiquidGlass](https://github.com/Kyant0/AndroidLiquidGlass) — 令人惊艳的液态玻璃物理透镜与 AGSL 距离场着色器管线实现。
- [InstallerX-Revived](https://github.com/rosan/InstallerX-Revived) (Rosan) — 悬浮底栏弹性微交互与阻尼力学设计参考。

### 开源依赖项目
- [AOSP](https://source.android.com/) & [AndroidX](https://developer.android.com/jetpack/androidx)
- [Jetpack Compose](https://developer.android.com/jetpack/compose) & [Compose Multiplatform](https://www.jetbrains.com/lp/compose-multiplatform/)
- [Kotlin](https://kotlinlang.org/) & [Kotlinx Coroutines](https://github.com/Kotlin/kotlinx.coroutines)
- [Ktor](https://ktor.io/)
- [Coil](https://coil-kt.github.io/coil/)
- [Compose Destinations](https://composedestinations.rafaelcosta.xyz/)
- [Arrow](https://arrow-kt.io/)
- [Telephoto](https://github.com/saket/telephoto)
- [libarchive](https://www.libarchive.org/)

---

## 免责声明

1. 本项目仅供移动端技术交流、Jetpack Compose 声明式 UI 开发规范以及物理着色器动效技术的学习研究之用，不得用于任何商业营利目的。
2. 软件本身为开源网络客户端工具，不内置、不提供、不分发任何受版权保护的媒体内容或商业数据；客户端的一切网络请求均由终端用户自主触发并发起至第三方公开网络接口。
3. 请使用者严格遵守当地法律法规。因用户使用本软件所产生的任何直接或间接后果，由使用者自行承担。

---

## 许可证

本项目遵循 **GNU General Public License v3.0 (GPLv3)** 开源许可证分发：

```text
EhViewer is free software: you can redistribute it and/or modify it under the terms of 
the GNU General Public License as published by the Free Software Foundation, 
either version 3 of the License, or (at your option) any later version.

EhViewer is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; 
without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. 
See the GNU General Public License for more details.
```

完整协议内容请参阅仓库根目录下的 [LICENSE](LICENSE) 文件。

---

## 源码构建（面向开发者）

如果你是一名 Android 开发者并希望从源码编译构建本项目，请遵循以下流程：

### 前置环境要求
- **JDK**：21 及以上
- **Android SDK**：API Level 35 (Build-tools 35.0.0+)
- **构建工具**：Gradle 9.x + Android Gradle Plugin 9.x
- **推荐 IDE**：Android Studio Ladybug (2024.2.1) 或更高版本

### 编译步骤

```bash
# 1. 克隆代码仓库
git clone https://github.com/137458/ehviewer-miuix.git
cd ehviewer-miuix

# 2. 编译 Default Release 生产包
./gradlew assembleDefaultRelease

# 3. 产物输出位置位于
# app/build/outputs/apk/default/release/app-default-*-release.apk
```
