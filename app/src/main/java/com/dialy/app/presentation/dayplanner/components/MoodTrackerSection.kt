package com.dialy.app.presentation.dayplanner.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dialy.app.domain.model.Mood
import com.dialy.app.domain.model.MoodType
import com.dialy.app.presentation.theme.DiaryColors

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MoodTrackerSection(
    currentMood: Mood?,
    onSelectMood: (MoodType) -> Unit,
    modifier: Modifier = Modifier
) {
    val moods = listOf(
        Pair(MoodType.VERY_HAPPY, "😊 Joyful"),
        Pair(MoodType.HAPPY, "🙂 Happy"),
        Pair(MoodType.CALM, "😌 Peaceful"),
        Pair(MoodType.NEUTRAL, "😐 Neutral"),
        Pair(MoodType.TIRED, "😴 Tired"),
        Pair(MoodType.STRESSED, "😣 Stressed"),
        Pair(MoodType.EXCITED, "🤩 Excited")
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DiaryColors.CardBackground)
            .border(1.dp, DiaryColors.BorderSubtle, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Text(
            text = "MOOD TRACKER",
            style = MaterialTheme.typography.titleMedium,
            color = DiaryColors.GoldAccent,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            moods.forEach { (type, label) ->
                val isSelected = currentMood?.type == type
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isSelected) DiaryColors.GoldAccent else DiaryColors.GoldSoft.copy(alpha = 0.5f))
                        .clickable { onSelectMood(type) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 12.5.sp,
                        color = if (isSelected) Color.White else DiaryColors.TextPrimary
                    )
                }
            }
        }
    }
}
