package com.example.quickbillposs.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.quickbillposs.ui.theme.PosSteelBlue
import com.example.quickbillposs.ui.theme.PosSteelBlueDark
import com.example.quickbillposs.ui.theme.PosTextWhite
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit
) {
    var startAnimation by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.5f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "LogoScale"
    )

    LaunchedEffect(Unit) {
        startAnimation = true
        delay(1200) // 1.2 second starting splash duration
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        PosSteelBlue,
                        PosSteelBlueDark
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.scale(scale)
        ) {
            // App Icon Circle
            Surface(
                shape = CircleShape,
                color = PosTextWhite,
                shadowElevation = 8.dp,
                modifier = Modifier.size(100.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.PointOfSale,
                        contentDescription = "QuickBill POS Logo",
                        tint = PosSteelBlue,
                        modifier = Modifier.size(54.dp)
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // App Name
            Text(
                text = "QuickBill POS",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp
                ),
                color = PosTextWhite
            )

            Spacer(Modifier.height(6.dp))

            // Tagline
            Text(
                text = "Fast, Reliable Point of Sale",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp
                ),
                color = PosTextWhite.copy(alpha = 0.85f)
            )

            Spacer(Modifier.height(32.dp))

            // Loading Indicator
            CircularProgressIndicator(
                color = PosTextWhite,
                strokeWidth = 3.dp,
                modifier = Modifier.size(24.dp)
            )
        }

        // Version watermark at bottom
        Text(
            text = "v1.0.60",
            style = MaterialTheme.typography.labelSmall,
            color = PosTextWhite.copy(alpha = 0.5f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
        )
    }
}
