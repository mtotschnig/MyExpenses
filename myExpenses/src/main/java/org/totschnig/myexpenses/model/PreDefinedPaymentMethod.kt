package org.totschnig.myexpenses.model

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.ui.graphics.vector.ImageVector
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R

enum class PreDefinedPaymentMethod(
    val paymentType: Int,
    val isNumbered: Boolean,
    val resId: Int,
    val icon: ImageVector
) {
    CHEQUE(-1, true, R.string.pm_cheque, Icons.Filled.EditNote),
    CREDITCARD(-1, false, R.string.pm_creditcard, Icons.Filled.CreditCard),
    DEPOSIT(1, false, R.string.pm_deposit, Icons.Filled.FileDownload),
    DIRECTDEBIT(-1, false, R.string.pm_directdebit, Icons.Filled.FileUpload);

    fun getLocalizedLabel(context: Context): String = context.getString(resId)
}