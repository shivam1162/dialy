package com.dialy.app.presentation.dayplanner.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dialy.app.presentation.theme.DiaryColors

@Composable
fun DailyReminderSection(
    reminder: String,
    onReminderChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var text by remember { mutableStateOf(reminder) }

    androidx.compose.runtime.LaunchedEffect(reminder) {
        if (reminder != text) {
            text = reminder
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(DiaryColors.SageSoft.copy(alpha = 0.5f))
            .border(1.dp, DiaryColors.SageAccent.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Text(
            text = "DAILY AFFIRMATION & REMINDER",
            style = MaterialTheme.typography.labelSmall,
            color = DiaryColors.SageAccent,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(4.dp))

        BasicTextField(
            value = text,
            onValueChange = {
                text = it
                onReminderChange(it)
            },
            textStyle = TextStyle(
                fontFamily = FontFamily.Serif,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                color = androidx.compose.ui.graphics.Color(0xFF263325)
            ),
            cursorBrush = SolidColor(DiaryColors.SageAccent),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { innerTextField ->
                Box {
                    if (text.isEmpty()) {
                        Text(
                            text = "“Be kind to yourself. You are doing your best.”",
                            style = TextStyle(
                                fontFamily = FontFamily.Serif,
                                fontStyle = FontStyle.Italic,
                                fontSize = 14.5.sp,
                                color = DiaryColors.SageAccent.copy(alpha = 0.75f)
                            )
                        )
                    }
                    innerTextField()
                }
            }
        )
    }
}
