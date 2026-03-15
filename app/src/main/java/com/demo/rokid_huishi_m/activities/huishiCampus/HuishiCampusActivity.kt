package com.demo.rokid_huishi_m.activities.huishiCampus

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class HuishiCampusActivity : ComponentActivity() {
    private var isRecognizing by mutableStateOf(false)
    private var isManualRecognizing by mutableStateOf(false)
    private var recognitionMessage by mutableStateOf("点击开始识别")
    private val recognizer by lazy {
        HuishiCampusRecognizer(
            onRecognizingChanged = { recognizing -> isRecognizing = recognizing },
            onManualRecognizingChanged = { recognizing ->
                runOnUiThread {
                    isManualRecognizing = recognizing
                }
            },
            onMessage = { message ->
                runOnUiThread {
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                }
            },
            onRecognitionUpdated = { message ->
                runOnUiThread {
                    recognitionMessage = message
                }
            },
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        recognizer.attach()
        setContent {
            HuishiCampusScreen(
                isRecognizing = isRecognizing,
                isManualRecognizing = isManualRecognizing,
                recognitionMessage = recognitionMessage,
                onStartClick = { recognizer.start() },
                onStopClick = { recognizer.stop() },
                onManualRecognizeClick = { recognizer.recognizeOnce() }
            )
        }
    }

    override fun onStop() {
        recognizer.pause()
        super.onStop()
    }

    override fun onDestroy() {
        recognizer.release()
        super.onDestroy()
    }
}

@Composable
fun HuishiCampusScreen(
    isRecognizing: Boolean,
    isManualRecognizing: Boolean,
    recognitionMessage: String,
    onStartClick: () -> Unit,
    onStopClick: () -> Unit,
    onManualRecognizeClick: () -> Unit
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
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = recognitionMessage,
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.height(20.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = {
                    if (isRecognizing) onStopClick() else onStartClick()
                },
                enabled = !isManualRecognizing
            ) {
                Text(text = if (isRecognizing) "停止识别" else "开始识别")
            }
            Button(
                onClick = onManualRecognizeClick,
                enabled = !isRecognizing && !isManualRecognizing
            ) {
                Text(text = if (isManualRecognizing) "单次识别中..." else "手动识别")
            }
        }
    }
}
