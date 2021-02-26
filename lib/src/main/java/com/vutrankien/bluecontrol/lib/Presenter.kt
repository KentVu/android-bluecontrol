package com.vutrankien.bluecontrol.lib

import com.vutrankien.lib.LogFactory
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class Presenter(private val env: Environment, private val view: View) : KoinComponent {
    fun onResume() {
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
                    askEnableBluetooth()
                }
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
            discover()
        }
    }

    private fun discover() {
        val pairedDevices = env.queryPairedDevices()
        pairedDevices.let {
            log.d("Paired devices:${it.count()}")
            it.forEach { device ->
                val deviceName = device.name
                val deviceHardwareAddress = device.address // MAC address
                log.i("d:$deviceHardwareAddress->$deviceName-$device")
            }
        }
    }

    private val logFactory: LogFactory by inject()
    private val log = logFactory.newLog("Presenter")
}