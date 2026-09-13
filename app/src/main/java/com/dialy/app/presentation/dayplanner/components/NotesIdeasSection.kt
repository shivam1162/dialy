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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dialy.app.presentation.theme.DiaryColors

@Composable
fun NotesIdeasSection(
    notes: String,
    onNotesChange: (String) -> Unit,
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
            text = "NOTES & IDEAS",
            style = MaterialTheme.typography.titleMedium,
            color = DiaryColors.GoldAccent,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        BasicTextField(
            value = notes,
            onValueChange = onNotesChange,
            textStyle = TextStyle(
                fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
                fontSize = 15.sp,
                lineHeight = 24.sp,
                letterSpacing = 0.2.sp,
                color = androidx.compose.ui.graphics.Color(0xFF2C2824)
            ),
            cursorBrush = SolidColor(DiaryColors.GoldAccent),
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            decorationBox = { innerTextField ->
                Box {
                    if (notes.isEmpty()) {
                        Text(
                            text = "Write your thoughts, ideas, or daily notes here...",
                            style = TextStyle(
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
                                fontSize = 14.sp,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                color = DiaryColors.GoldAccent.copy(alpha = 0.7f)
                            )
                        )
                    }
                    innerTextField()
                }
            }
        )
    }
}
