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
import com.dialy.app.domain.model.Reflection
import com.dialy.app.presentation.theme.DiaryColors

@Composable
fun ReflectionSection(
    reflection: Reflection,
    onUpdateReflection: (whatWentWell: String, whatCanImprove: String, proudOf: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var wentWell by remember { mutableStateOf(reflection.whatWentWell) }
    var improve by remember { mutableStateOf(reflection.whatCanImprove) }
    var proud by remember { mutableStateOf(reflection.proudOf) }

    androidx.compose.runtime.LaunchedEffect(reflection.whatWentWell) {
        if (reflection.whatWentWell != wentWell) wentWell = reflection.whatWentWell
    }
    androidx.compose.runtime.LaunchedEffect(reflection.whatCanImprove) {
        if (reflection.whatCanImprove != improve) improve = reflection.whatCanImprove
    }
    androidx.compose.runtime.LaunchedEffect(reflection.proudOf) {
        if (reflection.proudOf != proud) proud = reflection.proudOf
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
            text = "END-OF-DAY REFLECTION",
            style = MaterialTheme.typography.titleMedium,
            color = DiaryColors.LavenderAccent,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Prompt 1: What went well
        ReflectionPromptField(
            label = "🌱 What went well today?",
            value = wentWell,
            onValueChange = {
                wentWell = it
                onUpdateReflection(it, improve, proud)
            },
            accentColor = DiaryColors.SageAccent
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Prompt 2: What can I improve
        ReflectionPromptField(
            label = "💡 What could I improve tomorrow?",
            value = improve,
            onValueChange = {
                improve = it
                onUpdateReflection(wentWell, it, proud)
            },
            accentColor = DiaryColors.PeachAccent
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Prompt 3: Proud of
        ReflectionPromptField(
            label = "💖 I'm proud of myself for...",
            value = proud,
            onValueChange = {
                proud = it
                onUpdateReflection(wentWell, improve, it)
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
