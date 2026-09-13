package com.dialy.app.presentation.dayplanner.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dialy.app.domain.model.TodoItem
import com.dialy.app.presentation.theme.DiaryColors

@Composable
fun TodoListSection(
    todos: List<TodoItem>,
    onAddTodo: (String) -> Unit,
    onToggleTodo: (id: String, isCompleted: Boolean) -> Unit,
    onDeleteTodo: (id: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var newTodoText by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DiaryColors.CardBackground)
            .border(1.dp, DiaryColors.BorderSubtle, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "TO-DO LIST",
                style = MaterialTheme.typography.titleMedium,
                color = DiaryColors.SageAccent,
                letterSpacing = 1.sp
            )

            val completedCount = todos.count { it.isCompleted }
            if (todos.isNotEmpty()) {
                Text(
                    text = "$completedCount/${todos.size}",
                    style = MaterialTheme.typography.labelSmall,
                    color = DiaryColors.TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Existing tasks
        todos.forEach { todo ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Checkbox Box
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .border(
                            1.5.dp,
                            if (todo.isCompleted) DiaryColors.SageAccent else DiaryColors.BorderSubtle,
                            RoundedCornerShape(6.dp)
                        )
                        .background(if (todo.isCompleted) DiaryColors.SageAccent else Color.Transparent)
                        .clickable { onToggleTodo(todo.id, !todo.isCompleted) },
                    contentAlignment = Alignment.Center
                ) {
                    if (todo.isCompleted) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Completed",
                            tint = Color.White,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = todo.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (todo.isCompleted) DiaryColors.TextTertiary else DiaryColors.TextPrimary,
                    textDecoration = if (todo.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = { onDeleteTodo(todo.id) },
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Delete",
                        tint = DiaryColors.TextTertiary,
                        modifier = Modifier.size(14.dp)
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

        Spacer(modifier = Modifier.height(8.dp))

        // Direct in-page + Add Task row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add",
                tint = DiaryColors.SageAccent,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            BasicTextField(
                value = newTodoText,
                onValueChange = { newTodoText = it },
                textStyle = TextStyle(
                    fontSize = 13.5.sp,
                    color = DiaryColors.TextPrimary
                ),
                cursorBrush = SolidColor(DiaryColors.SageAccent),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (newTodoText.isNotBlank()) {
                            onAddTodo(newTodoText)
                            newTodoText = ""
                        }
                    }
                ),
                modifier = Modifier.weight(1f),
                decorationBox = { innerTextField ->
                    Box {
                        if (newTodoText.isEmpty()) {
                            Text(
                                text = "Add a task...",
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

            if (newTodoText.isNotBlank()) {
                Text(
                    text = "Add",
                    style = MaterialTheme.typography.labelSmall,
                    color = DiaryColors.SageAccent,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable {
                            onAddTodo(newTodoText)
                            newTodoText = ""
                        }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}
