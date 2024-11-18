package io.github.takusan23.androidbleperipheralcentralsample

import android.annotation.SuppressLint
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Build
import android.os.ParcelUuid
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/** BLE デバイスへ接続し GATT サーバーへ接続しサービスを探しキャラクタリスティックを操作する */
class BleCentralManager(private val context: Context) {

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager

    /** [readCharacteristic]等で使いたいので */
    private val _bluetoothGatt = MutableStateFlow<BluetoothGatt?>(null)

    /** コールバックの返り値をコルーチン側から受け取りたいので */
    private val _characteristicReadChannel = Channel<ByteArray>()

    /** 接続中かどうか */
    val isConnected = _bluetoothGatt.map { it != null }

    /** デバイスを探し、GATT サーバーへ接続する */
    @SuppressLint("MissingPermission")
    suspend fun connect() {
        // GATT サーバーのサービスを元に探す
        val bleDevice = findBleDevice() ?: return

        // GATT サーバーへ接続する
        bleDevice.connectGatt(context, false, object : BluetoothGattCallback() {

            // 接続できたらサービスを探す
            override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
                super.onConnectionStateChange(gatt, status, newState)
                when (newState) {
                    // 接続できたらサービスを探す
                    BluetoothProfile.STATE_CONNECTED -> gatt?.discoverServices()
                    // なくなった
                    BluetoothProfile.STATE_DISCONNECTED -> _bluetoothGatt.value = null
                }
            }

            // discoverServices() でサービスが見つかった
            override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
                super.onServicesDiscovered(gatt, status)
                // Flow に BluetoothGatt を入れる
                _bluetoothGatt.value = gatt
            }

            // onCharacteristicReadRequest で送られてきたデータを受け取る
            override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray, status: Int) {
                super.onCharacteristicRead(gatt, characteristic, value, status)
                _characteristicReadChannel.trySend(value)
            }

        // Android 12 ？以前はこっちを実装する必要あり
        override fun onCharacteristicRead(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
            super.onCharacteristicRead(gatt, characteristic, status)
            _characteristicReadChannel.trySend(characteristic?.value ?: byteArrayOf())
        }
        })
    }

    /** キャラクタリスティックへ read する */
    @SuppressLint("MissingPermission")
    suspend fun readCharacteristic(): ByteArray {
        // GATT サーバーとの接続を待つ
        // Flow に値が入ってくるまで（onServicesDiscovered() で入れている）一時停止する。コルーチン便利
        val gatt = _bluetoothGatt.filterNotNull().first()
        // GATT サーバーへ狙ったサービス内にあるキャラクタリスティックへ read を試みる
        val findService = gatt.services?.first { it.uuid == BleUuid.BLE_UUID_SERVICE }
        val findCharacteristic = findService?.characteristics?.first { it.uuid == BleUuid.BLE_UUID_CHARACTERISTIC }
        // 結果は onCharacteristicRead で
        gatt.readCharacteristic(findCharacteristic)
        return _characteristicReadChannel.receive()
    }

    /** キャラクタリスティックへ write する */
    @SuppressLint("MissingPermission")
    suspend fun writeCharacteristic(sendData: ByteArray) {
        // GATT サーバーとの接続を待つ
        val gatt = _bluetoothGatt.filterNotNull().first()
        // GATT サーバーへ狙ったサービス内にあるキャラクタリスティックへ write を試みる
        val findService = gatt.services?.first { it.uuid == BleUuid.BLE_UUID_SERVICE } ?: return
        val findCharacteristic = findService.characteristics?.first { it.uuid == BleUuid.BLE_UUID_CHARACTERISTIC } ?: return
        // 結果は onCharacteristicWriteRequest で
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            gatt.writeCharacteristic(findCharacteristic, sendData, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
        } else {
            findCharacteristic.setValue(sendData)
            gatt.writeCharacteristic(findCharacteristic)
        }
    }

    /** 終了する */
    @SuppressLint("MissingPermission")
    fun destroy() {
        _bluetoothGatt.value?.close()
    }

    @SuppressLint("MissingPermission")
    private suspend fun findBleDevice() = suspendCancellableCoroutine { continuation ->
        val bluetoothLeScanner = bluetoothManager.adapter.bluetoothLeScanner
        val bleScanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                super.onScanResult(callbackType, result)
                // 見つけたら返して、スキャンも終了させる
                continuation.resume(result?.device)
                bluetoothLeScanner.stopScan(this)
            }

            override fun onScanFailed(errorCode: Int) {
                super.onScanFailed(errorCode)
                continuation.resume(null)
            }
        }

        // GATT サーバーのサービス UUID を指定して検索を始める
        val scanFilter = ScanFilter.Builder().apply {
            setServiceUuid(ParcelUuid(BleUuid.BLE_UUID_SERVICE))
        }.build()
        bluetoothLeScanner.startScan(
            listOf(scanFilter),
            ScanSettings.Builder().build(),
            bleScanCallback
        )

        continuation.invokeOnCancellation {
            bluetoothLeScanner.stopScan(bleScanCallback)
        }
    }
}