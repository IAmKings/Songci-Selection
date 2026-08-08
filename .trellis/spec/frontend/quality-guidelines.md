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
