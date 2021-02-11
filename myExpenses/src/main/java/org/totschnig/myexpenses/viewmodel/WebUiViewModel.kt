package org.totschnig.myexpenses.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.feature.WebUiFeature

class WebUiViewModel(application: Application) : AndroidViewModel(application) {
    private val webUiFeature: WebUiFeature
        get() = getApplication<MyApplication>().appComponent.webuiFeature() ?: object : WebUiFeature {}

    val isBoundAndRunning: Boolean
        get() = webUiFeature.isBoundAndRunning

    fun bind(context: Context) {
        webUiFeature.bind(context)
    }

    fun unbind(context: Context) {
        webUiFeature.unbind(context)
    }

    fun toggle(context: Context) {
        webUiFeature.toggle(context)
    }
}