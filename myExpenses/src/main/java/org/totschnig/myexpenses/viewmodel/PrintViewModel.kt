package org.totschnig.myexpenses.viewmodel

import android.app.Application
import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

open class PrintViewModel(application: Application) : ContentResolvingAndroidViewModel(application) {
    protected val _pdfResult: MutableStateFlow<Result<Pair<Uri, String>>?> = MutableStateFlow(null)
    val pdfResult: StateFlow<Result<Pair<Uri, String>>?> = _pdfResult

    fun pdfResultProcessed() {
        _pdfResult.update {
            null
        }
    }
}