package com.dialy.app.presentation.dayplanner.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dialy.app.domain.model.ScheduleItem
import com.dialy.app.presentation.theme.DiaryColors

@Composable
fun ScheduleSection(
    schedule: List<ScheduleItem>,
    onUpdateScheduleSlot: (slot: String, activity: String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DiaryColors.CardBackground)
            .border(1.dp, DiaryColors.BorderSubtle, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Text(
            text = "DAILY SCHEDULE",
            style = MaterialTheme.typography.titleMedium,
            color = DiaryColors.PeachAccent,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        val displaySlots = if (schedule.isNotEmpty()) schedule else defaultSlots()

        displaySlots.forEach { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Time Slot Tag
                Text(
                    text = item.timeSlot,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = DiaryColors.PeachAccent,
                    modifier = Modifier.width(48.dp)
                )

                // Ruled Activity Line
                Column(modifier = Modifier.weight(1f)) {
                    BasicTextField(
                        value = item.activity,
                        onValueChange = {
                            onUpdateScheduleSlot(item.timeSlot, it)
                        },
                        textStyle = TextStyle(
                            fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif,
                            fontSize = 14.sp,
                            letterSpacing = 0.2.sp,
                            color = DiaryColors.TextPrimary
                        ),
                        cursorBrush = SolidColor(DiaryColors.PeachAccent),
                        modifier = Modifier.fillMaxWidth(),
                        decorationBox = { innerTextField ->
                            Box {
                                if (item.activity.isEmpty()) {
                                    Text(
                                        text = "Plan this time slot...",
                                        style = TextStyle(
                                            fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif,
                                            fontSize = 13.sp,
                                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                            color = DiaryColors.PeachAccent.copy(alpha = 0.55f)
                                        )
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(DiaryColors.RuledLine)
                    )
                }
            }
        }
    }
}

private fun defaultSlots(): List<ScheduleItem> {
    val times = listOf("07:00", "08:00", "09:00", "10:00", "11:00", "12:00", "13:00", "14:00", "15:00", "16:00", "17:00", "18:00", "19:00", "20:00", "21:00")
    return times.map { ScheduleItem(plannerDate = "", timeSlot = it) }
}
