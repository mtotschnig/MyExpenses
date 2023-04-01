package org.totschnig.myexpenses.viewmodel

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.model2.Account

private const val KEY_MORE_OPTIONS_SHOWN = "moreOptionsShown"
private const val KEY_ACCOUNT_COLOR = "accountColor"
class OnBoardingDataViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle
) : ContentResolvingAndroidViewModel(application) {
    private val _accountSaved = MutableLiveData<Result<Unit>>()
    val accountSave: LiveData<Result<Unit>> = _accountSaved

    fun saveAccount(account: Account) {
        viewModelScope.launch(context = coroutineContext()) {
            _accountSaved.postValue(kotlin.runCatching {
                account.createIn(repository)
                Unit
            })
        }
    }

    var moreOptionsShown: Boolean
        get() = savedStateHandle.get<Boolean>(KEY_MORE_OPTIONS_SHOWN) == true
        set(value) { savedStateHandle[KEY_MORE_OPTIONS_SHOWN] = value }

    var accountColor: Int
        get() = savedStateHandle.get<Int>(KEY_ACCOUNT_COLOR) ?: Account.DEFAULT_COLOR
        set(value) { savedStateHandle[KEY_ACCOUNT_COLOR] = value }
}