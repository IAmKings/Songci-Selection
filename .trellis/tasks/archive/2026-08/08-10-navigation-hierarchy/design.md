# Design — 导航分层重构

## 总览

```
Navigation 单栈 + 路由前缀约定:
  频道根: home/search/favorites/index(tab)
  内容层: detail/{id}(全屏,盖 tab)
  跳板:   rhythmic/{r} / author/{id}(内容层子页,可逛)
```

## 1. bottomBar 显隐(窄屏)

`SongciApp` 的 Scaffold bottomBar 按当前路由前缀判断:

```kotlin
val currentRoute = nav.currentBackStackEntryAsState().value?.destination?.route
val showBottomBar = currentRoute == null ||
    !(currentRoute.startsWith("detail/") || currentRoute.startsWith("rhythmic/") || currentRoute.startsWith("author/"))
bottomBar = if (!wide && showBottomBar) { ...现有 NavigationBar... } else {}
```

- 进入详情/词牌/作者 → bottomBar 消失(全屏内容层)
- 返回频道根 → tab 复现
- 宽屏逻辑不变(无 bottomBar,现有 WideTopBar)

## 2. 详情同层唯一(openPoem 统一入口)

所有进入详情的点(首页列表、搜索、收藏、词牌/作者选词、深链)统一走:

```kotlin
fun openPoem(id: Long) {
    val current = nav.currentBackStackEntry?.destination?.route ?: ""
    nav.navigate("detail/$id") {
        // 从跳板(词牌/作者)选词:保留跳板,弹掉其上的旧详情 → 详情同层唯一
        if (current.startsWith("rhythmic/") || current.startsWith("author/")) {
            popUpTo(current) { inclusive = false }
        }
    }
}
```

路径推演(常见):

| 路径 | 栈演变 | 返回 |
|---|---|---|
| 首页 → 详情A | [home, detailA] | detailA → home |
| 详情A → 词牌 → 选词B | [home, detailA, rhythmic] → popUpTo(rhythmic) → [home, rhythmic] → 压 detailB → [home, rhythmic, detailB] | detailB → rhythmic → home |
| 详情A → 词牌 → 点同一首A | 同上 → [home, rhythmic, detailA] | detailA → rhythmic → home(**无循环**) |
| 收藏 → 详情B | [favorites, detailB] | detailB → favorites |

已知近似(单栈限制,prd 记录):跨跳板链(详情A→词牌→作者→选词B)→ [home, detailA, rhythmic, author, detailB],旧详情A 在栈底,返回多步后清空——罕见路径,接受。

## 3. tab 切换语义(切 tab 放弃详情)

现状 `navigate(tab.route) { popUpTo(Routes.HOME) { saveState = true } ... }` 保留各 tab 独立栈——导致详情残留。改为:切 tab 时**清空详情层**:

```kotlin
// tab onClick:
if (current != tab.route) {
    nav.navigate(tab.route) {
        popUpTo(Routes.HOME) { saveState = true; inclusive = false }  // 弹掉详情层
        launchSingleTop = true
        restoreState = true
    }
}
```

现状 popUpTo(Routes.HOME) 已弹掉 home 之上的所有(含详情)——**核对现有实现是否已满足**:现有代码 `popUpTo(Routes.HOME) { saveState = true }` 正是弹到 home 保留 home;详情在 home 之上会被弹掉,但 saveState 会把详情也存进 tab 状态?saveState 保存**被弹出 destination 的状态**——切回时 restoreState 恢复该 tab 栈(含详情)!→ 详情残留的根源。修:**tab 导航弹详情时 saveState 不含详情** = 切 tab 时先把详情帧 pop 掉再 navigate(或在 tab onClick 先 popBackStack 到当前 tab 根):

```kotlin
// 切 tab 前:清掉内容层(详情/词牌/作者)
while (nav.currentBackStackEntry?.destination?.route != Routes.HOME &&
       !(nav.currentBackStackEntry?.destination?.route in TAB_ROUTES)) {
    nav.popBackStack()
}
nav.navigate(tab.route) { ...现有... }
```

实现时以"详情不随 tab 恢复"为准,具体用 popBackStack 清栈或 saveState=false 验证。

## 4. 深链

`LaunchedEffect(initialPoemId)` 已响应;深链进入详情 = openPoem(id) 同语义(冷启动无栈,直接压)。不回归。

## 5. 宽屏

- 宽屏无 bottomBar,现 WideTopBar;详情双栏已有
- 词牌/作者页宽屏现状全屏 → 不回归即可(侧栏化后续任务)
- openPoem/bottomBar 显隐逻辑与宽窄无关(宽屏 bottomBar 本就无)

## 验证矩阵

- 窄屏:三症状逐一复现验证消除
- 深链直达 + 返回
- 宽屏:现有双栏不回归
- desktopTest 全绿
