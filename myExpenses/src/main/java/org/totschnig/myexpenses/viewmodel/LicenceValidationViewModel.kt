package org.totschnig.myexpenses.viewmodel

import android.app.Application
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.retrofit.ValidationService
import org.totschnig.myexpenses.util.TextUtils.concatResStrings
import org.totschnig.myexpenses.util.licence.LicenceHandler
import org.totschnig.myexpenses.util.safeMessage
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Named

class LicenceValidationViewModel(application: Application) : BaseViewModel(application) {
    @Inject
    lateinit var licenceHandler: LicenceHandler

    @Inject
    lateinit var builder: OkHttpClient.Builder

    @Inject
    @Named("deviceId")
    lateinit var deviceId: String

    @Inject
    lateinit var converterFactory: GsonConverterFactory

    @Inject
    lateinit var prefHandler: PrefHandler

    private val _result: MutableStateFlow<Pair<Boolean, String>?> = MutableStateFlow(null)
    val result: StateFlow<Pair<Boolean, String>?> = _result

    private val licenceEmail: String
        get() = prefHandler.requireString(PrefKey.LICENCE_EMAIL,"")

    private val licenceKey: String
        get() = prefHandler.requireString(PrefKey.NEW_LICENCE,"")

    private val okHttpClient: OkHttpClient
        get() = builder
            .connectTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

    private val retrofit: Retrofit
        get() = Retrofit.Builder()
            .baseUrl(licenceHandler.backendUri)
            .addConverterFactory(converterFactory)
            .client(okHttpClient)
            .build()

    private val service: ValidationService
        get() = retrofit.create(ValidationService::class.java)

    fun removeLicence() {
        viewModelScope.launch(context = coroutineContext()) {
            _result.update {
                if (licenceKey.isEmpty() || licenceEmail.isEmpty()) {
                    false to "Email or Key Missing"
                } else {
                    val licenceCall = service.removeLicence(licenceEmail, licenceKey, deviceId)
                    try {
                        val licenceResponse = licenceCall.execute()
                        if (licenceResponse.isSuccessful || licenceResponse.code() == 404) {
                            prefHandler.remove(PrefKey.NEW_LICENCE)
                            prefHandler.remove(PrefKey.LICENCE_EMAIL)
                            licenceHandler.voidLicenceStatus(false)
                            true to getString(R.string.licence_removal_success)
                        } else {
                            false to licenceResponse.code().toString()
                        }
                    } catch (e: IOException) {
                        false to e.safeMessage
                    }
                }
            }
        }
    }

    fun validateLicence() {
        viewModelScope.launch(context = coroutineContext()) {
            _result.update {
                if (licenceKey.isEmpty() || licenceEmail.isEmpty()) {
                    false to "Email or Key Missing"
                } else {
                    val licenceCall = service.validateLicence(licenceEmail, licenceKey, deviceId)
                    try {
                        val licenceResponse = licenceCall.execute()
                        val licence = licenceResponse.body()
                        if (licenceResponse.isSuccessful && licence != null) {
                            licenceHandler.updateLicenceStatus(licence)
                            val type = licence.type
                            var successMessage =
                                localizedContext.getString(R.string.licence_validation_success)
                            successMessage += if (type == null) concatResStrings(
                                localizedContext,
                                ", ",
                                *licence.featureListAsResIDs(localizedContext)
                            ) else " " + localizedContext.getString(type.resId)
                            true to successMessage
                        } else {
                            false to when (licenceResponse.code()) {
                                452 -> {
                                    licenceHandler.voidLicenceStatus(true)
                                    getString(R.string.licence_validation_error_expired)
                                }
                                453 -> {
                                    licenceHandler.voidLicenceStatus(false)
                                    getString(R.string.licence_validation_error_device_limit_exceeded)
                                }
                                404 -> {
                                    licenceHandler.voidLicenceStatus(false)
                                    getString(R.string.licence_validation_failure)
                                }
                                else -> licenceResponse.code().toString()
                            }
                        }
                    } catch (e: IOException) {
                        false to e.safeMessage
                    }
                }
            }
        }
    }

    fun messageShown() {
        _result.update {
            null
        }
    }
}