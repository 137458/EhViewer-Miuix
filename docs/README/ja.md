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
  <strong>日本語</strong>
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
    <a href="#プロジェクト概要">概要</a>
    <span> | </span>
    <a href="#主な機能">機能</a>
    <span> | </span>
    <a href="#ダウンロードとインストール">ダウンロード</a>
    <span> | </span>
    <a href="#システム要件">要件</a>
    <span> | </span>
    <a href="#よくある質問-faq">FAQ</a>
    <span> | </span>
    <a href="#謝辞と沿革">謝辞</a>
    <span> | </span>
    <a href="#ライセンス">ライセンス</a>
  </h3>
</div>

---

## 重要なお知らせ・偽造防止に関する警告

> [!IMPORTANT]
> **公式唯一の配布チャネル**：
> 本アプリは純粋な技術交流と趣味によるオープンソースプロジェクトであり、**更新は GitHub Releases ページでのみ配布されています**。
> インターネット上で「EhViewer 公式サイト」を騙り、VIP 会員の販売、有料ダウンロード、またはサードパーティ製配布サイトでの「クラック版 / 広告削除版」の配布はすべて詐欺行為であり、悪意あるバックドアやアカウント盗難のリスクがあります。必ず公式オープンソースリポジトリを確認し、非公式のチャネルからインストールパッケージをダウンロードしないでください。

---

## プロジェクト概要

**EhViewer MIUIX** は、Xiaomi HyperOS / MIUI エコシステムおよびモダンな Android プラットフォーム向けに、極上の視覚表現と滑らかな操作体験を提供することを目指した高性能 EhViewer クライアントです。

Kotlin Multiplatform と Jetpack Compose を基盤とし、`top.yukonga.miuix.kmp` コンポーネントエコシステムと物理シェーダーレンダリングパイプラインを深く統合。アプリ全体のビジュアル言語とインタラクションコンポーネントを再構築し、純粋な HyperOS デザイン美学を体現しています。

---

## 主な機能

- **MIUIX デザインシステムの全面再構築**：ネイティブな MIUIX デザインガイドラインに完全準拠し、トップバー、カードコンテナ、グループ化された設定項目、ダイアログ、ボトムシートを刷新。HyperOS システムのスタイルとシームレスに調和します。
- **リキッドガラス浮動ボトムバー (Floating Bottom Bar)**：
  - **SDF 物理レンズ屈折**：符号付き距離場（SDF）方程式に基づき、物理レンズの屈折率とエッジの光学的歪みをリアルタイム計算。
  - **色散（分散）レインボーハロー**：実際の色分散（Chromatic Dispersion）とハイライト反射エッジを忠実にシミュレート。
  - **デュアルバックドロップサンプリング**：マルチレイヤーの背景リアルタイムサンプリングとガウスブラーの高度な合成。
  - **物理ダンピング操作感**：マイクロインタラクションのスプリング力学モデルを内蔵し、減衰ドラッグ切り替えとジャイロ傾きによるハイライト反射に対応。
- **没入型プログレッシブブラートップバー (BlurredBar)**：
  - Xiaomi HyperOS 標準のプログレッシブテクスチャブラーアルゴリズムを採用し、ステータスバーの完全なぼかしから下方のピクセルクリアなコンテンツへと自然にグラデーション減衰。
  - スクロールに連動したタイトルヘッダーのスムーズな拡大縮小とフェードイン・フェードアウト。
- **超楕円曲率 (Squircle Shape)**：
  - アプリ全体のカード、画像サムネイル、UI コントロールに連続曲率超楕円アルゴリズムを採用し、従来の角丸の不自然な折れ目を排除。
- **タブレット・折りたたみ端末へのレスポンシブ適応**：
  - 大画面デバイス向けの適応型レスポンシブレイアウト。
  - 横向き画面や大画面では、ボトムバーが自動的に左側の垂直ナビゲーションレール（NavigationRail）に移行し、ギャラリーやリストもマルチカラムグリッドへ最適化。
- **ハードウェアシェーダー互換レイヤー (RuntimeShaderCompat)**：
  - Android 13+ (API 33+) ではハードウェアアクセラレーション AGSL シェーダーを自動適用し、物理屈折や高度なぼかしを実現。
  - 以前のバージョンの Android では、最適化されたネイティブグラフィックパイプラインへ自動的かつスムーズにフォールバック。
- **充実したギャラリー＆読書体験**：
  - マルチスレッド非同期プリロードと滑らかな拡大・縮小。
  - 垂直スクロール、左から右、右から左、縦スクロール（Webtoon）モード、見開き表示モードに対応。
  - 高度なタグ検索、カテゴリフィルタリング、画像逆引き検索、検索履歴のクイック管理。
- **堅牢なダウンロードとアーカイブ管理**：
  - マルチタスク並行ダウンロード、レジューム機能、スマートリトライロジック、カスタム保存先、ZIP / CBZ アーカイブ出力に対応。

---

## ダウンロードとインストール

[GitHub Releases](https://github.com/137458/EhViewer-Miuix/releases) ページより最新のビルドパッケージをダウンロードしてください：

| フレーバー (Flavor) | 最小 Android バージョン | アーキテクチャ | 特徴 |
|:---|:---|:---|:---|
| **Default** | Android 8.0 (API 26) | arm64-v8a / Universal | MIUIX デザインシステムおよびシェーダーパイプラインを完全サポート（推奨） |
| **Marshmallow** | Android 6.0 (API 23) | 汎用アーキテクチャ | 旧端末互換用ビルド、一部の高度なアニメーションは自動的にフォールバック |

### インストールと設定のアドバイス

1. 端末に対応する `.apk` ファイルをダウンロードしてインストールします。
2. **バックグラウンドダウンロードの維持（HyperOS / MIUI 端末）**：
   - 端末の「設定」-「アプリ」-「アプリ管理」-「EhViewer」へ移動します。
   - 「バッテリーセーバー」を「制限なし」に変更し、「自動起動」を有効にすることで、画面ロック時のネットワークスリープによるダウンロード中断を防ぎます。
3. **視覚効果の推奨事項**：
   - フルハードウェア AGSL レンズ屈折およびプログレッシブブラー効果を十分に体感いただくため、Android 13 以上の環境での使用を推奨します。

---

## システム要件

- **最小動作要件**：Android 8.0 (API 26) 以上。
- **推奨動作環境**：
  - Android 13 (API 33) 以上。
  - Xiaomi HyperOS / MIUI 動作環境。
  - RuntimeShader ハードウェアレンダリングをサポートするモダンなモバイルプロセッサ。

---

## よくある質問 (FAQ)

### 1. 「権限がありません」「Sad Panda」が表示される、またはギャラリーを開けないのはなぜですか？
- アカウントに正しくログインしているか確認してください。
- 一部のギャラリーや ExHentai コンテンツの閲覧には、適切なアクセス権限を持つアカウントが必要です。アプリ内蔵の WebView からログインするか、有効な認証 Cookie を手動でインポートしてください。

### 2. 閲覧中に HTTP 509 エラーが表示される原因は何ですか？
- HTTP 509 は画像クォータ超過（Image limit exceeded）を示しています。
- これはサーバー側が特定の IP アドレスまたはアカウントに対して一定期間内に設けている制限であり、クライアントの不具合ではありません。ネットワークノードの切り替え、IP の再割り当て、またはクォータのリセットをお待ちください。

### 3. ネットワークに正常に接続できない、またはタイムアウトする場合は？
- システムのプロキシやルーティング設定を確認し、EhViewer がプロキシクライアントによって誤ってブロックされたり誤ルーティングされていないか確認してください。
- 規制されたネットワーク環境では、適切なネットワークプロキシを使用し、アプリ設定の内蔵 Hosts および DNS 設定を確認してください。

### 4. ストレージアクセスやダウンロードギャラリーの読み取りに失敗します
- Android 11 以降のストレージ分離制限（Scoped Storage）に対応するため、システム設定でアプリに必要なすべてのファイル管理権限を許可してください。
- ダウンロード保存先を変更する場合は、システムのドキュメントピッカーを使用してパブリックな Documents または Download フォルダを指定することをお勧めします。

---

## 謝辞と沿革

EhViewer の発展は、オープンソースコミュニティの先駆者や貢献者の長年の尽力によるものです。以下のプロジェクトと開発者に深く敬意を表します：

### プロジェクト先駆者および派生版貢献者
- [seven332 (Hippo)](https://github.com/seven332) — EhViewer プロジェクトの創設者・初代開発者。
- [NekoInverter](https://github.com/NekoInverter) — 初期アーキテクチャの近代化および発展への貢献。
- [Tarsin Norbin](https://github.com/TarsinNorbin) — 現代 EhViewer への進化を支えた主要メンテナー。
- [FooIbar](https://github.com/FooIbar/EhViewer) — 高性能 Compose アーキテクチャの再構築および派生版の開発。
- [xiaojieonly (SXJ)](https://github.com/xiaojieonly/Ehviewer_CN_SXJ) — 長期メンテナンスと豊富な新機能の貢献。

### UI フレームワーク＆物理シェーダープロジェクト
- [top.yukonga.miuix.kmp](https://github.com/miuix-kotlin-multiplatform/miuix) — 卓越した Kotlin Multiplatform MIUIX / HyperOS デザインコンポーネントライブラリ。
- [Kyant0/AndroidLiquidGlass](https://github.com/Kyant0/AndroidLiquidGlass) — 驚異的なリキッドガラス物理レンズおよび AGSL 距離場シェーダーパイプラインの実装。
- [InstallerX-Revived](https://github.com/rosan/InstallerX-Revived) (Rosan) — 浮動ボトムバーの減衰力学およびマイクロインタラクションの設計参照。

### オープンソース依存プロジェクト
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

## 免責事項

1. **技術交流および非営利目的**：本プロジェクトは個人の非営利オープンソース技術研究プロジェクトであり、Android Jetpack Compose の宣言型 UI アーキテクチャ、MIUIX インターフェース仕様、およびグラフィックシェーダー技術の学習・研究のみを目的としています。営利目的や不正な商用利用は固く禁止します。
2. **純粋なクライアント仕様・コンテンツ非保持**：本ソフトウェアは汎用的なネットワークデータ解析・閲覧クライアントツールとしてのみ動作します。プロジェクトおよびメンテナーはコンテンツサーバーを保有・運用しておらず、著作権で保護されたメディアコンテンツや商用データを一切内包、提供、配布、アップロードしません。データの取得・解析・ダウンロードはすべてエンドユーザー自身の操作により、外部の公開ネットワークサービスに対して直接行われます。
3. **公式との非関連性**：本プロジェクトは、E-Hentai.org、ExHentai.org およびその運営チームとの公式な提携、承認、スポンサーシップ、従属関係は一切ありません。
4. **法令順守およびユーザーの自己責任**：ユーザーは本ソフトウェアの利用にあたり、所在国・地域の関連法令および公序良俗を遵守する全責任を負います。違法またはセンシティブなコンテンツの検索、閲覧、ダウンロード、共有に起因するいかなる法的責任、行政処分、民事紛争、損害についても、ユーザー本人が単独で責任を負うものとします。プロジェクト開発者および貢献者はいかなる直接的・間接的・派生的な法的責任も負いません。
5. **現状有姿（AS-IS）の提供と無保証**：GNU General Public License v3.0 に基づき、本ソフトウェアは「現状有姿」（AS-IS）で提供され、商品性、特定目的への適合性、非侵害性を含む明示・黙示のいかなる保証も行いません。ソフトウェアの稼働継続性、安定性、データ保全性、外部 API の利用可能性について一切の責任を負いません。
6. **知的財産権と商標**：本リポジトリ内で言及されているサードパーティの名称、商標、サービスマーク、インターフェース仕様の権利は、すべて各権利者に帰属します。

---

## ライセンス

本プロジェクトは GPLv3 ライセンスに基づいて開発されており、**GNU General Public License v3.0 (GPLv3)** に基づいて配布されています：

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

完全なライセンステキストは、リポジトリのルートにある [LICENSE](LICENSE) ファイルをご参照ください。

---

## ソースコードからのビルド（開発者向け）

ソースコードからアプリケーションをコンパイルする場合の手順は以下の通りです：

### 事前環境要件
- **JDK**：21 以上
- **Android SDK**：API Level 35 (Build-tools 35.0.0+)
- **ビルドツール**：Gradle 9.x + Android Gradle Plugin 9.x
- **推奨 IDE**：Android Studio Ladybug (2024.2.1) 以降

### コンパイル手順

```bash
# 1. リポジトリのクローン
git clone https://github.com/137458/EhViewer-Miuix.git
cd EhViewer-Miuix

# 2. Default Release APK のビルド
./gradlew assembleDefaultRelease

# 3. ビルド成果物の出力先
# app/build/outputs/apk/default/release/app-default-*-release.apk
```
