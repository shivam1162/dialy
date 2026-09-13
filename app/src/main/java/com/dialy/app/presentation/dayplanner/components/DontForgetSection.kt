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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dialy.app.domain.model.ReminderItem
import com.dialy.app.presentation.theme.DiaryColors

@Composable
fun DontForgetSection(
    reminders: List<ReminderItem>,
    onAddReminder: (String) -> Unit,
    onToggleReminder: (id: String, isCompleted: Boolean) -> Unit,
    onDeleteReminder: (id: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var newText by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DiaryColors.CardBackground)
            .border(1.dp, DiaryColors.BorderSubtle, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Text(
            text = "DON'T FORGET",
            style = MaterialTheme.typography.titleMedium,
            color = DiaryColors.RoseAccent,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        reminders.forEach { reminder ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .border(
                            1.5.dp,
                            if (reminder.isCompleted) DiaryColors.RoseAccent else DiaryColors.BorderSubtle,
                            RoundedCornerShape(4.dp)
                        )
                        .background(if (reminder.isCompleted) DiaryColors.RoseAccent else Color.Transparent)
                        .clickable { onToggleReminder(reminder.id, !reminder.isCompleted) },
                    contentAlignment = Alignment.Center
                ) {
                    if (reminder.isCompleted) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Done",
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = reminder.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (reminder.isCompleted) DiaryColors.TextTertiary else DiaryColors.TextPrimary,
                    textDecoration = if (reminder.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = { onDeleteReminder(reminder.id) },
                    modifier = Modifier.size(18.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Delete",
                        tint = DiaryColors.TextTertiary,
                        modifier = Modifier.size(12.dp)
                    )
                }
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
            Text("📌", fontSize = 14.sp, modifier = Modifier.padding(end = 8.dp))
            BasicTextField(
                value = newText,
                onValueChange = { newText = it },
                textStyle = TextStyle(fontSize = 13.5.sp, color = DiaryColors.TextPrimary),
                cursorBrush = SolidColor(DiaryColors.RoseAccent),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (newText.isNotBlank()) {
                            onAddReminder(newText)
                            newText = ""
                        }
                    }
                ),
                modifier = Modifier.weight(1f),
                decorationBox = { innerTextField ->
                    Box {
                        if (newText.isEmpty()) {
                            Text(
                                text = "Add quick reminder...",
                                style = TextStyle(fontSize = 13.sp, color = DiaryColors.TextTertiary)
                            )
                        }
                        innerTextField()
                    }
                }
            )

            if (newText.isNotBlank()) {
                Text(
                    text = "Add",
                    style = MaterialTheme.typography.labelSmall,
                    color = DiaryColors.RoseAccent,
                    modifier = Modifier
                        .clickable {
                            onAddReminder(newText)
                            newText = ""
                        }
                        .padding(start = 6.dp)
                )
            }
        }
    }
}
