package com.demo.rokid_huishi_m.activities.huishiCampus

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rokid.cxr.client.extend.CxrApi
import com.rokid.cxr.client.extend.listeners.CustomViewListener
import com.rokid.cxr.client.utils.ValueUtil

class HuishiCampusActivity : ComponentActivity() {
    private val customViewListener = object : CustomViewListener {
        override fun onIconsSent() {
            Log.d("HuishiCampusActivity", "onIconsSent")
        }

        override fun onOpened() {
            Log.d("HuishiCampusActivity", "onOpened")
        }

        override fun onOpenFailed(p0: Int) {
            Log.e("HuishiCampusActivity", "onOpenFailed: $p0")
        }

        override fun onUpdated() {
            Log.d("HuishiCampusActivity", "onUpdated")
        }

        override fun onClosed() {
            Log.d("HuishiCampusActivity", "onClosed")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        CxrApi.getInstance().setCustomViewListener(customViewListener)
        setContent {
            HuishiCampusScreen(
                onShowOnGlassesClick = { showCampusTextOnGlasses() }
            )
        }
    }

    private fun showCampusTextOnGlasses() {
        val selfViewJson = """
            {
              "type": "LinearLayout",
              "props": {
                "layout_width": "match_parent",
                "layout_height": "match_parent",
                "orientation": "vertical",
                "gravity": "center",
                "backgroundColor": "#FF000000"
              },
              "children": [
                {
                  "type": "TextView",
                  "props": {
                    "id": "tv_title",
                    "layout_width": "wrap_content",
                    "layout_height": "wrap_content",
                    "text": "慧视校园",
                    "textSize": "28sp",
                    "textColor": "#FFFFFFFF",
                    "textStyle": "bold"
                  }
                }
              ]
            }
        """.trimIndent()

        when (CxrApi.getInstance().openCustomView(selfViewJson)) {
            ValueUtil.CxrStatus.REQUEST_SUCCEED,
            ValueUtil.CxrStatus.REQUEST_WAITING -> {
                Toast.makeText(this, "已发送到眼镜端", Toast.LENGTH_SHORT).show()
            }

            else -> {
                Toast.makeText(this, "发送失败，请先确认设备已连接", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        CxrApi.getInstance().closeCustomView()
        CxrApi.getInstance().setCustomViewListener(null)
        super.onDestroy()
    }
}

@Composable
fun HuishiCampusScreen(
    onShowOnGlassesClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "慧视校园",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Text(
            text = "校园应用页面",
            fontSize = 18.sp
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(onClick = onShowOnGlassesClick) {
            Text(text = "在眼镜端显示“慧视校园”")
        }
    }
}
