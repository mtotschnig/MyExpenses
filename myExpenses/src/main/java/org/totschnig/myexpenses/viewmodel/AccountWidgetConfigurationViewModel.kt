package org.totschnig.myexpenses.viewmodel

import android.app.Application
import androidx.lifecycle.LiveData
import org.totschnig.myexpenses.viewmodel.data.AccountMinimal

class AccountWidgetConfigurationViewModel(application: Application): ContentResolvingAndroidViewModel(application) {
    val accounts: LiveData<List<AccountMinimal>> by lazy {
        return@lazy accountsMinimal(withHidden = false)
    }
}
