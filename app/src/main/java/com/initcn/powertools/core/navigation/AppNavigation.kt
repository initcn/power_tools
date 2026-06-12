package com.initcn.powertools.core.navigation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.initcn.powertools.feature.callblocker.presentation.CallBlockerRoute
import com.initcn.powertools.feature.dns.presentation.DnsRoute
import com.initcn.powertools.feature.downloadsorganizer.presentation.DownloadsRoute
import com.initcn.powertools.feature.doze.presentation.DozeRoute
import com.initcn.powertools.feature.home.presentation.HomeScreen
import com.initcn.powertools.feature.vault.presentation.VaultRoute


@RequiresApi(Build.VERSION_CODES.R)
@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.HOME
    ) {
        composable(route = Routes.HOME) {
            HomeScreen(
                onToolSelected = { tool ->
                    navController.navigate(tool.route)
                }
            )
        }

        composable(route = Routes.DOZE) {
            DozeRoute(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(route = Routes.DNS) {
            DnsRoute(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(route = Routes.DOWNLOADS) {
            DownloadsRoute(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(route = Routes.VAULT_AUTH) {
            VaultRoute(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(route = Routes.CALL_BLOCKER) {
            CallBlockerRoute(onNavigateBack = { navController.popBackStack() })
        }
    }
}