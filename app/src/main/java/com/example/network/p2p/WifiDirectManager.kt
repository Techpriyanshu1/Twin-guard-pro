package com.example.network.p2p

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pDeviceList
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class WifiDirectManager(private val context: Context) {
    private val tag = "WifiDirectManager"
    private val manager: WifiP2pManager? = context.getSystemService(Context.WIFI_P2P_SERVICE) as? WifiP2pManager
    private var channel: WifiP2pManager.Channel? = null

    private val _isP2pEnabled = MutableStateFlow(false)
    val isP2pEnabled = _isP2pEnabled.asStateFlow()

    private val _peers = MutableStateFlow<List<WifiP2pDevice>>(emptyList())
    val peers = _peers.asStateFlow()

    private val _connectionInfo = MutableStateFlow<WifiP2pInfo?>(null)
    val connectionInfo = _connectionInfo.asStateFlow()

    private val _statusMessage = MutableStateFlow("P2P Ready")
    val statusMessage = _statusMessage.asStateFlow()

    private val receiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
                    val state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)
                    _isP2pEnabled.value = (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED)
                }
                WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                    manager?.requestPeers(channel) { peersList: WifiP2pDeviceList? ->
                        _peers.value = peersList?.deviceList?.toList() ?: emptyList()
                    }
                }
                WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                    manager?.requestConnectionInfo(channel) { info: WifiP2pInfo? ->
                        _connectionInfo.value = info
                        if (info?.groupFormed == true) {
                            val isOwner = info.isGroupOwner
                            val host = info.groupOwnerAddress?.hostAddress ?: ""
                            _statusMessage.value = if (isOwner) "Connected as Group Owner ($host)" else "Connected to Camera ($host)"
                        }
                    }
                }
            }
        }
    }

    fun initialize() {
        if (manager != null) {
            channel = manager.initialize(context, Looper.getMainLooper(), null)
            val filter = IntentFilter().apply {
                addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
                addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
                addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
                addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
            }
            context.registerReceiver(receiver, filter)
        }
    }

    @SuppressLint("MissingPermission")
    fun discoverPeers(onComplete: (Boolean, String) -> Unit = { _, _ -> }) {
        if (manager == null || channel == null) {
            onComplete(false, "Wi-Fi Direct not supported on this device")
            return
        }
        _statusMessage.value = "Discovering nearby CCTV devices..."
        manager.discoverPeers(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                _statusMessage.value = "Searching for nearby Wi-Fi Direct devices..."
                onComplete(true, "Discovery initiated")
            }
            override fun onFailure(reasonCode: Int) {
                val err = "Discovery failed: code $reasonCode"
                _statusMessage.value = err
                onComplete(false, err)
            }
        })
    }

    @SuppressLint("MissingPermission")
    fun connectToDevice(device: WifiP2pDevice, onResult: (Boolean, String) -> Unit) {
        if (manager == null || channel == null) {
            onResult(false, "Manager uninitialized")
            return
        }
        val config = WifiP2pConfig().apply {
            deviceAddress = device.deviceAddress
        }
        _statusMessage.value = "Connecting to ${device.deviceName}..."
        manager.connect(channel, config, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                _statusMessage.value = "Connection request sent to ${device.deviceName}"
                onResult(true, "Pairing initiated")
            }
            override fun onFailure(reason: Int) {
                val err = "Failed to pair: code $reason"
                _statusMessage.value = err
                onResult(false, err)
            }
        })
    }

    fun disconnect() {
        try {
            manager?.cancelConnect(channel, null)
            manager?.removeGroup(channel, null)
            _statusMessage.value = "Disconnected"
        } catch (e: Exception) {
            Log.e(tag, "Error disconnecting P2P", e)
        }
    }

    fun unregister() {
        try {
            context.unregisterReceiver(receiver)
        } catch (_: Exception) {}
    }
}
