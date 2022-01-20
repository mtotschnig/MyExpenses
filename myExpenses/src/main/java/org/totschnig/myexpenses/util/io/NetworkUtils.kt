package org.totschnig.myexpenses.util.io

import android.app.Service
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import android.text.format.Formatter
import androidx.annotation.IntRange
import androidx.annotation.RequiresApi
import java.net.Inet4Address

fun isConnectedWifi(context: Context) = getConnectionType(context) == 2

@IntRange(from = 0, to = 3)
fun getConnectionType(context: Context) =
    // Returns connection type. 0: none; 1: mobile data; 2: wifi; 3: vpn
    (context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager)?.let {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            getConnectionType23(it)
        else
            getConnectionTypeLegacy(it)
    } ?: 0

@RequiresApi(Build.VERSION_CODES.M)
private fun getConnectionType23(cm: ConnectivityManager): Int =
    cm.getNetworkCapabilities(cm.activeNetwork)?.run {
        when {
            hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> 2
            hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> 1
            hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> 3
            else -> 0
        }
    } ?: 0

@Suppress("DEPRECATION")
private fun getConnectionTypeLegacy(cm: ConnectivityManager) =
    when (cm.activeNetworkInfo?.type) {
        ConnectivityManager.TYPE_WIFI -> 2
        ConnectivityManager.TYPE_MOBILE -> 1
        ConnectivityManager.TYPE_VPN -> 3
        else -> 0
    }

fun getWifiIpAddress(context: Context): String =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        getWifiIpAddress23(context)
    else
        getWifiIpAddressLegacy(context)

@RequiresApi(Build.VERSION_CODES.M)
private fun getWifiIpAddress23(context: Context) =
    (context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager)?.let { cm ->
        cm.getLinkProperties(cm.activeNetwork)?.linkAddresses?.find { it.address is Inet4Address }?.address?.hostAddress
    } ?: ""

@Suppress("DEPRECATION")
private fun getWifiIpAddressLegacy(context: Context) =
    (context.applicationContext.getSystemService(Service.WIFI_SERVICE) as WifiManager).connectionInfo.ipAddress.let {
        Formatter.formatIpAddress(
            it
        )
    }