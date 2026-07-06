package com.mornshield.mobile.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mornshield.mobile.R

@Composable
fun StoreAppIcon() {
    Box(
        modifier = Modifier
            .size(512.dp)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF1E1A3D), Color(0xFF0C091A))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_launcher_foreground),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier.fillMaxSize(0.9f)
        )
    }
}

@Composable
fun StoreFeatureGraphic() {
    Box(
        modifier = Modifier
            .size(width = 1024.dp, height = 500.dp)
            .background(Color(0xFF0C091A)),
        contentAlignment = Alignment.Center
    ) {
        // Multi-layered Cinematic Shield Glow
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF7A60FF).copy(alpha = 0.25f), Color.Transparent),
                    center = Offset(size.width * 0.15f, size.height * 0.3f),
                    radius = 700f
                )
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFC0B3FF).copy(alpha = 0.2f), Color.Transparent),
                    center = Offset(size.width * 0.85f, size.height * 0.7f),
                    radius = 800f
                )
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFFD700).copy(alpha = 0.15f), Color.Transparent),
                    center = Offset(size.width * 0.5f, size.height * 1.2f),
                    radius = 1200f
                )
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(240.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "MornShield",
                fontSize = 96.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                letterSpacing = 4.sp,
                style = MaterialTheme.typography.displayLarge.copy(
                    shadow = Shadow(
                        color = Color(0xFF7A60FF).copy(alpha = 0.8f),
                        offset = Offset(0f, 6f),
                        blurRadius = 24f
                    )
                )
            )
            Text(
                text = "YOUR MORNING SHIELDED",
                fontSize = 28.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFFC0B3FF).copy(alpha = 0.8f),
                letterSpacing = 12.sp
            )
        }
    }
}

@Composable
fun StoreTvBanner() {
    Box(
        modifier = Modifier
            .size(width = 1280.dp, height = 720.dp)
            .background(Color(0xFF0C091A)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_tv_banner),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Preview(widthDp = 512, heightDp = 512)
@Composable
fun PreviewStoreAppIcon() {
    MaterialTheme {
        StoreAppIcon()
    }
}

@Preview(widthDp = 1024, heightDp = 500)
@Composable
fun PreviewStoreFeatureGraphic() {
    MaterialTheme {
        StoreFeatureGraphic()
    }
}

@Preview(widthDp = 1280, heightDp = 720)
@Composable
fun PreviewStoreTvBanner() {
    MaterialTheme {
        StoreTvBanner()
    }
}
