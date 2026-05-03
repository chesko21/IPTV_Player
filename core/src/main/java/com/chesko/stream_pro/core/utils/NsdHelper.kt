package com.chesko.stream_pro.core.utils

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class NsdHelper(context: Context) {
    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val serviceType = "_iptv_cast._tcp."
    private val serviceName = "IPTV_Player_${android.os.Build.MODEL}"

    private val _discoveredServices = MutableStateFlow<List<NsdServiceInfo>>(emptyList())
    val discoveredServices: StateFlow<List<NsdServiceInfo>> = _discoveredServices

    private var registrationListener: NsdManager.RegistrationListener? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null

    fun registerService(port: Int) {
        val serviceInfo = NsdServiceInfo().apply {
            serviceName = this@NsdHelper.serviceName
            serviceType = this@NsdHelper.serviceType
            setPort(port)
        }

        registrationListener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(NsdServiceInfo: NsdServiceInfo) {
                Log.d("NsdHelper", "Service registered: ${NsdServiceInfo.serviceName}")
            }
            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}
            override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) {}
            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}
        }

        nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
    }

    fun discoverServices() {
        discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) {
                Log.d("NsdHelper", "Discovery started")
            }

            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                if (serviceInfo.serviceType == serviceType) {
                    if (serviceInfo.serviceName != serviceName) {
                        resolveService(serviceInfo)
                    }
                }
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                _discoveredServices.value = _discoveredServices.value.filter { it.serviceName != serviceInfo.serviceName }
            }

            override fun onDiscoveryStopped(serviceType: String) {}
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                nsdManager.stopServiceDiscovery(this)
            }
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                nsdManager.stopServiceDiscovery(this)
            }
        }

        nsdManager.discoverServices(serviceType, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
    }

    private fun resolveService(serviceInfo: NsdServiceInfo) {
        nsdManager.resolveService(serviceInfo, object : NsdManager.ResolveListener {
            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}
            override fun onServiceResolved(resolvedServiceInfo: NsdServiceInfo) {
                val current = _discoveredServices.value.toMutableList()
                if (current.none { it.serviceName == resolvedServiceInfo.serviceName }) {
                    current.add(resolvedServiceInfo)
                    _discoveredServices.value = current
                }
            }
        })
    }

    fun stopDiscovery() {
        discoveryListener?.let { nsdManager.stopServiceDiscovery(it) }
        discoveryListener = null
    }

    fun unregisterService() {
        registrationListener?.let { nsdManager.unregisterService(it) }
        registrationListener = null
    }
}
