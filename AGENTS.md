# EhViewer-Miuix — Agent 约束入口

EhViewer（E-Hentai 阅读器）的 Miuix/HyperOS 风格定制 fork，基于 Kotlin Multiplatform + Compose，Rust 参与原生构建。

## 构建与验证

- 环境：JDK 21+、Android SDK API 35（Build-tools 35.0.0+）、Gradle 9.x + AGP 9.x
- 编译生产包：`./gradlew assembleDefaultRelease`
- 产物：`app/build/outputs/apk/default/release/app-default-*-release.apk`
- 完成标准 = 编译通过。
- 原生库：预编译的 `app/src/main/jniLibs/arm64-v8a/libehviewer.so` 已入库，标准命令直接产出可运行包；`externalNativeBuild` 默认关闭（`-PbuildNative` 才注册 CMake/Rust 构建，Windows 主机需 WSL）。**修改 `app/src/main/rust/` 或 `app/src/main/cpp/` 源码后必须带 `-PbuildNative` 重编并同步提交新 .so**，否则仓库二进制与源码脱钩。构建期由 `checkNativeLibs` task 兜底：.so 缺失时 assemble 直接失败（`-PskipNativeCheck` 豁免）。
- 发版流程：本地构建手动上传 GitHub Release，不经 CI。

## 项目约定

- 上游同步：origin 为 `137458/EhViewer-Miuix`，上游 `FooIbar/EhViewer`（另跟踪 `xiaojieonly/Ehviewer_CN_SXJ`）；从上游合并时注意保留 Miuix/HyperOS 定制层。
- 版本号与 CHANGELOG：条目中文，版本区块无 `v` 前缀（与既有 tag 风格一致）。

## 文档索引

| 何时读 | 文档 |
|---|---|
| 用户文档（多语言） | `docs/README/` |
| 更新日志 | `CHANGELOG.md` |
