# Component Guidelines

> Compose 组件构建约定:复用优先、私有组件、无新依赖。

---

## 复用组件(components/Common.kt)

- **SimpleListScreen**(title, back, content):标题栏 + 内容槽;内容内自行 LazyColumn
- **TextRowList**(title, back, rows, onClick, trailing?):通用文本行列表(朝代/作者/词牌共用);trailing 显示行尾小标签(词牌字数)
- **PoemList / PoemCard / EmptyState**:词作列表/卡片/空态

---

## 屏幕内组件

- 仅单屏使用的组件留在 screens/ 文件内并 `private`(如 RhythmicCard/TuneLine)
- 可测逻辑不下沉 UI(放 data 层纯函数,如 `RhythmicSpec.tuneLines`)

---

## 交互与状态

- 数据加载:`var state by remember { mutableStateOf<T?>(null) }` + `LaunchedEffect(key) { state = vm.load(key) }`;null → EmptyState「加载中…」
- 列表刷新:`collectAsState()` 订阅 ViewModel 的 StateFlow
- 可滚动列表一律 LazyColumn;整页滚动场景(卡片+列表)把头部作为首个 item

---

## 主题

- 一律用 `SongciColors` token(background/surfaceContainerLow/primary/error/outline/line/stone 等),禁止硬编码色值
- 字体/间距遵循 DESIGN.md「Classical Manuscript」
