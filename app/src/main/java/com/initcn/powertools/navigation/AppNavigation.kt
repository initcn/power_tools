package com.initcn.powertools.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.initcn.powertools.ui.screens.dns.DnsScreen
import com.initcn.powertools.ui.screens.doze.DozeScreen
import com.initcn.powertools.ui.screens.downloads.DownloadsScreen
import com.initcn.powertools.ui.screens.home.HomeScreen

@Composable
fun AppNavigation() {

    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.HOME
    ) {

        composable(
            route = Routes.HOME
        ) {

            HomeScreen(
                onToolSelected = { tool ->

                    navController.navigate(
                        tool.route
                    )
                }
            )
        }

        composable(
            route = Routes.DOZE
        ) {

            DozeScreen()
        }

        composable(
            route = Routes.DNS
        ) {

            DnsScreen()
        }

        composable(
            route = Routes.DOWNLOADS
        ) {

            DownloadsScreen()
        }
    }
}