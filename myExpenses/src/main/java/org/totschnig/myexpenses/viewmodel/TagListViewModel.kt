package org.totschnig.myexpenses.viewmodel

import android.app.Application
import android.database.Cursor
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.apache.commons.collections4.ListUtils
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.viewmodel.data.Tag
import java.util.*

class TagListViewModel(application: Application) : ContentResolvingAndroidViewModel(application) {
    private val tags = MutableLiveData<MutableList<Tag>>()

    fun loadTags(selected: ArrayList<Tag>?): LiveData<MutableList<Tag>> {
        if (tags.value == null) {
            disposable = briteContentResolver.createQuery(TransactionProvider.TAGS_URI, null, null, null, null, false)
                    .mapToList { cursor ->
                        val id = cursor.getLong(cursor.getColumnIndex(KEY_ROWID))
                        val label = cursor.getString(cursor.getColumnIndex(KEY_LABEL))
                        Tag(id, label, selected?.find { tag -> tag.label.equals(label) } != null)
                    }
                    .subscribe { list -> tags.postValue(selected?.let { ListUtils.union(it.filter { tag -> tag.id == -1L }, list) } ?: list) }
        }
        return tags
    }
}