package com.demo.rokid_huishi_m.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.demo.rokid_huishi_m.R
import com.demo.rokid_huishi_m.activities.huishiCampus.HuishiCampusActivity
import com.demo.rokid_huishi_m.activities.suixinYiting.SuixinYitingActivity
import com.demo.rokid_huishi_m.activities.user.UserActivity

class AppNavigationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppNavigationScreen(
                onHuishiCampusClick = {
                    startActivity(Intent(this, HuishiCampusActivity::class.java))
                },
                onSuixinYitingClick = {
                    startActivity(Intent(this, SuixinYitingActivity::class.java))
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
    onSuixinYitingClick: () -> Unit,
    onMyClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 应用列表标题
        Text(
            text = "应用列表",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 48.dp, bottom = 32.dp)
        )
        
        // 应用卡片区域
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AppCard(
                title = "慧视校园",
                description = "校园应用",
                onClick = onHuishiCampusClick,
                coverImage = R.drawable.huishicampus
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            AppCard(
                title = "随心一听",
                description = "音乐应用",
                onClick = onSuixinYitingClick,
                coverImage = R.drawable.suixinyiting
            )
        }
        
        // 底部导航栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 32.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "主页",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Text(
                text = "我的",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Blue,
                modifier = Modifier
                    .clickable { onMyClick() }
                    .padding(8.dp)
            )
        }
    }
}

@Composable
fun AppCard(
    title: String,
    description: String,
    onClick: () -> Unit,
    coverImage: Int? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White,
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            coverImage?.let {
                Image(
                    painter = painterResource(id = it),
                    contentDescription = title,
                    modifier = Modifier
                        .height(80.dp)
                        .width(80.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = description,
                fontSize = 14.sp,
                color = Color.Gray
            )
        }
    }
}
