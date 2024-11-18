package io.github.takusan23.androidbleperipheralcentralsample

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CentralScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val centralManager = remember { BleCentralManager(context) }
    val isConnected = centralManager.isConnected.collectAsState(initial = false)
    val writeRequestText = remember { mutableStateOf(Build.MODEL) }
    val readRequestList = remember { mutableStateOf(emptyList<String>()) }

    DisposableEffect(key1 = Unit) {
        scope.launch { centralManager.connect() }
        onDispose { centralManager.destroy() }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(text = "BLE セントラル") }) }
    ) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {

            // 接続中ならくるくる
            if (!isConnected.value) {
                CircularProgressIndicator()
                Text(text = "接続中です")
                return@Scaffold
            }

            Text(
                text = "キャラクタリスティック write で送信する値",
                fontSize = 20.sp
            )
            Row {
                OutlinedTextField(
                    modifier = Modifier.weight(1f),
                    value = writeRequestText.value,
                    onValueChange = { writeRequestText.value = it },
                    singleLine = true
                )
                Button(
                    onClick = {
                        scope.launch {
                            centralManager.writeCharacteristic(writeRequestText.value.toByteArray(Charsets.UTF_8))
                        }
                    }
                ) {
                    Text(text = "送信")
                }
            }
            HorizontalDivider()

            Text(
                text = "キャラクタリスティック read で読み出した値",
                fontSize = 20.sp
            )
            Button(onClick = {
                scope.launch {
                    readRequestList.value += centralManager.readCharacteristic().toString(Charsets.UTF_8)
                }
            }) {
                Text(text = "読み出す")
            }
            readRequestList.value.forEach { readText ->
                Text(text = readText)
            }
        }
    }
}