package com.vutrankien.bluecontrol

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.vutrankien.android.lib.AndroidLogFactory
import com.vutrankien.bluecontrol.lib.Presenter
import com.vutrankien.lib.LogFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job

class MainActivity : AppCompatActivity() {

    companion object {
        private val log: LogFactory.Log = AndroidLogFactory.instance.newLog("MainActivity")
        private const val REQUEST_ENABLE_BT = 1
    }

    private val presenter by lazy { Presenter(AndroidEnv(application), ViewImpl()) }

    inner class ViewImpl : com.vutrankien.bluecontrol.lib.View {

        override fun alert(msg: String, onDismiss: () -> Unit) {
            AlertDialog.Builder(this@MainActivity)
                .setMessage(msg)
                .setOnCancelListener { finish() }
                .show()

        }

        override fun finish() {
            this@MainActivity.finish()
        }

        override fun askEnableBluetooth() {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            enableBluetoothLauncher.launch(enableBtIntent)
        }

        override fun askLocationPermission() {
            permissionRequestLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }

    }

    val scope = CoroutineScope(Dispatchers.Main + Job())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    private val enableBluetoothLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { it: ActivityResult? ->
            log.i("Bluetooth enabled: $it")
        }

    override fun onResume() {
        super.onResume()
        log.d("onResume")
        presenter.onResume()
    }

    private val permissionRequestLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            log.i("User has granted permission: $isGranted")
        }

    @Suppress("UNUSED_PARAMETER") // requires for android:onClick
    fun onDiscoverClick(view: View) {
        presenter.onDiscoverClick()
    }

}