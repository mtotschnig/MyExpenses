package org.totschnig.myexpenses.viewmodel.repository

import android.content.Context
import android.content.pm.PackageManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.liveData
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.acra.util.StreamReader
import org.totschnig.myexpenses.BuildConfig
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.retrofit.Issue
import org.totschnig.myexpenses.retrofit.RoadmapService
import org.totschnig.myexpenses.retrofit.Vote
import org.totschnig.myexpenses.util.ResultUnit
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import timber.log.Timber
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.lang.System.currentTimeMillis
import java.security.GeneralSecurityException
import java.security.MessageDigest
import java.security.cert.CertificateFactory
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoadmapRepository @Inject constructor(
    private val gson: Gson,
    private val prefHandler: PrefHandler,
    private val roadmapService: RoadmapService,
    private val context: Context
) {
    companion object {
        const val VERSION = 4
        const val ISSUE_CACHE = "issue_cache.json"
        const val ISSUE_CACHE_LIFE_TIME_DAYS = 30
        const val ROADMAP_VOTE = "roadmap_vote_${VERSION}.json"
        private val isSandbox = BuildConfig.DEBUG
        val ROADMAP_URL = when {
            isSandbox -> "http://10.0.2.2:3000/"
            else -> "https://roadmap.myexpenses.mobi/"
        }
    }

    private val data = MutableLiveData<List<Issue>?>()
    fun getData(): LiveData<List<Issue>?> = data

    fun getLastVote(): LiveData<Vote?> = liveData {
        emit(readLastVoteFromFile())
    }

    fun getDaysPassedSinceLastVote(): LiveData<Long?> = liveData {
        emit(internalFile(ROADMAP_VOTE)?.let {
            TimeUnit.MILLISECONDS.toDays(
                currentTimeMillis() - withContext(Dispatchers.IO) { it.lastModified() }
            )
        })
    }

    suspend fun loadData(forceRefresh: Boolean) {
        data.postValue(
            (if (!forceRefresh) {
                readIssuesFromCache()
            } else null) ?: readIssuesFromNetwork()
        )
    }

    private suspend fun internalFile(fileName: String) = withContext(Dispatchers.IO) {
        File(context.filesDir, fileName).takeIf { it.exists() }
    }

    private suspend fun readIssuesFromCache(): List<Issue>? = withContext(Dispatchers.IO) {
        internalFile(ISSUE_CACHE)
            ?.takeIf {
                TimeUnit.MILLISECONDS.toDays(
                    currentTimeMillis() - it.lastModified()
                ) < ISSUE_CACHE_LIFE_TIME_DAYS
            }
            ?.let { file ->
                val listType = object : TypeToken<ArrayList<Issue>>() {}.type
                try {
                    gson.fromJson<ArrayList<Issue>>(readFromFile(file), listType)
                } catch (e: Exception) {
                    CrashHandler.report(e)
                    null
                }?.also {
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
    } catch (ex: CancellationException) {
        throw ex // Must let the CancellationException propagate
    } catch (e: Exception) {
        CrashHandler.report(e)
        null
    }

    private suspend fun writeToFile(fileName: String, json: String) = withContext(Dispatchers.IO) {
        context.openFileOutput(fileName, Context.MODE_PRIVATE).use {
            it.write(json.toByteArray())
        }
    }

    private suspend fun readFromFile(file: File) = withContext(Dispatchers.IO) {
        FileInputStream(file).use {
            StreamReader(it).read()
        }
    }

    private suspend fun readLastVoteFromFile(): Vote? = internalFile(ROADMAP_VOTE)?.let {
        gson.fromJson(readFromFile(it), Vote::class.java)
    }

    fun submitVote(vote: Vote): LiveData<Result<Unit>> = liveData {
        emit(
            runCatching {
                val voteResponse = roadmapService.createVote(getFingerprint(), vote)
                if (voteResponse.isSuccessful) {
                    writeToFile(ROADMAP_VOTE, gson.toJson(vote))
                } else {
                    val code = voteResponse.code()
                    throw Exception(
                        when (code) {
                            452 -> context.getString(R.string.roadmap_vote_outdated)
                            453 -> context.getString(R.string.roadmap_vote_validation_failure)
                            else -> "Received unexpected response: $code"
                        }
                    )
                }
            }
        )
    }

    private fun getFingerprint() = try {
        val ce = context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.GET_SIGNATURES
        ).signatures?.first()
        buildString {
            MessageDigest.getInstance("SHA-1")
                .digest(
                    CertificateFactory.getInstance("X509")
                        .generateCertificate(ByteArrayInputStream(ce?.toByteArray()))
                        .encoded
                ).forEach {
                    val appendString = Integer.toHexString(0xFF and it.toInt())
                    if (appendString.length == 1) append("0")
                    append(appendString)
                }
        }
    } catch (e: GeneralSecurityException) {
        CrashHandler.report(e)
        throw e
    }
}