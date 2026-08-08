# State Management

> AppViewModel + StateFlow/mutableStateOf 模式。

---

## 架构

- **单一 AppViewModel**(生命周期 ViewModel,viewModelScope):持有 Repository(db/dynasty/rhythmic)
- Repository 构造注入(App.kt 首启加载闸门内 `withContext(Dispatchers.Default)` 创建,避免主线程 IO)

---

## 状态形态

- **列表数据**:`MutableStateFlow<List<T>>`,init 时 `viewModelScope.launch { _x.value = repo.x() }`,UI `collectAsState()`
- **交互态**:`var x by mutableStateOf(...)`,如字号档位/搜索词/筛选条件
- **懒加载**:词作详情/作者词作等按需 `suspend fun` + 屏幕内 LaunchedEffect 拉取(不预载)

---

## 约定

- 数据加载统一后台:`repo.*` 内部 `withContext(Dispatchers.Default)`(SQLDelight 同步查询包装)
- 写操作幂等:收藏 `setFavorite(poem, target)` 按目标态写入,避免读-取反竞态
- 搜索状态机:`searchQuery/searchRhythmic/searchDynasty` → `runSearch()` 重算,筛选在内存做(词牌筛选按归并名 `cleanRhythmic`)
