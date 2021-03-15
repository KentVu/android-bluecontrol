package com.vutrankien.bluecontrol

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.view.View
import android.widget.*
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

    private val presenter by lazy { Presenter(AndroidLogFactory.instance, AndroidEnv(application), viewImpl) }

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
            val devList = devices.toList()
            findViewById<Spinner>(R.id.spin_devices).apply {
                adapter = ArrayAdapter(
                    this@MainActivity,
                    android.R.layout.simple_spinner_item,
                    devList.map { it.name }
                )
                onItemSelectedListener = object :AdapterView.OnItemSelectedListener {
                    override fun onNothingSelected(p0: AdapterView<*>?) {
                        log.w("onNothingSelected")
                    }

                    override fun onItemSelected(adt: AdapterView<*>?, p1: View?, pos: Int, id: Long) {
                        presenter.onDeviceSelected(devList[pos])
                    }
                }
            }
            }

        override fun updateStatus(sts: String) {
            log.d("updateStatus:$sts")
            findViewById<EditText>(R.id.edt_log).append("log:$sts\n")
        }

        override fun displayMsg(rcvMsg: String) {
            log.d("displayMsg:$rcvMsg")
            findViewById<EditText>(R.id.edt_log).append("$rcvMsg\n")
        }

        override fun disableStartBtn() {
            findViewById<Button>(R.id.btn_start).isEnabled = false
        }

    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        //findViewById<EditText>(R.id.edt_log).movementMethod = ScrollingMovementMethod()
        lifecycleScope.launch {
            presenter.onCreate()
        }
    }

    override fun onResume() {
        super.onResume()
        log.d("onResume")
    }

    override fun onDestroy() {
        super.onDestroy()
        presenter.onDestroy()
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
        lifecycleScope.launch {
            presenter.onSendClick(findViewById<EditText>(R.id.edt_msg).text.toString())
        }
    }

}

fun String.toEditable(): Editable =  Editable.Factory.getInstance().newEditable(this)
