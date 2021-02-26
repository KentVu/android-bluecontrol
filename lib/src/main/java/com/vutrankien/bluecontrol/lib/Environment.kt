package com.vutrankien.bluecontrol.lib

interface Environment {

    val locationPermissionGranted: Boolean

    fun bluetoothSupported(): Boolean
    fun bluetoothEnabled(): Boolean
    data class BluetoothDevice(val name: String, val address: String)
    fun queryPairedDevices(): Set<BluetoothDevice>

}
