package com.mornshield.tv.sync

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.ServerSocket
import java.net.Socket

class NsdSyncServer(
    private val context: Context,
    private val onEventReceived: (String) -> Unit
) {

    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val SERVICE_TYPE = "_mornshield._tcp"
    private val SERVICE_NAME = "MornShieldTV"

    private var serverSocket: ServerSocket? = null
    private var registrationListener: NsdManager.RegistrationListener? = null
    
    private val scope = CoroutineScope(Dispatchers.IO)
    private var serverJob: Job? = null
    private var localPort: Int = -1

    fun start() {
        stop()

        serverJob = scope.launch {
            try {
                // Initialize ServerSocket on ephemeral port
                serverSocket = ServerSocket(0).also {
                    this@NsdSyncServer.localPort = it.localPort
                }
                Log.d("NsdSyncServer", "Socket Server started on port $localPort")

                registerNsdService(localPort)

                // Listen loop
                while (serverSocket != null) {
                    val socket = serverSocket?.accept() ?: break
                    scope.launch { handleClientConnection(socket) }
                }
            } catch (e: Exception) {
                Log.e("NsdSyncServer", "Server Socket error: ${e.message}")
            }
        }
    }

    private fun registerNsdService(port: Int) {
        val serviceInfo = NsdServiceInfo().apply {
            serviceName = SERVICE_NAME
            serviceType = SERVICE_TYPE
            setPort(port)
        }

        registrationListener = object : NsdManager.RegistrationListener {
            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {
                Log.e("NsdSyncServer", "NSD Registration failed: $errorCode")
            }

            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {
                Log.e("NsdSyncServer", "NSD Unregistration failed: $errorCode")
            }

            override fun onServiceRegistered(serviceInfo: NsdServiceInfo?) {
                Log.d("NsdSyncServer", "NSD Service registered successfully: ${serviceInfo?.serviceName}")
            }

            override fun onServiceUnregistered(serviceInfo: NsdServiceInfo?) {
                Log.d("NsdSyncServer", "NSD Service unregistered successfully: ${serviceInfo?.serviceName}")
            }
        }

        try {
            nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
        } catch (e: Exception) {
            Log.e("NsdSyncServer", "Error registering service: ${e.message}")
        }
    }

    private fun handleClientConnection(socket: Socket) {
        try {
            val reader = BufferedReader(InputStreamReader(socket.getInputStream(), "UTF-8"))
            val line = reader.readLine()
            if (line != null) {
                Log.d("NsdSyncServer", "Received connection payload: $line")
                val json = JSONObject(line)
                val event = json.optString("event")
                if (event.isNotEmpty()) {
                    scope.launch(Dispatchers.Main) {
                        onEventReceived(event)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("NsdSyncServer", "Error reading socket client: ${e.message}")
        } finally {
            try {
                socket.close()
            } catch (e: Exception) {}
        }
    }

    fun stop() {
        // Unregister service
        registrationListener?.let {
            try {
                nsdManager.unregisterService(it)
            } catch (e: Exception) {}
        }
        registrationListener = null

        // Close socket
        try {
            serverSocket?.close()
        } catch (e: Exception) {}
        serverSocket = null

        serverJob?.cancel()
        serverJob = null
        localPort = -1
        Log.d("NsdSyncServer", "Socket Server stopped")
    }
}
