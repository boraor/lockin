package com.oruncak.lockin.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.oruncak.lockin.data.AppEntry

/**
 * Reused for both the per-alarm lock scope and the System Exemptions list —
 * same picker, just a different title and a different selected-set going in/out.
 */
@Composable
fun AppPickerSheet(
    title: String,
    subtitle: String,
    apps: List<AppEntry>,
    initiallySelected: Set<String>,
    onDismiss: () -> Unit,
    onSave: (Set<String>) -> Unit
) {
    var selected by remember { mutableStateOf(initiallySelected) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
            Row(
                Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
                Text(title, fontWeight = FontWeight.Bold)
                TextButton(onClick = { onSave(selected) }) { Text("Done") }
            }
            Text(subtitle, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(bottom = 8.dp))
            LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                items(apps) { app ->
                    val checked = selected.contains(app.packageName)
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Checkbox(
                            checked = checked,
                            onCheckedChange = {
                                selected = if (checked) selected - app.packageName else selected + app.packageName
                            }
                        )
                        Text(app.label, modifier = Modifier.weight(1f))
                    }
                }
                if (apps.isEmpty()) {
                    item { Text("No user-installed apps found on this device.", modifier = Modifier.padding(vertical = 16.dp)) }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}
