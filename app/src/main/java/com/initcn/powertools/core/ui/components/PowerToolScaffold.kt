package com.initcn.powertools.core.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PowerToolScaffold(
    title: String,
    topBar: @Composable (() -> Unit)? = null,
    content: @Composable (
        PaddingValues
    ) -> Unit
) {

    Scaffold(
        topBar = {

            if (topBar != null) {

                topBar()

            } else {

                TopAppBar(
                    title = {
                        Text(title)
                    }
                )
            }
        }
    ) { paddingValues ->

        content(
            paddingValues
        )
    }
}