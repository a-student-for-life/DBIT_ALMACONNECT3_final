package com.example.dbit_almaconnect3.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.dbit_almaconnect3.ui.theme.*
import kotlinx.coroutines.delay
import com.example.dbit_almaconnect3.ui.theme.BlueBrandColor

@Composable
fun SplashScreen(navController: NavHostController) {
    var startAnimation by remember { mutableStateOf(false) }
    var showText by remember { mutableStateOf(false) }

    // Get the current color scheme from MaterialTheme
    val colorScheme = MaterialTheme.colorScheme
    val isDarkTheme = isSystemInDarkTheme()

    // Animation states
    val scaleAnimation by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.5f,
        animationSpec = tween(
            durationMillis = 1000,
            easing = FastOutSlowInEasing
        ), label = "scaleAnimation"
    )

    // Trigger animations
    LaunchedEffect(key1 = true) {
        startAnimation = true
        delay(500)
        showText = true
        delay(1500) // Total delay now is 2000ms
        navController.navigate("login") {
            popUpTo("splash") { inclusive = true } // Remove splash from back stack
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDarkTheme) DarkBackground else LightBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            // Logo with scale animation
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .scale(scaleAnimation)
                    .clip(CircleShape)
                    .background(BlueBrandColor)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                // Using built-in material icon for graduation cap
                Icon(
                    imageVector = Icons.Default.School,
                    contentDescription = "App Logo",
                    modifier = Modifier
                        .size(100.dp),
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Animated text visibility
            AnimatedVisibility(
                visible = showText,
                enter = fadeIn(animationSpec = tween(1000)) +
                        slideInVertically(
                            animationSpec = tween(1000),
                            initialOffsetY = { it / 2 }
                        )
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "DBIT AlmaConnect",
                        style = MaterialTheme.typography.headlineMedium,
                        color = BlueBrandColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Connecting Students and Alumni",
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isDarkTheme) DarkTextSecondary else LightTextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewSplash() {
    DBIT_ALMACONNECT3Theme {
        SplashScreen(navController = rememberNavController())
    }
}

@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PreviewSplashDark() {
    DBIT_ALMACONNECT3Theme {
        SplashScreen(navController = rememberNavController())
    }
}

@Composable
fun rememberNavController(): NavHostController {
    return androidx.navigation.compose.rememberNavController()
}