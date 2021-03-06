package com.vutrankien.bluecontrol

import android.Manifest
import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.BatteryManager
import androidx.core.content.ContextCompat
import com.vutrankien.android.lib.AndroidLogFactory
import com.vutrankien.bluecontrol.lib.Environment
import java.io.InputStream
import java.io.OutputStream
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

    override fun listenBluetoothConnection(name: String, uuid: UUID): Environment.BlueServerSocket {
        val blueServerSocket =
            bluetoothAdapter!!.listenUsingRfcommWithServiceRecord(name, uuid)
        return AndroidBlueServerSocket(blueServerSocket)
    }

    class AndroidBlueServerSocket(private val realSocket: BluetoothServerSocket) : Environment.BlueServerSocket {
        override fun accept(): Environment.BlueSocket {
            return AndroidBlueSocket(realSocket.accept())
        }

        override fun close() {
            realSocket.close()
        }

    }

    override fun connectToDevice(
        device: Environment.BluetoothDevice,
        uuid: UUID
    ): Environment.BlueSocket {
        //bluetoothAdapter.cancelDiscovery()
        val socket = toRealDevices[device]!!.createRfcommSocketToServiceRecord(uuid)
        socket.connect()
        return AndroidBlueSocket(socket)
    }

    override fun queryApps(): List<String> = application.packageManager.let { pm ->
        pm.queryIntentActivities(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),
            0
        ).map { it.loadLabel(pm).toString() }
    }

    override val batteryStatus: String
        get() {
            val batteryStatus: Intent =
                IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
                    application.registerReceiver(null, ifilter)
                } ?: return "battery error!"
            val level: Int = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale: Int = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val batteryPct: Float = level * 100 / scale.toFloat()
            return "$batteryPct($level/$scale)"
        }

    class AndroidBlueSocket(private val socket: BluetoothSocket) :
        Environment.BlueSocket {
        override val inputStream: InputStream
            get() = socket.inputStream
        override val outputStream: OutputStream
            get() = socket.outputStream

        override fun toString(): String {
            return "${super.toString()}${socket.remoteDevice}"
        }

        override fun close() {
            socket.close()
        }

        override val isConnected: Boolean
            get() = socket.isConnected
    }

    override val locationPermissionGranted: Boolean
        get() = ContextCompat.checkSelfPermission(
            application,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

}
