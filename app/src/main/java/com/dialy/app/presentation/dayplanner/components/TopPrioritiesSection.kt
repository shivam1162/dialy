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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dialy.app.domain.model.PriorityItem
import com.dialy.app.presentation.theme.DiaryColors

@Composable
fun TopPrioritiesSection(
    priorities: List<PriorityItem>,
    onUpdatePriority: (order: Int, title: String, isCompleted: Boolean) -> Unit,
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
            text = "TOP PRIORITIES",
            style = MaterialTheme.typography.titleMedium,
            color = DiaryColors.RoseAccent,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        (1..3).forEach { order ->
            val priority = priorities.firstOrNull { it.order == order }
            var titleText by remember(order) { mutableStateOf(priority?.title ?: "") }
            val isCompleted = priority?.isCompleted ?: false

            androidx.compose.runtime.LaunchedEffect(priority?.title) {
                val remoteTitle = priority?.title ?: ""
                if (remoteTitle != titleText) {
                    titleText = remoteTitle
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Priority Number Badge / Checkbox
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(if (isCompleted) DiaryColors.RoseAccent else DiaryColors.RoseSoft)
                        .clickable {
                            onUpdatePriority(order, titleText, !isCompleted)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (isCompleted) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Completed",
                            tint = androidx.compose.ui.graphics.Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    } else {
                        Text(
                            text = "$order",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = DiaryColors.RoseAccent
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Editable priority line
                Column(modifier = Modifier.weight(1f)) {
                    BasicTextField(
                        value = titleText,
                        onValueChange = {
                            titleText = it
                            onUpdatePriority(order, it, isCompleted)
                        },
                        textStyle = TextStyle(
                            fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isCompleted) DiaryColors.TextTertiary else DiaryColors.TextPrimary,
                            textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None
                        ),
                        cursorBrush = SolidColor(DiaryColors.RoseAccent),
                        modifier = Modifier.fillMaxWidth(),
                        decorationBox = { innerTextField ->
                            Box {
                                if (titleText.isEmpty()) {
                                    Text(
                                        text = "Priority #$order...",
                                        style = TextStyle(
                                            fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif,
                                            fontSize = 13.5.sp,
                                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                            color = DiaryColors.RoseAccent.copy(alpha = 0.6f)
                                        )
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(4.dp))
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
