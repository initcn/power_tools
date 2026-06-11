package com.initcn.powertools.core.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun StatusMessage(
    message: String?,
    modifier: Modifier = Modifier
) {

    if (message.isNullOrBlank()) {
        return
    }

    Text(
        text = message,
        modifier = modifier,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}