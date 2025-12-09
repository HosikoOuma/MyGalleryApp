package com.example.nkdsify.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.example.nkdsify.ui.utils.parseQueryString

class TagVisualTransformation(private val backgroundColor: Color) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val annotatedString = buildAnnotatedString {
            append(text)
            val parsedQuery = parseQueryString(text.text)
            val tagStyle = SpanStyle(background = backgroundColor)

            text.text.split(' ').forEach { part ->
                if (part.isNotBlank()) {
                    val isTag = (part.startsWith("+") || part.startsWith("-")) && part.length > 1
                    if (isTag) {
                        val startIndex = text.text.indexOf(part)
                        val endIndex = startIndex + part.length
                        addStyle(tagStyle, startIndex, endIndex)
                    }
                }
            }
        }

        return TransformedText(annotatedString, OffsetMapping.Identity)
    }
}
