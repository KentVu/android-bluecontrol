package com.vutrankien.bluecontrol.lib

import com.vutrankien.lib.LogFactory
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class Presenter(private val env: Environment, private val view: View) : KoinComponent {
    fun onResume() {
        if (!env.bluetoothSupported()) {
            // Device doesn't support Bluetooth
            val msg = "Device does not support Bluetooth"
            log.e(msg)
            view.alert(msg) { view.finish() }
            return
        }
        if (!env.bluetoothEnabled()) {
            view.askEnableBluetooth()
        }
    }

    fun onDiscoverClick() {
        if (env.locationPermissionGranted) {
            val pairedDevices = env.queryPairedDevices()
            pairedDevices.let{
                log.d("Paired devices:${it.count()}")
                it.forEach { device ->
                    val deviceName = device.name
                    val deviceHardwareAddress = device.address // MAC address
                    log.i("d:$deviceHardwareAddress->$deviceName-$device")
                }
            }
        } else {
            view.askLocationPermission()
        }
    }

    private val logFactory: LogFactory by inject()
    private val log = logFactory.newLog("Presenter")
}