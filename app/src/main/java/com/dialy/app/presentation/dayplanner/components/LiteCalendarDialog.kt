package com.dialy.app.presentation.dayplanner.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.dialy.app.presentation.theme.DiaryColors
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Ultra-lightweight, zero-lag, stationery-aesthetic calendar dialog.
 * Opens instantly without heavyweight Material3 date picker overhead.
 */
@Composable
fun LiteCalendarDialog(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    var displayedYearMonth by remember(selectedDate) {
        mutableStateOf(YearMonth.of(selectedDate.year, selectedDate.month))
    }

    val today = remember { LocalDate.now() }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth(0.90f)
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, DiaryColors.BorderSubtle, RoundedCornerShape(20.dp)),
            color = DiaryColors.CardBackground,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header: Month & Year + Arrow Navigators
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(DiaryColors.PeachSoft)
                            .clickable {
                                displayedYearMonth = displayedYearMonth.minusMonths(1)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Previous Month",
                            tint = DiaryColors.GoldAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Text(
                        text = displayedYearMonth.format(
                            DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = DiaryColors.TextPrimary
                    )

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(DiaryColors.PeachSoft)
                            .clickable {
                                displayedYearMonth = displayedYearMonth.plusMonths(1)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Next Month",
                            tint = DiaryColors.GoldAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Days of week header (Su Mo Tu We Th Fr Sa)
                val dayNames = listOf("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    dayNames.forEach { dayName ->
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = dayName,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = DiaryColors.TextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Month Calendar Grid
                val firstDayOfMonth = displayedYearMonth.atDay(1)
                val daysInMonth = displayedYearMonth.lengthOfMonth()
                val startDayOfWeekOffset = firstDayOfMonth.dayOfWeek.value % 7 // Sunday = 0
                val totalCells = if (startDayOfWeekOffset + daysInMonth > 35) 42 else 35

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (row in 0 until totalCells / 7) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            for (col in 0 until 7) {
                                val cellIndex = (row * 7) + col
                                val dayNumber = cellIndex - startDayOfWeekOffset + 1
                                val isValidDay = dayNumber in 1..daysInMonth

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(38.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isValidDay) {
                                        val cellDate = displayedYearMonth.atDay(dayNumber)
                                        val isSelected = cellDate == selectedDate
                                        val isToday = cellDate == today

                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    when {
                                                        isSelected -> DiaryColors.GoldAccent
                                                        isToday -> DiaryColors.PeachSoft
                                                        else -> Color.Transparent
                                                    }
                                                )
                                                .then(
                                                    if (isToday && !isSelected) {
                                                        Modifier.border(1.dp, DiaryColors.GoldAccent, CircleShape)
                                                    } else {
                                                        Modifier
                                                    }
                                                )
                                                .clickable {
                                                    onDateSelected(cellDate)
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "$dayNumber",
                                                fontSize = 13.sp,
                                                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                                                color = when {
                                                    isSelected -> Color.White
                                                    isToday -> DiaryColors.GoldAccent
                                                    else -> DiaryColors.TextPrimary
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Bottom actions: "Today" shortcut and "Cancel"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            onDateSelected(today)
                        }
                    ) {
                        Text(
                            text = "Today",
                            color = DiaryColors.GoldAccent,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }

                    TextButton(
                        onClick = onDismissRequest
                    ) {
                        Text(
                            text = "Cancel",
                            color = DiaryColors.TextSecondary,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}
