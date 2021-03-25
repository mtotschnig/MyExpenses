package org.totschnig.myexpenses.retrofit

import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface RoadmapService {
    @GET("issues")
    suspend fun issues(): Response<List<Issue>>

    @POST("votes")
    suspend fun createVote(@Body query: Vote): Response<Unit>
}