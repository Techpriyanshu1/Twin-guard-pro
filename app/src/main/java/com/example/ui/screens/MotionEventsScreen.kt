package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.MotionPhotosOn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.db.MotionEventEntity
import com.example.data.db.TwinGuardDatabase
import com.example.recording.LocalRecorder
import com.example.ui.components.CctvTopBar
import com.example.ui.components.StatusBadge
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.BorderColor
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.SecurityAlertRed
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MotionEventsScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { TwinGuardDatabase.getDatabase(context) }
    val localRecorder = remember { LocalRecorder(context) }
    val events by db.motionEventDao().getAllEvents().collectAsState(initial = emptyList())

    var previewSnapshotPath by remember { mutableStateOf<String?>(null) }

    BackHandler {
        onNavigateBack()
    }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            CctvTopBar(
                title = "Motion Events",
                subtitle = "${events.size} Incidents Detected",
                onBack = onNavigateBack,
                actions = {
                    if (events.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                scope.launch {
                                    db.motionEventDao().clearAll()
                                    Toast.makeText(context, "All motion events cleared", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.testTag("clear_all_motion_events")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Clear All",
                                tint = SecurityAlertRed
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (events.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.MotionPhotosOn,
                        contentDescription = "No motion",
                        tint = TextSecondary,
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "No Motion Incidents Logged",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Instant snapshots will appear here whenever motion is detected.",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(events, key = { it.id }) { event ->
                    MotionEventCard(
                        event = event,
                        onViewSnapshot = { previewSnapshotPath = event.snapshotPath },
                        onDelete = {
                            scope.launch {
                                localRecorder.deleteMotionEvent(event)
                                Toast.makeText(context, "Event deleted", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }
        }
    }

    // Fullscreen Snapshot Modal
    if (previewSnapshotPath != null) {
        Dialog(onDismissRequest = { previewSnapshotPath = null }) {
            Surface(
                color = DarkSurface,
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Motion Snapshot", fontWeight = FontWeight.Bold, color = TextPrimary)
                        IconButton(onClick = { previewSnapshotPath = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    AsyncImage(
                        model = File(previewSnapshotPath!!),
                        contentDescription = "Motion Keyframe",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16 / 9f)
                            .clip(RoundedCornerShape(10.dp))
                    )
                }
            }
        }
    }
}

@Composable
fun MotionEventCard(
    event: MotionEventEntity,
    onViewSnapshot: () -> Unit,
    onDelete: () -> Unit
) {
    val dateStr = SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.US).format(Date(event.timestamp))

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Snapshot Thumbnail
                AsyncImage(
                    model = File(event.snapshotPath),
                    contentDescription = "Thumbnail",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onViewSnapshot)
                )

                Column {
                    Text(
                        text = "Motion Detected",
                        fontWeight = FontWeight.Bold,
                        color = WarningAmber,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = dateStr,
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        StatusBadge(text = "${event.confidence}% Score", color = WarningAmber, isPulsing = false)
                        StatusBadge(text = event.zoneName, color = AccentCyan, isPulsing = false)
                    }
                }
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.testTag("delete_event_${event.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = SecurityAlertRed
                )
            }
        }
    }
}
