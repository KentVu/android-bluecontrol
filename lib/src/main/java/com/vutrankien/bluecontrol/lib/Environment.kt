package com.vutrankien.bluecontrol.lib

import kotlinx.coroutines.flow.Flow
import java.util.*

interface Environment {

    val locationPermissionGranted: Boolean

    fun bluetoothSupported(): Boolean
    fun bluetoothEnabled(): Boolean
    data class BluetoothDevice(val name: String, val address: String)
    fun queryPairedDevices(): Set<BluetoothDevice>
    fun listenBluetoothConnection(name: String, uuid: UUID): Flow<ListenEvent>
    fun sendMsg(
        device: BluetoothDevice?,
        msg: String
    )

    sealed class ListenEvent {
        object LISTENING : ListenEvent()
        data class Accepted(val socket: BlueSocket): ListenEvent()
    }

    interface BlueSocket {

    }

}
