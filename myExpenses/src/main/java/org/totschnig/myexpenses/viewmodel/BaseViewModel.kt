package org.totschnig.myexpenses.viewmodel

import android.app.Application
import android.content.Context
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import org.totschnig.myexpenses.MyApplication
import java.util.*
import javax.inject.Inject

abstract class BaseViewModel(application: Application) : AndroidViewModel(application) {
    @Inject
    lateinit var coroutineDispatcher: CoroutineDispatcher

    val localizedContext: Context by lazy {
        getApplication<MyApplication>().wrappedContext
    }

    val userPreferredLocale: Locale
        get() = getApplication<MyApplication>().userPreferredLocale

    fun getString(@StringRes resId: Int, vararg formatArgs: Any?) =
        localizedContext.getString(resId, *formatArgs)

    fun getQuantityString(@PluralsRes resId: Int, quantity: Int, vararg formatArgs: Any?) =
        localizedContext.resources.getQuantityString(resId, quantity, *formatArgs)

    protected fun coroutineContext() = viewModelScope.coroutineContext + coroutineDispatcher
}