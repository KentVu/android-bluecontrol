package com.vutrankien.bluecontrol.lib

interface View {

    fun alert(msg: String, onDismiss: () -> Unit)
    fun finish()
    fun askEnableBluetooth()
    fun askLocationPermission()

}
