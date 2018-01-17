package org.totschnig.myexpenses.retrofit;

import org.totschnig.myexpenses.graphql.GraphQLQuery;
import org.totschnig.myexpenses.graphql.GraphQLResult;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface GithubService {
  @POST("graphql")
  Call<GraphQLResult> getIssues(@Header("Authorization") String authHeader, @Body GraphQLQuery query);
}
