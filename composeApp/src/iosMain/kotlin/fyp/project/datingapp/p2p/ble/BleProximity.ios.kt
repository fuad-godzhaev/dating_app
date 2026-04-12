package fyp.project.datingapp.p2p.ble

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * iOS stub (lazy state). TODO(iOS): CoreBluetooth — `CBPeripheralManager` to
 * advertise the beacon as a service and `CBCentralManager` to scan for it.
 * Reports unsupported and emits nothing until then.
 */
class IosBleProximity : BleProximity {
    override val isSupported: Boolean = false
    override fun start(beacon: BleBeacon): Flow<BleSighting> = emptyFlow()
    override fun stop() {}
}
