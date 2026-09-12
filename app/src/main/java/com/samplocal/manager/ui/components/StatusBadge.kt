package com.samplocal.manager.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.samplocal.manager.data.model.ServerStatus
import com.samplocal.manager.ui.strings.Strings
import com.samplocal.manager.ui.strings.badgeText
import com.samplocal.manager.ui.theme.Green
import com.samplocal.manager.ui.theme.Orange
import com.samplocal.manager.ui.theme.Red
import com.samplocal.manager.ui.theme.Yellow

@Composable
fun StatusBadge(status: ServerStatus, T: Strings) {
    val label = badgeText(status, T)
    val color = when (status) {
        ServerStatus.RUNNING -> Green
        ServerStatus.STARTING -> Yellow
        ServerStatus.STOPPING -> Orange
        ServerStatus.INSTALLING -> Yellow
        ServerStatus.CRASHED -> Red
        ServerStatus.ERROR -> Red
        ServerStatus.STOPPED -> Red
    }
    Row(
        modifier = Modifier
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Canvas(modifier = Modifier.size(10.dp)) {
            drawCircle(color = color)
        }
        Text(label, color = color, fontSize = 13.sp)
    }
}
