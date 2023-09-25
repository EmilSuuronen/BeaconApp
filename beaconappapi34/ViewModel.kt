package com.example.beaconlab

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.requestPermissions
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class BluetoothViewModel() : ViewModel() {
    private val _scanResults = MutableLiveData<List<ScanResult>>(emptyList())
    val scanResults: LiveData<List<ScanResult>> = _scanResults

    private val _scanning = MutableLiveData(false)
    val scanning: LiveData<Boolean> = _scanning

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private val bluetoothScanner: BluetoothLeScanner? = bluetoothAdapter?.bluetoothLeScanner

    private val mResults = HashMap<String, ScanResult>()

    private val leScanCallback: ScanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            val deviceAddress = device.address
            val deviceIsConnectable = result.isConnectable
            mResults[deviceAddress] = result
            _scanResults.postValue(mResults.values.toList())
            Log.d("SCANRESULT", "${deviceAddress}")
        }
    }

    @SuppressLint("MissingPermission")
    fun startScanning() {
        if (hasBluetoothPermissions()) {
            _scanning.postValue(true)
            val settings = android.bluetooth.le.ScanSettings.Builder()
                .setScanMode(android.bluetooth.le.ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setReportDelay(0)
                .build()
            bluetoothScanner?.startScan(null, settings, leScanCallback)
            viewModelScope.launch(Dispatchers.IO) {
                delay(SCAN_PERIOD)
                stopScanning()
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun stopScanning() {
        _scanning.postValue(false)
        bluetoothScanner?.stopScan(leScanCallback)
    }

    private fun hasBluetoothPermissions(): Boolean {
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled
    }

    companion object {
        const val SCAN_PERIOD: Long = 10000
    }
}