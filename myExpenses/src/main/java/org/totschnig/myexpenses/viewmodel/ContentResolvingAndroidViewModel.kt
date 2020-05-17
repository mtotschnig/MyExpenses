package org.totschnig.myexpenses.viewmodel

import android.app.Application
import android.content.ContentResolver
import androidx.lifecycle.AndroidViewModel
import com.squareup.sqlbrite3.BriteContentResolver
import io.reactivex.disposables.Disposable
import org.totschnig.myexpenses.MyApplication
import javax.inject.Inject

abstract class ContentResolvingAndroidViewModel(application: Application) : AndroidViewModel(application) {
    @Inject
    lateinit var briteContentResolver: BriteContentResolver
    var disposable: Disposable? = null

    val contentResolver: ContentResolver
        get() = getApplication<MyApplication>().contentResolver

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

    companion object {
        fun <K, V> lazyMap(initializer: (K) -> V): Map<K, V> {
            val map = mutableMapOf<K, V>()
            return map.withDefault { key ->
                val newValue = initializer(key)
                map[key] = newValue
                return@withDefault newValue
            }
        }
    }
}