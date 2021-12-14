package com.example.filesharingpro.fragments

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Fragment
import android.app.ProgressDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.net.wifi.WpsInfo
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.os.AsyncTask
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
//import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.filesharingpro.R
import com.example.filesharingpro.helpers.FilesUtil
import com.example.filesharingpro.helpers.PathUtil
import com.example.filesharingpro.activities.WiFiDirectActivity
import com.example.filesharingpro.service.FileTransferService
import java.io.*
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.ArrayList
import java.util.concurrent.Executors

class DeviceDetailFragment : Fragment(), WifiP2pManager.ConnectionInfoListener {

    private var mContentView: View? = null
    private var device: WifiP2pDevice? = null
    private var info: WifiP2pInfo? = null
    var progressDialog: ProgressDialog? = null
    var mArrayUri: ArrayList<Uri>? = null
    var imageUri: Uri? = null
    var filesLength = ArrayList<Long>()
    var fileNames = ArrayList<String>()

    private val receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val bundle = intent.extras
            if (bundle != null) {
                val resultCode = bundle.getInt(FileTransferService.RESULT)
                Toast.makeText(
                    context.applicationContext,
                    "File : $resultCode Sent",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        Log.d("Lifecycle", "OnActivityCreated")
    }

    override fun onStart() {
        super.onStart()
        Log.d("Lifecycle", "onStart")
    }

    @SuppressLint("InflateParams")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        mContentView = inflater.inflate(R.layout.device_detail, null)
        mArrayUri = ArrayList()

        mContentView!!.findViewById<View>(R.id.btn_connect).setOnClickListener {
            val config = WifiP2pConfig()
            becomeClient = true
            config.deviceAddress = device!!.deviceAddress
            config.wps.setup = WpsInfo.PBC
            if (progressDialog != null && progressDialog!!.isShowing) {
                progressDialog!!.dismiss()
            }
            progressDialog = ProgressDialog.show(
                activity,
                "Press back to cancel",
                "Connecting to :" + device!!.deviceAddress,
                true,
                true
                //                        new DialogInterface.OnCancelListener() {
                //
                //                            @Override
                //                            public void onCancel(DialogInterface dialog) {
                //                                ((DeviceActionListener) getActivity()).cancelDisconnect();
                //                            }
                //                        }
            )
            (activity as DeviceListFragment.DeviceActionListener?)?.connect(config)
        }
        // Disconnect with user
        mContentView!!.findViewById<View>(R.id.btn_disconnect)
            .setOnClickListener {
                (activity as DeviceListFragment.DeviceActionListener?)?.disconnect()

            }

        // Selecting Image Intent
        mContentView!!.findViewById<View>(R.id.btn_start_client).setOnClickListener {
            // Allow user to pick an image from Gallery or other
            // registered apps
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/*"
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            startActivityForResult(
                Intent.createChooser(intent, "Select photo"),
                CHOOSE_FILE_REQUEST_CODE
            )
        }

        // Selecting Apk Intent
        mContentView!!.findViewById<View>(R.id.btn_select_apk).setOnClickListener { v: View? ->
            // Allow user to pick an apk from File or other
            // registered apps
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "application/vnd.android.package-archive"
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            startActivityForResult(intent, CHOOSE_APK_REQUEST_CODE)
        }

        // Sending data on Peer
        mContentView!!.findViewById<View>(R.id.send_files).setOnClickListener { v: View? ->
            Toast.makeText(activity, "Sending Files!", Toast.LENGTH_SHORT).show()
            sendFiles()
            clearArrayLists()
        }

        return mContentView;
    }

    // Clear all array lists to avoid duplication of files
    private fun clearArrayLists() {
        fileNames.clear()
        filesLength.clear()
        mArrayUri!!.clear()
    }

    // Send Files to connected device using P2P technology
    private fun sendFiles() {

        // TextView statusText = (TextView) mContentView.findViewById(R.id.status_text);
        // statusText.setText("Sending: " + image);
        // Log.d(WiFiDirectActivity.TAG, "Intent----------- " + uri);
        if (!(fileNames.isEmpty() && filesLength.isEmpty() && mArrayUri!!.isEmpty())) {
            val serviceIntent = Intent(activity, FileTransferService::class.java)
            serviceIntent.action = FileTransferService.ACTION_SEND_FILE
            Log.d("NajeebDeviceDetailFragment", mArrayUri.toString())
            // sending Uris, FileName and FileLength to FileTransfer Activity
            serviceIntent.putParcelableArrayListExtra(
                FileTransferService.EXTRAS_FILE_PATH,
                mArrayUri
            )
            serviceIntent.putStringArrayListExtra(FileTransferService.EXTRAS_FILE_NAME, fileNames)
            serviceIntent.putExtra(FileTransferService.EXTRAS_FILE_LENGTH, filesLength)

            // Log.d(WiFiDirectActivity.TAG, "ArrayList of Uri " + mArrayUri.toString());
            if (info!!.isGroupOwner) {
                serviceIntent.putExtra(
                    FileTransferService.EXTRAS_GROUP_OWNER_ADDRESS,
                    clientIP
                )
                Log.d("Checkpoint", "I am Group Owner")
            } else {
                serviceIntent.putExtra(
                    FileTransferService.EXTRAS_GROUP_OWNER_ADDRESS,
                    info!!.groupOwnerAddress.hostAddress
                )

                Log.d("Checkpoint", "I am Group Member")
            }
            serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_OWNER_PORT, 8988)
            // requireActivity().startService(serviceIntent)
            // getActivity().startService(serviceIntent);
            /** Starting service **/
            FileTransferService.enqueueWork(activity, serviceIntent)
        } else {
            Toast.makeText(activity, "Please Select Files First.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendServerIPToGroupOwnerClient() {
        val executor = Executors.newSingleThreadExecutor()
        val handler = Handler(Looper.getMainLooper())
        Log.d("IAmHere", "sasas")
        executor.execute {

            //Background work here
            Log.d("Inside Member", info!!.groupOwnerAddress.hostAddress)
            val SOCKET_TIMEOUT = 5000
            val socket = Socket()
            try {
                socket.bind(null)
                socket.reuseAddress = true
                socket.connect(
                    InetSocketAddress(
                        info!!.groupOwnerAddress.hostAddress,
                        8987
                    ), SOCKET_TIMEOUT
                )
                val outputStream = socket.getOutputStream()
                val objectOutputStream =
                    ObjectOutputStream(outputStream)
                objectOutputStream.writeInt(4)
            } catch (e: IOException) {
                e.printStackTrace()
            }
            handler.post {
                //UI Thread work here
                Toast.makeText(
                    activity.applicationContext,
                    "Fetching Client IP",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun getServerIpFromGroupMemberServer() {
        val executor = Executors.newSingleThreadExecutor()
        val handler = Handler(Looper.getMainLooper())
        executor.execute {

            //Background work here
            try {
                val serverSocket = ServerSocket(8987)
                Log.d("Socket of owner", "Server: Socket opened")
                serverSocket.reuseAddress = true
                val client = serverSocket.accept()
                Log.d("Socket of owner", "Server: connection done")
                val inputstream = client.getInputStream()
                val objectInputStream =
                    ObjectInputStream(inputstream)
                clientIP = client.inetAddress.hostAddress
                Log.d(
                    "IP of client",
                    "Client IP address: " + client.inetAddress.hostAddress
                )
                inputstream.close()
                serverSocket.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
            handler.post {
                //UI Thread work here
                Toast.makeText(
                    activity.applicationContext,
                    "Client IP is :  $clientIP",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Successfully picked image, now transfer URI to an intent service which does the rest
        // User has picked an image. Transfer it to group owner i.e peer using
        // FileTransferService.
        Log.d("Checkpoint", "I am here")
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == CHOOSE_FILE_REQUEST_CODE) {
                if (data!!.clipData != null) {
                    val myClipData = data!!.clipData
                    val count = myClipData!!.itemCount
                    for (i in 0 until count) {
                        // adding imageUri in an array
                        imageUri = myClipData!!.getItemAt(i).uri
                        mArrayUri!!.add(imageUri!!)

                        var fileName: String? =
                            PathUtil.getPath(context, myClipData!!.getItemAt(i).uri)
                        filesLength.add(File(fileName).length())
                        fileName = FilesUtil.getFileName(fileName)
                        fileNames.add(fileName)
                        Log.d("File URI", myClipData!!.getItemAt(i).uri.toString())
                        Log.d("File Path", fileName)
                    }
                    val statusText = mContentView!!.findViewById<TextView>(R.id.status_text)
                    statusText.text = "Sending: $imageUri"
                    Log.d(WiFiDirectActivity.TAG, "Intent----------- $imageUri")
                } else {
                    val imageUri = data!!.data
                    mArrayUri!!.add(imageUri!!)
                    var fileName: String? = PathUtil.getPath(context, imageUri)
                    Log.d("Najeeb", fileName!!)
                    filesLength.add(File(fileName).length())
                    fileName = FilesUtil.getFileName(fileName)
                    fileNames.add(fileName)
                    val statusText = mContentView!!.findViewById<TextView>(R.id.status_text)
                    statusText.text = "Sending: $imageUri"
                    Log.d(WiFiDirectActivity.TAG, "Intent----------- $imageUri")
                }
            } else if (requestCode == CHOOSE_APK_REQUEST_CODE) {
                if (data!!.clipData != null) {
                    val myClipData = data!!.clipData
                    val count = myClipData!!.itemCount
                    for (i in 0 until count) {
                        // adding imageUri in an array
                        imageUri = myClipData!!.getItemAt(i).uri
                        mArrayUri!!.add(imageUri!!)
                        var fileName: String? =
                            PathUtil.getPath(context, myClipData!!.getItemAt(i).uri)
                        filesLength.add(File(fileName).length())
                        fileName = FilesUtil.getFileName(fileName)
                        fileNames.add(fileName)
                        Log.d("File URI", myClipData!!.getItemAt(i).uri.toString())
                        Log.d("File Path", fileName)
                    }
                    val statusText = mContentView!!.findViewById<TextView>(R.id.status_text)
                    statusText.text = getString(R.string.sending_txt) + imageUri
                    Log.d(WiFiDirectActivity.TAG, "Intent----------- $imageUri")
                } else {
                    val imageUri = data!!.data
                    mArrayUri!!.add(imageUri!!)
                    var fileName: String? = PathUtil.getPath(context, imageUri)
                    Log.d("Najeeb", fileName!!)
                    filesLength.add(File(fileName).length())
                    fileName = FilesUtil.getFileName(fileName)
                    fileNames.add(fileName)
                    val statusText = mContentView!!.findViewById<TextView>(R.id.status_text)
                    statusText.text = getString(R.string.sending_txt) + imageUri
                    Log.d(WiFiDirectActivity.TAG, "Intent----------- $imageUri")
                }
            } else {
                // Complimentary Else
                Log.d("Complimentary else", "Check Complimentary Else comment for troubleshooting")
            }
        } else {
            Log.d("ResultCode", "NotOkay")
        }
    }


    override fun onConnectionInfoAvailable(info: WifiP2pInfo) {
        if (progressDialog != null && progressDialog!!.isShowing) {
            progressDialog!!.dismiss()
        }
        this.info = info
        this.view?.visibility = View.VISIBLE

        // The owner IP is now known.
        var view = mContentView!!.findViewById<TextView>(R.id.group_owner)
        view.text =
            resources.getString(R.string.group_owner_text) + if (info!!.isGroupOwner) resources.getString(
                R.string.yes
            ) else resources.getString(R.string.no)

        // InetAddress from WifiP2pInfo struct.
        view = mContentView!!.findViewById(R.id.device_info)
        view.text = "Group Owner IP - " + info!!.groupOwnerAddress.hostAddress
        Log.d("IP of Group Owner:", info!!.groupOwnerAddress.hostAddress)

        // Fetching IP address of group member incase it becomes server. This enables both way file transfer(Owner<->Member).
        if (!becomeClient && !info!!.isGroupOwner) {
            sendServerIPToGroupOwnerClient()
        } else if (becomeClient && info!!.isGroupOwner) {
            getServerIpFromGroupMemberServer()
        }

        // After the group negotiation, we assign the group owner as the file
        // server. The file server is single threaded, single connection server
        // socket.
        if (info!!.groupFormed && !becomeClient) {

            FileServerAsyncTask(activity, mContentView!!.findViewById(R.id.status_text))
                .execute()
        } else if (info!!.groupFormed && becomeClient) {
            // The other device acts as the client. In this case, we enable the
            // get file button.
            becomeClient = false
            mContentView!!.findViewById<View>(R.id.btn_start_client).visibility = View.VISIBLE
            mContentView!!.findViewById<View>(R.id.btn_select_apk).visibility = View.VISIBLE
            mContentView!!.findViewById<View>(R.id.send_files).visibility = View.VISIBLE
            (mContentView!!.findViewById<View>(R.id.status_text) as TextView).setText(
                resources
                    .getString(R.string.client_text)
            )
        }
        // hide the connect button
        mContentView!!.findViewById<View>(R.id.btn_connect).visibility = View.GONE

    }

    /**
     * Updates the UI with device data
     *
     * @param device the device to be displayed
     */
    fun showDetails(device: WifiP2pDevice) {
        this.device = device
        this.view?.visibility = View.VISIBLE
        var view = mContentView!!.findViewById<TextView>(R.id.device_address)
        view.text = device.deviceAddress
        view = mContentView!!.findViewById(R.id.device_info)
        view.text = device.toString()
    }

    /**
     * Clears the UI fields after a disconnect or direct mode disable operation.
     */
    fun resetViews() {

        // mContentView!!.findViewById<View>(R.id.device_details_fragment).visibility = View.GONE
        mContentView!!.findViewById<View>(R.id.btn_connect).visibility = View.VISIBLE
        var view = mContentView!!.findViewById<TextView>(R.id.device_address)
        view.setText(R.string.empty)
        view = mContentView!!.findViewById(R.id.device_info)
        view.setText(R.string.empty)
        view = mContentView!!.findViewById(R.id.group_owner)
        view.setText(R.string.empty)
        view = mContentView!!.findViewById(R.id.status_text)
        view.setText(R.string.empty)
        mContentView!!.findViewById<View>(R.id.btn_start_client).visibility =
            View.GONE
        mContentView!!.findViewById<View>(R.id.btn_select_apk).visibility =
            View.GONE
        mContentView!!.findViewById<View>(R.id.send_files).visibility = View.GONE
        view!!.visibility = View.GONE
    }

    override fun onResume() {
        super.onResume()
        activity.registerReceiver(
            receiver, IntentFilter(
                FileTransferService.NOTIFICATION
            )
        )
        Log.d("Lifecycle", "onResume")
    }

    override fun onPause() {
        super.onPause()
        activity.unregisterReceiver(receiver)
        Log.d("Lifecycle", "onPause")
    }

    /**
     * A simple server socket that accepts connection and writes some data on
     * the stream.
     */


    companion object {

        private const val CHOOSE_FILE_REQUEST_CODE = 20
        private const val CHOOSE_APK_REQUEST_CODE = 30
        private var clientIP: String? = null
        private var becomeClient = false

        class FileServerAsyncTask(
            /**
             * RECEIVER SIDE
             */
            private val context: Context, statusText: View
        ) :
            AsyncTask<Void?, String?, String?>() {

            private val statusText: TextView

            override fun doInBackground(vararg params: Void?): String? {
                var len: Int? = null
                val buf = ByteArray(8192)
                return try {
                    /**
                     * Create a server socket.
                     */
                    val serverSocket = ServerSocket(8988)
                    Log.d(WiFiDirectActivity.TAG, "Server: Socket opened")
                    val client = serverSocket.accept()
                    Log.d(WiFiDirectActivity.TAG, "Server: connection done")
                    val inputstream = client.getInputStream()
                    val objectInputStream = ObjectInputStream(inputstream)
                    var globalFile: File? = null
                    // we get size of item to be received
                    val sizeOfItems = objectInputStream.readInt()

                    // Get filenames and size for now to read the file
                    val fileNames = objectInputStream.readObject() as ArrayList<String>
                    val fileSizes = objectInputStream.readObject() as ArrayList<Long>
                    var fileSize: Long
                    var fileSizeOriginal: Long
                    for (i in 0 until sizeOfItems) {
                        val fileName = fileNames[i]
                        fileSize = fileSizes[i]
                        fileSizeOriginal = fileSizes[i]
                        globalFile = if (fileName.endsWith(".apk")) {
                            //                        Log.d("fileName", fileName);
                            File(
                                context.getExternalFilesDir("received"),
                                "wifip2pshared-" + System.currentTimeMillis()
                                        + ".apk"
                            )
                        } else {
                            File(
                                context.getExternalFilesDir("received"),
                                "wifip2pshared-" + System.currentTimeMillis()
                                        + ".jpg"
                            )
                        }
                        val dirs = File(globalFile.parent)
                        if (!dirs.exists()) dirs.mkdirs()
                        globalFile.createNewFile()
                        Log.d(WiFiDirectActivity.TAG, "server: copying files $globalFile")
                        val outputStream: OutputStream = FileOutputStream(globalFile)
                        while (fileSize > 0 && objectInputStream.read(
                                buf, 0,
                                Math.min(buf.size.toLong(), fileSize).toInt()
                            ).also { len = it } != -1
                        ) {
                            len?.let { outputStream.write(buf, 0, it) }
                            //                        outputStream.flush();
                            fileSize -= len!!.toLong()
                        }
                        // Closing output stream after every iteration and publishing new value to localBroadcast
                        outputStream.close()
                        publishProgress(i.toString())
                    }

                    // close input stream one time from receiver side in the end of file sharing
                    objectInputStream.close()
                    serverSocket.close()
                    globalFile!!.absolutePath
                } catch (e: IOException) {
                    Log.e(WiFiDirectActivity.TAG, e.message!!)
                    null
                } catch (e: ClassNotFoundException) {
                    Log.e(WiFiDirectActivity.TAG, e.message!!)
                    null
                }
            }

            override fun onProgressUpdate(vararg values: String?) {
                super.onProgressUpdate(*values)
                Toast.makeText(
                    context.applicationContext,
                    "File $values shared.",
                    Toast.LENGTH_SHORT
                ).show()
            }

            @SuppressLint("SetTextI18n")
            override fun onPostExecute(result: String?) {
                result?.run {
                    statusText.text = "File copied : $result"
                    val recvFile = File(result)
                    val fileUri = FileProvider.getUriForFile(
                        context,
                        "com.example.filesharingpro.fileprovider",
                        recvFile
                    )
                    val intent = Intent()
                    intent.action = Intent.ACTION_VIEW
                    /** Display one of the images which are shared.
                     *
                     * intent.setDataAndType(fileUri, "image/ *");
                     * intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                     * context.startActivity(intent);
                     */

                    // Kotlin code
                    // val intent = Intent(android.content.Intent.ACTION_VIEW).apply {
                    // setDataAndType(Uri.parse("file://$result"), "image/*")
                    // }
                    // context.startActivity(intent)
                }
            }

            override fun onPreExecute() {
                statusText.setText(R.string.open_socket_server_txt)
            }

            init {
                this.statusText = statusText as TextView
            }
        }
    }

}