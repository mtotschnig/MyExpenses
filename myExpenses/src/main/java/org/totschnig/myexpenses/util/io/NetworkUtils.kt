package org.totschnig.myexpenses.util.io

import android.content.Context
import android.net.ConnectivityManager
import androidx.core.content.ContextCompat

fun isNetworkConnected(context: Context) =
        ContextCompat.getSystemService(context, ConnectivityManager::class.java)?.activeNetworkInfo?.isConnected == true