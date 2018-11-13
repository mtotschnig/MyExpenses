package org.totschnig.myexpenses.retrofit;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

public interface RoadmapService {
  @GET("issues")
  Call<List<Issue>> getIssues();
  @POST("votes")
  Call<Void> createVote(@Body Vote query);
}
