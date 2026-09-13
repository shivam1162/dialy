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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dialy.app.presentation.theme.DiaryColors

@Composable
fun NotesIdeasSection(
    notes: String,
    onNotesChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var text by remember { mutableStateOf(notes) }

    androidx.compose.runtime.LaunchedEffect(notes) {
        if (notes != text) {
            text = notes
        }
    }

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
            value = text,
            onValueChange = {
                text = it
                onNotesChange(it)
            },
            textStyle = TextStyle(
                fontSize = 14.sp,
                lineHeight = 22.sp,
                color = DiaryColors.TextPrimary
            ),
            cursorBrush = SolidColor(DiaryColors.GoldAccent),
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            decorationBox = { innerTextField ->
                Box {
                    if (text.isEmpty()) {
                        Text(
                            text = "Write your thoughts, ideas, or brain dump here...",
                            style = TextStyle(
                                fontSize = 13.5.sp,
                                color = DiaryColors.TextTertiary
                            )
                        )
                    }
                    innerTextField()
                }
            }
        )
    }
}
