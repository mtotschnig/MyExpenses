package org.totschnig.myexpenses.exception

import android.net.Uri
import java.io.IOException

class ExternalStorageNotAvailableException : IllegalStateException()

class UnknownPictureSaveException(var pictureUri: Uri, var homeUri: Uri, e: IOException) : java.lang.IllegalStateException(e)