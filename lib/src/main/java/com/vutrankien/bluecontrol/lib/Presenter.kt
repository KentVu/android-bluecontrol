package com.vutrankien.bluecontrol.lib

import com.vutrankien.lib.LogFactory
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class Presenter(private val env: Environment, private val view: View) : KoinComponent {
    fun onCreate() {
        view.apply {
            scope.launch {
                if (!env.bluetoothSupported()) {
                    // Device doesn't support Bluetooth
                    val msg = "Device does not support Bluetooth"
                    log.e(msg)
                    alert(msg) { finish() }
                    return@launch
                }
                if (!env.bluetoothEnabled()) {
                    if (!askEnableBluetooth()) {
                        alert("Please enable bluetooth!") { finish() }
                        return@launch
                    }
                    // user enabled bluetooth!
                }
                // bluetooth enabled
                discover()
            }
        }
    }

    fun onDiscoverClick() {
        view.scope.launch {
            if (!env.locationPermissionGranted) {
                if (!view.askLocationPermission()) {
                    view.alert("please enable location permission for bluetooth to function.") {
                        //view.finish()  // TODO location permission not required for listing paired device
                    }
                    //return@launch
                }
            }
        }
    }

    private fun discover() {
        val pairedDevices = env.queryPairedDevices()
        pairedDevices.let {
            log.d("Paired devices:${it.count()}")
            it.forEach { device ->
                log.i("d:$device")
            }
        }
    }

    private val logFactory: LogFactory by inject()
    private val log = logFactory.newLog("Presenter")
}