package com.vutrankien.bluecontrol

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.vutrankien.android.lib.AndroidLogFactory
import com.vutrankien.lib.LogFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.first
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    companion object {
        private val log: LogFactory.Log = AndroidLogFactory.instance.newLog("MainActivity")
        private const val REQUEST_ENABLE_BT = 1
    }
    data class ActivityResult(val requestCode: Int, val resultCode: Int)

    val scope = CoroutineScope(Dispatchers.Main + Job())
    private val activityResultChannel: Channel<ActivityResult> = Channel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
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
        scope.launch {
            if (!bluetoothAdapter.isEnabled) {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
                if (activityResultChannel.first { it.requestCode == REQUEST_ENABLE_BT }.resultCode == Activity.RESULT_OK) {
                    log.i("Bluetooth enabled")
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        scope.launch {
            activityResultChannel.send(ActivityResult(requestCode, resultCode))
        }
    }
}