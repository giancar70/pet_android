package com.petapp.android.features.reminders

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.petapp.android.core.model.Pet
import com.petapp.android.core.model.Reminder
import com.petapp.android.core.model.ReminderCategory
import com.petapp.android.features.incidents.spanishDate
import java.time.LocalDateTime

private val BrandGreen = Color(0xFF406E5F)
private val SubtitleGray = Color(0xFF666666)
private val ContentBackground = Color(0xFFE3FBF1)
private val CardBorder = Color(0xFFEFEFF4)
private val IconCircleBg = Color(0xFFD9FEF2)

internal fun categoryFor(apiValue: String): ReminderCategory? =
    ReminderCategory.entries.firstOrNull { it.apiValue == apiValue }

// due_date is a DateTimeField; this client always sends a naive "yyyy-MM-ddTHH:mm:ss"
// local string with no zone suffix (see AnadirRecordatorioScreen.kt), but rows created
// before that migration may come back with a "Z"/offset suffix from the DB cast -- strip
// anything past the first 19 chars so both shapes parse the same way.
internal fun formatReminderDate(iso: String): String = try {
    val dt = LocalDateTime.parse(iso.take(19))
    "${spanishDate(dt.toLocalDate())} · %02d:%02d".format(dt.hour, dt.minute)
} catch (e: Exception) {
    iso
}

@Composable
fun RecordatoriosListScreen(
    selectedPet: Pet?,
    onBack: () -> Unit,
    onOpenDetail: (Reminder) -> Unit,
    viewModel: RemindersViewModel = viewModel(),
) {
    val listState by viewModel.listState.collectAsState()

    LaunchedEffect(selectedPet?.id) {
        selectedPet?.id?.let { viewModel.fetchRecordatorios(it) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ContentBackground)
            .verticalScroll(rememberScrollState()),
    ) {
        BackHandler(onBack = onBack)
        IconButton(onClick = onBack, modifier = Modifier.padding(start = 12.dp, top = 12.dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Recordatorios",
                color = BrandGreen,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Revisa y gestiona los recordatorios de tu mascota.",
                color = SubtitleGray,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(20.dp))

            when (val state = listState) {
                is RecordatoriosListUiState.Loading -> LoadingBoxRecordatorios()
                is RecordatoriosListUiState.Error -> Text(
                    text = state.message,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 14.sp,
                )
                is RecordatoriosListUiState.Loaded -> {
                    if (state.reminders.isEmpty()) {
                        Text(
                            text = "Aún no tienes recordatorios.",
                            color = SubtitleGray,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 24.dp),
                        )
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 24.dp),
                        ) {
                            state.reminders.forEach { reminder ->
                                RecordatorioRowItem(reminder = reminder, onClick = { onOpenDetail(reminder) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecordatorioRowItem(reminder: Reminder, onClick: () -> Unit) {
    val category = categoryFor(reminder.category)
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, CardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(IconCircleBg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = category?.let { iconFor(it) } ?: Icons.Filled.Notifications,
                    contentDescription = null,
                    tint = BrandGreen,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = reminder.title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(
                    text = "${category?.label ?: reminder.category} · ${formatReminderDate(reminder.dueDate)}",
                    color = SubtitleGray,
                    fontSize = 12.sp,
                )
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = SubtitleGray)
        }
    }
}

@Composable
private fun LoadingBoxRecordatorios() {
    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = BrandGreen)
    }
}
