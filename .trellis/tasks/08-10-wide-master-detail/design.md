# Design — 宽屏词牌/作者侧栏化

## 总览

路由内双栏(方案 A):词牌/作者页 Screen 内按 wide 分支渲染 `Row[列表 | 详情区]`,选中词为内部状态,不导航。

## 组件分解

### 1. DetailBody 抽取(DetailScreen.kt)

现状 DetailScreen = 页面壳(返回/标题)+ 内容(NarrowDetail/WideDetail)。抽取共享内容:

```
DetailBody(poem, vm, scale, favorite, onToggleFavorite, onOpenAuthor, onOpenRhythmic)
  ├ 窄:原 NarrowDetail 内容(滚动 Column)
  └ 宽:原 WideDetail 内容(滚动 Column,双栏词句)
```

- 全屏详情页复用 DetailBody(壳不变,行为不变)
- 双栏右侧复用 DetailBody——**双栏右侧用窄版还是宽版?** 右侧区宽约 60-70%(宽屏),用 WideDetail(双栏词句)合理;但右侧高度受限(列表+详情同高)→ 滚动即可。**决定:右侧用 WideDetail 内容(保留双栏词句质感)**
- 抽取时保持两个现有壳(DETAIL 路由)行为逐字节一致,desktopTest 守护

### 2. 双栏页(RhythmicPoemsScreen / AuthorPoemsScreen)

```
wide 分支:
Row(fillMaxSize) {
  左列表: 现列表内容(LazyColumn),固定宽 ~300dp 或 weight(1/3),右侧边框
  右侧:   选中词 id 存在 → DetailBody(滚动)
          否则 → 空态("点击左侧词作")
}
选中状态: var selectedPoemId by remember { mutableStateOf<Long?>(initialPoemId) }
```

- 列表点击 → `selectedPoemId = it.id`(原地切换,不导航)
- 初始词:路由可选参数 `poemId`(从详情进入时携带)
- 操作区回调:onOpenAuthor/onOpenRhythmic 由 SongciApp 传入(与现有接线一致)
- 窄屏分支:现状全屏列表 + `onPoem` 导航,零改动

### 3. 路由参数(SongciApp.kt)

```
RHYTHMIC = "rhythmic/{rhythmic}?poemId={poemId}"     // poemId 可选(Long,默认 0/无)
AUTHOR   = "author/{authorId}?poemId={poemId}"
```

- 详情页 onOpenRhythmic/onOpenAuthor 调用带当前 poemId:`navigate("rhythmic/${encodePath(r)}?poemId=$poemId")`
- 首页/索引进入不带 poemId → 双栏右侧空态
- 窄屏:参数被忽略(Screen 窄分支不用)

### 4. 接线(SongciApp.kt)

- 词牌/作者 composable 加 `poemId` 参数读取 + `wide` 传入 Screen
- DetailScreen 的 onOpenRhythmic/onOpenAuthor 回调带当前词 id(闭包捕获 poemId)

## 边界

- 切 tab:双栏页属内容层(rhythmic//author/ 前缀)→ 清除规则不变
- 深链:全屏详情,不涉及
- 窄屏:wide=false 走原分支,零回归
- 平板:≥768dp 自动双栏(common 共享)

## 风险

- DetailBody 抽取:全屏详情渲染逐字节不变(desktopTest + 实机确认)
- 双栏右侧高度:DetailBody 宽版无滚动时可能超高 → 加 verticalScroll(现有 WideDetail 已有,保留)
