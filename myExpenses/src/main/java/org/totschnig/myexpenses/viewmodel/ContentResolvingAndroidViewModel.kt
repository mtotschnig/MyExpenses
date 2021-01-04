package org.totschnig.myexpenses.viewmodel

import android.app.Application
import android.content.ContentResolver
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.squareup.sqlbrite3.BriteContentResolver
import io.reactivex.disposables.Disposable
import kotlinx.coroutines.CoroutineDispatcher
import org.totschnig.myexpenses.MyApplication
import javax.inject.Inject

abstract class ContentResolvingAndroidViewModel(application: Application) : AndroidViewModel(application) {
    @Inject
    lateinit var briteContentResolver: BriteContentResolver
    @Inject
    lateinit var coroutineDispatcher: CoroutineDispatcher

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

    protected fun coroutineContext() = viewModelScope.coroutineContext + coroutineDispatcher

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