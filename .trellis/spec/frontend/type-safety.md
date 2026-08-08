# Type Safety

> 类型安全约定:不可变数据类 + 防御式解析。

---

## 数据模型

- 只读数据类:`Poem`/`Author`/`Favorite`/`RhythmicSpec`/`TuneLine`(data class,字段不可变)
- 映射类包装:`Dynasty`/`Rhythmic` 持有 Map,`of()`/`expand()` 返回 null/空列表表达缺失

---

## 解析防御

- 手写 JSON 解析器(`Dynasty.parseMap`/`Rhythmic.parseMap`):扁平 `key:value` 逐条解析,畸形行 `mapNotNull` 丢弃
- `parseSpec` 校验:字段数(8)/tune-rhythm 等长/segs 末值 = 末字索引;不过 → null
- **约定**:值字段内禁用 ASCII 逗号(JSON 条目按逗号拆分)——segs/aliases 用 `/` 分隔

---

## 空安全

- `authorId?.let(onOpenAuthor)` 等 null 链式调用;作者缺失词作 `authorName` 空串兜底
- 加载态 `T?` 与空态区分(「加载中…」vs「暂无词作」vs 数据缺失)

---

## 错误表达

- 不抛异常表达业务缺失;解析失败 = null/空,UI 空态兜底(不崩溃)
