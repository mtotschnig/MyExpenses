package org.totschnig.myexpenses.util.io

import android.app.Service
import android.content.ContentResolver
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities.TRANSPORT_CELLULAR
import android.net.NetworkCapabilities.TRANSPORT_VPN
import android.net.NetworkCapabilities.TRANSPORT_WIFI
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.OpenableColumns
import android.text.format.Formatter
import android.webkit.MimeTypeMap
import androidx.annotation.IntRange
import androidx.annotation.RequiresApi
import dagger.internal.Preconditions
import timber.log.Timber
import java.io.File
import java.net.Inet4Address


const val MIME_TYPE_OCTET_STREAM = "application/octet-stream"

fun isConnectedWifi(context: Context) = getConnectionType(context) == 2

@IntRange(from = 0, to = 3)
fun getConnectionType(context: Context) =
    // Returns connection type. 0: none; 1: mobile data; 2: wifi; 3: vpn
    (context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager)?.let {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            it.getConnectionType23()
        else
            it.getConnectionTypeLegacy()
    } ?: 0

@RequiresApi(Build.VERSION_CODES.M)
private fun ConnectivityManager.getConnectionType23(): Int =
    getNetworkCapabilities(activeNetwork)?.run {
        when {
            hasTransport(TRANSPORT_WIFI) -> 2
            hasTransport(TRANSPORT_CELLULAR) -> 1
            hasTransport(TRANSPORT_VPN) -> 3
            else -> 0
        }
    } ?: 0

@Suppress("DEPRECATION")
private fun ConnectivityManager.getConnectionTypeLegacy() =
    when (activeNetworkInfo?.type) {
        ConnectivityManager.TYPE_WIFI -> 2
        ConnectivityManager.TYPE_MOBILE -> 1
        ConnectivityManager.TYPE_VPN -> 3
        else -> 0
    }

fun getActiveIpAddress(context: Context): String? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        getActiveAddress23(context)
    else
        getWifiIpAddressLegacy(context)

@RequiresApi(Build.VERSION_CODES.M)
private fun getActiveAddress23(context: Context) =
    (context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager)?.let { cm ->
        cm.activeNetwork
            ?.let { cm.getLinkProperties(it) }
            ?.linkAddresses
            ?.find { it.address is Inet4Address }
            ?.address
            ?.hostAddress
    }

@Suppress("DEPRECATION")
private fun getWifiIpAddressLegacy(context: Context) =
    (context.applicationContext.getSystemService(Service.WIFI_SERVICE) as WifiManager)
        .connectionInfo
        .ipAddress
        .takeIf { it != 0 }
        ?.let { Formatter.formatIpAddress(it) }

fun calculateSize(contentResolver: ContentResolver, uri: Uri): Long {
    val size: Long
    Timber.d("Uri %s", uri)
    if ("file" == uri.scheme) {
        size = uri.path?.let { File(it) }?.length() ?: 0
    } else {
        contentResolver.query(uri, null, null, null, null).use { c ->
            size = if (c != null) {
                c.moveToFirst()
                c.getLong(c.getColumnIndexOrThrow(OpenableColumns.SIZE))
            } else {
                -1
            }
        }
    }
    Timber.d("Size %d", size)
    return size
}

fun getMimeType(fileName: String): String {
    val result = MimeTypeMap.getSingleton().getMimeTypeFromExtension(getFileExtension(fileName))
    return result ?: MIME_TYPE_OCTET_STREAM
}

//from Guava
fun getNameWithoutExtension(file: String): String {
    Preconditions.checkNotNull(file)
    val fileName = File(file).name
    val dotIndex = fileName.lastIndexOf('.')
    return if (dotIndex == -1) fileName else fileName.substring(0, dotIndex)
}

//from Guava
fun getFileExtension(fullName: String): String {
    Preconditions.checkNotNull(fullName)
    val fileName = File(fullName).name
    val dotIndex = fileName.lastIndexOf('.')
    return if (dotIndex == -1) "" else fileName.substring(dotIndex + 1)
}
