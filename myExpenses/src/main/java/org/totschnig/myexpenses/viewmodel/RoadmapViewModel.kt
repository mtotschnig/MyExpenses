package org.totschnig.myexpenses.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.retrofit.Issue
import org.totschnig.myexpenses.retrofit.Vote
import org.totschnig.myexpenses.viewmodel.repository.RoadmapRepository
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject

//TODO use mock network for test
class RoadmapViewModel(application: Application) : AndroidViewModel(application) {
    @Inject
    lateinit var roadmapRepository: RoadmapRepository
    @Inject
    lateinit var prefHandler: PrefHandler

    private val gson: Gson

    fun getData(): LiveData<List<Issue>?> = roadmapRepository.getData()

    fun getLastVote(): LiveData<Vote?> = roadmapRepository.getLastVote()

    fun getShouldShowVoteReminder(): LiveData<Boolean> = roadmapRepository.getDaysPassedSinceLastVote().map {
        it?.compareTo(VOTE_REMINDER_DAYS) == 1 && TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - prefHandler.getLong(PrefKey.VOTE_REMINDER_LAST_CHECK, 0)) > VOTE_REMINDER_DAYS
    }

    fun loadData(forceRefresh: Boolean) {
        viewModelScope.launch {
            roadmapRepository.loadData(forceRefresh)
        }
    }

    fun submitVote(vote: Vote): LiveData<Int> = roadmapRepository.submitVote(vote)

    fun cacheWeights(voteWeights: Map<Int, Int>) {
        prefHandler.putString(PrefKey.ROADMAP_VOTE, gson.toJson(voteWeights))
    }

    fun restoreWeights(): MutableMap<Int, Int> {
        val stored = prefHandler.getString(PrefKey.ROADMAP_VOTE, null)
        return if (stored != null) gson.fromJson(stored, object : TypeToken<Map<Int?, Int?>?>() {}.type) else HashMap()
    }


    companion object {
        const val EXPECTED_MINIMAL_VERSION = 2
        const val VOTE_REMINDER_DAYS = 100L
    }

    init {
        (application as MyApplication).appComponent.inject(this)
        gson = Gson()
    }
}