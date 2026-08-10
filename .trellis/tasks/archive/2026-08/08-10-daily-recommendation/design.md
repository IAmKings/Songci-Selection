# Design — 首页每日推荐池

## 总览

```
推荐池表(recommendation_pool): date(主键), poem_ids(20 个,逗号分隔)
标记: poems 表加 recommended_date 列(或独立 recommended 表)
生成时机: 0 点/当日首次进入 → 若当日池不存在 → 从未推荐+非异常随机 20 → 写池+标记
重置: 已推荐词数 ≥ 词库 90% → 清空标记(重新循环)
```

## 组件分解

### 1. 数据层(common,SQLDelight)

**方案:不新增表,复用现有 poems 表加列**——SQLDelight schema 变更(迁移:ALTER TABLE 加 recommended_date INTEGER 默认 NULL;推荐池需要"日期→20 id"持久化 → **需新表 recommendation_pool(date TEXT PRIMARY KEY, poem_ids TEXT)**)。

- `recommendation_pool`:当天池缓存(确定性:同日多次进入读缓存,不重新生成)
- poems.recommended_date:标记最近推荐日期(非 NULL = 已推荐过;重置=清 NULL)

**生成算法**(repository 层):
```
fun dailyPool(date: String): List<Long> {
    pool 表查 date → 有则返回
    val candidates = poems 全部 WHERE recommended_date IS NULL
        AND 非异常(见 §2)
    if (candidates.size < 20) { 重置标记(清 NULL); candidates = 全库非异常 }
    val picked = candidates.shuffled().take(20)
    写 pool(date, picked); 更新 picked 的 recommended_date = date
    return picked
}
```

**异常过滤(SQL 或内存)**:SQL 层 WHERE:
- 词牌:rhythmic 不含 '⿰' AND length(rhythmic) <= 12
- 缺字:content 不包含缺字集合(与子集化字体联动:硬编码 2 个实测缺字 + 可扩展)
- 格式:content 含换行符(至少 2 行)AND 单行最长 <= 40(超长单行=格式异常)

### 2. 首页接入

- AppViewModel:`dailyPoems` StateFlow;`refreshDaily()` 生成/读取当日池
- HomeScreen:LaunchedEffect(Unit) { vm.refreshDaily() };显示 dailyPoems(替换现有 randomPoems 随机 20)
- **跨 0 点刷新**:LaunchedEffect 轮询日期变化(kotlinx-datetime 已依赖?查——项目有 kotlinx-datetime 0.7.1)→ 日期变化 → refreshDaily()
- 保留 refreshRandom?首页改为每日池后,randomPoems 不再用于首页(移除或保留给测试)

### 3. 日期

- kotlinx-datetime LocalDate(项目已依赖 0.7.1)——`LocalDate.now()` 格式化 yyyyMMdd 做 key;测试注入日期(参数化 date)

## 风险

- SQLDelight schema 迁移(加列+新表):桌面已有 db 文件 → 迁移 SQL(ALTER/创建);user_version 递增(SongciDb.Schema 自动处理?SQLDelight 自动迁移需显式 schema 版本——查现有 schema 版本机制)
- 标记重置并发(单进程 app,无并发风险;widget 不参与)
- 日期注入:desktopTest 需可注入日期(函数参数化,不用全局时钟)
