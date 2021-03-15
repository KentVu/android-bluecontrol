package com.vutrankien.bluecontrol.lib

import java.io.Closeable
import java.io.InputStream
import java.io.OutputStream
import java.util.*

interface Environment {

    val locationPermissionGranted: Boolean

    fun bluetoothSupported(): Boolean
    fun bluetoothEnabled(): Boolean
    data class BluetoothDevice(val name: String, val address: String)
    fun queryPairedDevices(): Set<BluetoothDevice>
    fun listenBluetoothConnection(name: String, uuid: UUID): BlueServerSocket

    interface BlueServerSocket {
        fun accept(): BlueSocket

        fun close()
    }

    fun connectToDevice(
        device: BluetoothDevice?,
        uuid: UUID
    ): BlueSocket

    interface BlueSocket {

        val inputStream: InputStream
        val outputStream: OutputStream
        fun close()
        val isConnected: Boolean
    }

}
