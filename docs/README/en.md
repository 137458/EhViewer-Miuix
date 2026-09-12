<p align="right">
  <a href="/README.md">
  简体中文
  </a>
  <span> | </span>
  <strong>English</strong>
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
  <a href="https://github.com/137458/ehviewer-miuix/releases"><img src="https://img.shields.io/github/v/release/137458/ehviewer-miuix" alt="Release"></a>
  <a href="/LICENSE"><img src="https://img.shields.io/badge/License-GPLv3-blue.svg" alt="LICENSE"></a>
  <a href="https://github.com/137458/ehviewer-miuix/issues"><img src="https://img.shields.io/github/issues/137458/ehviewer-miuix" alt="Issues"></a>
  <img src="https://img.shields.io/badge/Platform-Android%208.0%2B-green.svg" alt="Android">
  <img src="https://img.shields.io/badge/Design-MIUIX%20%2F%20HyperOS-FF6900.svg" alt="Design">
  <img src="https://img.shields.io/badge/Language-Kotlin%202.4-7F52FF.svg" alt="Kotlin">
</p>

<div align="center">
  <h3>
    <a href="#overview">Overview</a>
    <span> | </span>
    <a href="#features">Features</a>
    <span> | </span>
    <a href="#download--installation">Download</a>
    <span> | </span>
    <a href="#system-requirements">Requirements</a>
    <span> | </span>
    <a href="#faq">FAQ</a>
    <span> | </span>
    <a href="#credits--heritage">Credits</a>
    <span> | </span>
    <a href="#license">License</a>
  </h3>
</div>

---

## Important Notice & Anti-Fraud Advisory

> [!IMPORTANT]
> **Official Distribution Channel**:
> This project is a free, non-commercial open-source project. **Official releases are distributed solely via GitHub Releases**.
> Any websites claiming to be the "official EhViewer website", charging subscription fees, selling VIP memberships, or offering repackaged APKs with unauthorized modifications are fraudulent and potentially malicious. Always verify the repository URL and release signatures.

---

## Overview

**EhViewer MIUIX** is a high-performance EhViewer client reimagined for the Xiaomi HyperOS / MIUI ecosystem and modern Android devices.

Powered by Kotlin Multiplatform and Jetpack Compose, the application integrates `top.yukonga.miuix.kmp` and a custom physical shader rendering pipeline to deliver an authentic HyperOS visual aesthetic and fluid micro-interactions.

---

## Features

- **MIUIX Design System**: Complete overhaul incorporating native MIUIX design specifications, including top app bars, card containers, grouped preference settings, window dialogs, and bottom sheets.
- **Liquid Glass Floating Bottom Bar**:
  - **SDF Lens Refraction**: Physics-based refraction and index calculation using signed distance fields.
  - **Chromatic Dispersion**: Realistic optical color dispersion and specular edge reflections.
  - **Dual Backdrop Sampling**: Multi-layer backdrop blur and real-time texture compositing.
  - **Damped Drag Physics**: Dynamic physics spring model with damped drag gestures and device tilt reflections.
- **Progressive Blur Top Bar (BlurredBar)**:
  - Xiaomi HyperOS-compliant progressive texture blur, blending smoothly from full blur in the status bar down to pixel-sharp content.
  - Coordinated scroll expansion and collapse between large and small title headers.
- **Squircle Superellipse Curvature**:
  - Continuous curvature geometry on cards, thumbnails, and interactive surfaces for seamless, modern aesthetics.
- **Adaptive Layout for Tablets and Foldables**:
  - Responsive screen size adaptation.
  - Seamlessly transforms from the floating bottom bar to a vertical NavigationRail in landscape or large-screen modes, with responsive multi-column gallery grids.
- **Hardware Shader Compatibility (RuntimeShaderCompat)**:
  - Utilizes hardware AGSL shaders on Android 13+ (API 33+) for physics refraction and blur.
  - Seamlessly falls back to optimized native graphics pipelines on earlier Android releases.
- **Rich Gallery & Reader Experience**:
  - Multi-threaded asynchronous preloading with smooth pan and zoom.
  - Supports vertical scrolling, left-to-right, right-to-left, webtoon continuous scrolling, and dual-page viewing modes.
  - Comprehensive tag searching, category filtering, image reverse search, and search history.
- **Robust Download & Archive Management**:
  - Multi-task parallel downloading, automatic retry logic, custom storage paths, and ZIP / CBZ archive export.

---

## Download & Installation

Download the latest APK package from the [GitHub Releases](https://github.com/137458/ehviewer-miuix/releases) page:

| Flavor | Minimum Android Version | Target Architecture | Description |
|:---|:---|:---|:---|
| **Default** | Android 8.0 (API 26) | arm64-v8a / Universal | Full MIUIX design system and shader visual pipeline (Recommended) |
| **Marshmallow** | Android 6.0 (API 23) | Universal | Legacy compatibility build with graceful visual fallbacks |

### Installation Tips

1. Download the appropriate `.apk` file and install it on your device.
2. **Background Download Optimization (HyperOS / MIUI)**:
   - Navigate to system Settings -> Apps -> Manage apps -> EhViewer.
   - Change Battery saver to "No restrictions" and enable Autostart to ensure uninterrupted background downloads when the screen is locked.
3. **Display Recommendation**:
   - Android 13 or newer is recommended to enjoy full hardware AGSL lens refraction and progressive blur effects.

---

## System Requirements

- **Minimum Version**: Android 8.0 (API 26) or higher.
- **Recommended Environment**:
  - Android 13 (API 33) or higher.
  - Xiaomi HyperOS / MIUI platform.
  - Modern mobile chipset supporting RuntimeShader hardware acceleration.

---

## FAQ

### 1. Why do I see "Permission Denied", "Sad Panda", or gallery loading failures?
- Ensure you have successfully logged in to your account.
- Certain galleries and ExHentai content require accounts with specific access privileges. You can log in via the built-in WebView or manually import valid authentication cookies.

### 2. What causes the HTTP 509 error?
- HTTP 509 signifies image quota exhaustion (Image limit exceeded).
- This is a server-side limitation applied per IP address or account over a rolling window. It is not an application defect. Switch network routes, obtain a new IP address, or wait for the quota to reset.

### 3. Connection timeouts or network failures?
- Verify your network proxy and routing configurations. Ensure EhViewer is not inadvertently blocked or misrouted by your proxy client.
- When in restricted network environments, use standard compliant network proxies and verify built-in Hosts and DNS settings in the app.

### 4. Storage or download access errors?
- On Android 11 and later, due to scoped storage requirements, please grant the app all necessary file management permissions in system settings.
- When selecting custom download directories, use the system document picker to select public Documents or Download folders.

---

## Credits & Heritage

The evolution of EhViewer reflects the dedication of many open-source contributors and pioneers across the years:

### Project Founders & Fork Maintainers
- [seven332 (Hippo)](https://github.com/seven332) — Founder and original creator of EhViewer.
- [NekoInverter](https://github.com/NekoInverter) — Early modernization and architectural contributions.
- [Tarsin Norbin](https://github.com/TarsinNorbin) — Key maintainer during modern EhViewer evolution.
- [FooIbar](https://github.com/FooIbar/EhViewer) — High-performance Compose rewrite and evolution fork.
- [xiaojieonly (SXJ)](https://github.com/xiaojieonly/Ehviewer_CN_SXJ) — Long-term maintenance and rich functional feature additions.

### UI Frameworks & Shader Engines
- [top.yukonga.miuix.kmp](https://github.com/miuix-kotlin-multiplatform/miuix) — Kotlin Multiplatform MIUIX / HyperOS component library.
- [Kyant0/AndroidLiquidGlass](https://github.com/Kyant0/AndroidLiquidGlass) — Liquid glass physical lens and AGSL distance field shader pipeline.
- [InstallerX-Revived](https://github.com/rosan/InstallerX-Revived) (Rosan) — Floating bottom bar damped drag physics and micro-interaction references.

### Open-Source Dependencies
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

## Disclaimer

1. This project is developed strictly for mobile UI/UX exploration, Jetpack Compose declarative paradigms, and physical shader rendering research. It must not be used for any commercial purposes.
2. The application functions as an open-source network client tool and does not bundle, host, or distribute copyrighted media or commercial data. All network interactions are initiated directly by the end user to third-party public network endpoints.
3. Users are responsible for complying with applicable local laws and regulations.

---

## License

This software is distributed under the **GNU General Public License v3.0 (GPLv3)**:

```text
EhViewer is free software: you can redistribute it and/or modify it under the terms of 
the GNU General Public License as published by the Free Software Foundation, 
either version 3 of the License, or (at your option) any later version.

EhViewer is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; 
without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. 
See the GNU General Public License for more details.
```

For the complete license text, refer to the [LICENSE](LICENSE) file in the repository root.

---

## Building from Source (Developers)

To compile the application from source, prepare your development environment as follows:

### Prerequisites
- **JDK**: Version 21 or higher
- **Android SDK**: API Level 35 (Build-tools 35.0.0+)
- **Build System**: Gradle 9.x + Android Gradle Plugin 9.x
- **Recommended IDE**: Android Studio Ladybug (2024.2.1) or later

### Build Instructions

```bash
# 1. Clone repository
git clone https://github.com/137458/ehviewer-miuix.git
cd ehviewer-miuix

# 2. Assemble Default Release APK
./gradlew assembleDefaultRelease

# 3. Output APK location
# app/build/outputs/apk/default/release/app-default-*-release.apk
```
