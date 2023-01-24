package org.totschnig.myexpenses.dialog

import android.content.DialogInterface
import android.content.DialogInterface.OnShowListener
import androidx.appcompat.app.AlertDialog

open class ButtonOnShowDisabler : OnShowListener {
    override fun onShow(dialog: DialogInterface) {
        val button = (dialog as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE)
        if (button != null) {
            button.isEnabled = false
        }
    }
}