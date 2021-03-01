package com.vutrankien.bluecontrol

import android.Manifest
import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.vutrankien.android.lib.AndroidLogFactory
import com.vutrankien.bluecontrol.lib.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.util.*

class AndroidEnv(private val application: Application) : Environment {

    private val log = AndroidLogFactory.instance.newLog("Environment")

    private val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter() ?: null

    override fun bluetoothSupported(): Boolean {
        return bluetoothAdapter != null
    }

    override fun bluetoothEnabled(): Boolean {
        return bluetoothAdapter!!.isEnabled
    }

    override fun queryPairedDevices(): Set<Environment.BluetoothDevice> {
        val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter!!.bondedDevices
        return pairedDevices!!.map {
            Environment.BluetoothDevice(it.name, it.address)
        }.toSet()
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    override fun listenBluetoothConnection(name: String, uuid: UUID): Flow<Environment.ListenEvent> = flow {
        val blueServerSocket =
            bluetoothAdapter!!.listenUsingRfcommWithServiceRecord(name, uuid)
        emit(Environment.ListenEvent.LISTENING)
        blueServerSocket.accept()
        emit(Environment.ListenEvent.ACCEPTED)
    }.flowOn(Dispatchers.IO)

    override val locationPermissionGranted: Boolean
        get() = ContextCompat.checkSelfPermission(
            application,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

}
