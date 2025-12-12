package com.example.nkdsify.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

class TagVisualTransformation(private val backgroundColor: Color) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val annotatedString = buildAnnotatedString {
            append(text)
            val tagStyle = SpanStyle(background = backgroundColor)

            text.text.split(' ').forEach { part ->
                if (part.isNotBlank()) {
                    val isTag = (part.startsWith("+") || part.startsWith("-") || part.startsWith("=")) && part.length > 1
                    if (isTag) {
                        var startIndex = text.text.indexOf(part)
                        while (startIndex != -1) {
                            val endIndex = startIndex + part.length
                            addStyle(tagStyle, startIndex, endIndex)
                            startIndex = text.text.indexOf(part, startIndex + 1)
                        }
                    }
                }
            }
        }

        return TransformedText(annotatedString, OffsetMapping.Identity)
    }
}
