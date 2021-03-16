package com.vutrankien.bluecontrol.lib

interface View {

    suspend fun alert(msg: String, onDismiss: () -> Unit)
    fun finish()
    suspend fun askEnableBluetooth(): Boolean
    suspend fun askLocationPermission(): Boolean
    fun populateDevices(devices: Set<Environment.BluetoothDevice>)
    fun updateStatus(sts: String)
    fun displayMsg(rcvMsg: String)
    val startBtn: Element

    interface Element {
        fun disable()
        fun enable()
    }

}
