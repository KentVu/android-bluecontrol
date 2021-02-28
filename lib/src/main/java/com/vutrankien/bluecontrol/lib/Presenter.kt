package com.vutrankien.bluecontrol.lib

import com.vutrankien.lib.LogFactory
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
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
        }
    }

    fun onStartClick() {
        val async = GlobalScope.async {
            env.listenBluetoothConnection(Conf.serviceName, Conf.uuid)
        }
    }

    fun onSendClick() {
        TODO("Not yet implemented")
    }

    private val logFactory: LogFactory by inject()
    private val log = logFactory.newLog("Presenter")
}