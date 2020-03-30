package org.totschnig.myexpenses.viewmodel

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.totschnig.myexpenses.model.Account
import org.totschnig.myexpenses.viewmodel.data.Tag
import java.util.*
import java.util.stream.Collectors

class TagListViewModel(application: Application) : ContentResolvingAndroidViewModel(application) {
    private val accountLiveData: Map<Long, LiveData<List<Tag>>> = lazyMap { transactionId ->
        val liveData = MutableLiveData<List<Tag>>()
        val tagList = mutableListOf<Tag>()
        val charPool: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')
        val current = Random()
        repeat(100) {
            val randomString = current
                    .ints(current.nextInt(10).toLong()+1, 0, charPool.size)
                    .mapToObj { index -> charPool[index].toString() }
                    .collect(Collectors.joining())
            tagList.add(Tag(randomString, false))
        }
        liveData.postValue(tagList)
        return@lazyMap liveData
    }
    fun loadTags(transactionId: Long): LiveData<List<Tag>> = accountLiveData.getValue(transactionId)
}