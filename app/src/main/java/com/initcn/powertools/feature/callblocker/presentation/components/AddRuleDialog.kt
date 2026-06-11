package com.initcn.powertools.feature.callblocker.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.initcn.powertools.core.theme.Dimens
import com.initcn.powertools.core.ui.components.PowerAlertDialog
import com.initcn.powertools.feature.callblocker.domain.RuleType

@Composable
fun AddRuleDialog(
    initialNumber: String,
    initialType: RuleType,
    onDismiss: () -> Unit,
    onConfirm: (pattern: String, type: RuleType, label: String?) -> Unit
) {
    var pattern by remember { mutableStateOf(initialNumber) }
    var label by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(initialType) }

    PowerAlertDialog(
        title = "Add Rule",
        confirmText = "Save",
        onDismiss = onDismiss,
        onConfirm = {
            if (pattern.isNotBlank()) onConfirm(
                pattern.trim(),
                selectedType,
                label.takeIf { it.isNotBlank() }
            )
        }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.SM)) {
            OutlinedTextField(
                value = pattern,
                onValueChange = { pattern = it },
                label = { Text("Number or Regex") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                label = { Text("Label (Optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(Dimens.XS))
            Text("Rule Type", style = MaterialTheme.typography.labelMedium)
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    RadioButton(
                        selected = selectedType == RuleType.BLOCKLIST_EXACT,
                        onClick = { selectedType = RuleType.BLOCKLIST_EXACT })
                    Text("Block Exact Number")
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    RadioButton(
                        selected = selectedType == RuleType.BLOCKLIST_REGEX,
                        onClick = { selectedType = RuleType.BLOCKLIST_REGEX })
                    Text("Block by Regex")
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    RadioButton(
                        selected = selectedType == RuleType.WHITELIST,
                        onClick = { selectedType = RuleType.WHITELIST })
                    Text("Whitelist (Always Allow)")
                }
            }
        }
    }
}