package com.vutrankien.bluecontrol

import android.Manifest
import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
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

    private val toRealDevices: MutableMap<Environment.BluetoothDevice, BluetoothDevice> = mutableMapOf()

    override fun queryPairedDevices(): Set<Environment.BluetoothDevice> {
        val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter!!.bondedDevices
        return pairedDevices!!.map {
            val bluetoothDevice = Environment.BluetoothDevice(it.name, it.address)
            toRealDevices.put(bluetoothDevice, it)
            bluetoothDevice
        }.toSet()
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    override fun listenBluetoothConnection(name: String, uuid: UUID): Flow<Environment.ConnectionEvent> = flow {
        val blueServerSocket =
            bluetoothAdapter!!.listenUsingRfcommWithServiceRecord(name, uuid)
        emit(Environment.ConnectionEvent.LISTENING)
        val s2cSocket = blueServerSocket.accept()
        emit(Environment.ConnectionEvent.Accepted(AndroidBlueSocket(s2cSocket)))
        blueServerSocket.close()
    }.flowOn(Dispatchers.IO)

    @Suppress("BlockingMethodInNonBlockingContext")
    override fun sendMsg(
        device: Environment.BluetoothDevice?,
        msg: String,
        uuid: UUID
    ) = flow<Environment.ConnectionEvent> {
        toRealDevices[device]!!.createRfcommSocketToServiceRecord(uuid).use {
            it.connect()
            emit(Environment.ConnectionEvent.Connected(AndroidBlueSocket(it)))
        }
    }.flowOn(Dispatchers.IO)

    class AndroidBlueSocket(s2cSocket: BluetoothSocket) :
        Environment.BlueSocket {

    }

    override val locationPermissionGranted: Boolean
        get() = ContextCompat.checkSelfPermission(
            application,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

}
