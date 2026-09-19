<p align="right">
  <a href="/README.md">
  简体中文
  </a>
  <span> | </span>
  <a href="/docs/README/en.md">
  English
  </a>
  <span> | </span>
  <strong>正體中文</strong>
  <span> | </span>
  <a href="/docs/README/ja.md">
  日本語
  </a>
</p>

<h1 align="center">
  <img src="../images/launcher_icon.svg" width="160" alt="EhViewer MIUIX">
  <br>EhViewer MIUIX<br>
</h1>

<p align="center">
  <a href="https://github.com/137458/EhViewer-Miuix/releases"><img src="https://img.shields.io/github/v/release/137458/EhViewer-Miuix" alt="Release"></a>
  <a href="/LICENSE"><img src="https://img.shields.io/badge/License-GPLv3-blue.svg" alt="LICENSE"></a>
  <a href="https://github.com/137458/EhViewer-Miuix/issues"><img src="https://img.shields.io/github/issues/137458/EhViewer-Miuix" alt="Issues"></a>
  <img src="https://img.shields.io/badge/Platform-Android%208.0%2B-green.svg" alt="Android">
  <img src="https://img.shields.io/badge/Design-MIUIX%20%2F%20HyperOS-FF6900.svg" alt="Design">
  <img src="https://img.shields.io/badge/Language-Kotlin%202.4-7F52FF.svg" alt="Kotlin">
</p>

<div align="center">
  <h3>
    <a href="#專案定位">專案定位</a>
    <span> | </span>
    <a href="#核心特性">核心特性</a>
    <span> | </span>
    <a href="#下載與安裝">下載安裝</a>
    <span> | </span>
    <a href="#系統要求">系統要求</a>
    <span> | </span>
    <a href="#常見問題解答-faq">常見問題</a>
    <span> | </span>
    <a href="#歷史淵源與致謝">致謝名單</a>
    <span> | </span>
    <a href="#許可證">許可證</a>
  </h3>
</div>

---

## 重要聲明與防偽警示

> [!IMPORTANT]
> **官方唯一發布管道**：
> 本應用為純粹的技術交流與興趣開源專案，**有且僅在 GitHub Releases 頁面發布更新**。
> 網際網路上所有自稱「EhViewer 官網」、兜售 VIP 會員、付費購買應用程式、或在第三方下載站上架的所謂「破解版 / 去廣告版」均屬虛假欺詐行為，可能存在惡意後門注入與帳號盜取風險。請廣大使用者認準官方開源倉庫，切勿在非官方管道下載任何安裝包。

---

## 專案定位

**EhViewer MIUIX** 致力於為 Xiaomi HyperOS / MIUI 生態及現代 Android 平台提供極致視覺表現與絲滑互動體驗的高性能 EhViewer 用戶端。

本專案以 Kotlin Multiplatform 與 Jetpack Compose 為架構基礎，深度融合 `top.yukonga.miuix.kmp` 元件生態與底層物理著色器渲染管線，重構了全應用視覺語言與互動元件，呈現純粹的 HyperOS 設計美學。

---

## 核心特性

- **MIUIX 設計體系全面重構**：全量接入原生 MIUIX 設計規範，深度重構頂欄、卡片容器、分組設定項、彈窗對話框與底部抽屜面板，與 HyperOS 系統風格完美融為一體。
- **液態玻璃懸浮底欄 (Floating Bottom Bar)**：
  - **SDF 物理透鏡折射**：基於距離場方程計算物理透鏡屈光與邊緣折射率。
  - **色散彩虹光暈**：模擬真實光學色散（Chromatic Dispersion）與高光反射邊緣。
  - **雙重背景即時取樣**：多圖層 backdrop 即時取樣與高斯模糊合成。
  - **物理阻尼手感**：內建微互動回彈力學模型，支援阻尼滑動切換與重力感應高光傾斜。
- **沉浸式漸進模糊頂欄 (BlurredBar)**：
  - 採用 Xiaomi HyperOS 標準漸進式紋理模糊演算法，狀態欄滿額模糊向下自然衰減至像素級清晰，杜絕生硬切割線條。
  - 協同滾動驅動大標題與小標題平滑轉換，滾動位移自適應淡入淡出。
- **平滑超橢圓曲率 (Squircle Shape)**：
  - 全域卡片、圖片縮圖與控制項統一採用平滑曲率超橢圓幾何演算法，消除傳統圓角的折角生硬感。
- **響應式大螢幕與折疊螢幕適配**：
  - 針對平板、折疊螢幕大螢幕裝置提供自適應響應式版面配置。
  - 在橫螢幕與寬螢幕場景下，底欄自動流轉切換至左側垂直導覽列（NavigationRail），畫廊與清單自適應切換為多列流式排版。
- **硬體著色器相容層 (RuntimeShaderCompat)**：
  - 在 Android 13+ (API 33+) 自動啟用硬體級 AGSL 著色器執行物理折射與複雜模糊。
  - 在低版本系統環境自動平滑降級至高性能原生備用管線，兼顧前沿視覺衝擊與極寬的裝置相容性。
- **完整的畫廊與閱讀體驗**：
  - 支援多執行緒非同步預先載入與平滑縮放。
  - 包含垂直滾動、從左到右、從右到左、條漫模式以及雙頁閱讀等多種模式。
  - 支援高級標籤多維搜尋、分類篩選、以圖搜圖與快速歷史封存。
- **穩定健全的下載與匯出系統**：
  - 支援多任務並行下載、斷點續傳、下載任務智慧重試、自訂儲存目錄與 ZIP / CBZ 封存匯出。

---

## 下載與安裝

請前往 [GitHub Releases](https://github.com/137458/EhViewer-Miuix/releases) 頁面下載最新建置安裝包：

| 產物檔案 | 適用系統 | 架構類型 | 說明與選擇建議 |
|:---|:---|:---|:---|
| `EhViewer-*-default-arm64-v8a.apk` | Android 8.0+ (API 26+) | 64 位元 ARM | **主流機型推薦**。套件名稱後綴 `.miuix`，原生最佳化無多餘相容墊片 |
| `EhViewer-*-default-universal.apk` | Android 8.0+ (API 26+) | 通用架構 | 通用兜底包，當前工程下與 arm64-v8a 二進位及體積完全一致 |
| `EhViewer-*-marshmallow-arm64-v8a.apk` | Android 6.0 ~ 7.1 (API 23+) | 64 位元 ARM | **老舊機型相容版**。套件名稱後綴 `.m`，內建脫糖支援與向下相容層 |
| `EhViewer-*-marshmallow-universal.apk` | Android 6.0 ~ 7.1 (API 23+) | 通用架構 | 老舊系統通用兜底包 |
| `EhViewer-*-mapping.txt` | 全平台通用 | 符號表 | **非安裝包**。R8 混淆對應表，僅供開發者排查崩潰堆疊，使用者無需下載 |

### 安裝與配置建議

1. 下載對應變體的 `.apk` 安裝檔案並在裝置上完成安裝。
2. **後台下載保活建議（HyperOS / MIUI 設備）**：
   - 前往系統「設定」-「應用設定」-「應用管理」-「EhViewer」。
   - 將「省電策略」從「智慧限制」修改為「無限制」，並開啟「自動啟動」權限，避免系統在鎖定螢幕後休眠網路導致批次下載中斷。
3. **視覺效果建議**：
   - 建議在 Android 13 及以上系統使用，即可開啟完整的即時物理透鏡折射與漸進式模糊效果。

---

## 系統要求

- **最低運作要求**：Android 8.0 (API 26) 及以上。
- **推薦運作環境**：
  - Android 13 (API 33) 或更高版本。
  - Xiaomi HyperOS / MIUI 運作環境。
  - 支援 RuntimeShader 硬體級圖形渲染的現代行動晶片。

---

## 常見問題解答 (FAQ)

### 1. 為什麼提示「沒有權限」、「Sad Panda」或無法正常進入畫廊？
- 請檢查是否已正確登入帳號。
- 部分畫廊與里站（ExHentai）資源需要帳號滿足相應存取權限條件。可透過應用程式內的 WebView 網頁登入，或手動匯入具備有效身分驗證憑證的 Cookie。

### 2. 為什麼瀏覽時出現 HTTP 509 錯誤？
- HTTP 509 代表圖像配額耗盡（Image limit exceeded）。
- 這是伺服端對單一 IP 或帳號在固定時間週期內的配額限制，並非用戶端缺陷。可透過更換網路節點、重置網路分配、或稍作等待以恢復每日配額。

### 3. 網路無法正常連線或超時如何處理？
- 請檢查系統代理或網路分流規則，確保 EhViewer 未被代理軟體誤攔截或設定錯誤。
- 若處於特定網路限制環境中，請使用合規的網路代理接入，並在應用程式設定中確認內建 Hosts 與網路解析設定。

### 4. 儲存與下載畫廊讀取失敗？
- 在 Android 11 及以上系統，因系統儲存沙盒限制，請進入系統設定授予應用程式必要的檔案存取管理權限。
- 若需修改下載儲存路徑，建議使用系統檔案選擇器指定公共 Documents 或 Download 目錄。

---

## 歷史淵源與致謝

EhViewer 的發展凝結了開源社群眾多先驅與貢獻者的心血，特此向以下專案與開發者致以崇高的敬意：

### 專案先驅與分支貢獻
- [seven332 (Hippo)](https://github.com/seven332) — EhViewer 專案奠基人與最初締造者。
- [NekoInverter](https://github.com/NekoInverter) — 早期架構現代化維護與演進貢獻。
- [Tarsin Norbin](https://github.com/TarsinNorbin) — 現代 EhViewer 演進關鍵維護者。
- [FooIbar](https://github.com/FooIbar/EhViewer) — 高性能 Compose 架構重塑與演進分支。
- [xiaojieonly (SXJ)](https://github.com/xiaojieonly/Ehviewer_CN_SXJ) — 國內分支長線維護與豐富功能特性貢獻。

### UI 框架與物理著色器核心專案
- [top.yukonga.miuix.kmp](https://github.com/miuix-kotlin-multiplatform/miuix) — 卓越的 Kotlin Multiplatform MIUIX / HyperOS 設計規範元件庫。
- [Kyant0/AndroidLiquidGlass](https://github.com/Kyant0/AndroidLiquidGlass) — 令人驚艷的液態玻璃物理透鏡與 AGSL 距離場著色器管線實現。
- [InstallerX-Revived](https://github.com/rosan/InstallerX-Revived) (Rosan) — 懸浮底欄彈性微互動與阻尼力學設計參考。

### 開源依賴專案
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

## 免責聲明

1. **技術交流與非商業性質**：本專案屬於個人非營利性開源技術探索專案，僅供 Android Jetpack Compose 現代化元件化架構、MIUIX 介面系統規範及底層渲染動效技術的學習與研究之用，嚴禁將本專案及其衍生建置版本用於任何商業牟利或非法經營行為。
2. **純用戶端定位與無內容代管**：本軟體僅作為通用的網路資料解析與展示用戶端工具，專案本身及其維護者不擁有、不營運任何內容伺服器，亦不內建、不提供、不分發、不上傳任何受版權保護的媒體資源或第三方商業數據。軟體運作過程中產生的所有數據檢索、內容解析與檔案下載，均由終端使用者在本地設備自主配置並直連第三方公開網路服務發起。
3. **無官方關聯聲明**：本專案與 E-Hentai.org、ExHentai.org 及其營運團隊不存在任何官方合作、授權認可、商業推廣或從屬關聯。
4. **法律合規與使用者完全責任自負**：使用者在下載、安裝及使用本軟體時，必須嚴格遵守所在國家與地區的現行法律法規與公序良俗。因使用者自行檢索、瀏覽、下載、傳播違規或敏感內容，或以任何不符合當地法律法規的方式使用本軟體而導致的一切法律責任、行政處罰、民事糾紛或財產損失，均由使用者本人完全自行承擔。本專案開發者與開源貢獻者概不承擔任何直接、間接、附帶、特殊、衍生或連帶法律責任。
5. **按「現狀」提供與無明示/默示擔保（AS-IS）**：根據 GNU General Public License v3.0 協議規定，本軟體按「現狀」（AS-IS）分發，不提供任何明示或暗示的擔保（包括但不限於對適銷性、特定用途適用性、系統相容性及無侵權之擔保）。開發者不對軟體運作的持續性、穩定性、資料完整性及第三方介面的可用性作任何承諾。
6. **智慧財產權與商標**：專案中涉及或引用的第三方名稱、商標、服務標記及介面規範，其權利均歸屬於各自合法的權利人所有。

---

## 許可證

本專案基於 GPLv3 許可證衍生開發，遵循 **GNU General Public License v3.0 (GPLv3)** 開源許可證分發：

```text
EhViewer MIUIX - Modern EhViewer client rebuilt with MIUIX and Jetpack Compose.
Copyright (C) 2024-2026 137458, EhViewer MIUIX contributors
Based on EhViewer:
Copyright (C) 2014-2023 Hippo, EhViewer contributors
Copyright (C) 2023-2024 FooIbar

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.
```

完整協議內容請參閱倉庫根目錄下的 [LICENSE](LICENSE) 檔案。

---

## 原始碼建置（面向開發者）

如果你是一名 Android 開發者並希望從原始碼編譯建置本專案，請遵循以下流程：

### 前置環境要求
- **JDK**：21 及以上
- **Android SDK**：API Level 35 (Build-tools 35.0.0+)
- **建置工具**：Gradle 9.x + Android Gradle Plugin 9.x
- **推薦 IDE**：Android Studio Ladybug (2024.2.1) 或更高版本

### 編譯步驟

```bash
# 1. 複製程式碼倉庫
git clone https://github.com/137458/EhViewer-Miuix.git
cd EhViewer-Miuix

# 2. 編譯 Default Release 生產包
./gradlew assembleDefaultRelease

# 3. 產物輸出位置位於
# app/build/outputs/apk/default/release/app-default-*-release.apk
```
