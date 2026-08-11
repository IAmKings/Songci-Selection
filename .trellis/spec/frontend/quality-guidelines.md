# Quality Guidelines

> UI 层质量约定:自适应、主题一致性、无新依赖、验收闭环。

---

## 自适应

- **768dp 断点**:宽屏(≥768dp)详情双栏(上下阕并置)/顶部导航;窄屏单栏 + 底部导航
- 列表/卡片布局不写死宽度,`fillMaxWidth` + 内容 padding

---

## 主题一致性

- 颜色/字体必须走 `SongciColors`/MaterialTheme token,禁止硬编码
- 古典手稿风格(DESIGN.md):纸张底色、墨蓝主色、衬线字体(Noto Serif SC)

---

## 性能与滚动

- 列表一律 LazyColumn(懒加载);长内容页(词牌详情 = 格律卡片 + 词作)用单一 LazyColumn 统一滚动
- 大文本(21k 词库)搜索走后台线程 + LIMIT,不阻塞主线程

---

## 验收闭环

- 新功能跑通「构建(desktopTest/assembleDebug/packageDmg)+ 桌面运行目视确认」双验证
- 交互问题先数据层复现(desktopTest),再修 UI
- trellis-check 全绿 + ponytail-review 无冗余后提交

---

## 小组件(Android Glance / WidgetKit)经验

- **Glance 自定义字体不可用**(平台限制):widget 文本由 launcher 进程渲染,app 内 TTF 无法跨进程传递,FontFamily 仅系统字体(1.1.1→1.3.0-alpha02 全系)。不要重复调研升级
- **Glance ColorProvider 陷阱**:`ColorProvider(Int)` 构造器是 colorRes(资源 ID),必须包 `ColorProvider(Color(0xFF...))`,否则 launcher 报 "No package ID" 渲染失败
- **Glance 整卡点击**:`actionStartActivity` 的参数经 trampoline 变 intent extra,不进 `intent.data` → 深链要用 `ActionCallback` + 显式 `ACTION_VIEW`
- **macOS 非沙盒 host 写 App Group 容器**:触发 TCC 弹窗("访问其他App的数据"),开发期每次重签都弹,正式签名仅首次;不要用 delay 回避(阻塞数据加载链路)
- **部署脚本敏感信息**:签名证书等本机身份从 `CERT_IDENTITY` 环境变量/`~/.songci-signing.env` 读取,禁止硬编码入库(仓库公开!)
- **凌晨定时刷新模式(2026-08-11)**:每日凌晨自动刷新 = Android WorkManager 一次性延迟 + 尾部重排(`enqueueUniqueWork("midnight-refresh", REPLACE)` 幂等防堆积;延迟复用 `msUntilNextMidnight()`,纯算术本地 0 点);iOS/macOS = Timeline policy `.after(nextMidnight())`(Calendar.nextDate 算下一本地 0 点),系统到点自行刷新无需 app 运行。选型:updatePeriodMillis 最小 30min 无法对齐凌晨;AlarmManager 需手动 BOOT_COMPLETED 重挂,WorkManager 内置重启恢复

## 导航分层模型(2026-08-10 决策)

- **两层模型**:频道层(tab/rail,4 tab:首页/索引/收藏/设置)↔ 内容层(详情/词牌/作者,全屏盖导航)
- **进入详情统一走 openPoem(id)**:从跳板(词牌/作者)选词 = popUpTo 跳板保留 + 压新详情(同层唯一,防循环入栈);其余入口直接压
- **切 tab 差异化**:先清内容层(详情/词牌/作者)→ 频道层 saveState/restoreState(索引子页浏览位置保留,挖宝翻阅感);索引是唯一有深子页的 tab
- **宽屏 rail**:频道导航用 NavigationRail(内容层隐藏),搜索入口统一在首页顶栏,不重复放
- **图标语义**:自绘 AppIcons.kt(core 集无书签/填充书本)——书本=索引(书页挑选),书签=收藏(保留),逻辑闭环
- **长名称**:词牌名 maxLines=2 截断;变体名列表 FlowRow 换行(一剪梅多体场景);窗口最小 360px(工程值,设计稿 375 为手机样张)
- **已知近似**:跨跳板链(详情→词牌→作者→选词)旧详情留栈底,单栈限制,返回多步清空(prd 记录)

## 桌面端跨线程事件进 Compose(2026-08-10 实证)

- **规范**:外部线程事件(深链/热键/文件打开/菜单回调)一律走「线程安全队列(ConcurrentLinkedQueue)+ 组合内 LaunchedEffect 轮询消费」,消费时直接驱动导航/操作;**禁止**「事件线程写 mutableStateOf + 参数传递」模式
- **实证坑**:OpenURIHandler(AppKit 线程)写 state 后,即便切 UI 线程、即便组合作用域内写,`App(deepLinkToken = token)` 参数侧仍恒读旧值(token=0)——组合调用点参数求值与协程内写之间存在快照/调度脱节,effect 不重跑,同词重复深链失效
- **平台差异**:Android(新 Activity)/iOS(控制器重建)每次深链全量重建,首帧 effect 必跑,天然免疫;只有 macOS 常驻窗口依赖「状态变化→重组」链路,踩坑
- **深链同词重复**:iOS 用重建键含 token(`.id("id-token")`);macOS 用队列直通(事件即导航);Android 天然新实例

## 字体(2026-08-10)

- **内嵌字体必须子集化**:词库实测 5275 字 + UI 白名单 → `scripts/subset-fonts.sh`(fonttools pyftsubset);WenKai 25MB→2.6MB、新致宋 11.9MB→2.2MB;词库变更后重跑(幂等);子集化后用 cmap 全量覆盖验证(缺字须确认原字体本就不含)
- **授权文件勿放 composeResources**(插件为文件名生成访问器,`.` 非法):放 `licenses/`
- **字体风格切换**:SongciTheme 双层(外层默认 + 内层按 vm.fontStyle,加载完成后生效);Typography 参数化;词文字体 WenKai(楷体)/ 新致宋(宋体)
- **widget 无法用自定义字体**(平台限制,渲染在系统进程):字体设置标注「仅应用内生效」
- **构建缓存残留**:APK 出现源码不存在的字体(如 noto_serif_sc)→ gradle clean 重建,勿手工删

## kotlinx-datetime 0.7 陷阱(2026-08-11 实证)

- **转换 API 方向反转**:`Instant.toLocalDateTime(TimeZone)`(0.6)→ 0.7 为 `TimeZone.toLocalDateTime(Instant)`/`toLocalDateTime(instant, zone)` 扩展;`kotlinx.datetime.Clock` 移除,改用 `kotlin.time.Clock.System`(@ExperimentalTime)
- **DAY 常量位置变化**:`DateTimeUnit.DAY` → 嵌套 companion(`DayBased.DAY` 等),`LocalDate.plus` 签名也变;**0.7 的日期算术 API 不稳定,优先纯算术**(本地午夜 = localMillis 对齐 86400000,DST 只影响 1-4 点,0 点恒成立)
- 扩展函数必须显式 import(`toLocalDateTime`/`offsetAt` 等),全限定调用同样可行
- **db 资源不入库**(大二进制,db/build.py 本地生成):schema 变更 = 改 build.py → 重建 → 复制 composeResources/files → 递增 db_version.txt(客户端判新复制)
