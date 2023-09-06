package org.totschnig.myexpenses.viewmodel

import android.app.Application
import android.net.Uri
import org.totschnig.myexpenses.dialog.DialogUtils
import org.totschnig.myexpenses.util.io.FileUtils

class QifImportViewModel(application: Application) : ImportDataViewModel(application) {

    val fileUri: Uri
        get() = TODO()

    override val defaultAccountName: String
        get()  {
            var displayName = DialogUtils.getDisplayName(fileUri)
            if (FileUtils.getExtension(displayName).equals("qif", ignoreCase = true)) {
                displayName = displayName.substring(0, displayName.lastIndexOf('.'))
            }
            return displayName.replace('-', ' ').replace('_', ' ')
        }
}