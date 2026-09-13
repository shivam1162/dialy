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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dialy.app.domain.model.Mood
import com.dialy.app.domain.model.MoodType
import com.dialy.app.presentation.theme.DiaryColors

@Composable
fun MoodTrackerSection(
    currentMood: Mood?,
    onSelectMood: (MoodType) -> Unit,
    modifier: Modifier = Modifier
) {
    // 5 faces from sad to happiest
    val moodFaces = listOf(
        Pair(MoodType.STRESSED, "😞"),
        Pair(MoodType.SAD, "🙁"),
        Pair(MoodType.NEUTRAL, "😐"),
        Pair(MoodType.HAPPY, "🙂"),
        Pair(MoodType.VERY_HAPPY, "😄")
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

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            moodFaces.forEach { (type, emoji) ->
                val isSelected = currentMood?.type == type
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSelectMood(type) }
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) DiaryColors.PeachSoft else DiaryColors.SubtleCard.copy(alpha = 0.6f)
                            )
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) DiaryColors.GoldAccent else DiaryColors.BorderSubtle,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = emoji,
                            fontSize = 24.sp
                        )
                    }

                    // Heart icon beneath face matching stationery reference
                    Text(
                        text = if (isSelected) "♥" else "♡",
                        fontSize = 15.sp,
                        color = if (isSelected) DiaryColors.RoseAccent else DiaryColors.TextTertiary
                    )
                }
            }
        }
    }
}
