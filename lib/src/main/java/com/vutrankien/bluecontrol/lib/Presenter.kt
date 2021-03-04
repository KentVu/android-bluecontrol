package com.vutrankien.bluecontrol.lib

import com.vutrankien.lib.LogFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class Presenter(private val env: Environment, private val view: View) : KoinComponent {
    suspend fun onCreate() {
        view.apply {
            if (!env.bluetoothSupported()) {
                // Device doesn't support Bluetooth
                val msg = "Device does not support Bluetooth"
                log.e(msg)
                alert(msg) { finish() }
                return
            }
            if (!env.bluetoothEnabled()) {
                if (!askEnableBluetooth()) {
                    alert("Please enable bluetooth!") { finish() }
                    return
                }
                // user enabled bluetooth!
            }
            // bluetooth enabled
            populatePairedDevices()
            onStartClick()
        }
    }

    suspend fun onDiscoverClick() {
        if (!env.locationPermissionGranted) {
            if (!view.askLocationPermission()) {
                view.alert("please enable location permission for bluetooth to function.") {
                    //view.finish()  // TODO location permission not required for listing paired device
                }
                //return@launch
            }
        }
    }

    private fun populatePairedDevices() {
        val pairedDevices = env.queryPairedDevices()
        pairedDevices.also {
            log.d("Paired devices:${it.count()}")
            it.forEach { device ->
                log.i("d:$device")
            }
            view.populateDevices(it)
            //selectedDevice = it.first()
        }
    }

    /**
     * Start listening.
     */
    suspend fun onStartClick() {
        env.listenBluetoothConnection(Conf.serviceName, Conf.uuid).collect { event ->
            when (event) {
                Environment.ConnectionEvent.LISTENING -> {
                    view.updateStatus("Server socket listening...")
                }
                is Environment.ConnectionEvent.Accepted -> {
                    view.updateStatus("Server socket accepted!")
                    val rcvMsg = withContext(Dispatchers.IO) {
                        event.socket.inputStream.bufferedReader().use {
                            @Suppress("BlockingMethodInNonBlockingContext")
                            it.readLine()
                        }
                    }
                    log.d("received msg:$rcvMsg")
                    view.displayMsg("Client:$rcvMsg")
                }
            }
        }
    }

    suspend fun onSendClick(msg: String) {
        log.d("onSendClick:$msg")
        view.displayMsg("Client:$msg")
        env.sendMsg(selectedDevice, msg, Conf.uuid).collect { event ->
            when(event) {
                is Environment.ConnectionEvent.Connected -> {
                    view.updateStatus("Connected to ${event.socket}")
                    withContext(Dispatchers.IO) {
                        @Suppress("BlockingMethodInNonBlockingContext")
                        event.socket.outputStream.bufferedWriter().use {
                            it.write(msg)
                            it.newLine()
                        }
                    }
                }
                else -> log.e("Unsupported event: $event")
            }
        }
    }

    fun onDeviceSelected(device: Environment.BluetoothDevice) {
        log.d("device selected:$device")
        selectedDevice = device
    }

    private var selectedDevice: Environment.BluetoothDevice? = null
    private val logFactory: LogFactory by inject()
    private val log = logFactory.newLog("Presenter")
}