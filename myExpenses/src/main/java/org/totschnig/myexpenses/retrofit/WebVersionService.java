package org.totschnig.myexpenses.retrofit;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface WebVersionService {

  @POST("my-expenses")
  Call<Object> sendFiles(@Body Object obj);
}
