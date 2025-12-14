package com.example.nkdsify.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nkdsify.R
import com.example.nkdsify.data.GoogleSansFontFamily
import com.example.nkdsify.data.JetBrainsMonoFontFamily
import com.example.nkdsify.data.Theme
import kotlinx.coroutines.delay

@Composable
fun WelcomeScreen(onGrantPermissionClick: () -> Unit,  theme: Theme) {
    val fonts = listOf(FontFamily.Default, GoogleSansFontFamily, JetBrainsMonoFontFamily)
    var currentFont by remember { mutableStateOf(fonts[0]) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            currentFont = fonts[(fonts.indexOf(currentFont) + 1) % fonts.size]
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "icon rotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "button scale"
    )

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier.height(64.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Nekolery",
                    style = MaterialTheme.typography.displaySmall.copy(fontFamily = currentFont),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = stringResource(id = R.string.easter_egg_button),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .size(128.dp)
                    .graphicsLayer {
                        rotationZ = rotation
                        transformOrigin = TransformOrigin(0.5f, 0.7f)
                    },
                fontSize = 120.sp
            )
            Spacer(modifier = Modifier.height(72.dp))
            Text(
                text = stringResource(id = R.string.welcome_title),
                style = MaterialTheme.typography.headlineLarge,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(id = R.string.welcome_description),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(48.dp))
            Button(
                onClick = onGrantPermissionClick,
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(horizontal = 48.dp, vertical = 24.dp),
                modifier = Modifier.graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
            ) {
                Text(text = stringResource(id = R.string.grant_permission_button))
            }
        }
    }
}
