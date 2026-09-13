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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dialy.app.presentation.theme.DiaryColors

@Composable
fun TodaysFocusSection(
    focus: String,
    onFocusChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var text by remember { mutableStateOf(focus) }

    androidx.compose.runtime.LaunchedEffect(focus) {
        if (focus != text) {
            text = focus
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(DiaryColors.GoldSoft.copy(alpha = 0.6f))
            .border(1.dp, DiaryColors.GoldAccent.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Text(
            text = "TODAY'S MAIN FOCUS",
            style = MaterialTheme.typography.labelSmall,
            color = DiaryColors.GoldAccent,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp
        )

        Spacer(modifier = Modifier.height(6.dp))

        BasicTextField(
            value = text,
            onValueChange = {
                text = it
                onFocusChange(it)
            },
            textStyle = TextStyle(
                fontFamily = FontFamily.Serif,
                fontSize = 16.5.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 24.sp,
                letterSpacing = 0.4.sp,
                color = androidx.compose.ui.graphics.Color(0xFF2B2621)
            ),
            cursorBrush = SolidColor(DiaryColors.GoldAccent),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { innerTextField ->
                Box {
                    if (text.isEmpty()) {
                        Text(
                            text = "Tap to set today's singular focus...",
                            style = TextStyle(
                                fontFamily = FontFamily.Serif,
                                fontSize = 15.sp,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                letterSpacing = 0.3.sp,
                                color = DiaryColors.GoldAccent.copy(alpha = 0.75f)
                            )
                        )
                    }
                    innerTextField()
                }
            }
        )
    }
}
