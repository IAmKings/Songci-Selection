package com.songci.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.songci.app.data.Dynasty
import com.songci.app.data.Rhythmic
import com.songci.app.data.SongciRepository
import com.songci.app.data.createDatabaseDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.songci.app.theme.SongciTheme
import com.songci.app.ui.screens.AuthorPoemsScreen
import com.songci.app.ui.screens.AuthorsScreen
import com.songci.app.ui.screens.DetailScreen
import com.songci.app.ui.screens.DynastyAuthorsScreen
import com.songci.app.ui.screens.DynastyListScreen
import com.songci.app.ui.screens.FavoritesScreen
import com.songci.app.ui.screens.HomeScreen
import com.songci.app.ui.screens.IndexScreen
import com.songci.app.ui.screens.RhythmicPoemsScreen
import com.songci.app.ui.screens.RhythmicsScreen
import com.songci.app.ui.screens.SearchScreen
import com.songci.app.ui.screens.SettingsScreen
import androidx.savedstate.SavedState
import androidx.savedstate.read

/** CMP navigation 2.9 的参数为 SavedState,经 SavedStateReader 读取。 */
private fun SavedState?.string(key: String): String? =
    this?.read { getStringOrNull(key) }

private fun SavedState?.long(key: String): Long =
    this?.read { getLongOrNull(key) } ?: 0L

private object Routes {
    const val HOME = "home"
    const val SEARCH = "search"
    const val FAVORITES = "favorites"
    const val SETTINGS = "settings"
    const val INDEX = "index"
    const val INDEX_DYNASTY = "index/dynasty"
    const val INDEX_DYNASTY_AUTHORS = "index/dynasty/{dynasty}"
    const val INDEX_AUTHORS = "index/authors"
    const val INDEX_RHYTHMICS = "index/rhythmics"
    const val AUTHOR = "author/{authorId}?poemId={poemId}"
    const val RHYTHMIC = "rhythmic/{rhythmic}?poemId={poemId}"
    const val DETAIL = "detail/{poemId}"
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val TABS = listOf(
    Tab(Routes.HOME, "首页", Icons.Filled.Home),
    Tab(Routes.INDEX, "索引", IndexIcon),
    Tab(Routes.FAVORITES, "收藏", BookmarkIcon),   // 书签语义:书页挑选→书签保留
    Tab(Routes.SETTINGS, "设置", Icons.Filled.Settings),
)

@Composable
fun SongciApp(
    initialPoemId: Long? = null,
    deepLinkToken: Int = 0,
    deepLinkQueue: kotlinx.coroutines.channels.Channel<Long>? = null,
) {
    SongciTheme {
        var repository by remember { mutableStateOf<SongciRepository?>(null) }
        LaunchedEffect(Unit) {
            repository = withContext(Dispatchers.Default) {
                SongciRepository(
                    db = com.songci.app.data.db.SongciDb(createDatabaseDriver()),
                    dynasty = Dynasty.load(),
                    rhythmic = Rhythmic.load(),
                )
            }
        }
        val repo = repository
        if (repo == null) {
            LoadingScreen()
            return@SongciTheme
        }
        val vm: AppViewModel = viewModel { AppViewModel(repo) }
        val nav = rememberNavController()

        /** 内容层路由(详情/词牌/作者):全屏盖 tab。 */
        fun isContentRoute(route: String?) = route != null &&
            (route.startsWith("detail/") || route.startsWith("rhythmic/") || route.startsWith("author/"))

        /**
         * 进入详情(统一入口):同层唯一。
         * 当前在内容层时替换,避免详情叠加:
         * - 详情页收深链(inclusive=true 连自身弹掉,彻底替换,否则目标=栈顶时保留自身会叠层)
         * - 跳板(词牌/作者)选词(inclusive=false 保留跳板)
         * 其余入口(首页/搜索/收藏/冷启动深链)直接压栈。
         * 跨跳板链(详情→词牌→作者→选词)旧详情留在栈底,单栈限制的已知近似(prd 记录)。
         */
        fun openPoem(id: Long) {
            val current = nav.currentBackStackEntry?.destination?.route
            nav.navigate("detail/$id") {
                if (current != null && isContentRoute(current)) {
                    popUpTo(current) { inclusive = current.startsWith("detail/") }
                }
                launchSingleTop = true
            }
        }

        /**
         * 切 tab 差异化规则:
         * 1. 先清内容层(详情/词牌/作者)——切 tab 放弃内容层上下文(已定决策)
         * 2. 频道层内部栈 saveState/restoreState——索引 tab 子页浏览位置保留(挖宝翻阅感),
         *    内容层已提前弹出,不会被恢复成"双详情"
         */
        fun switchTab(route: String, current: String?) {
            if (current == route) return
            while (isContentRoute(nav.currentBackStackEntry?.destination?.route)) {
                nav.popBackStack()
            }
            nav.navigate(route) {
                popUpTo(Routes.HOME) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }

        // 深链事件通道(macOS):组合内挂起迭代直接导航——事件→导航闭环,
        // 不经参数传递(state 参数层快照脱节,实证 token 恒 0)。同词重复事件天然逐次处理
        LaunchedEffect(nav, deepLinkQueue) {
            for (id in deepLinkQueue ?: return@LaunchedEffect) {
                val current = nav.currentBackStackEntry?.destination?.route
                nav.navigate("detail/$id") {
                    if (current != null && isContentRoute(current)) {
                        popUpTo(current) { inclusive = current.startsWith("detail/") }
                    }
                    launchSingleTop = true
                }
            }
        }

        // deep link 起始词作(iOS/Android 经 initialPoemId 参数):首帧跳转;值变化也响应;
        // deepLinkToken 每次深链事件递增——同一词重复点击也强制重导航(LaunchedEffect 按值判重)
        LaunchedEffect(initialPoemId, deepLinkToken) {
            initialPoemId?.let { openPoem(it) }
        }

        BoxWithConstraints(Modifier.fillMaxSize()) {
            val wide = maxWidth >= 768.dp
            val currentRoute = nav.currentBackStackEntryAsState().value?.destination?.route
            val showChrome = !isContentRoute(currentRoute)   // 内容层(详情/词牌/作者)全屏盖导航

            Row(Modifier.fillMaxSize()) {
                // 宽屏:频道层用 rail(图标+标签竖排);内容层隐藏
                if (wide && showChrome) {
                    NavigationRail(containerColor = com.songci.app.theme.SongciColors.surfaceContainerLow) {
                        TABS.forEach { tab ->
                            NavigationRailItem(
                                selected = currentRoute == tab.route,
                                onClick = { switchTab(tab.route, currentRoute) },
                                icon = { Icon(tab.icon, contentDescription = tab.label) },
                                label = { Text(tab.label, style = MaterialTheme.typography.labelMedium) },
                            )
                        }
                        // 搜索入口统一在首页顶栏(与窄屏一致),rail 不重复放
                    }
                }
                Box(Modifier.weight(1f)) {
                    Scaffold(
                        containerColor = com.songci.app.theme.SongciColors.background,
                        topBar = {},
                        bottomBar = if (!wide && showChrome) {
                            {
                                NavigationBar(containerColor = com.songci.app.theme.SongciColors.surfaceContainerLow) {
                                    TABS.forEach { tab ->
                                        val current = nav.currentBackStackEntryAsState().value?.destination?.route
                                        NavigationBarItem(
                                            selected = current == tab.route,
                                            onClick = { switchTab(tab.route, current) },
                                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                                            label = { Text(tab.label, style = MaterialTheme.typography.labelMedium) },
                                        )
                                    }
                                }
                            }
                        } else {
                            {}
                        },
                    ) { padding ->
                        Box(Modifier.fillMaxSize().padding(padding)) {
                    NavHost(navController = nav, startDestination = Routes.HOME) {
                        composable(Routes.HOME) {
                            HomeScreen(
                                vm = vm,
                                onOpenSearch = { nav.navigate(Routes.SEARCH) },
                                onOpenPoem = { id -> openPoem(id) },
                            )
                        }
                        composable(Routes.SEARCH) {
                            SearchScreen(vm = vm, onBack = { nav.popBackStack() }) { poem ->
                                openPoem(poem.id)
                            }
                        }
                        composable(Routes.FAVORITES) {
                            FavoritesScreen(vm = vm) { id -> openPoem(id) }
                        }
                        composable(Routes.SETTINGS) {
                            SettingsScreen(vm = vm)
                        }
                        composable(Routes.INDEX) {
                            IndexScreen(onOpen = { route -> nav.navigate(route) })
                        }
                        composable(Routes.INDEX_DYNASTY) {
                            DynastyListScreen(vm = vm, onBack = { nav.popBackStack() }) { d ->
                                nav.navigate("index/dynasty/${encodePath(d)}")
                            }
                        }
                        composable(
                            Routes.INDEX_DYNASTY_AUTHORS,
                            arguments = listOf(navArgument("dynasty") { type = NavType.StringType }),
                        ) { entry ->
                            val dynasty = entry.arguments.string("dynasty").orEmpty()
                            DynastyAuthorsScreen(vm = vm, dynasty = dynasty, onBack = { nav.popBackStack() }) { id ->
                                nav.navigate("author/$id")
                            }
                        }
                        composable(Routes.INDEX_AUTHORS) {
                            AuthorsScreen(vm = vm, onBack = { nav.popBackStack() }) { id ->
                                nav.navigate("author/$id")
                            }
                        }
                        composable(Routes.INDEX_RHYTHMICS) {
                            RhythmicsScreen(vm = vm, onBack = { nav.popBackStack() }) { r ->
                                nav.navigate("rhythmic/${encodePath(r)}")
                            }
                        }
                        composable(
                            Routes.AUTHOR,
                            arguments = listOf(
                                navArgument("authorId") { type = NavType.LongType },
                                navArgument("poemId") { type = NavType.LongType; defaultValue = -1L },
                            ),
                        ) { entry ->
                            val authorId = entry.arguments.long("authorId")
                            AuthorPoemsScreen(
                                vm = vm, authorId = authorId,
                                wide = wide,
                                initialPoemId = entry.arguments.long("poemId").takeIf { it > 0 },
                                onBack = { nav.popBackStack() },
                                onPoem = { id -> openPoem(id) },
                                onOpenAuthor = { id, curId -> nav.navigate("author/$id?poemId=$curId") },
                                onOpenRhythmic = { r, curId -> nav.navigate("rhythmic/${encodePath(r)}?poemId=$curId") },
                            )
                        }
                        composable(
                            Routes.RHYTHMIC,
                            arguments = listOf(
                                navArgument("rhythmic") { type = NavType.StringType },
                                navArgument("poemId") { type = NavType.LongType; defaultValue = -1L },
                            ),
                        ) { entry ->
                            val rhythmic = entry.arguments.string("rhythmic").orEmpty()
                            val currentPoemId = entry.arguments.long("poemId").takeIf { it > 0 } ?: -1L
                            RhythmicPoemsScreen(
                                vm = vm, rhythmic = rhythmic,
                                wide = wide,
                                initialPoemId = entry.arguments.long("poemId").takeIf { it > 0 },
                                onBack = { nav.popBackStack() },
                                onPoem = { id -> openPoem(id) },
                                onOpenAuthor = { id, curId -> nav.navigate("author/$id?poemId=$curId") },
                                onOpenRhythmic = { r, curId -> nav.navigate("rhythmic/${encodePath(r)}?poemId=$curId") },
                            )
                        }
                        composable(
                            Routes.DETAIL,
                            arguments = listOf(navArgument("poemId") { type = NavType.LongType }),
                        ) { entry ->
                            val poemId = entry.arguments.long("poemId")
                            DetailScreen(
                                vm = vm, poemId = poemId, wide = wide,
                                onBack = { nav.popBackStack() },
                                onOpenAuthor = { id -> nav.navigate("author/$id?poemId=$poemId") },
                                onOpenRhythmic = { r -> nav.navigate("rhythmic/${encodePath(r)}?poemId=$poemId") },
                            )
                        }
                    }
                        }
                    }
                }
            }
        }
    }
}

/** 首启加载闸门:数据库复制/打开在后台线程完成后进入主界面。 */
@Composable
private fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize().background(com.songci.app.theme.SongciColors.background),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "正在加载词库…",
            style = MaterialTheme.typography.titleMedium,
            color = com.songci.app.theme.SongciColors.stone,
        )
    }
}

