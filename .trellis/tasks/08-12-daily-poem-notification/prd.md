# 每日一词定时通知(本地通知+自研时间选择器)

## Goal

用户在设置页选择一个每日通知时间(自研时间选择器,贴合 Classical Manuscript 设计风格),此后每天该时刻推送一首词,点击通知直达该词详情页。纯本地通知,零服务器;未授予通知权限则跳过调度,不引导弹窗。

## Requirements

- **时间选择器**:设置页新增「每日一词」入口,自研时/分选择器(禁用 Material3 TimePicker——风格与古典设计系统不搭)。样式复用 `SongciColors` token、主题字体(楷体)、现有布局语言(‹ 返回、stone 辅助色、米白底)。
- **调度**:用户设定的时间每日触发一次;纯本地:
  - Android:`WorkManager` 每日周期任务,**触发时刻选词**(内容每天新鲜)
  - iOS:`UNCalendarNotificationTrigger` 非重复,「启动时滚动排未来 7 天」——每次 app 启动补齐窗口,每天一条各随机选词(官方推荐模式;pending 上限 64 条,7 天无压力)
  - macOS:与 iOS 同构——JNA 绑定 `UNUserNotificationCenter`(纯 JVM 无第一方通知 API,绑定是必要成本),滚动排 7 天;点击回调经现有 `deepLinkChannel` 直达词详情
- **通知词选取(方案 1 独立随机)**:复用 `repo.randomPoems(1)`——SQL 层过滤 ⿰/缺字/超长词牌/单行,与首页/小组件**同源推荐规范**;不写推荐池、不预支标记、不跨日期依赖。Android/iOS 两端行为一致(各自时刻随机)。
- **权限门**:调度前检查通知授权——未授权则跳过调度(不弹系统引导)。授权状态变化后(如用户在系统设置里关掉)下次启动时校验并停调。
- **关闭**:设置页提供关闭开关(关闭 = 取消调度 + 清除记录)。
- **深链补缺**:Android `MainActivity` 需补 `onNewIntent`(当前仅 `onCreate` 解析 intent——app 后台存活时通知/widget 点击深链失效),iOS 冷启动走 launchOptions → 转 URL。
- **不做的**:不做远程推送(FCM/APNs)、不做通知扩展、不做 Live Activity、不做未授权引导弹窗。
- **依赖**:新增 `net.java.dev.jna:jna`(desktop 目标)——唯一新增依赖,用于 macOS 调 UNUserNotificationCenter(纯 JVM 无第一方通知 API;项目"零新依赖"约定在此项豁免,理由:平台能力必要项,同 sqldelight driver)。

## Acceptance Criteria

- [ ] 设置页可设定每日通知时间(时/分),选择器视觉与现有界面一致,无新依赖
- [ ] 设定后 Android 每日该时刻收到通知,点击直达对应词详情(冷启动与后台存活两种状态)
- [ ] iOS 每日该时刻收到通知,点击直达对应词详情(含冷启动)
- [ ] 未授权通知权限:不调度、不弹引导;后续授权后设置生效
- [ ] 关闭开关取消调度,重启 app 后状态保持
- [ ] 时间选择器在桌面端(宽/窄)与移动端布局不越界
- [ ] Android 通知内容为触发时刻新选的词(非调度时快照)
- [ ] 既有测试全绿(desktopTest);新增调度状态持久化测试

## Notes

- macOS 是否纳入本轮由任务评审决定(纯 JVM 无第一方通知 API,JNA 绑定是额外工程量)。
- 通知的展示样式(小图标、渠道名)遵循各平台最小实现,不做品牌定制。
