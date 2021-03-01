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

    enum class ListenEvent {
        LISTENING,
        ACCEPTED
    }

}
