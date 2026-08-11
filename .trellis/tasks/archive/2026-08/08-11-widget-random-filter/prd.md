# widget 随机诗异常字符过滤

## Goal

widget 随机诗与首页推荐池统一过滤逻辑：异常字符/缺字词（⿰ 词牌或内容、𠴇/𫍙 缺字、超长词牌、单行词）不进入随机推荐，让用户通过索引主动发掘。

## Requirements

- R1: Android widget 随机诗过滤异常字符（SQL 层统一）
- R2: 首页"随机"入口（`randomPoems(20)`）同步受益（同一查询）
- R3: iOS/macOS widget Swift SQL 同步过滤
- R4: 过滤条件与 `dailyCandidates` 一致：词牌无 ⿰、内容无 ⿰、词牌 ≤12 字、内容 ≥2 行、无 𠴇/𫍙 缺字

## Acceptance Criteria

- [ ] AC1: `randomPoems` 查询含过滤条件，widget 随机诗不再出现 ⿰/缺字词
- [ ] AC2: 首页随机入口行为不变（数量/语义），测试 `randomPoemsReturnsLimit` 通过
- [ ] AC3: iOS/macOS 两个 Swift 文件 SQL 同步过滤
- [ ] AC4: 编译 + 既有测试全绿

## Out of Scope

- `dailyCandidates` 不动（已有过滤）
- widget UI 优化（后续迭代）

## Notes

- 风险：`randomPoems` 调用方 = widget provideGlance + AppViewModel 首页随机，impact LOW
- 过滤下沉查询层，一处修改三端受益
