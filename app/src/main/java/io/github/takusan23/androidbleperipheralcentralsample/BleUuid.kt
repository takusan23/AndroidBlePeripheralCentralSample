package io.github.takusan23.androidbleperipheralcentralsample

import java.util.UUID

/** BLE UUID 定数 */
object BleUuid {

    /** GATT サーバー サービスの UUID */
    val BLE_UUID_SERVICE = UUID.fromString("a1bf5691-1851-4d0c-bddd-cd5c9f516595")

    /** GATT サーバー キャラクタリスティックの UUID */
    val BLE_UUID_CHARACTERISTIC = UUID.fromString("03f06708-4119-4841-893e-4de78b22c3d4")

}