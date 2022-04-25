package org.totschnig.myexpenses.viewmodel

import android.app.Application
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import org.totschnig.myexpenses.MyApplication
import javax.inject.Inject

abstract class BaseViewModel(application: Application) : AndroidViewModel(application) {
    @Inject
    lateinit var coroutineDispatcher: CoroutineDispatcher
    fun getString(@StringRes resId: Int, vararg formatArgs: Any?) =
        getApplication<MyApplication>().getString(resId, *formatArgs)

    fun getQuantityString(@PluralsRes resId: Int, quantity: Int, vararg formatArgs: Any?) =
        getApplication<MyApplication>().resources.getQuantityString(resId, quantity, *formatArgs)

    protected fun coroutineContext() = viewModelScope.coroutineContext + coroutineDispatcher
}