package com.aura.p2p.ble

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelUuid
import android.util.Log
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Android [BleProximity] over [BluetoothLeAdvertiser] + the LE scanner. Advertises
 * the beacon as service data under a fixed app UUID (non-connectable) and scans,
 * filtered to the same UUID. No GATT, no mesh, no cold-start.
 *
 * Permission model: API 31+ uses BLUETOOTH_ADVERTISE / BLUETOOTH_SCAN (declared
 * `neverForLocation`) / BLUETOOTH_CONNECT; pre-31 uses legacy BLUETOOTH(_ADMIN)
 * plus ACCESS_FINE_LOCATION for scanning. If a required permission is missing or
 * the radio is off, [start] emits nothing rather than crashing.
 *
 * TODO: emulators have no BLE radio — verify on physical devices. A future GATT
 * handshake would exchange the full DID + dialable multiaddrs (the beacon only
 * carries truncated hints today).
 */
class AndroidBleProximity(private val context: Context) : BleProximity {

    private val manager: BluetoothManager? =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val adapter get() = manager?.adapter

    @Volatile private var advertiser: BluetoothLeAdvertiser? = null
    @Volatile private var advertiseCallback: AdvertiseCallback? = null

    override val isSupported: Boolean
        get() = context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE) &&
            adapter?.isEnabled == true &&
            adapter?.bluetoothLeAdvertiser != null

    @SuppressLint("MissingPermission") // guarded by hasPermissions()
    override fun start(beacon: BleBeacon): Flow<BleSighting> = callbackFlow {
        if (!isSupported || !hasPermissions()) {
            Log.w(TAG, "BLE not started (supported=$isSupported, permissions=${hasPermissions()})")
            close()
            return@callbackFlow
        }
        val a = adapter!!

        // ---- advertise ----
        val adv = a.bluetoothLeAdvertiser
        val advSettings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setConnectable(false)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
            .build()
        val advData = AdvertiseData.Builder()
            .addServiceUuid(SERVICE_UUID)
            .addServiceData(SERVICE_UUID, encodeBeacon(beacon))
            .build()
        val advCb = object : AdvertiseCallback() {
            override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) { Log.i(TAG, "advertising") }
            override fun onStartFailure(errorCode: Int) { Log.w(TAG, "advertise failed: $errorCode") }
        }
        runCatching { adv?.startAdvertising(advSettings, advData, advCb) }
        advertiser = adv
        advertiseCallback = advCb

        // ---- scan ----
        val scanner = a.bluetoothLeScanner
        val filters = listOf(ScanFilter.Builder().setServiceUuid(SERVICE_UUID).build())
        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        val scanCb = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val sd = result.scanRecord?.getServiceData(SERVICE_UUID)
                val (did, peer) = decodeBeacon(sd)
                trySend(BleSighting(result.device.address, did, peer, result.rssi))
            }
            override fun onScanFailed(errorCode: Int) { Log.w(TAG, "scan failed: $errorCode") }
        }
        runCatching { scanner?.startScan(filters, scanSettings, scanCb) }

        awaitClose {
            runCatching { scanner?.stopScan(scanCb) }
            runCatching { adv?.stopAdvertising(advCb) }
            advertiser = null
            advertiseCallback = null
        }
    }

    @SuppressLint("MissingPermission")
    override fun stop() {
        val adv = advertiser
        val cb = advertiseCallback
        if (adv != null && cb != null) runCatching { adv.stopAdvertising(cb) }
        advertiser = null
        advertiseCallback = null
        // Scanning is torn down via the flow's awaitClose when its collector cancels.
    }

    private fun hasPermissions(): Boolean {
        val needed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            listOf(
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
            )
        } else {
            listOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        return needed.all { context.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }
    }

    private fun encodeBeacon(b: BleBeacon): ByteArray =
        "${b.didSuffix}|${b.peerIdSuffix}".encodeToByteArray()

    private fun decodeBeacon(bytes: ByteArray?): Pair<String?, String?> {
        if (bytes == null) return null to null
        val s = runCatching { bytes.decodeToString() }.getOrNull() ?: return null to null
        val parts = s.split('|')
        return parts.getOrNull(0) to parts.getOrNull(1)
    }

    private companion object {
        const val TAG = "BleProximity"
        // App-private GATT-style service UUID for the proximity beacon.
        val SERVICE_UUID: ParcelUuid = ParcelUuid.fromString("9f3a5b00-1c2d-4e5f-8a9b-0c1d2e3f4a5b")
    }
}
