package com.ecjtu.sharebox.network

import android.text.TextUtils
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.lang.Exception
import java.net.HttpURLConnection
import java.net.URL

/**
 * Created by Ethan_Xiang on 2017/7/14.
 */
abstract class BaseNetwork {
    companion object {
        const val TIME_OUT = 5 * 1000
        const val CHARSET = "UTF-8"
        const val HEADER_CONTENT_ENCODING = "Content-Encoding"
        const val HEADER_CONTENT_LENGTH = "Content-Length"
        const val HTTP_PREFIX = "http://"
        const val CACHE_SIZE = 5 * 1024
    }

    object Method {
        const val OPTIONS = "OPTIONS"
        const val GET = "GET"
        const val HEAD = "HEAD"
        const val POST = "POST"
        const val PUT = "PUT"
        const val DELETE = "DELETE"
        const val TRACE = "TRACE"
    }

    private var mCallback: IRequestCallback? = null

    private var mHttpUrlConnection: HttpURLConnection? = null

    private var mInputStream: InputStream? = null

    private var mOutputStream: OutputStream? = null

    fun setRequestCallback(callback: IRequestCallback) {
        mCallback = callback
    }

    @JvmOverloads
    open fun request(urlStr: String, mutableMap: MutableMap<String, String>? = null) {
        var ex: Exception? = null

        var ret = ""
        try {
            var url = URL(urlStr)
            mHttpUrlConnection = url.openConnection() as HttpURLConnection
            setupRequest(mHttpUrlConnection!!)
            var paramStr = setParams(mHttpUrlConnection!!, mutableMap)
            connect()
            pushContent(mHttpUrlConnection!!, paramStr)
            ret = getContent(mHttpUrlConnection!!)
        } catch (e: Exception) {
            e.printStackTrace()
            ex = e
        } finally {
            if (ex != null) {
                mCallback?.onError(mHttpUrlConnection, ex)
            } else {
                mCallback?.onSuccess(mHttpUrlConnection, ret)
            }
            mHttpUrlConnection?.disconnect()
        }
    }

    open fun setupRequest(httpURLConnection: HttpURLConnection) {
        httpURLConnection.apply {
            doInput = true
            doOutput = true
            requestMethod = Method.GET
            connectTimeout = TIME_OUT
            readTimeout = TIME_OUT
            setRequestProperty("Content-Type", "*/*")
            setRequestProperty(HEADER_CONTENT_ENCODING, CHARSET)
        }
    }

    open fun setParams(httpURLConnection: HttpURLConnection, mutableMap: MutableMap<String, String>? = null): String {
        var ret = ""
        mutableMap?.let {
            httpURLConnection.requestMethod = Method.POST

            var param: String = ""

            for (obj in mutableMap.entries) {
                if (!TextUtils.isEmpty(param)) {
                    param += "&"
                }
                param += "${obj.key}=${obj.value}"
            }
            httpURLConnection.setRequestProperty(HEADER_CONTENT_LENGTH, param.toByteArray().size.toString())
            ret = param
        }
        return ret
    }

    open fun connect() {
        try {
            mHttpUrlConnection?.connect()
        } catch (io: IOException) {
            throw io
        }
    }

    open fun getContent(httpURLConnection: HttpURLConnection): String {
        var ret = ""
        try {
            if (httpURLConnection.responseCode == HttpURLConnection.HTTP_OK) {
                var os = ByteArrayOutputStream()
                var temp = ByteArray(CACHE_SIZE, { index -> 0 })
                var `is` = httpURLConnection.inputStream
                mInputStream = `is`
                var len: Int
                len = `is`.read(temp)
                while (len > 0) {
                    os.write(temp, 0, len)
                    len = `is`.read(temp)
                }
                ret = String(os.toByteArray())
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
            throw ex
        }
        return ret
    }

    open fun cancel() {
        try {
            mOutputStream?.close()
            mInputStream?.close()
        } catch (e: Exception) {
            throw e
        } finally {
            mHttpUrlConnection?.disconnect()
        }
    }

    fun pushContent(httpURLConnection: HttpURLConnection, param: String) {
        if (httpURLConnection.requestMethod == Method.POST) {
            if (!TextUtils.isEmpty(param)) {
                mOutputStream = httpURLConnection.outputStream
                mOutputStream?.write(param.toByteArray())
                mOutputStream?.flush()
            }
        }
    }
}