package org.totschnig.myexpenses.util.distrib

import android.content.Context
import androidx.fragment.app.FragmentActivity

interface ReviewManager {
    fun init(context: Context) {}
    fun onEditTransactionResult(activity: FragmentActivity)
}