package com.example.filesharingpro.Utils

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File
import java.lang.Exception

class FilesUtil {

    companion object {

        fun openFile(context: Context, file: File?) {
            val uri = FileProvider.getUriForFile(
                context, "com.app.wi_fi_direct",
                file!!
            )
            val intent = Intent(Intent.ACTION_VIEW)
            intent.addCategory(Intent.CATEGORY_BROWSABLE)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            intent.setDataAndType(uri, "*/*")
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            Log.d("Receiver uri", uri.toString())
            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun getFileName(fileName: String?): String {
            val len = fileName!!.length
            var start = len - 1
            val temp = fileName.toCharArray()
            while (true) {
                if (temp[start] == '/') break
                start--
                if (start == -1) break
            }
            return fileName.substring(start + 1)
        }
    }
}