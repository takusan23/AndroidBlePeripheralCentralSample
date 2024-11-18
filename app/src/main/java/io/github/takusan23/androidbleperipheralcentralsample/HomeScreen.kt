package io.github.takusan23.androidbleperipheralcentralsample

import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

/** 必要な権限たち */
private val PERMISSION_LIST = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    listOf(
        android.Manifest.permission.BLUETOOTH,
        android.Manifest.permission.BLUETOOTH_CONNECT,
        android.Manifest.permission.BLUETOOTH_SCAN,
        android.Manifest.permission.BLUETOOTH_ADVERTISE,
        android.Manifest.permission.ACCESS_COARSE_LOCATION,
        android.Manifest.permission.ACCESS_FINE_LOCATION
    )
} else {
    listOf(
        android.Manifest.permission.BLUETOOTH,
        android.Manifest.permission.BLUETOOTH_ADMIN,
        android.Manifest.permission.ACCESS_COARSE_LOCATION,
        android.Manifest.permission.ACCESS_FINE_LOCATION
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onPeripheralClick: () -> Unit,
    onCentralClick: () -> Unit
) {
    val context = LocalContext.current

    // 権限を求めるまでボタンを出さない
    val isPermissionAllGranted = remember {
        mutableStateOf(PERMISSION_LIST.all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED })
    }

    // リクエストするやつ
    val permissionRequest = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { isPermissionAllGranted.value = it.all { it.value } }
    )

    // 権限をリクエスト
    LaunchedEffect(key1 = Unit) {
        permissionRequest.launch(PERMISSION_LIST.toTypedArray())
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(text = "BLE ホーム画面") }) }
    ) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {

            // 権限がなければ
            if (!isPermissionAllGranted.value) {
                Text(text = "権限が付与されていません")
                return@Scaffold
            }

            Button(onClick = onPeripheralClick) {
                Text(text = "ペリフェラル側になる")
            }

            Button(onClick = onCentralClick) {
                Text(text = "セントラル側になる")
            }
        }
    }
}