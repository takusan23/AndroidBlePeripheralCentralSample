package io.github.takusan23.androidbleperipheralcentralsample

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeripheralScreen() {
    val context = LocalContext.current

    val readRequestText = remember { mutableStateOf(Build.MODEL) }
    val writeRequestList = remember { mutableStateOf(emptyList<String>()) }

    val peripheralManager = remember {
        BlePeripheralManager(
            context = context,
            onCharacteristicReadRequest = { readRequestText.value.toByteArray(Charsets.UTF_8) },
            onCharacteristicWriteRequest = { writeRequestList.value += it.toString(Charsets.UTF_8) }
        )
    }
    val connectedDeviceList = peripheralManager.connectedDeviceList.collectAsState()

    // 開始・終了処理
    DisposableEffect(key1 = Unit) {
        peripheralManager.start()
        onDispose { peripheralManager.destroy() }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(text = "BLE ペリフェラル") }) }
    ) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {

            Text(
                text = "キャラクタリスティック read で送り返す値",
                fontSize = 20.sp
            )
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = readRequestText.value,
                onValueChange = { readRequestText.value = it },
                singleLine = true
            )

            HorizontalDivider()

            Text(
                text = "接続中デバイス数：${connectedDeviceList.value.size}",
                fontSize = 20.sp
            )

            HorizontalDivider()

            Text(
                text = "キャラクタリスティック write で受信した値",
                fontSize = 20.sp
            )
            writeRequestList.value.forEach { writeText ->
                Text(text = writeText)
            }

        }
    }
}