package org.totschnig.myexpenses.util.ui

import android.content.DialogInterface
import androidx.appcompat.app.AlertDialog

fun AlertDialog.withOkClick(onOkClick: () -> Unit): AlertDialog {
    setOnShowListener { dialog: DialogInterface ->
        val button = (dialog as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE)
        button.setOnClickListener { onOkClick() }
    }
    return this
}
