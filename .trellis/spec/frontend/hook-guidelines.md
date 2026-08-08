# Hook Guidelines

> 本项目无 hooks(非 React)——对应模式为 Compose 状态获取。

---

## Compose 等价模式

| React hooks | Compose 对应 |
|---|---|
| useState | `remember { mutableStateOf(...) }` |
| useEffect | `LaunchedEffect(key) { ... }` |
| useContext | ViewModel 注入(单 VM 全局) |
| useMemo | `remember(key) { 计算 }` |
| 订阅 | `collectAsState()` |

---

## 约定

- `LaunchedEffect` 的 key 必须包含依赖(poemId/rhythmic),避免陈旧数据
- 首启一次性加载(数据库/映射)用顶层 `LaunchedEffect(Unit)`,加载完成前显示闸门
- 不把副作用写在组合函数体内(用 LaunchedEffect/DisposableEffect 包装)
