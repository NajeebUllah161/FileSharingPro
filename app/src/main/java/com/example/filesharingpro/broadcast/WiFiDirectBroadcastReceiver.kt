package com.example.filesharingpro.broadcast

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.NetworkInfo
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pManager
import android.net.wifi.p2p.WifiP2pManager.PeerListListener
import android.os.Parcelable
import android.util.Log
import com.example.filesharingpro.R
import com.example.filesharingpro.activities.WiFiDirectActivity
import com.example.filesharingpro.fragments.DeviceDetailFragment
import com.example.filesharingpro.fragments.DeviceListFragment

/**
 * A BroadcastReceiver that notifies of important wifi p2p events.
 */
class WiFiDirectBroadcastReceiver
/**
 * @param manager WifiP2pManager system service
 * @param channel Wifi p2p channel
 * @param activity activity associated with the receiver
 */(
    private val manager: WifiP2pManager?, private val channel: WifiP2pManager.Channel,
    private val activity: WiFiDirectActivity
) : BroadcastReceiver() {

    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context?, intent: Intent?) {
        val action = intent!!.action
        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION == action) {

            // UI update to indicate wifi p2p status.
            val state = intent?.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)
            if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                // Wifi Direct mode is enabled
                activity.setIsWifiP2pEnabled(true)
            } else {
                activity.setIsWifiP2pEnabled(false)
                activity.resetData()
            }
            Log.d(WiFiDirectActivity.TAG, "P2P state changed - $state")
        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION == action) {

            // request available peers from the wifi p2p manager. This is an
            // asynchronous call and the calling activity is notified with a
            // callback on PeerListListener.onPeersAvailable()
            manager?.requestPeers(
                channel, activity.fragmentManager
                    .findFragmentById(R.id.frag_list) as PeerListListener
            )
            Log.d(WiFiDirectActivity.TAG, "P2P peers changed")
        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION == action) {
            if (manager == null) {
                return
            }
            val networkInfo = intent
                ?.getParcelableExtra<Parcelable>(WifiP2pManager.EXTRA_NETWORK_INFO) as NetworkInfo?
            if (networkInfo!!.isConnected) {

                // we are connected with the other device, request connection
                // info to find group owner IP
                val fragment = activity
                    .fragmentManager.findFragmentById(R.id.frag_detail) as DeviceDetailFragment
                manager.requestConnectionInfo(channel, fragment)
            } else {
                // It's a disconnect
                activity.resetData()
            }
        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION == action) {
            val fragment = activity.fragmentManager
                .findFragmentById(R.id.frag_list) as DeviceListFragment
            if (intent != null) {
                fragment.updateThisDevice(
                    (intent.getParcelableExtra<Parcelable>(
                        WifiP2pManager.EXTRA_WIFI_P2P_DEVICE
                    ) as WifiP2pDevice?)!!
                )
            }
        }
    }
}