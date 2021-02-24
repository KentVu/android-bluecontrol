package com.vutrankien.bluecontrol

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.vutrankien.android.lib.AndroidLogFactory
import com.vutrankien.lib.LogFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job

class MainActivity : AppCompatActivity() {

    companion object {
        private val log: LogFactory.Log = AndroidLogFactory.instance.newLog("MainActivity")
        private const val REQUEST_ENABLE_BT = 1
    }

    val scope = CoroutineScope(Dispatchers.Main + Job())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    private val enableBluetoothLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            log.i("Bluetooth enabled: $it")
        }

    override fun onResume() {
        super.onResume()
        log.d("onResume")
        val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter == null) {
            // Device doesn't support Bluetooth
            log.e("Device does not support Bluetooth")
            return
        }
        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            enableBluetoothLauncher.launch(enableBtIntent)
        }
    }

    private val permissionRequestLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            log.i("User has granted permission: $isGranted")
        }

    fun onDiscoverClick(view: View) {
        permissionRequestLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }
}