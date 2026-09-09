package com.oruncak.lockin.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.oruncak.lockin.data.AppEntry
import com.oruncak.lockin.data.Repository

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
    val context = LocalContext.current
    val repo = remember { Repository.get(context) }
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
                        val bitmap = remember(app.packageName) {
                            repo.appIcon(app.packageName)?.let {
                                runCatching { it.toBitmap(width = 96, height = 96).asImageBitmap() }.getOrNull()
                            }
                        }
                        if (bitmap != null) {
                            Image(
                                painter = BitmapPainter(bitmap),
                                contentDescription = null,
                                modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp))
                            )
                        } else {
                            Box(Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant))
                        }
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
