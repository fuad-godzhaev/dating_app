package com.aura.p2p.discovery

import android.content.Context
import com.aura.p2p.transport.DeviceNet

/** Android [LocalAddressProvider] backed by [DeviceNet] (ConnectivityManager). */
class AndroidLocalAddressProvider(private val context: Context) : LocalAddressProvider {
    override suspend fun localIpv4(): String? = DeviceNet.localIpv4(context)
}
