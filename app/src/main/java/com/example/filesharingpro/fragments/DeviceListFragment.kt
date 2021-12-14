package com.example.filesharingpro.fragments

import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.Context
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pDeviceList
import android.net.wifi.p2p.WifiP2pManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
//import androidx.fragment.app.ListFragment
import android.app.ListFragment
import com.example.filesharingpro.R
import com.example.filesharingpro.activities.WiFiDirectActivity
import java.util.ArrayList


class DeviceListFragment : ListFragment(), WifiP2pManager.PeerListListener {

    private val peers: MutableList<WifiP2pDevice> = ArrayList()
    var progressDialog: ProgressDialog? = null
    private var mContentView: View? = null
    private var device: WifiP2pDevice? = null

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        this.listAdapter = WiFiPeerListAdapter(
            activity, R.layout.row_devices, peers
        )
    }

    //replace this with init
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireActivityContext = activity

    }

    @SuppressLint("InflateParams")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        mContentView = inflater.inflate(R.layout.device_list, null)
        return mContentView
    }

    /**
     * @return this device
     */
    fun getDevice(): WifiP2pDevice? {
        return device
    }

    companion object {

        @SuppressLint("StaticFieldLeak")
        var requireActivityContext: Context? = null

        private fun getDeviceStatus(deviceStatus: Int): String? {
            Log.d(WiFiDirectActivity.TAG, "Peer status :$deviceStatus")
            return when (deviceStatus) {
                WifiP2pDevice.AVAILABLE -> "Available"
                WifiP2pDevice.INVITED -> "Invited"
                WifiP2pDevice.CONNECTED -> "Connected"
                WifiP2pDevice.FAILED -> "Failed"
                WifiP2pDevice.UNAVAILABLE -> "Unavailable"
                else -> "Unknown"
            }
        }
    }

    override fun onListItemClick(l: ListView, v: View, position: Int, id: Long) {
        super.onListItemClick(l, v, position, id)
        val device = listAdapter!!.getItem(position) as WifiP2pDevice
        (activity as DeviceActionListener?)?.showDetails(
            device
        )
        Toast.makeText(activity, "Clicked $position", Toast.LENGTH_SHORT).show()
    }

    /**
     * Array adapter for ListFragment that maintains WifiP2pDevice list.
     */
    private class WiFiPeerListAdapter
    /**
     * @param context
     * @param textViewResourceId
     * @param objects
     */(
        context: Context?, textViewResourceId: Int,
        private val items: List<WifiP2pDevice>
    ) : ArrayAdapter<WifiP2pDevice?>(context!!, textViewResourceId, items) {
        @SuppressLint("InflateParams")
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            var v = convertView
            if (v == null) {
                val vi = requireActivityContext?.getSystemService(
                    Context.LAYOUT_INFLATER_SERVICE
                ) as LayoutInflater
                v = vi.inflate(R.layout.row_devices, null)
            }
            val device = items[position]
            if (device != null) {
                val top = v!!.findViewById<TextView>(R.id.device_name)
                val bottom = v.findViewById<TextView>(R.id.device_details)
                if (top != null) {
                    top.text = device.deviceName
                }
                bottom?.text = getDeviceStatus(device.status)
            }
            return v!!
        }
    }

    /**
     * Update UI for this device.
     *
     * @param device WifiP2pDevice object
     */
    fun updateThisDevice(device: WifiP2pDevice) {
        this.device = device
        var view = mContentView!!.findViewById<TextView>(R.id.my_name)
        view.text = device.deviceName
        view = mContentView!!.findViewById(R.id.my_status)
        view.text = getDeviceStatus(device.status)
    }

    override fun onPeersAvailable(peerList: WifiP2pDeviceList) {
        if (progressDialog != null && progressDialog!!.isShowing) {
            progressDialog!!.dismiss()
        }
        peers.clear()
        peers.addAll(peerList.deviceList)
        (listAdapter as WiFiPeerListAdapter?)!!.notifyDataSetChanged()
        if (peers.size == 0) {
            Log.d(WiFiDirectActivity.TAG, "No devices found")
            return
        }
    }

    fun clearPeers() {
        peers.clear()
        (listAdapter as WiFiPeerListAdapter?)!!.notifyDataSetChanged()
    }

    /**
     *
     */
    fun onInitiateDiscovery() {
        if (progressDialog != null && progressDialog!!.isShowing) {
            progressDialog!!.dismiss()
        }
        progressDialog = ProgressDialog.show(
            activity, "Press back to cancel", "finding peers", true,
            true
        ) {
            Log.d("onInitiateDiscovery", "onCancel")
        }
    }

    /**
     * An interface-callback for the activity to listen to fragment interaction
     * events.
     */
    interface DeviceActionListener {
        fun showDetails(device: WifiP2pDevice?)
        fun cancelDisconnect()
        fun connect(config: WifiP2pConfig?)
        fun disconnect()
    }

}