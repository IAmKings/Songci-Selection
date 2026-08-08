# Error Handling

> 数据层错误处理约定:防御性解析 + 校验拒写 + 明确失败。

---

## Error Types

- 无自定义异常类型;Kotlin 用 `null` 表示解析失败(防御式),Python 用 `SystemExit` 带提示退出
- `Rhythmic.parseSpec`/`Dynasty.parseMap`:畸形数据返回 `null`/空 Map,**不抛异常不崩溃**

---

## Error Handling Patterns

- **解析器防御**:`parseSpec` 校验字段数/长度一致/segs 末值后才构造;测试覆盖畸形输入(`parseSpecRejectsMalformed`)
- **生成脚本校验拒写**:`restore.py` 对 index 越界、还原后仍含 ⿰ 直接 `SystemExit` 拒绝写入(dry-run 可预览)
- **幂等保障**:脚本重跑必须输出一致(生成物 `git diff` 干净);`restore.py` 重复执行 0 变化
- **数据源缺失提示**:`rhythmic_map.py` 等数据源缺失时 `SystemExit` 并附 clone 命令

---

## API Error Responses

- 不适用(本地应用无 API);数据加载失败 UI 显示「加载中…」闸门或空态,不白屏不崩溃

---

## Common Mistakes

- 解析器对畸形数据抛异常导致整页崩溃 → 改为返回 null + 调用方空态
- 生成脚本静默失败(如 ci.json 格式被压缩破坏) → 写回必须保持 `indent=4` 原格式
- 数据更新后应用读旧缓存库(桌面 `~/.songci/songci.db` 存在即不覆盖) → 驱动按资源大小对比重新复制
