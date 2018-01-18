package org.totschnig.myexpenses.retrofit;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;

public interface RoadmapService {
  @GET("issues")
  Call<List<Issue>> getIssues();
}
