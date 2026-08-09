package com.songci.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
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
    const val AUTHOR = "author/{authorId}"
    const val RHYTHMIC = "rhythmic/{rhythmic}"
    const val DETAIL = "detail/{poemId}"
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val TABS = listOf(
    Tab(Routes.HOME, "首页", Icons.Filled.Home),
    Tab(Routes.FAVORITES, "收藏", Icons.Filled.Favorite),
    Tab(Routes.SETTINGS, "设置", Icons.Filled.Settings),
)

@Composable
fun SongciApp(initialPoemId: Long? = null) {
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
        // deep link 起始词作(小组件阅读全文): 首帧跳转;值变化(macOS 运行中点击)也响应
        LaunchedEffect(initialPoemId) {
            initialPoemId?.let { nav.navigate("detail/$it") { launchSingleTop = true } }
        }

        BoxWithConstraints(Modifier.fillMaxSize()) {
            val wide = maxWidth >= 768.dp

            Scaffold(
                containerColor = com.songci.app.theme.SongciColors.background,
                topBar = if (wide) {
                    {
                        val currentRoute = nav.currentBackStackEntryAsState().value?.destination?.route
                        WideTopBar(currentRoute, nav::navigate)
                    }
                } else {
                    {}
                },
                bottomBar = if (!wide) {
                    {
                        NavigationBar(containerColor = com.songci.app.theme.SongciColors.surfaceContainerLow) {
                            TABS.forEach { tab ->
                                val current = nav.currentBackStackEntryAsState().value?.destination?.route
                                NavigationBarItem(
                                    selected = current == tab.route,
                                    onClick = {
                                        if (current != tab.route) {
                                            nav.navigate(tab.route) {
                                                popUpTo(Routes.HOME) { saveState = true }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    },
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
                                onOpenIndex = { nav.navigate(Routes.INDEX) },
                                onOpenSearch = { nav.navigate(Routes.SEARCH) },
                                onOpenPoem = { id -> nav.navigate("detail/$id") },
                            )
                        }
                        composable(Routes.SEARCH) {
                            SearchScreen(vm = vm, onBack = { nav.popBackStack() }) { poem ->
                                nav.navigate("detail/${poem.id}")
                            }
                        }
                        composable(Routes.FAVORITES) {
                            FavoritesScreen(vm = vm) { id -> nav.navigate("detail/$id") }
                        }
                        composable(Routes.SETTINGS) {
                            SettingsScreen(vm = vm)
                        }
                        composable(Routes.INDEX) {
                            IndexScreen(onBack = { nav.popBackStack() }) { route -> nav.navigate(route) }
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
                            arguments = listOf(navArgument("authorId") { type = NavType.LongType }),
                        ) { entry ->
                            val authorId = entry.arguments.long("authorId")
                            AuthorPoemsScreen(
                                vm = vm, authorId = authorId,
                                onBack = { nav.popBackStack() },
                                onPoem = { id -> nav.navigate("detail/$id") },
                            )
                        }
                        composable(
                            Routes.RHYTHMIC,
                            arguments = listOf(navArgument("rhythmic") { type = NavType.StringType }),
                        ) { entry ->
                            val rhythmic = entry.arguments.string("rhythmic").orEmpty()
                            RhythmicPoemsScreen(
                                vm = vm, rhythmic = rhythmic,
                                onBack = { nav.popBackStack() },
                                onPoem = { id -> nav.navigate("detail/$id") },
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
                                onOpenAuthor = { id -> nav.navigate("author/$id") },
                                onOpenRhythmic = { r -> nav.navigate("rhythmic/${encodePath(r)}") },
                            )
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

/** 宽屏顶栏:标题 + 首页/收藏/设置页签 + 搜索。 */
@Composable
private fun WideTopBar(currentRoute: String?, navigate: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().background(com.songci.app.theme.SongciColors.surfaceContainerLow).padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "宋词选粹",
            style = MaterialTheme.typography.headlineMedium,
            color = com.songci.app.theme.SongciColors.primary,
            modifier = Modifier.padding(end = 32.dp),
        )
        TABS.forEach { tab ->
            val route = currentRoute
            Text(
                tab.label,
                style = MaterialTheme.typography.labelLarge,
                color = if (route == tab.route) com.songci.app.theme.SongciColors.primary else com.songci.app.theme.SongciColors.stone,
                modifier = Modifier.padding(end = 24.dp).clickable {
                    if (route != tab.route) navigate(tab.route)
                },
            )
        }
        Text(
            "搜索",
            style = MaterialTheme.typography.labelLarge,
            color = com.songci.app.theme.SongciColors.primary,
            modifier = Modifier.padding(start = 16.dp).clickable { navigate(Routes.SEARCH) },
        )
    }
}
