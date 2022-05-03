package org.totschnig.myexpenses.service

import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import org.totschnig.myexpenses.retrofit.ITransactionDAO
import org.totschnig.myexpenses.retrofit.RetrofitClientInstance
import timber.log.Timber
import java.io.File


class TransactionService() {
    private var transactionService: ITransactionDAO? = null

    constructor(urlBaseWebService: String) : this() {
        this.transactionService = RetrofitClientInstance.getInstance(urlBaseWebService)
            ?.create(ITransactionDAO::class.java)
    }

    fun sendFileListToWebService(fileUriList: List<Uri>) {
        for (uri in fileUriList) {
            sendFileToWebservice(uri)
        }
    }

    fun sendFileToWebservice(uri: Uri) {
        val file = File(uri.path)

        val formData = MultipartBody.Part.createFormData(
            "file",
            file.name,
            file.asRequestBody("multipart/form-data".toMediaTypeOrNull())
        )

        CoroutineScope(Dispatchers.IO).launch {

            val response = transactionService?.saveFileToWebservice(formData)
            if (response != null) {
                if (response.isSuccessful) {
                    response.body()?.let {
                        Timber.i(it.string())
                    }
                } else {
                    Timber.e(response.errorBody()?.string())
                }
            }
        }
    }
}