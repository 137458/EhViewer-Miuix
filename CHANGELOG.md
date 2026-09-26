# 更新日志

## [未发布]

### 修复

- 修复了检查更新在无 CI 记录或快照构建下抛出数组越界异常崩溃的问题：优化 CI 工作流解析容错并支持安全回退至官方 Release 通道。
- 修复了检查更新资产匹配可能误选混淆映射表文本文件而非安装包的问题：限定仅匹配 APK 安装包并优先使用官方直链下载。
- 修复了更新安装包在网络波动或跨国下载时因硬性超时看门狗导致下载中断失败的问题：改用自适应流式传输与长连接超时配置。
- 修复了在非 Activity 协程作用域拉起未知来源安装权限时抛出异常导致已完成下载被覆盖报错的问题。
- 修复了当应用已处于最新版本时检查更新页面未展示版本说明的问题：支持在最新版本状态下常驻呈现当前版本说明与完整更新特性。
- 修复了设备旋转至横屏时导航宿主被条件分支卸载导致闪退崩溃的问题：优化主界面导航层级挂载结构，确保屏幕旋转与宽屏切换时路由注册表与返回栈平稳持久化。
- 修复了横屏手机被误判为宽屏导致侧边导航栏竖排条目溢出、被裁切且无法点击的问题：宽屏判据改为宽高同时达到 600dp，横屏手机保留悬浮底栏。
- 修复了侧边导航栏在可用高度不足时条目被裁切的问题：改用可滚动侧栏并把应用图标移入侧栏头部槽位，同时避免与侧栏内部窗口内边距重复消费。
- 修复了横屏矮视口下日历与时钟选择器、圆形选择弹层超出窗口被裁切的问题：横屏改用紧凑输入模式并按可用高度收缩圆形布局。
- 修复了横屏下筛选面板、更新弹窗、以图搜图页内容超出视口被裁切的问题：补充纵向滚动兜底。
- 修复了顶栏折叠阈值未计入分类条高度导致标题栏无法完全折叠、顶部长期残留空白的问题。
- 修复了横屏下分类条未消费水平内边距而被刘海或侧边导航栏遮挡的问题。
- 修复了阅读器顶栏与底栏在横屏下占用近半屏高度、设置面板分页左右不对称、页码贴屏幕下沿的问题。
- 修复了阅读器横屏全屏时页面铺到侧边刘海或导航栏下方的问题。
- 修复了阅读器横屏缩放开关在屏幕旋转后不重新计算的问题。
- 修复了侧栏抽屉在横屏下覆盖近整屏宽度，以及侧栏标签与收藏列表重复消费底部内边距造成双倍留白的问题。
- 修复了展开搜索栏时筛选面板因嵌套纵向滚动抛出异常崩溃的问题：滚动兜底移至底部弹层，列表项内不再嵌套滚动容器。
- 修复了详情列表在宽屏下仍可能退化为单列满宽的问题：列宽改为按网格实际可用宽度并扣除列间距计算。
- 修复了更新说明产物表格在可用宽度内仍残留横向滚动的问题：可用宽度改为扣除容器与数据行的水平内边距。
- 修复了手机端底部导航栏只有 6 项导致「历史记录」完全没有入口、「排行榜」只能靠设置启动页进入的问题：在下载页溢出菜单补充历史记录入口，在热门页顶栏补充排行榜入口。
- 修复了下载页网格模式下长按改为进入多选后画廊详情页彻底无法进入的问题：恢复缩略图点击进入详情，并补上网格项的选中指示。
- 修复了下载页网格模式多选后看不到任何选中反馈的问题：网格项改用与列表一致的选中指示器。
- 修复了画廊详情页宽屏分支预览列数反而少于竖屏的问题：改用按可用宽度计算的固定列数，不再因自适应最小列宽取整而少排一列。
- 修复了评论区在需要补拉画廊详情且拉取失败时永久停在加载指示器、既无错误提示也无法重试的问题。
- 修复了设备存储整理在其中一个数据库整理失败时仍上报成功的问题：改为两个数据库都成功才算成功。
- 修复了「在刘海屏区域显示内容」在横屏下失效的问题：该开关不再消费刘海内边距，仅保留对侧边导航栏的避让。
- 修复了列表加载失败后点「重试」按钮会永久变灰无法再次重试的问题。
- 修复了预览图加载失败时下拉刷新可在刷新进行中重复触发的问题。
- 修复了设置页滑块拖动过程中每一帧都写入偏好设置的问题：改为松手时才写回，并恢复步进吸附。
- 修复了设置页下拉菜单不显示当前选中项的问题。
- 修复了收藏备注输入框丢失「已用字节数 / 上限」计数提示的问题。
- 修复了展示身份 Cookie 的弹窗失去防截图保护的问题：重新为该弹窗窗口加上安全标志。
- 修复了检查更新在接口限流或网络失败时被当成「已是最新版本」而静默失败的问题：失败改为向上抛出并提示用户。
- 修复了发布页只有映射表等非安装包资源时仍提示有更新、点击下载却拿到空地址的问题：仅回落匹配 APK 资源，无可用安装包时不提示更新。
- 修复了点击「安装」在未知来源安装权限被拒时毫无反应的问题：改为显式报错。
- 修复了下载队列在异常路径下提前推进、可能跳过一个等待任务的问题。
- 修复了下载前台服务启动失败时所有异常都被吞掉、服务在未进入前台的情况下继续运行的问题。
- 修复了阅读器页面自动重试额度用尽后无法再自动恢复的问题：页面被外部重新请求或重新就绪时重置额度。
- 修复了偏好设置「写入后立刻读取」仍读到旧值的问题：本地写入缓存改为优先于异步快照。
- 修复了偏好文件损坏或不可读时后台收集器崩溃、导致此后所有设置读取永久过期的问题。
- 修复了已登录状态查询在偏好设置初始化线程同步写盘并回写 exhentai Cookie 的问题：查询改为纯读取，Cookie 同步移至启动与登录流程。
- 修复了数据库文档（SAF）读取到文件末尾时可能陷入无限循环的问题：按 Okio 约定将文件末尾映射为 -1。
- 修复了数据库文档写入可能发生短写而静默截断数据的问题：改为循环写满并在失败时显式报错。

### 优化

- 优化了更新说明产物表格在多端与横屏下的自适应排版：规范表头与各数据行按列宽严格垂向对齐，基于容器可用宽度智能计算最小列宽与横向伸展，支持竖屏平滑横向滚动与横屏卡片全宽铺展。
- 优化了更新页面与更新弹窗在横屏下的布局比例：横屏状态下自动收缩 Hero 图标并调整更新日志视口高度，避免大幅留白与视口溢出。
- 优化了阅读器翻页预加载与解码流水线：引入视口邻近页智能后台预解码机制，当页面在磁盘就绪或预加载落盘时，自动在后台提前完成紧邻后 1~2 页的位图解码并置入内存缓存，翻页瞬间直接命中就绪状态，彻底消除翻页转圈菊花等待。
- 优化了在线阅读模式下的 Token 获取流水线：当 Token 已缓存在内存中时免除全局锁与 1000ms 强制排队延迟，释放后台预加载的真实并发带宽。
- 优化了阅读器视口页面缓冲深度：平滑扩充 Compose 视口预保留页数，与后台预解码机制无缝衔接。
- 优化了全应用横屏与宽屏响应式策略：统一由窗口宽高判定导航形态、内容可读宽度与网格列数，消除多套断点并存导致的横屏表现不一致。
- 优化了单栏页面在横屏下的排版：设置、历史、评论、许可与以图搜图等页面统一限制可读宽度，避免内容被拉伸到接近整屏宽。
- 优化了列表在横屏下的列数：缩略图瀑布流按可用宽度补足列数，详情列表保证至少两列，不再退化为单列满宽。
- 优化了登录页在横屏下的排版：登录卡按统一可读宽度呈现，两栏布局按可用空间均分，消除大幅留白与横向溢出。
- 优化了画廊详情页在横屏下的排版：去掉头部信息卡的弹性空白间隔，宽屏分支真正按可用宽度排布更多预览列。
- 升级了 Miuix 组件库至 v0.9.4 正式版：同步适配侧边导航栏固定紧凑布局接口与选中态图标背景呈现。

## [1.15.0] - 2026-09-19

### 新增

- 支持了上下文以图搜图：在阅读器长按页面底栏与画廊详情页封面中接入以图搜图入口，支持直接利用本地缓存或图片特征快速唤起以图搜图结果。
- 添加了深度存储清理功能：在高级设置中支持一键清理缩略图缓存、临时文件与崩溃日志，并自动清除本地文件已失效的下载记录和整理数据库释放磁盘空间。
- 支持了 ComicInfo v2.1 元数据导出规范：新增漫画阅读流向、年龄分级以及画廊摘要等元数据字段，深度兼容主流漫画管理与阅读软件。
- 添加了首页横向分类胶囊过滤栏与常驻筛选入口：支持一键快速切换画廊分类，并在列表滑动时动态将顶栏搜索框收缩折叠为紧凑图标。
- 添加了首页悬浮操作胶囊：整合随机漫游、极速刷新、页码与日期快速跳转等常用功能。
- 添加了全新的检查更新页面与更新弹窗：全面对齐 HyperOS / Miuix 设计规范，支持更新日志展示、实时下载速率与进度显示、更新通道管理与版本忽略机制。
- 支持了手动 Cookie 登录：支持从剪贴板智能解析整段 Cookie，亦支持手动填写多字段校验登录。
- 支持了全面适配 HyperOS 与 Miuix 设计语言的全新用户界面：全面升级主页、画廊详情、阅读器、评论区、设置与弹窗交互。
- 添加了全新品牌视觉风格：适配科技蓝与天水蓝主题图标与 Android TV 横幅。

### 优化

- 优化了阅读器网络通道并发调度：引入双层信号量机制，为用户当前浏览页面保留专属网络槽位，防止后台预加载任务占满连接导致当前页饥饿卡顿。
- 优化了图床节点测速与容差重试机制：增加冷启动免测宽限期与指数退避重试延时，减少网络瞬时抖动导致的加载失败。
- 优化了缩略图磁盘缓存策略：根据设备可用存储空间自适应动态分配缓存上限，提升高分辨率设备上的列表滑动流畅度。
- 优化了全局界面视觉质感：全面升级搜索栏、分类筛选条、悬浮底栏与阅读器控制栏为液态玻璃悬浮质感，呈现精致的高光边缘与物理透镜折射效果。
- 优化了设置项读取与持久化性能：引入内存快照缓存与轻量异步刷新，彻底消除界面滑动与渲染时的卡顿掉帧隐患。
- 优化了全应用的无障碍交互与读屏支持：为所有操作图标补充屏幕阅读器标签，并规范滑动删除反馈与点击区域。
- 优化了全应用网页界面与辅助页面排版：评论区、进度页、关于页与网页登录界面全面对齐响应式布局。
- 优化了关于页面的更新设置层级：移除了关于设置中与更新中心重复的选项开关，更新策略统一收敛至更新页面管理。

### 修复

- 修复了阅读器当前浏览页可能无法加载、卡在空白占位或显示错误的问题：纠正解码与下载被手势或视口切出取消时误标为失败的状态异常，支持回收位图自愈重载与当前视口页自动落焦激活。
- 修复了删除历史记录时可能误删已下载或已收藏画廊记录的问题，并保证数据清理的原子性与一致性。
- 修复了下载设置检查目录时可能误删空目录的问题。
- 修复了后台下载与解析队列在特定并发场景下可能出现的卡死、状态不同步或异常崩溃问题。
- 修复了 Android 14 及更高版本上前台下载服务启动时偶发的兼容性崩溃问题。
- 修复了生成压缩包归档时可能发生的文件句柄耗尽与闪退问题。
- 修复了超大网络响应体导致缓冲区溢出引发的界面冻结与 CPU 忙等异常。
- 修复了特定网络环境下下载遇到带宽配额超限时的异常识别问题。
- 修复了阅读器在图片加载失败时偶发的高频重复重试循环问题。
- 修复了阅读器在极端加载失败场景下缺少退出出口的问题。
- 修复了阅读器状态栏图标在特定深浅背景下辨识度不足的问题。
- 修复了搜索页与筛选弹窗中分类标签展示不全、历史搜索排版重叠以及键盘弹出时遮挡内容的问题。
- 修复了检查更新弹窗在点击后台下载后任务中断、版本比对误判以及部分机型上打开更新页面闪退的问题。
- 修复了网页登录过程中偶发的页面挂起、Cookie 解析异常崩溃以及清除凭据后未同步保存的问题。
- 修复了悬浮底栏遮挡列表尾部内容与提示信息、浅色模式下指示条辨识度不足、以及重复点击导航项时的动画错位问题。
- 修复了宽屏与横屏模式下侧边导航栏未避让摄像头挖孔和状态栏安全区的问题。
- 修复了启动目的地设置为排行榜或历史记录时主底部导航栏隐藏且无法切换的问题。
- 修复了页面过渡时毛玻璃采样偶现黑边、渲染死循环崩溃以及顶栏搜索框与状态栏重叠的问题。
- 修复了下载页面网格模式下长按无法进入批量多选的问题。
- 修复了下载标签拖动排序和删除特定标签时可能触发的闪退问题。
- 修复了画廊列表重试按钮防连击控制缺失、以及应用后台恢复时画廊评论页偶发的空指针异常。
- 修复了设置页面在大标题模式下的双重标题重叠与关于页面配置项重复的问题。


## [1.14.6] - 2025-12-17

### 新功能

* 支持显示画廊阅读进度（于 EH 设置中开启） [#2277](https://github.com/FooIbar/EhViewer/issues/2277)

### 改进

* 画廊列表变化时重新显示悬浮操作按钮 [#2681](https://github.com/FooIbar/EhViewer/issues/2681)
* 移除文件名中的尾随空格和点以兼容 Windows [#2727](https://github.com/FooIbar/EhViewer/issues/2727)
* 长按画廊详情中的标签时显示标签名称 [#2769](https://github.com/FooIbar/EhViewer/issues/2769)
* 杂项优化

### Bug 修复

* 对话框中的下拉菜单显示错位
* HTTP 状态错误显示为“什么都没有找到”
* 某些老设备无法通过 Cloudflare 验证 [#2794](https://github.com/FooIbar/EhViewer/issues/2794)
* 杂项修复


## [1.14.5] - 2025-10-19

### 改进

* [阅读器] 显示画廊标题
* [阅读器] 显示菜单时改变标题栏颜色

### Bug 修复

* 横屏模式下打开阅读器设置会闪退 [#2673](https://github.com/FooIbar/EhViewer/issues/2673)
* 强制横屏时竖直拿取设备加载图片会横竖屏乱跳 [#2674](https://github.com/FooIbar/EhViewer/issues/2674)


## [1.14.4] - 2025-10-12

### 改进

* [阅读器] 支持禁用使用音量键翻页 [#2593](https://github.com/FooIbar/EhViewer/issues/2593)
* 视搜索警告为错误 [#2604](https://github.com/FooIbar/EhViewer/issues/2604)
* 余额不足时停止下载并显示通知 [#2479](https://github.com/FooIbar/EhViewer/issues/2479)
* [阅读器] 使用默认阅读模式时对带有 webtoon 标签的画廊自动使用条漫模式 [#2632](https://github.com/FooIbar/EhViewer/issues/2632)
* 优化按页数排序下载项目的速度
* 开启显示标签投票状态时为 power >= 100 的标签添加下划线
* 杂项优化

### Bug 修复

* Android 16 QPR1 上画廊详情中的标签文本对比度不足
* 初始化 DownloadManager 用时过长导致 ANR [#2588](https://github.com/FooIbar/EhViewer/issues/2588)
* 某些设备上无法下载名称较长的画廊 [#2607](https://github.com/FooIbar/EhViewer/issues/2607)
* 评论区链接实际可点击区域比画面显示上的略长 [#2615](https://github.com/FooIbar/EhViewer/issues/2615)
* 主页快速搜索再次打开出现以前的画廊重复出现在最上层 [#2628](https://github.com/FooIbar/EhViewer/issues/2628)
* 非条漫模式下原始大小较小的动图不播放 [#2652](https://github.com/FooIbar/EhViewer/issues/2652)
* Android 9 以下打开评论区切屏后回来会闪退 [#2655](https://github.com/FooIbar/EhViewer/issues/2655)
* 杂项修复


## [1.14.3] - 2025-09-01

### 新功能

* 支持以 label: 语法搜索下载标签

### Bug 修复

* [阅读器] 关闭滤镜设置时崩溃 [#2573](https://github.com/FooIbar/EhViewer/issues/2573)
* 编辑评论时上下文菜单缺少格式化选项
* 某些情况下搜索相似画廊时出错


## [1.14.2] - 2025-08-16

### 改进

* [下载] 支持将最低下载速度设为 0 [#2545](https://github.com/FooIbar/EhViewer/issues/2545)
* [下载] 移除令人困惑的连接超时设置 [#2556](https://github.com/FooIbar/EhViewer/issues/2556)

### Bug 修复

* 下载速度计算不准确导致下载失败误报 [#2545](https://github.com/FooIbar/EhViewer/issues/2545)
* 无法解析带有 location: 标签的画廊


## [1.14.1] - 2025-07-31

### 改进

* 更新 GitHub 访问令牌


## [1.14.0] - 2025-07-23

### 重大变更

* 默认隐藏分数为 -100 的评论
* 迁移部分组件到 Material 3 Expressive

### 新功能

* [EH] 支持显示标签投票状态
* 支持添加画廊标签时自动补全
* [下载] 支持指定连接超时和最低响应速度 [#1915](https://github.com/FooIbar/EhViewer/issues/1915)
* [阅读器] 支持反转物理按键控制 [#1962](https://github.com/FooIbar/EhViewer/issues/1962)
* [高级] 支持禁用 QUIC 支持
* [高级] 重新添加桌面版网站选项以绕过 Cloudflare [#2230](https://github.com/FooIbar/EhViewer/issues/2230)
* 支持 Android 6.0 上的动画 WebP

### 改进

* 优化压缩包加载性能
* 优化预加载策略
* 更新日本语翻译
* 为已选中的项目使用不同的颜色 [#2121](https://github.com/FooIbar/EhViewer/issues/2121)
* 缩略图模式下显示已收藏图标 [#2128](https://github.com/FooIbar/EhViewer/issues/2128)
* 迁移大部分解析器到 Rust 实现
* 优化动画 WebP 解码性能 [#2415](https://github.com/FooIbar/EhViewer/issues/2415)
* 优化标签自动补全命中率 [#2473](https://github.com/FooIbar/EhViewer/issues/2473)
* 在状态栏后绘制半透明背景以提高对比度
* 杂项优化

### Bug 修复

* 退出阅读器时崩溃
* Android 8.0 以下的设备无法读取某些压缩包
* 列表模式下点击画廊封面无法进入详情页
* 点击某些评论时崩溃
* 开启裁剪边缘时某些图片被不正确裁剪
* 下载数较多时无网络时启动 app 时崩溃 [#2041](https://github.com/FooIbar/EhViewer/issues/2041)
* 标签翻译数据无法更新 [#2075](https://github.com/FooIbar/EhViewer/issues/2075)
* 画廊详情页面显示的收藏夹名称错误 [#2081](https://github.com/FooIbar/EhViewer/issues/2081)
* 不使用 CI 频道时无法更新
* 添加后移动快捷搜索闪退 [#2124](https://github.com/FooIbar/EhViewer/issues/2124)
* 某些数据无法导入
* App 位于后台时清除下载通知时崩溃
* 杂项修复


## [1.13.1] - 2024-11-11

### 重大变更

* 由于网站限制，移除搜索封面功能
* 移除桌面版网站选项，默认使用 1280x 分辨率

### 改进

* 更新日本语翻译
* 优化压缩包读取
* 缓解某些小米系统上应用闪退 [#1826](https://github.com/FooIbar/EhViewer/issues/1826)
* 杂项优化

### Bug 修复

* Android 9 上的下载失败误报 [#1914](https://github.com/FooIbar/EhViewer/issues/1914)


## [1.13.0] - 2024-11-01

### 重大变更

* 不再捆绑 Cronet 库，在不支持 HttpEngine 的设备上回退到 OkHttp
* 适配 E 站缩略图和 WebP 等相关变更

### 改进

* 使用磁力链接代替种子下载
* 在搜索栏右侧显示论坛头像
* 支持重置下载路径
* 为 Android 10 以下且没有 DocumentsUI 的设备创建默认下载目录 [#1735](https://github.com/FooIbar/EhViewer/issues/1735)
* 重新添加了强制使用 e-hentai 缩略图服务器的选项
* 阅读器支持 PageUp/PageDown 和方向键上/下翻页 [#1801](https://github.com/FooIbar/EhViewer/issues/1801)
* 未找到搜索结果时显示可能存在的警告 [#1787](https://github.com/FooIbar/EhViewer/issues/1787)
* 更新了日本语翻译
* 杂项优化

### Bug 修复

* 阅读器屏幕方向与手机屏幕方向不同时闪退 [#1779](https://github.com/FooIbar/EhViewer/issues/1779)
* 下载文件哈希不匹配导致下载失败 [#1811](https://github.com/FooIbar/EhViewer/issues/1811)
* 无法同时下载和查看 [#1822](https://github.com/FooIbar/EhViewer/issues/1822)
* 杂项修复


## [1.12.1] - 2024-09-28

### 新功能

* 屏蔽含有二维码的图片（位于高级设置）

### 改进

* 移除高级设置中的自定义 User Agent，改为桌面版网站选项
* 画廊预览与画廊详情页面合并
* 为快速搜索和下载标签列表添加了滚动条
* 无网络条件下启动时自动转到下载页面
* 杂项优化

### Bug 修复

* 从左到右或右到左模式观看使用鼠标滚轮无法翻页 [#1541](https://github.com/FooIbar/EhViewer/issues/1541)
* 状态栏颜色未跟随阅读器主题变化 [#1542](https://github.com/FooIbar/EhViewer/issues/1542)
* 打开 EH 设置时闪退 [#1563](https://github.com/FooIbar/EhViewer/issues/1563)
* 画廊种子过多时显示没有种子 [#1561](https://github.com/FooIbar/EhViewer/issues/1561)
* 加载大于 100 MB 的位图时崩溃
* 阅读器加载小圆圈在黑色背景下不自动变色 [#1579](https://github.com/FooIbar/EhViewer/issues/1579)
* 因闪退造成的无法挽回的下载失败 [#1602](https://github.com/FooIbar/EhViewer/issues/1602)
* 某些情况下闪退 [#1548](https://github.com/FooIbar/EhViewer/issues/1548) [#1555](https://github.com/FooIbar/EhViewer/issues/1555) [#1568](https://github.com/FooIbar/EhViewer/issues/1568)
* Android 7.0 及以下的设备无法加载图片 [#1633](https://github.com/FooIbar/EhViewer/issues/1633)
* 某些情况下导入数据失败 [#1634](https://github.com/FooIbar/EhViewer/issues/1634)
* 下载时创建重复文件夹 [#1619](https://github.com/FooIbar/EhViewer/issues/1619)
* 滚动方向改变后预载图片不生效 [#1560](https://github.com/FooIbar/EhViewer/issues/1560)
* 选择无效的下载位置后闪退
* 杂项修复


## [1.12.0] - 2024-08-19

### 重大变更

* Compose 阅读器已稳定，移除了旧阅读器实现 [#847](https://github.com/FooIbar/EhViewer/issues/847)
* 由于 E 站缩略图服务器变更，移除了强制使用 e-hentai 缩略图服务器的选项

### 改进

* 更新高级搜索选项中页数过滤器的范围限制以遵循 E 站变更

### Bug 修复

* 评论页面刷新指示器边距不正确 [#1428](https://github.com/FooIbar/EhViewer/issues/1428)
* 关闭打开失败的压缩包时崩溃
* 无法打开带密码的压缩包 [#1454](https://github.com/FooIbar/EhViewer/issues/1454)
* 使用音量键翻页时无法关闭过渡动画 [#1467](https://github.com/FooIbar/EhViewer/issues/1467)
* 带有图片的评论显示错位 [#1468](https://github.com/FooIbar/EhViewer/issues/1468)
* 在包含相同项目的页面间切换时崩溃 [#1490](https://github.com/FooIbar/EhViewer/issues/1490)
* 无法检查更新 [#1512](https://github.com/FooIbar/EhViewer/issues/1512)
* 画廊详情未加载完成时退出阅读器导致崩溃 [#1507](https://github.com/FooIbar/EhViewer/issues/1507)
* 无法为带有临时标签的画廊生成 ComicInfo.xml [#1509](https://github.com/FooIbar/EhViewer/issues/1509)
* 已下载的画廊翻页时出现加载动画 [#1510](https://github.com/FooIbar/EhViewer/issues/1510)
* 画廊详情页面的缩略图无法加载 [#1529](https://github.com/FooIbar/EhViewer/issues/1529)
* 图片未加载完成时无法通过点击翻页
* 搜索结果为空时解析失败 [#1418](https://github.com/FooIbar/EhViewer/issues/1418)
* 缩略图分辨率设置不起作用


## [1.11.7] - 2024-07-10

### Bug 修复

* 无法下载标题包含 # 或 % 的画廊
* 下载页数较多的画廊时应用无响应
* 退出搜索栏时文本闪烁


## [1.11.6] - 2024-07-09 [YANKED]

### 改进

* 某些设备上无法加载长图时可尝试减小 设置-高级 中的硬件位图阈值 [#1321](https://github.com/FooIbar/EhViewer/issues/1321)
* 清理下载冗余时显示确认对话框 [#1369](https://github.com/FooIbar/EhViewer/issues/1369)
* 修改收藏时显示收藏备注 [#1392](https://github.com/FooIbar/EhViewer/issues/1392)

### Bug 修复

* 图片完整性校验误报 [#1286](https://github.com/FooIbar/EhViewer/issues/1286)
* 某些情况下应用崩溃 [#1290](https://github.com/FooIbar/EhViewer/issues/1290) [#1302](https://github.com/FooIbar/EhViewer/issues/1302)
* 某些情况下渲染问题 [#1284](https://github.com/FooIbar/EhViewer/issues/1284)
* 无法下载标题过长的画廊 [#1340](https://github.com/FooIbar/EhViewer/issues/1340)
* 某些界面拖动滚动条时黑屏 [#1329](https://github.com/FooIbar/EhViewer/issues/1329)
* 搜索栏多行文本遮挡其他内容 [#1365](https://github.com/FooIbar/EhViewer/issues/1365)
* CIFS Documents Provider 兼容问题 [#1356](https://github.com/FooIbar/EhViewer/issues/1356)
* 清理下载冗余未按预期运行 [#1369](https://github.com/FooIbar/EhViewer/issues/1369)
* 读取压缩包失败时崩溃
* 某些设备上崩溃 [#1276](https://github.com/FooIbar/EhViewer/issues/1276)
* 检查更新失效


## [1.11.6-RC2] - 2024-06-06

### 改进

* 下载图片时执行完整性校验以防止文件损坏/被篡改


## [1.11.6-RC1] - 2024-06-02

### 改进

* 下载列表默认显示全部下载项目
* 替换搜索文本中的换行符为空格 [#1258](https://github.com/FooIbar/EhViewer/issues/1258)

### Bug 修复

* 评价画廊时未使用输入的评分
* 搜索时意外选择 Non-H 类别 [#1246](https://github.com/FooIbar/EhViewer/issues/1246)
* 侧边栏与导航抽屉重叠 [#1262](https://github.com/FooIbar/EhViewer/issues/1262)
* 快速搜索/下载标签排序失效 [#1267](https://github.com/FooIbar/EhViewer/issues/1267)
* 加载压缩包/种子列表出错时 UI 冻结
* 低版本 Android 上某些 UI 被遮挡


## [1.11.5] - 2024-05-19

### 重大变更

* 不再支持 32 位 x86

### 新功能

* 支持按作者对下载分组
* 由于 E 站不再强制要求人机验证，重新支持账号密码登录

### 改进

* 在登录时加载收藏名称 [#1077](https://github.com/FooIbar/EhViewer/issues/1077)
* 优化归档和种子界面 UI
* 禁止在 cookie 界面截图以防止泄露
* 在缩略图模式下显示画廊页数
* 记住最低评分和页数搜索参数
* 更新 User-Agent 以缓解 IP 封禁 [#1182](https://github.com/FooIbar/EhViewer/issues/1182)
* 账号密码登录支持自动填充
* 更新中文（台灣）翻译

### Bug 修复

* 非触摸模式下无法退出搜索界面 [#1060](https://github.com/FooIbar/EhViewer/issues/1060)
* 某些设备上无法安装更新 [#1067](https://github.com/FooIbar/EhViewer/issues/1067)
* 某些设备上崩溃/卡顿 [#996](https://github.com/FooIbar/EhViewer/issues/996) [#1023](https://github.com/FooIbar/EhViewer/issues/1023)
* 搜索记录中存在重复条目时崩溃 [#1130](https://github.com/FooIbar/EhViewer/issues/1130)
* 条漫模式下缩小后点按区域偏移 [#127](https://github.com/FooIbar/EhViewer/issues/127)
* 多窗口模式下点按区域偏移
* 点击下载通知时未清除状态
* 保存未完成加载的图片时崩溃 [#1154](https://github.com/FooIbar/EhViewer/issues/1154)
* 历史记录界面无法在项目上滑动打开抽屉 [#464](https://github.com/FooIbar/EhViewer/issues/464)
* 导入数据失败时显示导入成功 [#1174](https://github.com/FooIbar/EhViewer/issues/1174)
* 某些条件下收藏界面崩溃 [#1190](https://github.com/FooIbar/EhViewer/issues/1190)
* 反转点按区域功能失效 [#1217](https://github.com/FooIbar/EhViewer/issues/1217)

### 已知问题

* 某些设备上从后台返回时某些 UI 消失，可通过关闭列表项目动画部分缓解 [#1184](https://github.com/FooIbar/EhViewer/issues/1184)


## [1.11.4] - 2024-04-21

### 新功能

* 更新前自动备份数据

### 改进

* 在 设置-高级 中加入了禁用列表项目动画的选项，该功能在部分设备上导致崩溃/卡顿 [#996](https://github.com/FooIbar/EhViewer/issues/996) [#1023](https://github.com/FooIbar/EhViewer/issues/1023)
* 下载列表多选模式下避免误触 [#1016](https://github.com/FooIbar/EhViewer/issues/1016)
* 杂项 UI 优化

### Bug 修复

* 重命名下载标签后删除该标签时崩溃 [#1008](https://github.com/FooIbar/EhViewer/issues/1008)
* 搜索栏中某些输入法长按退格无法连续删除 [#606](https://github.com/FooIbar/EhViewer/issues/606)
* 评论/屏蔽/搜索建议/下载标签/快速搜索列表卡顿 [#1041](https://github.com/FooIbar/EhViewer/issues/1041)


## [1.11.3] - 2024-04-06

### 改进

* 优化预加载逻辑和图片加载顺序
* 优化画廊详情和评论页面 UI

### Bug 修复

* 某些模式下搜索结果不显示画廊语言 [#923](https://github.com/FooIbar/EhViewer/issues/923)
* 某些情况下在搜索结果中直接阅读画廊后崩溃 [#927](https://github.com/FooIbar/EhViewer/issues/927)
* 更新至最新版本后仍提示有新版本 [#927](https://github.com/FooIbar/EhViewer/issues/927)
* 从旧版本升级时包含不兼容数据导致崩溃/无法导出数据 [#940](https://github.com/FooIbar/EhViewer/issues/940) [#974](https://github.com/FooIbar/EhViewer/issues/974)
* Android 10 以下的设备打开 设置-隐私 时崩溃 [#953](https://github.com/FooIbar/EhViewer/issues/953)
* 下载标签无法拖动
* 刷新 和 刷新（原图） 无法在已下载的图片上使用
* 临时 IP 封禁错误显示为解析失败
* 某些情况下下载时闪退 [#980](https://github.com/FooIbar/EhViewer/issues/980)
* 某些设备上下载列表中的缩略图无法显示 [#977](https://github.com/FooIbar/EhViewer/issues/977)


## [1.11.2] - 2024-03-17

### 重大变更

* Default 变种最低支持版本更改为 Android 8.0，重新添加了最低支持 Android 6.0 的 Marshmallow 变种

### 改进

* 重新添加了删除快速搜索/下载标签时的确认对话框

### Bug 修复

* 收藏页面 搜索栏中文标签转英文异常 [#902](https://github.com/FooIbar/EhViewer/issues/902)
* 在收藏或者取消收藏之后，收藏按钮会无反应 [#908](https://github.com/FooIbar/EhViewer/issues/908)
* 横屏页面下阅读设置UI重合 [#909](https://github.com/FooIbar/EhViewer/issues/909)
* Search bar does not work properly in Japanese [#906](https://github.com/FooIbar/EhViewer/issues/906)
* 搜索结果中的 Disowned 画廊不显示上传者
* 使用 CI 频道更新时，有时会获取到错误的更新链接


## [1.11.1] - 2024-03-12

### 重大变更

* 由于 E 站强制要求人机验证，仅支持通过网页登录
* 默认支持 Android 6，不再提供 Marshmallow 变种
* 集成 Cronet 库，不再提供 GMS 变种

### 改进

* 支持在排序下载列表前按下载标签分组

### Bug 修复

* 下载画廊时崩溃 [#893](https://github.com/FooIbar/EhViewer/issues/893)


## [1.11.0] - 2024-03-09

### 重大变更

* 由于 E 站 Cookie 策略变更，不再支持 Cookie 登录
* 登录时自动选择 exhentai （如果可用）
* 不再提供默认下载路径
* 手动排序更改为以添加时间/上传时间/标题/下载标签/页数对下载列表排序
* 由于数据库结构变更，此版本导出的数据将无法被旧版本导入

### 新功能

* 下载画廊时生成 ComicInfo.xml 格式元数据
* 归档下载时生成 ComicInfo.xml 格式元数据 [#711](https://github.com/FooIbar/EhViewer/issues/711)
* 归档下载支持第三方下载管理器 [#79](https://github.com/FooIbar/EhViewer/issues/79)
* 保存下载结果为 CBZ 压缩包 [#660](https://github.com/FooIbar/EhViewer/issues/660)
* 清除 `igneous` Cookie （将于下次搜索时重新获取）
* 随机打开本地收藏/下载中的画廊 [#695](https://github.com/FooIbar/EhViewer/issues/695)
* 随机在搜索结果中跳转
* 以网格模式查看下载列表
* 本地收藏/历史/下载中的标签搜索 [#860](https://github.com/FooIbar/EhViewer/issues/860)

### 改进

* 搜索时优先显示已选择的类别卡片
* 多处性能优化

### Bug 修复

* 显示模式为 Thumbnail 时无法点击上传者卡片 [#653](https://github.com/FooIbar/EhViewer/issues/653)
* [Marshmallow] GIF 只显示第一帧
* 无法在 `定时请求新闻页面` 关闭时开启 `隐藏 HV 事件通知`
* 响应正文含有无效 UTF-8 字符导致解码失败 [#643](https://github.com/FooIbar/EhViewer/issues/643)
* 某些情况下应用崩溃 [#710](https://github.com/FooIbar/EhViewer/issues/710)
* 画廊名称包含非法路径字符时无法保存图片 [#815](https://github.com/FooIbar/EhViewer/issues/815)
* 无法解码超长图片 [#732](https://github.com/FooIbar/EhViewer/issues/732)
* E 站变更导致种子解析失败 [#880](https://github.com/FooIbar/EhViewer/issues/880)
* E 站不再支持相似扫描导致图片搜索无法使用
* 杂项修复

### 已知问题

* Android 14 上使用手势导航返回时过渡动画过早结束 [#775](https://github.com/FooIbar/EhViewer/issues/775)


## [1.10.3] - 2024-01-15

### 新功能

* [Marshmallow] 支持裁剪边缘、打开压缩包和 GIF

### 改进

* 由于部分设备上的非预期行为 [#578](https://github.com/FooIbar/EhViewer/issues/578) [#596](https://github.com/FooIbar/EhViewer/issues/596)，默认禁用预见式返回导航动画，可于 设置-高级 中开启
* 杂项优化

### Bug 修复

* 刷新后页面无响应 [#579](https://github.com/FooIbar/EhViewer/issues/579)
* 浏览中保存图片后点击操作被阻挡 [#581](https://github.com/FooIbar/EhViewer/issues/581)
* 在画廊详情页面切换至后台后点击评论闪退 [#580](https://github.com/FooIbar/EhViewer/issues/580)
* 已下载的画廊中未阅读缓存过的图片解码失败 [#592](https://github.com/FooIbar/EhViewer/issues/592)
* 在收藏页多选并下载会使得下载状态出现问题 [#603](https://github.com/FooIbar/EhViewer/issues/603)
* 未能在不重启情况下切换 Http 引擎 [#610](https://github.com/FooIbar/EhViewer/issues/610)
* 在有进度的画廊阅读界面无法直接回到第一页 [#614](https://github.com/FooIbar/EhViewer/issues/614)
* 请求新闻界面失效 [#605](https://github.com/FooIbar/EhViewer/issues/605)
* 收藏夹为空解析失败 [#630](https://github.com/FooIbar/EhViewer/issues/630)


## [1.10.2] - 2023-12-31

### 新功能

* [阅读器] 支持裁剪图片黑色/白色边缘

### 改进

* 切换 HTTP 引擎无需重启
* 优化过渡动画
* 杂项性能优化

### Bug 修复

* 修复搜索建议中出现关键词相同的条目时崩溃 [#533](https://github.com/FooIbar/EhViewer/issues/533) [#555](https://github.com/FooIbar/EhViewer/issues/555)
* 修复重命名下载标签时不显示原标签 [#534](https://github.com/FooIbar/EhViewer/issues/534)
* 修复调整下载项目顺序时卡顿 [#552](https://github.com/FooIbar/EhViewer/issues/552)
* 修复新增或修改评论后黑屏 [#554](https://github.com/FooIbar/EhViewer/issues/554)
* 修复剪贴板读取bug [#566](https://github.com/FooIbar/EhViewer/issues/566)
* 修复画廊信息对话框无法通过返回按钮/手势关闭 [#413](https://github.com/FooIbar/EhViewer/issues/413)


## [1.10.1] - 2023-12-19

### 重大变更

* 由于 Apache HttpClient 5 在 Android 10 以下无法使用，后备 HTTP 引擎恢复为 OkHttp [#514](https://github.com/FooIbar/EhViewer/issues/514)

### Bug 修复

* 修复获取 pToken 失败导致无限加载 [#508](https://github.com/FooIbar/EhViewer/issues/508)
* 修复阅读页快速滑动滑块导致下载延时过多累计 [#512](https://github.com/FooIbar/EhViewer/issues/512)
* 修复 Android 9 以下的设备上无法退出搜索栏 [#513](https://github.com/FooIbar/EhViewer/issues/513)


## [1.10.0] - 2023-12-14 [YANKED]

### 重大变更

* 后备 HTTP 引擎变更为 Apache HttpClient 5
* 类别和高级搜索选项移至搜索栏下方

### 新功能

* 支持在搜索时指定画廊语言

### 改进

* 在下载中阅读画廊时会更新历史记录
* 多处性能改进

### Bug 修复

* 修复无法由外部应用进入画廊页面 [#436](https://github.com/FooIbar/EhViewer/issues/436)
* 修复点击精确到页的画廊链接闪退 [#430](https://github.com/FooIbar/EhViewer/issues/430)
* 修复无法搜索本地收藏 [#491](https://github.com/FooIbar/EhViewer/issues/491)
* 修复 CI 版本不显示变更日志


## [1.9.0] - 2023-12-01

### 重大变更

* 除阅读器外全部迁移至 Compose

### 新功能

* 设置-高级 中增加实验性滑动手势敏感度设置
* 支持通过滑动手势打开右侧抽屉
* 支持预见式返回动画，相关设置位于 设置-高级

### 改进

* [阅读器] 画廊页数过多时隐藏进度条刻度并禁用触感反馈
* 优化原图下载重试策略
* 优化小屏设备上的搜索类别布局
* 优化过渡动画
* 优化历史记录删除动画
* 设置-EH 中的缩略图大小设置改为缩略图列数

### Bug 修复

* 修复点击通知栏的下载通知闪退 [#175](https://github.com/FooIbar/EhViewer/issues/175)
* 修复收藏状态显示错误 [#189](https://github.com/FooIbar/EhViewer/issues/189)
* 修复双击导航按钮后无法打开导航抽屉
* 修复某些情况下闪退 [#142](https://github.com/FooIbar/EhViewer/issues/142) [#208](https://github.com/FooIbar/EhViewer/issues/208)
* 修复收藏界面点击重试崩溃 [#193](https://github.com/FooIbar/EhViewer/issues/193)
* 修复本地收藏滚动位置丢失 [#192](https://github.com/FooIbar/EhViewer/issues/192)
* 修复手速过快页面切换过快使加载圈圈不自动消失 [#209](https://github.com/FooIbar/EhViewer/issues/209)
* 修复打开并关闭右侧抽屉后，状态栏一片白色 [#101](https://github.com/FooIbar/EhViewer/issues/101)
* 修复使用深色主题时，切换界面会闪过亮色界面 [#121](https://github.com/FooIbar/EhViewer/issues/121)
* [Marshmallow] 修复小内存设备上图片解码失败 [#229](https://github.com/FooIbar/EhViewer/issues/229) [#372](https://github.com/FooIbar/EhViewer/issues/372)
* 修复已舍弃画廊上传者解析

### 已知问题

* 画廊信息对话框无法通过返回关闭 [#413](https://github.com/FooIbar/EhViewer/issues/413)
