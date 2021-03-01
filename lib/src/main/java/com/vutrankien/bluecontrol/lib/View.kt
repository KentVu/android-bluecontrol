package com.vutrankien.bluecontrol.lib

import kotlinx.coroutines.CoroutineScope

interface View {

    suspend fun alert(msg: String, onDismiss: () -> Unit)
    fun finish()
    suspend fun askEnableBluetooth(): Boolean
    suspend fun askLocationPermission(): Boolean
    fun populateDevices(devices: Set<Environment.BluetoothDevice>)
    fun updateStatus(msg: String)

}
