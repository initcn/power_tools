package com.initcn.powertools.feature.home.domain

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.EnhancedEncryption
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.ui.graphics.vector.ImageVector
import com.initcn.powertools.core.navigation.Routes
import com.initcn.powertools.R

enum class PowerTool(
    val route: String,
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
    val icon: ImageVector
) {
    DOZE(Routes.DOZE, R.string.tool_doze_title, R.string.tool_doze_desc, Icons.Outlined.Bedtime),
    COLOR_FILTER(Routes.COLOR_FILTER, R.string.tool_color_title, R.string.tool_color_desc, Icons.Outlined.Palette),
    DNS(Routes.DNS, R.string.tool_dns_title, R.string.tool_dns_desc, Icons.Outlined.Dns),
    DOWNLOADS(Routes.DOWNLOADS, R.string.tool_downloads_title, R.string.tool_downloads_desc, Icons.Outlined.Folder),
    SECURE_VAULT(Routes.VAULT_AUTH, R.string.tool_vault_title, R.string.tool_vault_desc, Icons.Outlined.EnhancedEncryption),
    CALL_BLOCKER(Routes.CALL_BLOCKER, R.string.tool_call_blocker_title, R.string.tool_call_blocker_desc, Icons.Outlined.Block),
    FLIP_ACTION(Routes.FLIP_ACTION, R.string.tool_flip_title, R.string.tool_flip_desc, Icons.Outlined.Vibration)
}