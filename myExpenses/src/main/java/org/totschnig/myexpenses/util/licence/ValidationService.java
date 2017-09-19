package org.totschnig.myexpenses.util.licence;

import retrofit2.Call;
import retrofit2.http.DELETE;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface ValidationService {
  @PUT("licences/{licence}/devices/{device}")
  Call<Licence> validateLicence(@Path("licence") String licence, @Path("device") String device);

  @DELETE("licences/{licence}/devices/{device}")
  Call<Void> removeLicence(@Path("licence") String licence, @Path("device") String device);
}
