package com.initcn.powertools.feature.callblocker.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.initcn.powertools.core.theme.Dimens
import com.initcn.powertools.feature.callblocker.data.CallRuleEntity
import com.initcn.powertools.feature.callblocker.domain.RuleType

@Composable
fun RuleListTab(
    rules: List<CallRuleEntity>,
    onDeleteRule: (CallRuleEntity) -> Unit
) {
    if (rules.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No rules found.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyColumn(contentPadding = PaddingValues(bottom = Dimens.ListBottomFabClearance)) {
            items(rules, key = { it.id }) { rule ->
                ListItem(
                    headlineContent = { Text(rule.pattern, fontWeight = FontWeight.Bold) },
                    supportingContent = {
                        Text(buildString {
                            append(if (rule.ruleType == RuleType.BLOCKLIST_REGEX) "Regex Pattern" else "Exact Match")
                            if (!rule.label.isNullOrBlank()) append(" • ${rule.label}")
                        })
                    },
                    trailingContent = {
                        IconButton(onClick = { onDeleteRule(rule) }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                )
                HorizontalDivider(thickness = 0.5.dp)
            }
        }
    }
}