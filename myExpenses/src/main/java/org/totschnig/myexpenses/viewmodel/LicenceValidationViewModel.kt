package org.totschnig.myexpenses.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.preference.requireString
import org.totschnig.myexpenses.retrofit.ValidationService
import org.totschnig.myexpenses.ui.ContextHelper
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
    lateinit var gson: Gson

    @Inject
    lateinit var prefHandler: PrefHandler

    private val _result: MutableStateFlow<String?> = MutableStateFlow(null)
    val result: StateFlow<String?> = _result

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
            .addConverterFactory(GsonConverterFactory.create(gson))
            .client(okHttpClient)
            .build()

    private val service: ValidationService
        get() = retrofit.create(ValidationService::class.java)

    private val context: Context
        get() = with(getApplication<MyApplication>()) {
            ContextHelper.wrap(this, this.appComponent.userLocaleProvider().getUserPreferredLocale())
        }

    fun removeLicence() {
        viewModelScope.launch(context = coroutineContext()) {
            _result.update {
                if (licenceKey.isEmpty() || licenceEmail.isEmpty()) {
                    "Email or Key Missing"
                } else {
                    val licenceCall = service.removeLicence(licenceEmail, licenceKey, deviceId)
                    try {
                        val licenceResponse = licenceCall.execute()
                        if (licenceResponse.isSuccessful || licenceResponse.code() == 404) {
                            prefHandler.remove(PrefKey.NEW_LICENCE)
                            prefHandler.remove(PrefKey.LICENCE_EMAIL)
                            licenceHandler.voidLicenceStatus(false)
                            getString(R.string.licence_removal_success)
                        } else {
                            licenceResponse.code().toString()
                        }
                    } catch (e: IOException) {
                        e.safeMessage
                    }
                }
            }
        }
    }

    fun validateLicence() {
        viewModelScope.launch(context = coroutineContext()) {
            _result.update {
                if (licenceKey.isEmpty() || licenceEmail.isEmpty()) {
                    "Email or Key Missing"
                } else {
                    val licenceCall = service.validateLicence(licenceEmail, licenceKey, deviceId)
                    try {
                        val licenceResponse = licenceCall.execute()
                        val licence = licenceResponse.body()
                        if (licenceResponse.isSuccessful && licence != null) {
                            licenceHandler.updateLicenceStatus(licence)
                            val type = licence.type
                            var successMessage =
                                context.getString(R.string.licence_validation_success)
                            successMessage += if (type == null) concatResStrings(
                                context,
                                ", ",
                                *licence.featureListAsResIDs(context)
                            ) else " " + context.getString(type.resId)
                            successMessage
                        } else {
                            when (licenceResponse.code()) {
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
                        e.safeMessage
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