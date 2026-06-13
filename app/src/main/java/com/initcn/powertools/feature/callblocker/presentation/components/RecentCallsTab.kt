package com.initcn.powertools.feature.callblocker.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.initcn.powertools.R
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.initcn.powertools.core.theme.Dimens
import com.initcn.powertools.feature.callblocker.data.CallRuleEntity
import com.initcn.powertools.feature.callblocker.domain.CallLogEntry
import com.initcn.powertools.feature.callblocker.domain.RuleType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RecentCallsTab(
    recentCalls: List<CallLogEntry>,
    whitelist: List<CallRuleEntity>,
    exactBlocklist: List<CallRuleEntity>,
    regexBlocklist: List<CallRuleEntity>,
    onAddToRule: (String, RuleType) -> Unit,
    onRemoveRule: (CallRuleEntity) -> Unit // Added callback for removing rules
) {
    Column(modifier = Modifier.fillMaxSize()) {

        // EMPTY STATE
        if (recentCalls.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.no_recent_calls),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            return@Column
        }

        // LIST STATE
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(Dimens.MD),
            verticalArrangement = Arrangement.spacedBy(Dimens.SM)
        ) {
            items(recentCalls, key = { "${it.number}_${it.date}" }) { call ->

                // Find exact entities instead of just checking if they exist
                val whitelistedRule = whitelist.find { it.pattern == call.number }
                val blockedRule = exactBlocklist.find { it.pattern == call.number }

                RecentCallItem(
                    call = call,
                    whitelistedRule = whitelistedRule,
                    blockedRule = blockedRule,
                    onAddToRule = onAddToRule,
                    onRemoveRule = onRemoveRule
                )
            }
        }
    }
}

@Composable
fun RecentCallItem(
    call: CallLogEntry,
    whitelistedRule: CallRuleEntity?,
    blockedRule: CallRuleEntity?,
    onAddToRule: (String, RuleType) -> Unit,
    onRemoveRule: (CallRuleEntity) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    val isBlocked = blockedRule != null
    val isWhitelisted = whitelistedRule != null

    // Format timestamp
    val formatter = remember { SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault()) }
    val dateString = remember(call.date) { formatter.format(Date(call.date)) }

    // Determine call type icon
    val callIcon = when (call.type) {
        1 -> Icons.AutoMirrored.Filled.CallReceived // Incoming
        2 -> Icons.AutoMirrored.Filled.CallMade     // Outgoing
        3 -> Icons.AutoMirrored.Filled.CallMissed   // Missed
        5 -> Icons.Default.Block                    // Rejected/Blocked
        else -> Icons.Default.Call
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.SM),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon Avatar
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = callIcon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(Dimens.MD))

            // Call Info
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = call.name ?: call.number,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Show status indicator if active in rules
                    if (isBlocked) {
                        Spacer(modifier = Modifier.width(Dimens.XS))
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Blocked",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                    } else if (isWhitelisted) {
                        Spacer(modifier = Modifier.width(Dimens.XS))
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Whitelisted",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (call.name != null) {
                        Text(
                            text = call.number,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = " • ",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = dateString,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Actions Menu
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More options")
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    if (isBlocked) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.remove_from_blocklist), color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                onRemoveRule(blockedRule)
                                showMenu = false
                            }
                        )
                    } else {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.block_number)) },
                            onClick = {
                                onAddToRule(call.number, RuleType.BLOCKLIST_EXACT)
                                showMenu = false
                            }
                        )
                    }

                    if (isWhitelisted) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.remove_from_whitelist), color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                onRemoveRule(whitelistedRule)
                                showMenu = false
                            }
                        )
                    } else {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.whitelist_number)) },
                            onClick = {
                                onAddToRule(call.number, RuleType.WHITELIST)
                                showMenu = false
                            }
                        )
                    }
                }
            }
        }
    }
}