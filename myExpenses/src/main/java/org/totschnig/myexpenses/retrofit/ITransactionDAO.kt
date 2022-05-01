package org.totschnig.myexpenses.retrofit

import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Multipart
import retrofit2.http.PUT
import retrofit2.http.Part

interface ITransactionDAO {
    @Multipart
    @PUT(".")
    suspend fun saveFileToWebservice(@Part filePart: MultipartBody.Part): Response<ResponseBody>
}
