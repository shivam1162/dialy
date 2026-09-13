package com.dialy.app.presentation.dayplanner.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dialy.app.domain.model.GratitudeItem
import com.dialy.app.presentation.theme.DiaryColors

@Composable
fun GratitudeSection(
    gratitudeList: List<GratitudeItem>,
    onAddGratitude: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var newGratitudeText by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DiaryColors.CardBackground)
            .border(1.dp, DiaryColors.BorderSubtle, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Text(
            text = "DAILY GRATITUDE",
            style = MaterialTheme.typography.titleMedium,
            color = DiaryColors.RoseAccent,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        gratitudeList.forEachIndexed { index, item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${index + 1}.",
                    style = MaterialTheme.typography.labelSmall,
                    color = DiaryColors.RoseAccent,
                    modifier = Modifier.width(20.dp)
                )
                Text(
                    text = item.text,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(DiaryColors.RuledLine)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Direct in-page entry
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "✨",
                fontSize = 14.sp,
                modifier = Modifier.padding(end = 8.dp)
            )
            BasicTextField(
                value = newGratitudeText,
                onValueChange = { newGratitudeText = it },
                textStyle = TextStyle(
                    fontSize = 13.5.sp,
                    color = DiaryColors.TextPrimary
                ),
                cursorBrush = SolidColor(DiaryColors.RoseAccent),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (newGratitudeText.isNotBlank()) {
                            onAddGratitude(newGratitudeText)
                            newGratitudeText = ""
                        }
                    }
                ),
                modifier = Modifier.weight(1f),
                decorationBox = { innerTextField ->
                    Box {
                        if (newGratitudeText.isEmpty()) {
                            Text(
                                text = "I am grateful for...",
                                style = TextStyle(
                                    fontSize = 13.5.sp,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                    color = DiaryColors.TextTertiary
                                )
                            )
                        }
                        innerTextField()
                    }
                }
            )

            if (newGratitudeText.isNotBlank()) {
                Text(
                    text = "Save",
                    style = MaterialTheme.typography.labelSmall,
                    color = DiaryColors.RoseAccent,
                    modifier = Modifier
                        .clickable {
                            onAddGratitude(newGratitudeText)
                            newGratitudeText = ""
                        }
                        .padding(start = 6.dp)
                )
            }
        }
    }
}
