package org.totschnig.myexpenses.viewmodel

import android.app.Application
import android.content.ContentUris
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.annotations.NotNull
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.feature.Callback
import org.totschnig.myexpenses.feature.FeatureManager
import org.totschnig.myexpenses.feature.OcrFeatureProvider
import org.totschnig.myexpenses.model.Account
import org.totschnig.myexpenses.model.AggregateAccount
import org.totschnig.myexpenses.model.Grouping
import org.totschnig.myexpenses.model.SortDirection
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_HIDDEN
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.util.PictureDirHelper
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import java.io.File
import javax.inject.Inject

class MyExpensesViewModel(application: Application) : ContentResolvingAndroidViewModel(application) {

    init {
        (application as MyApplication).appComponent.inject(this)
        featureManager.registerCallback(object : Callback {
            override fun onAvailable() {
                featureState.postValue(Pair(FeatureState.AVAILABLE, null))
            }

            override fun onAsyncStarted(feature: FeatureManager.Feature) {
                featureState.postValue(Pair(FeatureState.LOADING, null))
            }

            override fun onError(throwable: Throwable) {
                featureState.postValue(Pair(FeatureState.ERROR, throwable.message))
            }

        })
    }

    @Inject
    lateinit var featureManager: FeatureManager

    @Inject
    lateinit var prefHandler: PrefHandler

    enum class FeatureState {
        LOADING, AVAILABLE, ERROR;
    }

    private val featureState = MutableLiveData<Pair<FeatureState, String?>>()

    private val hasHiddenAccounts = MutableLiveData<Boolean>()

    fun getHasHiddenAccounts(): LiveData<Boolean> {
        return hasHiddenAccounts
    }

    fun getFeatureState(): LiveData<Pair<FeatureState, String?>> {
        return featureState
    }

    fun loadHiddenAccountCount() {
        disposable = briteContentResolver.createQuery(TransactionProvider.ACCOUNTS_URI,
                arrayOf("count(*)"), "$KEY_HIDDEN = 1", null, null, false)
                .mapToOne { cursor -> cursor.getInt(0) > 0 }
                .subscribe { hasHiddenAccounts.postValue(it) }
    }

    fun persistGrouping(accountId: Long, grouping: Grouping) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                if (accountId == Account.HOME_AGGREGATE_ID) {
                    AggregateAccount.persistGroupingHomeAggregate(prefHandler, grouping)
                    contentResolver.notifyChange(TransactionProvider.ACCOUNTS_URI, null, false)
                } else {
                    contentResolver.update(ContentUris.withAppendedId(TransactionProvider.ACCOUNT_GROUPINGS_URI, accountId).buildUpon()
                            .appendPath(grouping.name).build(),
                            null, null, null)
                }
            }
        }
    }

    fun persistSortDirection(accountId: Long, sortDirection: SortDirection) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                contentResolver.update(ContentUris.withAppendedId(Account.CONTENT_URI, accountId).buildUpon().appendPath("sortDirection").appendPath(sortDirection.name).build(),
                        null, null, null);
            }
        }
    }

    fun persistSortDirectionAggregate(currency: String, sortDirection: SortDirection) {
        AggregateAccount.persistSortDirectionAggregate(prefHandler, currency, sortDirection);
        contentResolver.notifyChange(TransactionProvider.ACCOUNTS_URI, null, false)
    }

    fun persistSortDirectionHomeAggregate(sortDirection: SortDirection) {
        AggregateAccount.persistSortDirectionHomeAggregate(prefHandler, sortDirection)
        contentResolver.notifyChange(TransactionProvider.ACCOUNTS_URI, null, false)
    }

    fun startOcrFeature(scanFile: @NotNull File, fragmentActivity: FragmentActivity) {
        try {
            val ocrProvider = Class.forName("org.totschnig.ocr.OcrFeatureProviderImpl").kotlin.objectInstance as OcrFeatureProvider
            ocrProvider.start(scanFile, fragmentActivity)
        } catch (e: ClassNotFoundException) {
            CrashHandler.report(e)
        }
    }

    fun isOcrAvailable() = featureManager.isFeatureInstalled(FeatureManager.Feature.OCR)

    fun requestOcrFeature() = featureManager.requestFeature(FeatureManager.Feature.OCR)

    fun getScanFile(action: (file: File) -> Unit) {
        viewModelScope.launch {
            action(withContext(Dispatchers.IO) {
                PictureDirHelper.getOutputMediaFile("SCAN", true, false)
            })
        }
    }
}
