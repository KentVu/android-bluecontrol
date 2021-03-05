package com.vutrankien.bluecontrol

import android.Manifest
import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.pm.PackageManager
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

    @Suppress("BlockingMethodInNonBlockingContext")
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

    @Suppress("BlockingMethodInNonBlockingContext")
    override fun connectToDevice(
        device: Environment.BluetoothDevice?,
        uuid: UUID
    ): Environment.BlueSocket {
        return AndroidBlueSocket(toRealDevices[device]!!.createRfcommSocketToServiceRecord(uuid))
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
    }

    override val locationPermissionGranted: Boolean
        get() = ContextCompat.checkSelfPermission(
            application,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

}
