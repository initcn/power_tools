// MainActivity.kt
package com.initcn.powertools

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.fragment.app.FragmentActivity
import com.initcn.powertools.core.navigation.AppNavigation
import com.initcn.powertools.core.theme.PowerToolsTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PowerToolsTheme {
                // Navigate straight to the app. Permissions are handled per-feature now.
                AppNavigation()
            }
        }
    }
}