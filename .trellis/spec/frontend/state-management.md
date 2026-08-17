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
- 搜索状态机:`searchQuery/searchDynasty` → `runSearch()` 重算,筛选在内存做(朝代筛选;词牌筛选已于 2026-08-17 移除——主搜索框已支持中文模糊+拼音首字母,双输入框造成 UI 混淆)

---

## 异步竞态铁律(严重事故固化 2026-08-17)

**凡 `viewModelScope.launch` 异步写入共享状态(StateFlow/mutableStateOf),必须同时满足:① 取消旧协程;② 写入前校验输入未变。**

```kotlin
private var searchJob: Job? = null

private fun runSearch() {
    searchJob?.cancel()                 // ① 取消旧查询(旧协程不再写入)
    searchJob = viewModelScope.launch {
        val q = searchQuery.trim()
        val results = ...                // 异步查询
        if (searchQuery.trim() != q) return@launch   // ② 过期校验:只允许最新输入落盘
        _searchResults.value = results
    }
}
```

### 事故复盘(2026-08-17 搜索列表跳变)

- **现象**:输入 `ss`,结果先正确(扫市舞),随后「突然插入」相思令·相思儿令/霜天晓角/踏莎行等词牌;返回重进搜索页,输入框 `ss` 但结果显示十二时·忆少年。
- **根因**:输入每字符触发一次 `runSearch()` launch,**无取消、无过期校验**。单字符 `s` 命中数百词牌(慢查询),双字符 `ss` 仅 24 词牌(快查询)——慢查询后到**覆盖**快查询结果。AppViewModel 为全局单例,竞态残留的 `results` 与 `searchQuery` 跨页面保留,造成状态不一致。
- **排查方法论(可复用)**:
  1. 对比「真机 vs 桌面」结果差异 → 排除 db 数据问题(adb 拉真机 db 逐项核对 abbr 列)
  2. 用户观察「动态刷新/突然插入」→ 指向**多次异步写入**,而非静态数据
  3. 命中集合分析:所有乱入词牌(相思令 `xslxsel`/霜天晓角 `stxj`/踏莎行 `tsx`/十二时·忆少年 `sesysn`)abbr 均含 `s` → 锁定是 `searchWithMatch("s")` 的慢查询后到
- **修复**:上述双保险(取消 + 过期校验);搜索页进入时重跑一次搜索,清残留。

### 检查清单(新增/改动异步状态时逐条过)

- [ ] 写入共享状态的协程是否有句柄可取消?输入/筛选变化时是否 `cancel()`?
- [ ] 异步结果到达时,是否校验「触发条件(query/筛选)未变」?变了必须丢弃
- [ ] 全局单例 VM 的状态(搜索词/结果/筛选)是否在页面重进时与 UI 一致?(必要时进入时重算)
- [ ] 慢查询(宽匹配,如单字符拼音)是否会被快速连续输入放大?必要时限流/截断

---

## 事故关联记录

- **拼音查询无 `ORDER BY`(2026-08-17 遗留)**:`rhythmicsByPinyinAbbr`/`authorsByPinyinAbbr` 返回顺序依赖 SQLite 查询计划,桌面(JDBC)与真机(Android)可能不同,叠加 `take(50)` 截断导致两端结果集合不一致。待补确定性排序(`ORDER BY rhythmic`/`ORDER BY id`)。
- **拼音作者名缺失(已修)**:`searchWithMatch` 构造 `Poem` 时曾硬编码 `authorName=""`,绕过 `toPoem()` 扩展——凡经 Repository 映射的结果一律走 `toPoem()`,禁止手搓 `Poem(...)`。
