package org.totschnig.myexpenses.viewmodel.repository

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.liveData
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.totschnig.myexpenses.BuildConfig
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.retrofit.Issue
import org.totschnig.myexpenses.retrofit.RoadmapService
import org.totschnig.myexpenses.retrofit.Vote
import org.totschnig.myexpenses.util.io.StreamReader
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Suppress("BlockingMethodInNonBlockingContext")
@Singleton
class RoadmapRepository @Inject constructor(private val gson: Gson, private val prefHandler: PrefHandler, private val roadmapService: RoadmapService, private val context: Context) {
    companion object {
        const val ISSUE_CACHE = "issue_cache.json"
        const val ROADMAP_VOTE = "roadmap_vote.json"
        val ROADMAP_URL = if (BuildConfig.DEBUG) "https://votedb-staging.herokuapp.com/" else "https://roadmap.myexpenses.mobi/"
    }

    private val data = MutableLiveData<List<Issue>?>()
    fun getData(): LiveData<List<Issue>?> = data

    fun getLastVote(): LiveData<Vote?> = liveData {
        emit(readLastVoteFromFile())
    }

    fun getDaysPassedSinceLastVote(): LiveData<Long?> = liveData {
        emit(internalFile(ROADMAP_VOTE)?.let {
            TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - withContext(Dispatchers.IO) { it.lastModified() })
        })
    }

    suspend fun loadData(forceRefresh: Boolean) {
        data.postValue(
                (if (!forceRefresh) {
                    readIssuesFromCache()
                } else null) ?: readIssuesFromNetwork())
    }

    private suspend fun internalFile(fileName: String) = withContext(Dispatchers.IO) {
        File(context.filesDir, fileName).takeIf { it.exists() }
    }

    private suspend fun readIssuesFromCache(): List<Issue>? = withContext(Dispatchers.IO) {
        internalFile(ISSUE_CACHE)?.takeIf { TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - it.lastModified()) < 30 }?.let { file ->
            val listType = object : TypeToken<ArrayList<Issue>>() {}.type
            gson.fromJson<ArrayList<Issue>>(readFromFile(file), listType)?.also {
                Timber.i("Loaded %d issues from cache", it.size)
            }
        }
    }

    private suspend fun readIssuesFromNetwork(): List<Issue>? = try {
        val response = roadmapService.issues()
        val version = response.headers()["X-Version"]
        response.body()?.also {
            if (version != null) {
                val versionInt: Int
                try {
                    versionInt = version.toInt()
                    prefHandler.putInt(PrefKey.ROADMAP_VERSION, versionInt)
                } catch (ignored: NumberFormatException) {
                }
            }
            Timber.i("Loaded %d issues (version %s) from network", it.size, version)
            writeToFile(ISSUE_CACHE, gson.toJson(it))
        }
    } catch (e: Exception) {
        Timber.i(e)
        null
    }

    private suspend fun writeToFile(fileName: String, json: String) = withContext(Dispatchers.IO) {
        val fos = context.openFileOutput(fileName, Context.MODE_PRIVATE)
        fos.write(json.toByteArray())
        fos.close()
    }

    private suspend fun readFromFile(file: File) = withContext(Dispatchers.IO) {
        FileInputStream(file).use {
            StreamReader(it).read()
        }
    }

    private suspend fun readLastVoteFromFile(): Vote? = internalFile(ROADMAP_VOTE)?.let {
        gson.fromJson(readFromFile(it), Vote::class.java)
    }

    fun submitVote(vote: Vote): LiveData<Int> = liveData {
        emit(try {
            val voteResponse = roadmapService.createVote(vote)
            when {
                voteResponse.isSuccessful -> {
                    writeToFile(ROADMAP_VOTE, gson.toJson(vote))
                    R.string.roadmap_vote_success
                }
                voteResponse.code() == 452 -> {
                    R.string.roadmap_vote_outdated
                }
                else -> null
            }
        } catch (e: Exception) {
            Timber.i(e)
            null
        } ?: R.string.roadmap_vote_failure)
    }
}