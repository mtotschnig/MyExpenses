package org.totschnig.myexpenses.viewmodel

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import org.totschnig.myexpenses.model.Account

private const val KEY_MORE_OPTIONS_SHOWN = "moreOptionsShown"
private const val KEY_ACCOUNT_COLOR = "accountColor"
class OnBoardingViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle
) : ContentResolvingAndroidViewModel(application) {

    var moreOptionsShown: Boolean
        get() = savedStateHandle.get<Boolean>(KEY_MORE_OPTIONS_SHOWN) == true
        set(value) { savedStateHandle[KEY_MORE_OPTIONS_SHOWN] = value }

    var accountColor: Int
        get() = savedStateHandle.get<Int>(KEY_ACCOUNT_COLOR) ?: Account.DEFAULT_COLOR
        set(value) { savedStateHandle[KEY_ACCOUNT_COLOR] = value }
}