package com.initcn.powertools.ui.screens.callblocker

import android.Manifest
import android.provider.CallLog
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.initcn.powertools.data.callblocker.CallRuleEntity
import com.initcn.powertools.data.callblocker.RuleType
import com.initcn.powertools.features.callblocker.CallRoleManager
import com.initcn.powertools.features.callblocker.ui.CallBlockerViewModel
import com.initcn.powertools.model.CallLogEntry
import com.initcn.powertools.permissions.PermissionChecker
import com.initcn.powertools.ui.components.PowerToolScaffold
import com.initcn.powertools.ui.theme.Dimens
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallBlockerScreen(
    viewModel: CallBlockerViewModel = viewModel()
) {
    val context = LocalContext.current

    // --- State & Data ---
    var selectedTab by remember { mutableIntStateOf(0) }
    var showAddDialog by remember { mutableStateOf(false) }
    var prefillNumber by remember { mutableStateOf("") }
    var prefillType by remember { mutableStateOf(RuleType.BLOCKLIST_EXACT) }

    val recentCalls by viewModel.recentCalls.collectAsStateWithLifecycle()
    val exactBlocklist by viewModel.exactBlocklist.collectAsStateWithLifecycle()
    val regexBlocklist by viewModel.regexBlocklist.collectAsStateWithLifecycle()
    val whitelist by viewModel.whitelist.collectAsStateWithLifecycle()

    // Filters & Behaviors
    val blockHidden by viewModel.blockHiddenNumbers.collectAsStateWithLifecycle()
    val blockUnsaved by viewModel.blockUnsavedContacts.collectAsStateWithLifecycle()
    val disallowCall by viewModel.disallowCall.collectAsStateWithLifecycle()
    val rejectCall by viewModel.rejectCall.collectAsStateWithLifecycle()
    val skipNotif by viewModel.skipNotif.collectAsStateWithLifecycle()
    val silenceCall by viewModel.silenceCall.collectAsStateWithLifecycle()

    // --- Permissions & Roles ---
    var hasScreeningRole by remember { mutableStateOf(CallRoleManager.hasCallScreeningRole(context)) }
    var hasCallLogPerm by remember { mutableStateOf(PermissionChecker.hasCallLogAccess(context)) }

    val roleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        hasScreeningRole = CallRoleManager.hasCallScreeningRole(context)
    }

    val callLogPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCallLogPerm = granted
        if (granted) viewModel.fetchRecentCalls()
    }

    // --- Import / Export Launchers ---
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.importRules(it) }
    }

    // --- UI Event Listener (Toasts for success/error) ---
    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    LifecycleResumeEffect(Unit) {
        hasScreeningRole = CallRoleManager.hasCallScreeningRole(context)
        hasCallLogPerm = PermissionChecker.hasCallLogAccess(context)
        if (hasCallLogPerm) viewModel.fetchRecentCalls()
        onPauseOrDispose { }
    }

    PowerToolScaffold(title = "Call Blocker") { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            // Role Warning Banner
            if (!hasScreeningRole) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth().padding(Dimens.MD)
                ) {
                    Row(modifier = Modifier.padding(Dimens.MD), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(Dimens.MD))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Call Screening Disabled", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                            Text("Tap to set PowerTools as default to enable active system level call blocking.", style = MaterialTheme.typography.bodySmall)
                        }
                        Button(
                            onClick = {
                                CallRoleManager.createRoleRequestIntent(context)?.let { roleLauncher.launch(it) }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) { Text("Enable") }
                    }
                }
            }

            // Tabs
            PrimaryTabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Recent") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Blocklist") })
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Whitelist") })
                Tab(selected = selectedTab == 3, onClick = { selectedTab = 3 }, text = { Text("Settings") })
            }

            // Content
            Box(modifier = Modifier.weight(1f)) {
                when (selectedTab) {
                    0 -> CallLogTab(
                        hasPermission = hasCallLogPerm,
                        recentCalls = recentCalls,
                        whitelist = whitelist,
                        exactBlocklist = exactBlocklist,
                        regexBlocklist = regexBlocklist,
                        onRequestPermission = { callLogPermissionLauncher.launch(Manifest.permission.READ_CALL_LOG) },
                        onAddToRule = { number, type -> prefillNumber = number; prefillType = type; showAddDialog = true }
                    )
                    1 -> RulesTab(rules = exactBlocklist + regexBlocklist, onDeleteRule = { viewModel.removeRule(it) })
                    2 -> RulesTab(rules = whitelist, onDeleteRule = { viewModel.removeRule(it) })
                    3 -> SettingsTab(
                        blockHidden = blockHidden, onToggleHidden = { viewModel.toggleBlockHiddenNumbers(it) },
                        blockUnsaved = blockUnsaved, onToggleUnsaved = { viewModel.toggleBlockUnsavedContacts(it) },
                        disallowCall = disallowCall, onToggleDisallow = { viewModel.toggleDisallowCall(it) },
                        rejectCall = rejectCall, onToggleReject = { viewModel.toggleRejectCall(it) },
                        skipNotif = skipNotif, onToggleSkip = { viewModel.toggleSkipNotif(it) },
                        silenceCall = silenceCall, onToggleSilence = { viewModel.toggleSilenceCall(it) },
                        onExportRules = { viewModel.exportRules() },
                        onImportRules = { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) }
                    )
                }
            }
        }

        // FAB
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
                    viewModel.addRule(pattern, type, label)
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
private fun SettingsTab(
    blockHidden: Boolean, onToggleHidden: (Boolean) -> Unit,
    blockUnsaved: Boolean, onToggleUnsaved: (Boolean) -> Unit,
    disallowCall: Boolean, onToggleDisallow: (Boolean) -> Unit,
    rejectCall: Boolean, onToggleReject: (Boolean) -> Unit,
    skipNotif: Boolean, onToggleSkip: (Boolean) -> Unit,
    silenceCall: Boolean, onToggleSilence: (Boolean) -> Unit,
    onExportRules: () -> Unit,
    onImportRules: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // --- FILTERS SECTION ---
        Text(
            text = "Call Filters",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = Dimens.MD, top = Dimens.MD, bottom = Dimens.XS)
        )

        ListItem(
            headlineContent = { Text("Block Hidden Numbers") },
            supportingContent = { Text("Automatically intercept private or restricted caller IDs.") },
            trailingContent = { Switch(checked = blockHidden, onCheckedChange = onToggleHidden) }
        )
        HorizontalDivider(thickness = 0.5.dp)

        ListItem(
            headlineContent = { Text("Block Unsaved Contacts") },
            supportingContent = { Text("Only allow numbers saved in your phone book.") },
            trailingContent = { Switch(checked = blockUnsaved, onCheckedChange = onToggleUnsaved) }
        )
        HorizontalDivider(thickness = 0.5.dp)

        // --- BEHAVIOR SECTION ---
        Text(
            text = "Interception Behavior",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = Dimens.MD, top = Dimens.MD, bottom = Dimens.XS)
        )

        ListItem(
            headlineContent = { Text("Off") },
            supportingContent = { Text("Filtered calls are allowed to ring normally.") },
            leadingContent = {
                RadioButton(selected = !disallowCall && !silenceCall, onClick = { onToggleDisallow(false); onToggleSilence(false) })
            },
            modifier = Modifier.clickable { onToggleDisallow(false); onToggleSilence(false) }
        )

        ListItem(
            headlineContent = { Text("Silence") },
            supportingContent = { Text("Mute the device ringer, but let the call through.") },
            leadingContent = {
                RadioButton(selected = !disallowCall && silenceCall, onClick = { onToggleDisallow(false); onToggleSilence(true) })
            },
            modifier = Modifier.clickable { onToggleDisallow(false); onToggleSilence(true) }
        )

        ListItem(
            headlineContent = { Text("Block") },
            supportingContent = { Text("Prevent the call from reaching your device.") },
            leadingContent = {
                RadioButton(selected = disallowCall, onClick = { onToggleDisallow(true) })
            },
            modifier = Modifier.clickable { onToggleDisallow(true) }
        )

        AnimatedVisibility(visible = disallowCall, enter = expandVertically(), exit = shrinkVertically()) {
            Column(modifier = Modifier.padding(start = Dimens.XXL)) {
                ListItem(
                    headlineContent = { Text("Reject Call (Hang Up)") },
                    supportingContent = { Text("Instantly drop the connection instead of letting it ring silently.") },
                    trailingContent = { Switch(checked = rejectCall, onCheckedChange = onToggleReject) }
                )
                ListItem(
                    headlineContent = { Text("Skip Notifications") },
                    supportingContent = { Text("Keep your missed calls list and notification tray clean.") },
                    trailingContent = { Switch(checked = skipNotif, onCheckedChange = onToggleSkip) }
                )
            }
        }
        HorizontalDivider(thickness = 0.5.dp)

        // --- DATA MANAGEMENT SECTION ---
        Text(
            text = "Data Management",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = Dimens.MD, top = Dimens.MD, bottom = Dimens.XS)
        )

        ListItem(
            headlineContent = { Text("Export Rules") },
            supportingContent = { Text("Backup your blocklists and whitelists to a JSON file.") },
            leadingContent = { Icon(Icons.Default.Upload, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
            modifier = Modifier.clickable { onExportRules() }
        )
        ListItem(
            headlineContent = { Text("Import Rules") },
            supportingContent = { Text("Restore rules from a previously saved JSON backup.") },
            leadingContent = { Icon(Icons.Default.Download, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
            modifier = Modifier.clickable { onImportRules() }
        )
        Spacer(modifier = Modifier.height(Dimens.XXL))
    }
}

// ... (CallLogTab, RulesTab, and AddRuleDialog remain unchanged from the previous version)

@Composable
private fun CallLogTab(
    hasPermission: Boolean,
    recentCalls: List<CallLogEntry>,
    whitelist: List<CallRuleEntity>,
    exactBlocklist: List<CallRuleEntity>,
    regexBlocklist: List<CallRuleEntity>,
    onRequestPermission: () -> Unit,
    onAddToRule: (String, RuleType) -> Unit
) {
    if (!hasPermission) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Call Log access required.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(Dimens.SM))
                Button(onClick = onRequestPermission) {
                    Text("Grant Permission")
                }
            }
        }
        return
    }

    if (recentCalls.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No recent calls found.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyColumn(contentPadding = PaddingValues(bottom = 80.dp)) {
            items(recentCalls) { call ->

                val dateStr = remember(call.date) {
                    SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault()).format(Date(call.date))
                }

                val cleanNumber = remember(call.number) {
                    call.number.filter { it.isDigit() || it == '+' }
                }

                val isWhitelisted = remember(cleanNumber, whitelist) {
                    whitelist.any { it.pattern.filter { c -> c.isDigit() || c == '+' } == cleanNumber }
                }

                val isBlocked = remember(cleanNumber, call.number, exactBlocklist, regexBlocklist) {
                    exactBlocklist.any { it.pattern.filter { c -> c.isDigit() || c == '+' } == cleanNumber } ||
                            regexBlocklist.any { rule ->
                                try { Regex(rule.pattern).containsMatchIn(call.number) } catch (_: Exception) { false }
                            }
                }

                ListItem(
                    headlineContent = {
                        Text(text = call.number, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    },
                    supportingContent = {
                        Text(
                            text = if (!call.name.isNullOrBlank()) "${call.name} • $dateStr" else dateStr,
                            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    leadingContent = {
                        when {
                            isBlocked -> Icon(Icons.Default.Block, contentDescription = "Blocked", tint = MaterialTheme.colorScheme.error)
                            isWhitelisted -> Icon(Icons.Default.CheckCircle, contentDescription = "Whitelisted", tint = MaterialTheme.colorScheme.primary)
                            call.type == CallLog.Calls.OUTGOING_TYPE -> Icon(Icons.AutoMirrored.Filled.CallMade, contentDescription = "Outgoing", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            call.type == CallLog.Calls.MISSED_TYPE -> Icon(Icons.AutoMirrored.Filled.CallMissed, contentDescription = "Missed", tint = MaterialTheme.colorScheme.error)
                            call.type == CallLog.Calls.BLOCKED_TYPE || call.type == CallLog.Calls.REJECTED_TYPE -> Icon(Icons.Default.Block, contentDescription = "System Blocked", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            else -> Icon(Icons.AutoMirrored.Filled.CallReceived, contentDescription = "Incoming", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    },
                    trailingContent = {
                        if (!isBlocked && !isWhitelisted) {
                            IconButton(onClick = { onAddToRule(call.number, RuleType.BLOCKLIST_EXACT) }) {
                                Icon(Icons.Default.Add, contentDescription = "Add Rule", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                )
                HorizontalDivider(thickness = 0.5.dp)
            }
        }
    }
}

@Composable
private fun RulesTab(rules: List<CallRuleEntity>, onDeleteRule: (CallRuleEntity) -> Unit) {
    if (rules.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No rules found.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyColumn(contentPadding = PaddingValues(bottom = 80.dp)) {
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
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                )
                HorizontalDivider(thickness = 0.5.dp)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddRuleDialog(
    initialNumber: String,
    initialType: RuleType,
    onDismiss: () -> Unit,
    onConfirm: (pattern: String, type: RuleType, label: String?) -> Unit
) {
    var pattern by remember { mutableStateOf(initialNumber) }
    var label by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(initialType) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Rule") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.SM)) {
                OutlinedTextField(value = pattern, onValueChange = { pattern = it }, label = { Text("Number or Regex") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = label, onValueChange = { label = it }, label = { Text("Label (Optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(Dimens.XS))
                Text("Rule Type", style = MaterialTheme.typography.labelMedium)
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        RadioButton(selected = selectedType == RuleType.BLOCKLIST_EXACT, onClick = { selectedType = RuleType.BLOCKLIST_EXACT })
                        Text("Block Exact Number")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        RadioButton(selected = selectedType == RuleType.BLOCKLIST_REGEX, onClick = { selectedType = RuleType.BLOCKLIST_REGEX })
                        Text("Block by Regex")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        RadioButton(selected = selectedType == RuleType.WHITELIST, onClick = { selectedType = RuleType.WHITELIST })
                        Text("Whitelist (Always Allow)")
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { if (pattern.isNotBlank()) onConfirm(pattern.trim(), selectedType, label.takeIf { it.isNotBlank() }) }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}