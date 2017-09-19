package org.totschnig.myexpenses.task;

import android.os.AsyncTask;
import android.provider.Settings;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.preference.PrefKey;
import org.totschnig.myexpenses.util.LicenceHandler;
import org.totschnig.myexpenses.util.Result;
import org.totschnig.myexpenses.util.Utils;
import org.totschnig.myexpenses.util.licence.Licence;
import org.totschnig.myexpenses.util.licence.ValidationService;

import java.io.IOException;

import javax.inject.Inject;

import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ValidateLicenceTask extends AsyncTask<Void, Void, Result> {
  private final TaskExecutionFragment taskExecutionFragment;

  @Inject
  LicenceHandler licenceHandler;
  ValidateLicenceTask(TaskExecutionFragment tTaskExecutionFragment) {
    this.taskExecutionFragment = tTaskExecutionFragment;
    MyApplication.getInstance().getAppComponent().inject(this);
  }

  @Override
  protected void onPreExecute() {
    super.onPreExecute();
  }

  @Override
  protected Result doInBackground(Void... voids) {
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl("https://licencedb.myexpenses.mobi/")
        .addConverterFactory(GsonConverterFactory.create())
        .build();

    ValidationService service = retrofit.create(ValidationService.class);
    Call<Licence> licenceCall = service.validateLicence(PrefKey.NEW_LICENCE.getString(""),
        Settings.Secure.getString(MyApplication.getInstance().getContentResolver(), Settings.Secure.ANDROID_ID));
    try {
      Response<Licence> licenceResponse = licenceCall.execute();
      Licence licence = licenceResponse.body();
      if (licenceResponse.isSuccessful() && licence != null && licence.getType() != null) {
        licenceHandler.updateLicenceStatus(licence);
        return new Result(true, Utils.concatResStrings(MyApplication.getInstance(), " ",
            R.string.licence_validation_success, licence.getType().getResId()));
      } else {
        switch (licenceResponse.code()) {
          case 452:
            licenceHandler.updateLicenceStatus(null);
            return new Result(false, R.string.licence_validation_error_expired);
          case 453:
            licenceHandler.updateLicenceStatus(null);
            return new Result(false, R.string.licence_validation_error_device_limit_exceeded);
          case 404:
            licenceHandler.updateLicenceStatus(null);
            return new Result(false, R.string.licence_validation_error_not_found);
          default:
            return new Result(false, R.string.error, String.valueOf(licenceResponse.code()));
        }
      }
    } catch (IOException e) {
      return new Result(false, R.string.error, e.getMessage());
    }
  }

  @Override
  protected void onPostExecute(Result result) {
    if (this.taskExecutionFragment.mCallbacks != null) {
      this.taskExecutionFragment.mCallbacks.onPostExecute(
          TaskExecutionFragment.TASK_VALIDATE_LICENCE, result);
    }
  }
}
