package com.vutrankien.bluecontrol.lib

import kotlinx.coroutines.CoroutineScope

interface View {

    val scope: CoroutineScope

    suspend fun alert(msg: String, onDismiss: () -> Unit)
    fun finish()
    suspend fun askEnableBluetooth(): Boolean
    suspend fun askLocationPermission(): Boolean

}
