package org.totschnig.myexpenses.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.squareup.sqlbrite3.BriteContentResolver
import io.reactivex.disposables.Disposable
import org.totschnig.myexpenses.MyApplication
import javax.inject.Inject

abstract class ContentResolvingAndroidViewModel(application: Application) : AndroidViewModel(application) {
    @Inject
    lateinit var briteContentResolver: BriteContentResolver
    var disposable: Disposable? = null
    init {
        (application as MyApplication).appComponent.inject(this)
    }
    override fun onCleared() {
        dispose()
    }

    fun dispose() {
        disposable?.let {
            if (!it.isDisposed) it.dispose()
        }
    }
}