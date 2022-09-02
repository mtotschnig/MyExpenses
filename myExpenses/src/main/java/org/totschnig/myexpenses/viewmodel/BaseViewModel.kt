package org.totschnig.myexpenses.viewmodel

import android.app.Application
import android.content.Context
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.ui.ContextHelper
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.util.locale.UserLocaleProvider
import javax.inject.Inject

abstract class BaseViewModel(application: Application) : AndroidViewModel(application) {
    @Inject
    lateinit var coroutineDispatcher: CoroutineDispatcher
    @Inject
    lateinit var userLocaleProvider: UserLocaleProvider

    val localizedContext: Context
        get() = if (::userLocaleProvider.isInitialized) ContextHelper.wrap(getApplication(), userLocaleProvider.getUserPreferredLocale()) else {
            CrashHandler.report(Exception("Missing inject call on viewModel of type ${this::class.java}"))
            getApplication()
        }

    fun getString(@StringRes resId: Int, vararg formatArgs: Any?) =
        localizedContext.getString(resId, *formatArgs)

    fun getQuantityString(@PluralsRes resId: Int, quantity: Int, vararg formatArgs: Any?) =
        localizedContext.resources.getQuantityString(resId, quantity, *formatArgs)

    protected fun coroutineContext() = viewModelScope.coroutineContext + coroutineDispatcher
}