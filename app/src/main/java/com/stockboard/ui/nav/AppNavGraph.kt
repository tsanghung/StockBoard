package com.stockboard.ui.nav

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.stockboard.ui.ads.BannerAdComposable
import com.stockboard.ui.home.HomeScreen
import com.stockboard.ui.manage.ManageScreen
import com.stockboard.ui.manage.WatchlistScreen
import com.stockboard.ui.news.NewsScreen
import com.stockboard.ui.theme.CardDark
import com.stockboard.ui.theme.ColorUp
import com.stockboard.ui.theme.TextSecondary

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        bottomBar = {
            Column {
                BannerAdComposable(adUnitId = com.stockboard.BuildConfig.ADMOB_BANNER_ID)

                NavigationBar(containerColor = CardDark) {
                    NavigationBarItem(
                        icon = { Icon(Icons.Filled.Home, contentDescription = "首頁") },
                        label = { Text("首頁") },
                        selected = currentDestination?.hierarchy?.any { it.route == "home" } == true,
                        onClick = {
                            navController.navigate("home") {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = ColorUp,
                            selectedTextColor = ColorUp,
                            indicatorColor = ColorUp.copy(alpha = 0.15f),
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary
                        )
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Filled.Newspaper, contentDescription = "新聞") },
                        label = { Text("財經新聞") },
                        selected = currentDestination?.hierarchy?.any { it.route == "news" } == true,
                        onClick = {
                            navController.navigate("news") {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = ColorUp,
                            selectedTextColor = ColorUp,
                            indicatorColor = ColorUp.copy(alpha = 0.15f),
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary
                        )
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Filled.Add, contentDescription = "新增") },
                        label = { Text("新增自選") },
                        selected = currentDestination?.hierarchy?.any { it.route == "manage" } == true,
                        onClick = {
                            navController.navigate("manage") {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = ColorUp,
                            selectedTextColor = ColorUp,
                            indicatorColor = ColorUp.copy(alpha = 0.15f),
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary
                        )
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Filled.Delete, contentDescription = "刪除") },
                        label = { Text("管理清單") },
                        selected = currentDestination?.hierarchy?.any { it.route == "watchlist" } == true,
                        onClick = {
                            navController.navigate("watchlist") {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = ColorUp,
                            selectedTextColor = ColorUp,
                            indicatorColor = ColorUp.copy(alpha = 0.15f),
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("home") { HomeScreen() }
            composable("news") { NewsScreen() }
            composable("manage") { ManageScreen() }
            composable("watchlist") { WatchlistScreen() }
        }
    }
}
