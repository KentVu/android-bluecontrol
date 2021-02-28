package com.vutrankien.bluecontrol.lib

import java.util.*

interface Environment {

    val locationPermissionGranted: Boolean

    fun bluetoothSupported(): Boolean
    fun bluetoothEnabled(): Boolean
    data class BluetoothDevice(val name: String, val address: String)
    fun queryPairedDevices(): Set<BluetoothDevice>
    fun listenBluetoothConnection(name: String, uuid: UUID)

}
