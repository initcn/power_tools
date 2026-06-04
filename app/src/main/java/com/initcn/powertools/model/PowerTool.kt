package com.initcn.powertools.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Dns
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
        description = "Temporarily override screen timeout duration.",
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
        description = "Sort Downloads by file type.",
        icon = Icons.Outlined.Folder
    )
}