package com.example.filesharingpro.activities

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import com.example.filesharingpro.R
import com.example.filesharingpro.broadcast.WiFiDirectBroadcastReceiver
import com.example.filesharingpro.fragments.DeviceDetailFragment
import com.example.filesharingpro.fragments.DeviceListFragment

class WiFiDirectActivity : Activity(), WifiP2pManager.ChannelListener,
    DeviceListFragment.DeviceActionListener {

    private var manager: WifiP2pManager? = null
    private var isWifiP2pEnabled = false
    private var retryChannel = false
    private val intentFilter = IntentFilter()
    private var channel: WifiP2pManager.Channel? = null
    private var receiver: BroadcastReceiver? = null

    companion object {
        const val TAG = "filesharingpro"
        private const val PERMISSIONS_REQUEST_CODE_ACCESS_FINE_LOCATION = 1001
    }

    @SuppressLint("ObsoleteSdkInt")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main)

        // add necessary intent values to be matched.

        // manager.removeGroup(channel,null);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)

        if (!initP2p()) {
            finish()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
            && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                PERMISSIONS_REQUEST_CODE_ACCESS_FINE_LOCATION
            )
            // After this point you wait for callback in
            // onRequestPermissionsResult(int, String[], int[]) overridden method
        }
    }

    /**
     * @param isWifiP2pEnabled the isWifiP2pEnabled to set
     */
    fun setIsWifiP2pEnabled(isWifiP2pEnabled: Boolean) {
        this.isWifiP2pEnabled = isWifiP2pEnabled
    }

    private fun initP2p(): Boolean {
        // Device capability definition check
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_WIFI_DIRECT)) {
            Log.e(
                TAG,
                "Wi-Fi Direct is not supported by this device."
            )
            return false
        }

        // Hardware capability check
        val wifiManager = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
        if (wifiManager == null) {
            Log.e(
                TAG,
                "Cannot get Wi-Fi system service."
            )
            return false
        }
        if (!wifiManager.isP2pSupported) {
            Log.e(
                TAG,
                "Wi-Fi Direct is not supported by the hardware or Wi-Fi is off."
            )
            return false
        }
        manager = getSystemService(WIFI_P2P_SERVICE) as WifiP2pManager
        if (manager == null) {
            Log.e(
                TAG,
                "Cannot get Wi-Fi Direct system service."
            )
            return false
        }
        channel = manager!!.initialize(this, mainLooper, null)
        if (channel == null) {
            Log.e(
                TAG,
                "Cannot initialize Wi-Fi Direct."
            )
            return false
        }
        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSIONS_REQUEST_CODE_ACCESS_FINE_LOCATION -> if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Log.e(
                    TAG,
                    "Fine location permission is not granted!"
                )
                finish()
            }
        }
    }

    /** register the BroadcastReceiver with the intent values to be matched  */
    override fun onResume() {
        super.onResume()
        receiver = channel?.let { WiFiDirectBroadcastReceiver(manager, it, this) }
        registerReceiver(receiver, intentFilter)
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(receiver)
    }

    /**
     * Remove all peers and clear all fields. This is called on
     * BroadcastReceiver receiving a state change event.
     */
    fun resetData() {
        val fragmentList: DeviceListFragment = fragmentManager
            .findFragmentById(R.id.frag_list) as DeviceListFragment
        val fragmentDetails: DeviceDetailFragment = fragmentManager
            .findFragmentById(R.id.frag_detail) as DeviceDetailFragment
        if (fragmentList != null) {
            fragmentList.clearPeers()
        }
        if (fragmentDetails != null) {
            fragmentDetails.resetViews()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.action_items, menu)
        return true
    }

    @SuppressLint("MissingPermission")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.atn_direct_enable -> {
                if (manager != null && channel != null) {

                    // Since this is the system wireless settings activity, it's
                    // not going to send us a result. We will be notified by
                    // WiFiDeviceBroadcastReceiver instead.
                    startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS))
                } else {
                    Log.e(
                        TAG,
                        "channel or manager is null"
                    )
                }
                true
            }
            R.id.atn_direct_discover -> {
                if (!isWifiP2pEnabled) {
                    Toast.makeText(
                        this@WiFiDirectActivity, R.string.p2p_off_warning,
                        Toast.LENGTH_SHORT
                    ).show()
                    return true
                }
                val fragment: DeviceListFragment = fragmentManager
                    .findFragmentById(R.id.frag_list) as DeviceListFragment
                fragment.onInitiateDiscovery()
                manager!!.discoverPeers(channel, object : WifiP2pManager.ActionListener {
                    override fun onSuccess() {
                        Toast.makeText(
                            this@WiFiDirectActivity, "Discovery Initiated",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    override fun onFailure(reasonCode: Int) {
                        Toast.makeText(
                            this@WiFiDirectActivity, "Discovery Failed : $reasonCode",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                })
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun showDetails(device: WifiP2pDevice?) {
        val fragment: DeviceDetailFragment = fragmentManager
            .findFragmentById(R.id.frag_detail) as DeviceDetailFragment
        if (device != null) {
            fragment.showDetails(device)
        }
    }

    @SuppressLint("MissingPermission")
    override fun connect(config: WifiP2pConfig?) {
        manager!!.connect(channel, config, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                // WiFiDirectBroadcastReceiver will notify us. Ignore for now.
            }

            override fun onFailure(reason: Int) {
                Toast.makeText(
                    this@WiFiDirectActivity, "Connect failed. Retry.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    override fun disconnect() {
        val fragment: DeviceDetailFragment = fragmentManager
            .findFragmentById(R.id.frag_detail) as DeviceDetailFragment
        fragment.resetViews()
        manager!!.removeGroup(channel, object : WifiP2pManager.ActionListener {
            override fun onFailure(reasonCode: Int) {
                Log.d(TAG, "Disconnect failed. Reason :$reasonCode")
            }

            override fun onSuccess() {
                fragment.view?.visibility = View.GONE
            }
        })
    }

    override fun onChannelDisconnected() {

        // we will try once more
        if (manager != null && !retryChannel) {
            Toast.makeText(this, "Channel lost. Trying again", Toast.LENGTH_LONG).show()
            resetData()
            retryChannel = true
            manager!!.initialize(this, mainLooper, this)
        } else {
            Toast.makeText(
                this,
                "Severe! Channel is probably lost permanently. Try Disable/Re-Enable P2P.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun cancelDisconnect() {

        /**
         * A cancel abort request by user. Disconnect i.e. removeGroup if
         * already connected. Else, request WifiP2pManager to abort the ongoing
         * request
         */
        if (manager != null) {
            val fragment = fragmentManager
                .findFragmentById(R.id.frag_list) as DeviceListFragment
            if (fragment.getDevice() == null
                || fragment.getDevice()!!.status === WifiP2pDevice.CONNECTED
            ) {
                disconnect()
            } else if (fragment.getDevice()!!.status === WifiP2pDevice.AVAILABLE
                || fragment.getDevice()!!.status === WifiP2pDevice.INVITED
            ) {
                manager!!.cancelConnect(channel, object : WifiP2pManager.ActionListener {
                    override fun onSuccess() {
                        Toast.makeText(
                            this@WiFiDirectActivity, "Aborting connection",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    override fun onFailure(reasonCode: Int) {
                        Toast.makeText(
                            this@WiFiDirectActivity,
                            "Connect abort request failed. Reason Code: $reasonCode",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                })
            }
        }
    }

    fun saveProfileInformation(view: View) {
        Toast.makeText(this, "Clicked", Toast.LENGTH_SHORT).show();
    }
}