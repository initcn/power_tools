package com.initcn.powertools

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.initcn.powertools.navigation.AppNavigation
import com.initcn.powertools.permissions.PermissionChecker
import com.initcn.powertools.ui.onboarding.PermissionOnboardingScreen
import com.initcn.powertools.ui.theme.PowerToolsTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        setContent {

            PowerToolsTheme {

                val context =
                    LocalContext.current

                var missingPermissions by remember {

                    mutableStateOf(
                        PermissionChecker
                            .getMissingPermissions(
                                context
                            )
                    )
                }

                var optionalPermissions by remember {

                    mutableStateOf(
                        PermissionChecker
                            .getOptionalPermissions(
                                context
                            )
                    )
                }

                LifecycleResumeEffect(Unit) {

                    missingPermissions =
                        PermissionChecker
                            .getMissingPermissions(
                                context
                            )

                    optionalPermissions =
                        PermissionChecker
                            .getOptionalPermissions(
                                context
                            )

                    onPauseOrDispose { }
                }

                if (
                    missingPermissions.isEmpty()
                ) {

                    AppNavigation()

                } else {

                    PermissionOnboardingScreen(
                        missingPermissions =
                            missingPermissions,

                        optionalPermissions =
                            optionalPermissions
                    )
                }
            }
        }
    }
}