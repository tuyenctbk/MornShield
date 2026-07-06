package com.mornshield.core.sync

import android.content.Context
import android.util.Log
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import org.json.JSONObject

class NearbySyncManager(private val context: Context) {

    private val connectionsClient = Nearby.getConnectionsClient(context)
    private val STRATEGY = Strategy.P2P_STAR
    private val SERVICE_ID = "com.mornshield.sync"

    fun startAdvertising() {
        val options = AdvertisingOptions.Builder().setStrategy(STRATEGY).build()
        connectionsClient.startAdvertising(
            "MornShieldTV",
            SERVICE_ID,
            object : ConnectionLifecycleCallback() {
                override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
                    connectionsClient.acceptConnection(endpointId, PayloadCallbackImpl())
                }
                override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {}
                override fun onDisconnected(endpointId: String) {}
            },
            options
        )
    }

    fun startDiscovery() {
        val options = DiscoveryOptions.Builder().setStrategy(STRATEGY).build()
        connectionsClient.startDiscovery(
            SERVICE_ID,
            object : EndpointDiscoveryCallback() {
                override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
                    connectionsClient.requestConnection("MornShieldNode", endpointId, object : ConnectionLifecycleCallback() {
                        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
                            connectionsClient.acceptConnection(endpointId, PayloadCallbackImpl())
                        }
                        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {}
                        override fun onDisconnected(endpointId: String) {}
                    })
                }
                override fun onEndpointLost(endpointId: String) {}
            },
            options
        )
    }

    private class PayloadCallbackImpl : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            payload.asBytes()?.let {
                val json = JSONObject(String(it))
                Log.d("NearbySync", "Received: ${json.optString("event")}")
            }
        }
        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {}
    }
}
