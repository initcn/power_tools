package com.initcn.powertools.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.initcn.powertools.R
import com.initcn.powertools.model.PowerTool
import com.initcn.powertools.ui.components.PowerCard
import com.initcn.powertools.ui.theme.Dimens
import com.initcn.powertools.utils.AppInfo

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

            item {
                Text(
                    text = stringResource(
                        R.string.home_title
                    ),
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(
                    modifier = Modifier.height(Dimens.SM)
                )

                Text(
                    text = stringResource(
                        R.string.home_subtitle
                    ),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

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