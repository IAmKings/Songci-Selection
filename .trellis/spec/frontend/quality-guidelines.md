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
