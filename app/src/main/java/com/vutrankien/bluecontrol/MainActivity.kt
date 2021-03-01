package com.vutrankien.bluecontrol

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.vutrankien.android.lib.AndroidLogFactory
import com.vutrankien.bluecontrol.lib.Environment
import com.vutrankien.bluecontrol.lib.Presenter
import com.vutrankien.lib.LogFactory
import kotlinx.coroutines.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class MainActivity : AppCompatActivity() {

    companion object {
        private val log: LogFactory.Log = AndroidLogFactory.instance.newLog("MainActivity")
        private const val REQUEST_ENABLE_BT = 1
    }

    private val presenter by lazy { Presenter(AndroidEnv(application), viewImpl) }

    private val viewImpl = ViewImpl()
    inner class ViewImpl : com.vutrankien.bluecontrol.lib.View {
        val scope = CoroutineScope(Dispatchers.Main + Job())

        override suspend fun alert(msg: String, onDismiss: () -> Unit) = suspendCoroutine<Unit> { continuation ->
            AlertDialog.Builder(this@MainActivity)
                .setMessage(msg)
                .setOnCancelListener {
                    onDismiss()
                    continuation.resume(Unit)
                }
                .show()
        }

        override fun finish() {
            this@MainActivity.finish()
        }

        private var completableDeferred: CompletableDeferred<Boolean>? = null

        private val enableBluetoothLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { it: ActivityResult? ->
                log.i("Bluetooth enabled: $it")
                completableDeferred!!.complete(it?.resultCode == Activity.RESULT_OK)
            }

        override suspend fun askEnableBluetooth(): Boolean {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            completableDeferred = CompletableDeferred()
            enableBluetoothLauncher.launch(enableBtIntent)
            return completableDeferred!!.await()
        }

        private val permissionRequestLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                log.i("User has granted permission: $isGranted")
                completableDeferred!!.complete(isGranted)
            }

        override suspend fun askLocationPermission(): Boolean {
            completableDeferred = CompletableDeferred()
            permissionRequestLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            return completableDeferred!!.await()
        }

        override fun populateDevices(devices: Set<Environment.BluetoothDevice>) {
            findViewById<Spinner>(R.id.spin_devices).adapter = ArrayAdapter(
                this@MainActivity,
                android.R.layout.simple_spinner_item,
                devices.toList().map { it.name }
            )
        }

        override fun updateStatus(msg: String) {
            findViewById<TextView>(R.id.txt_status).text = msg
        }

    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        lifecycleScope.launch {
            presenter.onCreate()
        }
    }

    override fun onResume() {
        super.onResume()
        log.d("onResume")
    }

    @Suppress("UNUSED_PARAMETER") // requires for android:onClick
    fun onDiscoverClick(view: View) {
        lifecycleScope.launch {
            presenter.onDiscoverClick()
        }
    }

    fun onStartClick(view: View) {
        lifecycleScope.launch {
            presenter.onStartClick()
        }
    }

    fun onSendClick(view: View) {
        presenter.onSendClick()
    }

}