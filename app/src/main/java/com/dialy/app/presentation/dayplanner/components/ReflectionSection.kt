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
import com.dialy.app.domain.model.Reflection
import com.dialy.app.presentation.theme.DiaryColors

@Composable
fun ReflectionSection(
    reflection: Reflection,
    onUpdateReflection: (whatWentWell: String, whatCanImprove: String, proudOf: String) -> Unit,
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
            text = "END-OF-DAY REFLECTION",
            style = MaterialTheme.typography.titleMedium,
            color = DiaryColors.LavenderAccent,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Prompt 1: What went well
        ReflectionPromptField(
            label = "🌱 What went well today?",
            value = reflection.whatWentWell,
            onValueChange = {
                onUpdateReflection(it, reflection.whatCanImprove, reflection.proudOf)
            },
            accentColor = DiaryColors.SageAccent
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Prompt 2: What can I improve
        ReflectionPromptField(
            label = "💡 What could I improve tomorrow?",
            value = reflection.whatCanImprove,
            onValueChange = {
                onUpdateReflection(reflection.whatWentWell, it, reflection.proudOf)
            },
            accentColor = DiaryColors.PeachAccent
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Prompt 3: Proud of
        ReflectionPromptField(
            label = "💖 I'm proud of myself for...",
            value = reflection.proudOf,
            onValueChange = {
                onUpdateReflection(reflection.whatWentWell, reflection.whatCanImprove, it)
            },
            accentColor = DiaryColors.RoseAccent
        )
    }
}

@Composable
private fun ReflectionPromptField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    accentColor: androidx.compose.ui.graphics.Color
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = DiaryColors.TextSecondary
        )
        Spacer(modifier = Modifier.height(4.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(
                fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
                fontSize = 14.5.sp,
                lineHeight = 22.sp,
                color = DiaryColors.TextPrimary
            ),
            cursorBrush = SolidColor(accentColor),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { innerTextField ->
                Box {
                    if (value.isEmpty()) {
                        Text(
                            text = "Reflect here...",
                            style = TextStyle(
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
                                fontSize = 13.5.sp,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                color = accentColor.copy(alpha = 0.65f)
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
