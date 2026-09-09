package com.oruncak.lockin.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * A number that steps with –/+ but can also be tapped to type an exact value directly.
 * The edit field uses a fixed comfortable width (not IntrinsicSize — that fights Compose's
 * text field layout and stretches unpredictably) sized generously enough for up to 4 digits.
 *
 * @param display how to render the committed value, e.g. { "$it min" } or { "%02d".format(it) }.
 */
@Composable
fun EditableNumberField(
    value: Int,
    min: Int,
    max: Int,
    wrap: Boolean = false,
    display: (Int) -> String = { it.toString() },
    onChange: (Int) -> Unit
) {
    var editing by remember { mutableStateOf(false) }
    var draft by remember(editing) { mutableStateOf(value.toString()) }

    if (editing) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it.filter { c -> c.isDigit() }.take(4) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                textStyle = MaterialTheme.typography.titleMedium,
                modifier = Modifier.width(84.dp)
            )
            IconButton(onClick = {
                val v = draft.toIntOrNull()?.coerceIn(min, max) ?: value
                onChange(v)
                editing = false
            }) { Text("✓", fontWeight = FontWeight.Bold) }
        }
    } else {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(onClick = {
                val next = value - 1
                onChange(if (next < min) (if (wrap) max else min) else next)
            }) { Text("–", fontSize = 18.sp, fontWeight = FontWeight.Bold) }
            Text(
                display(value),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .padding(horizontal = 2.dp)
                    .clickable { editing = true }
            )
            IconButton(onClick = {
                val next = value + 1
                onChange(if (next > max) (if (wrap) min else max) else next)
            }) { Text("+", fontSize = 18.sp, fontWeight = FontWeight.Bold) }
        }
    }
}
