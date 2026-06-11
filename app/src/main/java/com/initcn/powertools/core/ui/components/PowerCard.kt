package com.initcn.powertools.core.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.initcn.powertools.core.theme.Dimens

@Composable
fun PowerCard(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    onClick: () -> Unit
) {

    Card(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = Dimens.CardMinHeight)
            .clickable(
                enabled = enabled,
                onClick = onClick
            ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = Dimens.ElevationSM
        )
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.MD),
            verticalAlignment = Alignment.CenterVertically
        ) {

            if (icon != null) {

                Icon(
                    imageVector = icon,
                    contentDescription = null
                )

                Spacer(
                    modifier = Modifier.width(Dimens.SM)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(
                    Dimens.XS
                )
            ) {

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}