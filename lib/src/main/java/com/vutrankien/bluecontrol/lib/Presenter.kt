package com.vutrankien.bluecontrol.lib

import com.vutrankien.lib.LogFactory
import kotlinx.coroutines.flow.collect
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
            discover()
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

    private fun discover() {
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

    suspend fun onStartClick() {
        env.listenBluetoothConnection(Conf.serviceName, Conf.uuid).collect {
            when (it) {
                Environment.ConnectionEvent.LISTENING -> {
                    view.updateStatus("Server socket listening...")
                }
                is Environment.ConnectionEvent.Accepted -> {
                    view.updateStatus("Server socket accepted!")
                }
            }
        }
    }

    suspend fun onSendClick(msg: String) {
        log.d("onSendClick:$msg")
        env.sendMsg(selectedDevice, msg, Conf.uuid).collect {
            when(it) {
                is Environment.ConnectionEvent.Connected -> {
                    view.updateStatus("Connected to $it")
                }
                else -> log.e("Unsupported event: $it")
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