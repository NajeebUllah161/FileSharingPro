package com.example.filesharingpro.service

//import android.app.IntentService
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.Parcelable
import android.util.Log
import android.widget.Toast
import androidx.core.app.JobIntentService
import com.example.filesharingpro.activities.WiFiDirectActivity
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.io.ObjectOutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.util.ArrayList

/**
 * A service that process each file transfer request i.e Intent by opening a
 * socket connection with the WiFi Direct Group Owner and writing the file
 */
class FileTransferService : JobIntentService() {

    companion object {
        // Change socket timeout to 500 to detect any change
        private const val SOCKET_TIMEOUT = 5000
        const val ACTION_SEND_FILE = "com.example.filesharingpro.SEND_FILE"
        const val EXTRAS_FILE_PATH = "file_url"
        const val EXTRAS_FILE_NAME = "file_name"
        const val EXTRAS_FILE_LENGTH = "file_length"
        const val EXTRAS_GROUP_OWNER_ADDRESS = "go_host"
        const val EXTRAS_GROUP_OWNER_PORT = "go_port"
        const val RESULT = "result"
        const val NOTIFICATION = "com.example.filesharingpro"

        private const val JOB_ID = 1
        fun enqueueWork(context: Context?, intent: Intent?) {
            enqueueWork(context!!, FileTransferService::class.java, JOB_ID, intent!!)
        }

    }

    /**
     * We extract Uri, get stream from socketServer and write it to file
     **/
    override fun onHandleWork(intent: Intent) {
        /** SENDER SIDE */
        val context = applicationContext
        if (intent!!.action == ACTION_SEND_FILE) {
            val fileUri =
                intent.extras!!.getParcelableArrayList<Parcelable>(EXTRAS_FILE_PATH)
            val filesLength =
                intent.getSerializableExtra(EXTRAS_FILE_LENGTH) as ArrayList<Long>?
            val fileNames = intent.getStringArrayListExtra(EXTRAS_FILE_NAME)
            var len: Int
            // Reduce buffer size to check change in speed.
            val buf = ByteArray(8192)

            Log.d("NajeebFileTransferService", fileUri.toString());
            Log.d("NajeebFileTransferService", fileUri?.get(0).toString())

            val host = intent.extras!!.getString(EXTRAS_GROUP_OWNER_ADDRESS)
            val socket = Socket()
            val port = intent.extras!!.getInt(EXTRAS_GROUP_OWNER_PORT)
            try {
                Log.d(WiFiDirectActivity.TAG, "Opening client socket - ")
                socket.bind(null)
                socket.connect(InetSocketAddress(host, port), SOCKET_TIMEOUT)
                Log.d(WiFiDirectActivity.TAG, "Client socket - " + socket.isConnected)
                val outputStream = socket.getOutputStream()
                val objectOutputStream = ObjectOutputStream(outputStream)
                val cr = context.contentResolver
                objectOutputStream.writeInt(fileUri!!.size)
                objectOutputStream.writeObject(fileNames)
                // objectOutputStream.flush();
                objectOutputStream.writeObject(filesLength)
                // objectOutputStream.flush();
                var inputStream: InputStream? = null
                var i = 1
                for (singleUri in fileUri) {
                    Log.d("NajeebFileTransferService", singleUri.toString())
                    try {
                        inputStream = cr.openInputStream(Uri.parse(singleUri.toString()))
                        while (inputStream!!.read(buf).also { len = it } != -1) {
                            objectOutputStream.write(buf, 0, len)
                        }
                        inputStream.close()
                    } catch (e: FileNotFoundException) {
                        Log.d(WiFiDirectActivity.TAG, e.toString())
                    }
                    publishResults(i)
                    // Log.d("FileProgressSender", String.valueOf(i));
                    i++
                    // DeviceDetailFragment.copyFile(inputStream, outputStream);
                }
                // close output stream one time from sender side
                objectOutputStream.flush()
                outputStream.close()
                Log.d(WiFiDirectActivity.TAG, "Client: Data written")
            } catch (e: IOException) {
                Log.e(WiFiDirectActivity.TAG, e.message!!)
            } finally {
                /**
                 * Clean up any open sockets when done
                 * transferring or if an exception occurred.
                 */
                socket.takeIf { it.isConnected }?.apply {
                    close()
                }
            }
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        toast("All work complete")
    }

    private val mHandler: Handler = Handler(Looper.getMainLooper())

    // Helper for showing tests
    private fun toast(text: CharSequence?) {
        mHandler.post(Runnable {
            Toast.makeText(
                this@FileTransferService,
                text,
                Toast.LENGTH_SHORT
            ).show()
        })
    }

    private fun publishResults(result: Int) {
        val intent = Intent(NOTIFICATION)
        intent.putExtra(RESULT, result)
        sendBroadcast(intent)
    }

}