package com.initcn.powertools.feature.home.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.initcn.powertools.core.theme.Dimens
import com.initcn.powertools.core.ui.components.PowerCard
import com.initcn.powertools.core.utils.AppInfo
import com.initcn.powertools.feature.home.domain.PowerTool

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onToolSelected: (PowerTool) -> Unit
) {
    val tools = PowerTool.entries

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(text = AppInfo.APP_NAME)
                }
            )
        }
    ) { paddingValues ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(Dimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.MD)
        ) {

            items(
                items = tools,
                key = { it.name }
            ) { tool ->

                PowerCard(
                    title = tool.title,
                    subtitle = tool.description,
                    icon = tool.icon,
                    onClick = {
                        onToolSelected(tool)
                    }
                )
            }
        }
    }
}