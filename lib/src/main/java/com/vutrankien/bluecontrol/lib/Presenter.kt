package com.vutrankien.bluecontrol.lib

import com.vutrankien.lib.LogFactory
import kotlinx.coroutines.*

class Presenter(
    logFactory: LogFactory,
    private val env: Environment,
    private val view: View
) {
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

    suspend fun onSendClick(msg: String) = coroutineScope {
        log.d("onSendClick:client.connected?${client.connected}")
        if (!client.connected) {
            withContext(Dispatchers.IO) {
                client.connectTo(selectedDevice!!)
            }
            view.updateStatus("Connected to $selectedDevice")
            client.startListenChannel()
            //return@coroutineScope
        }
        view.displayMsg("Client:$msg")
        client.msgChan.send(msg)
    }

    fun onDeviceSelected(device: Environment.BluetoothDevice) {
        log.d("device selected:$device")
        selectedDevice = device
    }

    suspend fun onStartClick() = coroutineScope {
        view.updateStatus("Server socket listening...")
        view.startBtn.disable()
        try {
            withContext(Dispatchers.IO) {
                server.startListening()
            }
        } catch (e: Exception) {
            log.w("onStartClick: Can startListening:${e.message}")
            return@coroutineScope
        }
        view.updateStatus("Server socket accepted!")
        launch {
            for (msg in server.receiveFromClient(CoroutineScope(Dispatchers.Default))) {
                view.displayMsg("Client:${msg}")
            }
            view.updateStatus("Client exited.")
            view.startBtn.enable()
        }
    }

    fun onDestroy() {
        server.end()
        client.closeSendChannel()
    }

    private val server: Server = Server(logFactory, env)

    private val client: Client =
        Client(logFactory, env)

    private var selectedDevice: Environment.BluetoothDevice? = null
    private val log = logFactory.newLog("Presenter")
}