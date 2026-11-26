package com.mydreamlinks.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mydreamlinks.app.DreamLinksApp
import com.mydreamlinks.app.ui.screens.DreamDetailScreen
import com.mydreamlinks.app.ui.screens.DreamEditorScreen
import com.mydreamlinks.app.ui.screens.DreamscapeScreen
import com.mydreamlinks.app.ui.screens.FeedScreen
import com.mydreamlinks.app.ui.screens.JournalScreen
import com.mydreamlinks.app.ui.screens.LinksScreen
import com.mydreamlinks.app.ui.screens.ProfileScreen
import com.mydreamlinks.app.ui.screens.ReadingsScreen
import com.mydreamlinks.app.ui.screens.SearchScreen
import com.mydreamlinks.app.ui.screens.SettingsScreen
import com.mydreamlinks.app.ui.theme.Ink

/** Where screens can send the user. */
class Go(private val nav: NavHostController) {
    fun back() { nav.popBackStack() }
    fun dream(id: Int) = nav.navigate("dream/$id")
    fun editDream(id: Int) = nav.navigate("dream/$id/edit")
    fun newDream() = nav.navigate("dream/new")
    /** After logging a dream, show it in place of the empty editor. */
    fun savedNewDream(id: Int) = nav.navigate("dream/$id") { popUpTo("dream/new") { inclusive = true } }
    /** With readings on, go to Readings instead, which spins until the new dream's reading lands. */
    fun readNewDream() = nav.navigate("readings") { popUpTo("dream/new") { inclusive = true } }
    fun user(id: Int) = nav.navigate("user/$id")
    fun compare(id: Int) = nav.navigate("dreamscape?with=$id")
    fun search() = nav.navigate("search")
    fun links() = nav.navigate("links")
    fun settings() = nav.navigate("settings")
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val TABS = listOf(
    Tab("feed", "Feed", Icons.Filled.Home),
    Tab("journal", "Journal", Icons.AutoMirrored.Filled.List),
    Tab("dream/new", "Log", Icons.Filled.AddCircle),
    Tab("dreamscape?with={with}", "Map", Icons.Filled.Share),
    Tab("readings", "Readings", Icons.Filled.Star),
)

@Composable
fun AppNav(app: DreamLinksApp) {
    val nav = rememberNavController()
    val go = Go(nav)
    val api = app.api
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route

    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) {
            NavHost(nav, startDestination = "feed") {
                composable("feed") { FeedScreen(api, go) }
                composable("journal") { JournalScreen(api, go) }
                composable("dream/new") { DreamEditorScreen(app, go, null) }
                composable("dream/{id}", listOf(navArgument("id") { type = NavType.IntType })) {
                    DreamDetailScreen(api, go, it.arguments!!.getInt("id"))
                }
                composable("dream/{id}/edit", listOf(navArgument("id") { type = NavType.IntType })) {
                    DreamEditorScreen(app, go, it.arguments!!.getInt("id"))
                }
                composable("dreamscape?with={with}", listOf(navArgument("with") { type = NavType.IntType; defaultValue = 0 })) {
                    DreamscapeScreen(api, go, it.arguments!!.getInt("with").takeIf { w -> w > 0 }) { with ->
                        nav.navigate("dreamscape?with=${with ?: 0}") { popUpTo("dreamscape?with={with}") { inclusive = true } }
                    }
                }
                composable("readings") { ReadingsScreen(api, go) }
                composable("user/{id}", listOf(navArgument("id") { type = NavType.IntType })) {
                    ProfileScreen(api, go, it.arguments!!.getInt("id"))
                }
                composable("search") { SearchScreen(api, go) }
                composable("links") { LinksScreen(api, go) }
                composable("settings") { SettingsScreen(api, go) }
            }
        }
        if (TABS.any { it.route == route }) {
            BottomBar(route) { tab ->
                nav.navigate(tab.route.replace("{with}", "0")) {
                    popUpTo("feed") { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        }
    }
}

/** The same bottom bar the website shows on phones: a sticker strip of five tabs. */
@Composable
private fun BottomBar(current: String?, onTab: (Tab) -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Box(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 10.dp, vertical = 8.dp)) {
        Box(Modifier.matchParentSize().padding(start = 4.dp, top = 4.dp).background(Ink.ink, shape))
        Row(
            Modifier.fillMaxWidth().padding(end = 4.dp, bottom = 4.dp).clip(shape).background(Ink.card).border(2.5.dp, Ink.ink, shape).padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            TABS.forEach { tab ->
                val on = tab.route == current
                Column(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (on) Ink.yellow else Ink.card)
                        .then(if (on) Modifier.border(2.dp, Ink.ink, RoundedCornerShape(14.dp)) else Modifier)
                        .clickable { onTab(tab) }
                        .semantics { role = Role.Tab; selected = on }
                        .padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(tab.icon, contentDescription = null, tint = Ink.ink, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.height(2.dp))
                    Text(tab.label, style = MaterialTheme.typography.labelMedium, color = Ink.ink)
                }
            }
        }
    }
}
