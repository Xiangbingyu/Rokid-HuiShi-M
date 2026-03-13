package com.demo.rokid_huishi_m.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.demo.rokid_huishi_m.R
import com.demo.rokid_huishi_m.activities.huishiCampus.HuishiCampusActivity
import com.demo.rokid_huishi_m.activities.user.UserActivity

private data class NavigationEntry(
    val title: String,
    val description: String,
    val coverImage: Int,
    val onClick: () -> Unit
)

class AppNavigationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppNavigationScreen(
                onHuishiCampusClick = {
                    startActivity(Intent(this, HuishiCampusActivity::class.java))
                },
                onMyClick = {
                    startActivity(Intent(this, UserActivity::class.java))
                }
            )
        }
    }
}

@Composable
fun AppNavigationScreen(
    onHuishiCampusClick: () -> Unit,
    onMyClick: () -> Unit
) {
    val palette = appNavigationPalette()
    val entries = listOf(
        NavigationEntry(
            title = "慧视校园",
            description = "校园应用",
            coverImage = R.drawable.huishicampus,
            onClick = onHuishiCampusClick
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
    ) {
        HeaderBar(
            title = "应用列表",
            palette = palette
        )
        
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            entries.forEach { entry ->
                AppCard(
                    title = entry.title,
                    description = entry.description,
                    coverImage = entry.coverImage,
                    onClick = entry.onClick,
                    palette = palette
                )
            }
        }

        BottomNavigationBar(
            palette = palette,
            onMyClick = onMyClick
        )
    }
}

@Composable
private fun AppCard(
    title: String,
    description: String,
    coverImage: Int,
    onClick: () -> Unit,
    palette: AppNavigationPalette
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, palette.border),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(
            containerColor = palette.surface,
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = coverImage),
                contentDescription = title,
                modifier = Modifier
                    .height(80.dp)
                    .width(80.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = palette.textMain
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = description,
                fontSize = 13.sp,
                color = palette.textMuted
            )
        }
    }
}

@Composable
private fun BottomNavigationBar(
    palette: AppNavigationPalette,
    onMyClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "主页",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = palette.textMain
        )
        Text(
            text = "我的",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = palette.primary,
            modifier = Modifier
                .clickable(onClick = onMyClick)
                .padding(8.dp)
        )
    }
}

private data class AppNavigationPalette(
    val primary: Color,
    val background: Color,
    val surface: Color,
    val textMain: Color,
    val textMuted: Color,
    val border: Color
)

private fun appNavigationPalette() = AppNavigationPalette(
    primary = Color(0xFF6A5ACD),
    background = Color(0xFFF9FAFB),
    surface = Color(0xFFFFFFFF),
    textMain = Color(0xFF111827),
    textMuted = Color(0xFF6B7280),
    border = Color(0xFFE5E7EB)
)

@Composable
private fun HeaderBar(
    title: String,
    palette: AppNavigationPalette
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.surface)
            .border(BorderStroke(1.dp, palette.border))
            .statusBarsPadding()
            .padding(top = 12.dp, bottom = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = palette.textMain
        )
    }
}
