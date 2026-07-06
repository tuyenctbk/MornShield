package com.mornshield.core.sync

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.launch
import java.io.OutputStreamWriter
import java.net.Socket

class NsdSyncClient(context: Context) {

    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val SERVICE_TYPE = "_mornshield._tcp"
    
    private var discoveryListener: NsdManager.DiscoveryListener? = null
    private var resolvedServiceInfo: NsdServiceInfo? = null
    
    private val scope = CoroutineScope(Dispatchers.IO)

    fun startDiscovery() {
        stopDiscovery()
        
        discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onStartDiscoveryFailed(serviceType: String?, errorCode: Int) {
                Log.e("NsdSyncClient", "Start discovery failed: $errorCode")
            }

            override fun onStopDiscoveryFailed(serviceType: String?, errorCode: Int) {
                Log.e("NsdSyncClient", "Stop discovery failed: $errorCode")
            }

            override fun onDiscoveryStarted(serviceType: String?) {
                Log.d("NsdSyncClient", "NSD discovery started")
            }

            override fun onDiscoveryStopped(serviceType: String?) {
                Log.d("NsdSyncClient", "NSD discovery stopped")
            }

            override fun onServiceFound(serviceInfo: NsdServiceInfo?) {
                if (serviceInfo == null) return
                Log.d("NsdSyncClient", "Service found: ${serviceInfo.serviceName}")
                
                if (serviceInfo.serviceType.contains(SERVICE_TYPE)) {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        nsdManager.registerServiceInfoCallback(serviceInfo, Dispatchers.IO.asExecutor(), object : NsdManager.ServiceInfoCallback {
                            override fun onServiceInfoCallbackRegistrationFailed(errorCode: Int) {
                                Log.e("NsdSyncClient", "Callback registration failed: $errorCode")
                            }

                            override fun onServiceUpdated(resolvedInfo: NsdServiceInfo) {
                                val host = resolvedInfo.host?.hostAddress ?: "unknown"
                                Log.d("NsdSyncClient", "Service updated: $host:${resolvedInfo.port}")
                                resolvedServiceInfo = resolvedInfo
                            }

                            override fun onServiceLost() {
                                Log.d("NsdSyncClient", "Service lost via callback")
                                resolvedServiceInfo = null
                            }

                            override fun onServiceInfoCallbackUnregistered() {}
                        })
                    } else {
                        @Suppress("DEPRECATION")
                        nsdManager.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                            override fun onResolveFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {
                                Log.e("NsdSyncClient", "Resolve failed: $errorCode")
                            }

                            override fun onServiceResolved(resolvedInfo: NsdServiceInfo?) {
                                if (resolvedInfo == null) return
                                Log.d("NsdSyncClient", "Service resolved: ${resolvedInfo.host}:${resolvedInfo.port}")
                                resolvedServiceInfo = resolvedInfo
                            }
                        })
                    }
                }
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo?) {
                Log.d("NsdSyncClient", "Service lost: ${serviceInfo?.serviceName}")
                if (serviceInfo?.serviceName == resolvedServiceInfo?.serviceName) {
                    resolvedServiceInfo = null
                }
            }
        }

        try {
            nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
        } catch (e: Exception) {
            Log.e("NsdSyncClient", "Error starting NSD: ${e.message}")
        }
    }

    fun stopDiscovery() {
        discoveryListener?.let {
            try {
                nsdManager.stopServiceDiscovery(it)
            } catch (e: Exception) {}
        }
        discoveryListener = null
        resolvedServiceInfo = null
    }

    /**
     * Sends a synchronization event to the resolved Android TV TCP Socket Server.
     * Implements a simple retry mechanism.
     */
    fun sendSyncEvent(event: String, retryCount: Int = 0) {
        val serviceInfo = resolvedServiceInfo
        if (serviceInfo == null) {
            Log.w("NsdSyncClient", "Cannot send event: TV service not resolved. Starting discovery...")
            startDiscovery()
            return
        }

        scope.launch {
            var socket: Socket? = null
            try {
                // Connect socket with 3-second timeout
                socket = Socket(serviceInfo.host, serviceInfo.port).apply {
                    soTimeout = 3000
                }
                
                val writer = OutputStreamWriter(socket.getOutputStream(), "UTF-8")
                val jsonPayload = "{\"event\": \"$event\", \"timestamp\": ${System.currentTimeMillis()}}\n"
                
                writer.write(jsonPayload)
                writer.flush()
                Log.d("NsdSyncClient", "Successfully synced event $event to TV")
            } catch (e: Exception) {
                Log.e("NsdSyncClient", "Socket sync exception: ${e.message}")
                if (retryCount < 2) {
                    Log.d("NsdSyncClient", "Retrying sync event $event (attempt ${retryCount + 1})")
                    kotlinx.coroutines.delay(2000)
                    sendSyncEvent(event, retryCount + 1)
                }
            } finally {
                try {
                    socket?.close()
                } catch (e: Exception) {}
            }
        }
    }
}
