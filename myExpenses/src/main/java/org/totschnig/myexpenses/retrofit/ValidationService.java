package org.totschnig.myexpenses.retrofit;

import org.totschnig.myexpenses.util.licence.Licence;

import retrofit2.Call;
import retrofit2.http.DELETE;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface ValidationService {
  @PUT("users/{email}/licences/{licence}/devices/{device}")
  Call<Licence> validateLicence(@Path("email") String email, @Path("licence") String licence, @Path("device") String device);

  @DELETE("users/{email}/licences/{licence}/devices/{device}")
  Call<Void> removeLicence(@Path("email") String email, @Path("licence") String licence, @Path("device") String device);
}
