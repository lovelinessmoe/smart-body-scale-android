package com.example.dianzicheng.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.dianzicheng.data.ble.BleScaleClient

@Composable
fun PairingScreen(
    viewModel: ScaleViewModel,
    onPairingComplete: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.connection) {
        if (uiState.connection == BleScaleClient.ConnectionState.CONNECTED) {
            onPairingComplete()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("欢迎使用体脂秤", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Text("请站在秤上并点击下方按钮开始配对", style = MaterialTheme.typography.bodyLarge)
        
        Spacer(modifier = Modifier.height(64.dp))
        
        if (uiState.connection == BleScaleClient.ConnectionState.SCANNING) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text("正在搜索体脂秤...")
        } else {
            Button(
                onClick = { viewModel.startScanning() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("开始配To")
            }
        }
        
        TextButton(onClick = onPairingComplete) {
            Text("跳过配对")
        }
    }
}
