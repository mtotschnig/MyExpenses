package org.totschnig.myexpenses.retrofit;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface RoadmapService {
  @GET("issues")
  Call<List<Issue>> getIssues();
  @POST("votes")
  Call<Void> createVote(@Header("X-Version") int version, @Body Vote query);
}
