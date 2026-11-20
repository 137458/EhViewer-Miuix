# EhViewer-Miuix — Agent 约束入口

EhViewer（E-Hentai 阅读器）的 Miuix/HyperOS 风格定制 fork，基于 Kotlin Multiplatform + Compose，Rust 参与原生构建。

## 构建与验证

- 环境：JDK 21（各模块 `jvmToolchain(21)`；仓库根 `.java-version` 为 25，仅决定版本管理器运行 Gradle 所用的 JVM）、Android SDK API 37（`compileSdk = 37`、`targetSdk = 37`、Build-tools 37.0.0）、Gradle 9.7.1 + AGP 9.4.1；NDK 29.0.14206865（仅 `-PbuildNative` 原生构建需要）
- 编译生产包：`./gradlew assembleDefaultRelease`
- 产物：`app/build/outputs/apk/default/release/app-default-*-release.apk`
- 完成标准 = 编译通过。
- 原生库：预编译的 `app/src/main/jniLibs/arm64-v8a/libehviewer.so` 已入库，标准命令直接产出可运行包；`externalNativeBuild` 默认关闭（`-PbuildNative` 才注册 CMake/Rust 构建，Windows 主机需 WSL）。**修改 `app/src/main/rust/` 或 `app/src/main/cpp/` 源码后必须带 `-PbuildNative` 重编并同步提交新 .so**，否则仓库二进制与源码脱钩。构建期由 `checkNativeLibs` task 兜底：.so 缺失时 assemble 直接失败（`-PskipNativeCheck` 豁免）。
- 版本号：`app/build.gradle.kts` 的 `versionName = "1.16.0"`、`versionCode = 180065`；不带 `-Prelease` 时为 `1.16.0-SNAPSHOT`。
- 桌面端（`:desktop`，Compose Desktop）：`./gradlew :desktop:run` 运行，`./gradlew :desktop:desktopFatJar` 产出 `desktop/build/libs/desktop-all.jar`；桌面端列表解析依赖 JNI 库 `ehviewer_rust`，该 dll 未入库（`desktop/src/desktopMain/resources/native/` 被忽略），需先自行构建并 `./gradlew :desktop:copyRustDll` 就位或用 `EHVIEWER_RUST_DLL` 指定，否则抛 `Rust bindings unavailable`。
- 发版流程：本地构建手动上传 GitHub Release，不经 CI。

## 项目约定

- 模块：`:app`（`namespace` 为 `com.hippo.ehviewer`，`default`/`marshmallow` 两变体）、`:core:{common,data,i18n,shell,ui}`、`:desktop`、`:benchmark`；`build-logic` 以 `includeBuild` 提供 `ehviewer.*` 约定插件。
- 上游同步：origin 为 `137458/EhViewer-Miuix`，上游 `FooIbar/EhViewer`（另跟踪 `xiaojieonly/Ehviewer_CN_SXJ`）；从上游合并时注意保留 Miuix/HyperOS 定制层。
- 版本号与 CHANGELOG：条目中文，版本区块无 `v` 前缀（与既有 tag 风格一致）。

## 文档索引

| 何时读 | 文档 |
|---|---|
| 用户文档（多语言） | `docs/README/` |
| 更新日志 | `CHANGELOG.md` |
