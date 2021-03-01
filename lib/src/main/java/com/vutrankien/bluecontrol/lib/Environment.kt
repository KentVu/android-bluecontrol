package com.vutrankien.bluecontrol.lib

import kotlinx.coroutines.flow.Flow
import java.util.*

interface Environment {

    val locationPermissionGranted: Boolean

    fun bluetoothSupported(): Boolean
    fun bluetoothEnabled(): Boolean
    data class BluetoothDevice(val name: String, val address: String)
    fun queryPairedDevices(): Set<BluetoothDevice>
    fun listenBluetoothConnection(name: String, uuid: UUID): Flow<ConnectionEvent>
    fun sendMsg(
        device: BluetoothDevice?,
        msg: String,
        uuid: UUID
    ): Flow<ConnectionEvent>

    sealed class ConnectionEvent {
        object LISTENING : ConnectionEvent()

        data class Accepted(val socket: BlueSocket): ConnectionEvent()
        data class Connected(val socket: BlueSocket) : ConnectionEvent()
    }

    interface BlueSocket {

    }

}
