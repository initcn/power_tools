package com.initcn.powertools.feature.callblocker.presentation

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.initcn.powertools.R
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.initcn.powertools.core.permissions.RequiredPermission
import com.initcn.powertools.core.theme.Dimens
import com.initcn.powertools.core.ui.components.FeaturePermissionGuard
import com.initcn.powertools.core.ui.components.PowerToolScaffold
import com.initcn.powertools.feature.callblocker.domain.RuleType
import com.initcn.powertools.feature.callblocker.presentation.components.AddRuleDialog
import com.initcn.powertools.feature.callblocker.presentation.components.CallBlockerSettingsTab
import com.initcn.powertools.feature.callblocker.presentation.components.RecentCallsTab
import com.initcn.powertools.feature.callblocker.presentation.components.RuleListTab
import com.initcn.powertools.feature.callblocker.service.CallRoleManager
import kotlinx.coroutines.launch

// ROUTE (Smart Composable)
// Handles JIT Permissions, Hilt, Context, Launchers, and Lifecycle.

@Composable
fun CallBlockerRoute(
    onNavigateBack: () -> Unit,
    viewModel: CallBlockerViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // 1. JIT Guard handles standard Required Android Permissions
    FeaturePermissionGuard(
        requiredPermissions = listOf(
            RequiredPermission.READ_CALL_LOG,
            RequiredPermission.READ_CONTACTS
        ),
        onNavigateBack = onNavigateBack
    ) {
        // This content only runs when Call Logs and Contacts are granted!

        var hasScreeningRole by remember { mutableStateOf(CallRoleManager.hasCallScreeningRole(context)) }

        val roleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            hasScreeningRole = CallRoleManager.hasCallScreeningRole(context)
        }

        val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let { viewModel.onEvent(CallBlockerEvent.ImportRules(it)) }
        }

        LaunchedEffect(Unit) {
            viewModel.uiEvent.collect { message ->
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        }

        LifecycleResumeEffect(Unit) {
            hasScreeningRole = CallRoleManager.hasCallScreeningRole(context)
            // Fetch calls automatically since JIT Guard guarantees we have the permission
            viewModel.onEvent(CallBlockerEvent.FetchRecentCalls)
            onPauseOrDispose { }
        }

        CallBlockerScreen(
            state = state,
            hasScreeningRole = hasScreeningRole,
            onEvent = viewModel::onEvent,
            onRequestRole = { CallRoleManager.createRoleRequestIntent(context)?.let { roleLauncher.launch(it) } },
            onImportRules = { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) }
        )
    }
}

//  SCREEN (Dumb Composable)
// Pure UI representation. No standard permission logic here!

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallBlockerScreen(
    state: CallBlockerUiState,
    hasScreeningRole: Boolean,
    onEvent: (CallBlockerEvent) -> Unit,
    onRequestRole: () -> Unit,
    onImportRules: () -> Unit
) {
    // Pager state for swiping between tabs
    val pagerState = rememberPagerState(pageCount = { 4 })
    val coroutineScope = rememberCoroutineScope()

    var showAddDialog by remember { mutableStateOf(false) }
    var prefillNumber by remember { mutableStateOf("") }
    var prefillType by remember { mutableStateOf(RuleType.BLOCKLIST_EXACT) }

    PowerToolScaffold(title = stringResource(R.string.call_blocker)) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {

            // 2. The Special System Role UI (Non-blocking warning card)
            if (!hasScreeningRole) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth().padding(Dimens.MD)
                ) {
                    Row(
                        modifier = Modifier.padding(Dimens.MD),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(Dimens.MD))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Call Screening Disabled", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                            Text("Tap to set PowerTools as default to enable active system level call blocking.", style = MaterialTheme.typography.bodySmall)
                        }
                        Button(
                            onClick = onRequestRole,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) { Text("Enable") }
                    }
                }
            }

            PrimaryTabRow(selectedTabIndex = pagerState.currentPage) {
                val tabs = listOf("Recent", "Blocklist", "Whitelist", "Settings")
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        },
                        text = { Text(title) }
                    )
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                when (page) {
                    // Inside CallBlockerScreen -> HorizontalPager block
                    0 -> RecentCallsTab(
                        recentCalls = state.recentCalls,
                        whitelist = state.whitelist,
                        exactBlocklist = state.exactBlocklist,
                        regexBlocklist = state.regexBlocklist,
                        onAddToRule = { number, type ->
                            prefillNumber = number
                            prefillType = type
                            showAddDialog = true
                        },
                        onRemoveRule = { rule ->
                            onEvent(CallBlockerEvent.RemoveRule(rule))
                        }
                    )
                    1 -> RuleListTab(rules = state.exactBlocklist + state.regexBlocklist, onDeleteRule = { onEvent(CallBlockerEvent.RemoveRule(it)) })
                    2 -> RuleListTab(rules = state.whitelist, onDeleteRule = { onEvent(CallBlockerEvent.RemoveRule(it)) })
                    3 -> CallBlockerSettingsTab(
                        state = state,
                        onEvent = { event ->
                            if (event is CallBlockerEvent.ImportRules) {
                                onImportRules()
                            } else {
                                onEvent(event)
                            }
                        }
                    )
                }
            }
        }

        val currentTab = pagerState.currentPage
        if (currentTab == 1 || currentTab == 2) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomEnd) {
                FloatingActionButton(
                    onClick = {
                        prefillNumber = ""
                        prefillType = if (currentTab == 1) RuleType.BLOCKLIST_EXACT else RuleType.WHITELIST
                        showAddDialog = true
                    },
                    modifier = Modifier.padding(Dimens.LG)
                ) { Icon(Icons.Default.Add, contentDescription = "Add Rule") }
            }
        }

        if (showAddDialog) {
            AddRuleDialog(
                initialNumber = prefillNumber,
                initialType = prefillType,
                onDismiss = { showAddDialog = false },
                onConfirm = { pattern, type, label ->
                    onEvent(CallBlockerEvent.AddRule(pattern, type, label))
                    showAddDialog = false
                }
            )
        }
    }
}