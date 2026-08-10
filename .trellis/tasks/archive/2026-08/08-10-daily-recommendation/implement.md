# Implement — 首页每日推荐池

## 执行清单

1. **Schema**:`SongciDb.sq` 加 `recommendation_pool` 表 + poems 加 `recommended_date` 列;确认 SQLDelight schema 版本机制(迁移)
2. **数据层**:`dailyPool(date)` 生成算法(池缓存/不重复/异常过滤/90% 重置);异常过滤规则 SQL 化;日期参数化(测试注入)
3. **AppViewModel**:`dailyPoems` StateFlow + `refreshDaily(date = LocalDate.now())`
4. **首页**:展示每日池;跨 0 点轮询刷新(kotlinx-datetime)
5. **测试**(desktopTest):
   - 同一天多次调用同池;不同日期不同池
   - 不重复:连续 N 天(注入日期)无重复词
   - 异常过滤:⿰ 词牌/超长/缺字/格式异常词不进池
   - 重置:标记满 90% → 循环
6. **构建 + 实机验证**:首页 20 首、跨天刷新(改系统日期或等待)
7. **提交 + 合并**(用户验收)

## 验证命令

```bash
cd app && ./gradlew :composeApp:desktopTest
./gradlew :composeApp:assembleDebug
```

## 回滚点

- 分支工作;Schema 迁移前备份 db(迁移失败可重置)
- 步骤 4 前:数据层独立,首页未动

## Review Gate

- 步骤 5:AC2/AC3 测试通过
- 步骤 6:AC1/AC5 实机验收 → 合并
