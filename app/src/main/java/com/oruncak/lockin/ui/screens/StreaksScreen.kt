package com.oruncak.lockin.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oruncak.lockin.data.Repository

@Composable
fun StreaksScreen(repo: Repository) {
    // Demo history — swap for a real completion log once alarms record actual completions.
    val habitGrid = remember {
        listOf(1,1,1,0,1,1,1, 1,1,1,1,1,1,1, 1,1,0,1,1,1,1, 1,0,1,1,1,0,0)
    }

    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Text("Analytics", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile("Completion", "94%", Modifier.weight(1f))
            StatTile("Tasks", "114", Modifier.weight(1f))
            StatTile("Avg React", "4m12s", Modifier.weight(1f))
        }
        Spacer(Modifier.height(20.dp))
        Text("HABIT GRID — LAST 4 WEEKS", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Card(Modifier.fillMaxWidth()) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                modifier = Modifier.padding(12.dp).height(140.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(habitGrid) { v ->
                    Box(
                        Modifier.aspectRatio(1f).background(
                            if (v == 1) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(5.dp)
                        )
                    )
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        Text("STREAK MILESTONES", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp)) {
                Text("30-Day Perfect Lock — 12 days left", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(Modifier.height(6.dp))
                Text("100 Habits Enforced — Completed", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier) {
        Column(Modifier.padding(12.dp)) {
            Text(label.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}
