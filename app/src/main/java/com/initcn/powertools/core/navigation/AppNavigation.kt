package com.initcn.powertools.core.navigation

// FIXED: Changed the import to track VaultMainScreen instead of the non-existent VaultAuthScreen
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.initcn.powertools.core.ui.components.PowerToolScaffold
import com.initcn.powertools.feature.callblocker.presentation.CallBlockerScreen
import com.initcn.powertools.feature.dns.presentation.DnsScreen
import com.initcn.powertools.feature.downloadsorganizer.presentation.DownloadsScreen
import com.initcn.powertools.feature.doze.DozeScreen
import com.initcn.powertools.feature.home.presentation.HomeScreen
import com.initcn.powertools.feature.vault.presentation.VaultMainScreen

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

        composable(route = Routes.DOZE) { DozeScreen() }
        composable(route = Routes.DNS) { DnsScreen() }
        composable(route = Routes.DOWNLOADS) { DownloadsScreen() }


        composable(route = Routes.VAULT_AUTH) {
            VaultMainScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(route = Routes.VAULT_DASHBOARD) {
            PowerToolScaffold(title = "Vault Dashboard") {
                Text(
                    text = "Vault initialized and decrypted. Minimize the application and open your default system 'Files' picker to browse or drag items directly inside!",
                    modifier = Modifier.padding(it)
                )
            }
        }

        composable(route = Routes.CALL_BLOCKER) { CallBlockerScreen() }
    }
}