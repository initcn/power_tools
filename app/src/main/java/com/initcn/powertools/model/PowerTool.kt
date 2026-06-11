package com.initcn.powertools.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.EnhancedEncryption
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.ui.graphics.vector.ImageVector
import com.initcn.powertools.navigation.Routes

enum class PowerTool(
    val route: String,
    val title: String,
    val description: String,
    val icon: ImageVector
) {

    DOZE(
        route = Routes.DOZE,
        title = "Screen Doze",
        description = "Override screen timeout duration.",
        icon = Icons.Outlined.Bedtime
    ),

    DNS(
        route = Routes.DNS,
        title = "DNS Switcher",
        description = "Quickly switch between DNS providers.",
        icon = Icons.Outlined.Dns
    ),

    DOWNLOADS(
        route = Routes.DOWNLOADS,
        title = "Downloads Organizer",
        description = "Sort Downloads folder by file type.",
        icon = Icons.Outlined.Folder
    ),

    SECURE_VAULT(
        route = Routes.VAULT_AUTH,
        title = "Secure Vault",
        description = "Hardware-encrypted virtual sandbox for sensitive files.",
        icon = Icons.Outlined.EnhancedEncryption
    ),

    CALL_BLOCKER(
    route = Routes.CALL_BLOCKER,
    title = "Call Blocker",
    description = "Advanced call screening, regex blocking, and precise whitelisting.",
    icon = Icons.Outlined.Block
    )
}