package io.github.takusan23.androidbleperipheralcentralsample

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.content.Context
import android.os.ParcelUuid
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * BLE ペリフェラル側の処理をまとめたクラス
 *
 * @param onCharacteristicReadRequest キャラクタリスティックへ read が要求された（送り返す）
 * @param onCharacteristicWriteRequest キャラクタリスティックへ write が要求された（受信）
 */
class BlePeripheralManager(
    private val context: Context,
    private val onCharacteristicReadRequest: () -> ByteArray,
    private val onCharacteristicWriteRequest: (ByteArray) -> Unit
) {

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothLeAdvertiser = bluetoothManager.adapter.bluetoothLeAdvertiser

    /** アドバタイジングのコールバック */
    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            super.onStartSuccess(settingsInEffect)
        }

        override fun onStartFailure(errorCode: Int) {
            super.onStartFailure(errorCode)
        }
    }

    /** GATT サーバー */
    private var bleGattServer: BluetoothGattServer? = null

    private val _connectedDeviceList = MutableStateFlow(emptyList<BluetoothDevice>())

    /** 接続中端末の配列 */
    val connectedDeviceList = _connectedDeviceList.asStateFlow()

    /** GATT サーバーとアドバタイジングするやつを開始する */
    fun start() {
        startGattServer()
        startAdvertising()
    }

    /** 終了する */
    @SuppressLint("MissingPermission")
    fun destroy() {
        bleGattServer?.close()
        bluetoothLeAdvertiser.stopAdvertising(advertiseCallback)
    }

    @SuppressLint("MissingPermission") // TODO 権限チェックをする
    private fun startGattServer() {
        bleGattServer = bluetoothManager.openGattServer(context, object : BluetoothGattServerCallback() {

            // セントラル側デバイスと接続したら
            // UI に表示するため StateFlow で通知する
            override fun onConnectionStateChange(device: BluetoothDevice?, status: Int, newState: Int) {
                super.onConnectionStateChange(device, status, newState)
                device ?: return
                when (newState) {
                    BluetoothProfile.STATE_DISCONNECTED -> _connectedDeviceList.value -= _connectedDeviceList.value.first { it.address == device.address }
                    BluetoothProfile.STATE_CONNECTED -> _connectedDeviceList.value += device
                }
            }

            // readCharacteristic が要求されたら呼ばれる
            // セントラルへ送信する
            override fun onCharacteristicReadRequest(device: BluetoothDevice?, requestId: Int, offset: Int, characteristic: BluetoothGattCharacteristic?) {
                super.onCharacteristicReadRequest(device, requestId, offset, characteristic)
                val sendByteArray = onCharacteristicReadRequest()
                // オフセットを考慮する
                // TODO バイト数スキップするのが面倒で ByteArrayInputStream 使ってるけど多分オーバースペック
                val sendOffsetByteArray = sendByteArray.inputStream().apply { skip(offset.toLong()) }.readBytes()
                bleGattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, sendOffsetByteArray)
            }

            // writeCharacteristic が要求されたら呼ばれる
            // セントラルから受信する
            override fun onCharacteristicWriteRequest(device: BluetoothDevice?, requestId: Int, characteristic: BluetoothGattCharacteristic?, preparedWrite: Boolean, responseNeeded: Boolean, offset: Int, value: ByteArray?) {
                super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value)
                value ?: return
                onCharacteristicWriteRequest(value)
                bleGattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, null)
            }
        })

        //サービスとキャラクタリスティックを作る
        val gattService = BluetoothGattService(BleUuid.BLE_UUID_SERVICE, BluetoothGattService.SERVICE_TYPE_PRIMARY)
        val gattCharacteristics = BluetoothGattCharacteristic(
            BleUuid.BLE_UUID_CHARACTERISTIC,
            BluetoothGattCharacteristic.PROPERTY_READ or BluetoothGattCharacteristic.PROPERTY_WRITE,
            BluetoothGattCharacteristic.PERMISSION_READ or BluetoothGattCharacteristic.PERMISSION_WRITE
        )
        // サービスにキャラクタリスティックを入れる
        gattService.addCharacteristic(gattCharacteristics)
        // GATT サーバーにサービスを追加
        bleGattServer?.addService(gattService)
    }

    @SuppressLint("MissingPermission") // TODO 権限チェック
    private fun startAdvertising() {
        // アドバタイジング。これがないと見つけてもらえない
        val advertiseSettings = AdvertiseSettings.Builder().apply {
            setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_POWER)
            setTimeout(0)
        }.build()
        val advertiseData = AdvertiseData.Builder().apply {
            addServiceUuid(ParcelUuid(BleUuid.BLE_UUID_SERVICE))
        }.build()
        // アドバタイジング開始
        bluetoothLeAdvertiser.startAdvertising(advertiseSettings, advertiseData, advertiseCallback)
    }

}