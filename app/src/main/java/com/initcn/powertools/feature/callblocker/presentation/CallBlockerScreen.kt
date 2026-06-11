package com.initcn.powertools.feature.callblocker.presentation

import android.Manifest
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.initcn.powertools.core.permissions.PermissionChecker
import com.initcn.powertools.core.theme.Dimens
import com.initcn.powertools.core.ui.components.PowerToolScaffold
import com.initcn.powertools.feature.callblocker.domain.RuleType
import com.initcn.powertools.feature.callblocker.presentation.components.AddRuleDialog
import com.initcn.powertools.feature.callblocker.presentation.components.CallBlockerSettingsTab
import com.initcn.powertools.feature.callblocker.presentation.components.RecentCallsTab
import com.initcn.powertools.feature.callblocker.presentation.components.RuleListTab
import com.initcn.powertools.feature.callblocker.service.CallRoleManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallBlockerScreen(
    viewModel: CallBlockerViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    // MVI Single Source of Truth
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) }
    var showAddDialog by remember { mutableStateOf(false) }
    var prefillNumber by remember { mutableStateOf("") }
    var prefillType by remember { mutableStateOf(RuleType.BLOCKLIST_EXACT) }

    var hasScreeningRole by remember { mutableStateOf(CallRoleManager.hasCallScreeningRole(context)) }

    // Track both permissions
    var hasCallLogPerm by remember { mutableStateOf(PermissionChecker.hasCallLogAccess(context)) }
    var hasContactsPerm by remember { mutableStateOf(PermissionChecker.hasContactsAccess(context)) }

    val roleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        hasScreeningRole = CallRoleManager.hasCallScreeningRole(context)
    }

    val callLogPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasCallLogPerm = granted
        if (granted) viewModel.onEvent(CallBlockerEvent.FetchRecentCalls)
    }

    val contactsPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasContactsPerm = granted
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
        hasCallLogPerm = PermissionChecker.hasCallLogAccess(context)
        hasContactsPerm = PermissionChecker.hasContactsAccess(context)
        if (hasCallLogPerm) viewModel.onEvent(CallBlockerEvent.FetchRecentCalls)
        onPauseOrDispose { }
    }

    PowerToolScaffold(title = "Call Blocker") { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {

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
                            onClick = { CallRoleManager.createRoleRequestIntent(context)?.let { roleLauncher.launch(it) } },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) { Text("Enable") }
                    }
                }
            }

            PrimaryTabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Recent") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Blocklist") })
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Whitelist") })
                Tab(selected = selectedTab == 3, onClick = { selectedTab = 3 }, text = { Text("Settings") })
            }

            Box(modifier = Modifier.weight(1f)) {
                when (selectedTab) {
                    0 -> RecentCallsTab(
                        hasCallLogPermission = hasCallLogPerm,
                        hasContactsPermission = hasContactsPerm,
                        recentCalls = state.recentCalls,
                        whitelist = state.whitelist,
                        exactBlocklist = state.exactBlocklist,
                        regexBlocklist = state.regexBlocklist,
                        onRequestCallLogPermission = { callLogPermissionLauncher.launch(Manifest.permission.READ_CALL_LOG) },
                        onRequestContactsPermission = { contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS) },
                        onAddToRule = { number, type -> prefillNumber = number; prefillType = type; showAddDialog = true }
                    )
                    1 -> RuleListTab(rules = state.exactBlocklist + state.regexBlocklist, onDeleteRule = { viewModel.onEvent(CallBlockerEvent.RemoveRule(it)) })
                    2 -> RuleListTab(rules = state.whitelist, onDeleteRule = { viewModel.onEvent(CallBlockerEvent.RemoveRule(it)) })
                    3 -> CallBlockerSettingsTab(
                        state = state,
                        hasCallLogPermission = hasCallLogPerm, // ADD THIS
                        hasContactsPermission = hasContactsPerm,
                        onRequestCallLogPermission = { callLogPermissionLauncher.launch(Manifest.permission.READ_CALL_LOG) }, // ADD THIS
                        onRequestContactsPermission = { contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS) },
                        onEvent = { event ->
                            if (event is CallBlockerEvent.ImportRules) {
                                importLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                            } else {
                                viewModel.onEvent(event)
                            }
                        }
                    )
                }
            }
        }

        if (selectedTab == 1 || selectedTab == 2) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomEnd) {
                FloatingActionButton(
                    onClick = {
                        prefillNumber = ""
                        prefillType = if (selectedTab == 1) RuleType.BLOCKLIST_EXACT else RuleType.WHITELIST
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
                    viewModel.onEvent(CallBlockerEvent.AddRule(pattern, type, label))
                    showAddDialog = false
                }
            )
        }
    }
}