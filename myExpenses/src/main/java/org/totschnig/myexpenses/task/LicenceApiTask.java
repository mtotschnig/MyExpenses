package org.totschnig.myexpenses.task;

import android.os.AsyncTask;
import android.support.annotation.NonNull;

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
import javax.inject.Named;

import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class LicenceApiTask extends AsyncTask<Void, Void, Result> {
  private final TaskExecutionFragment taskExecutionFragment;
  private final int taskId;

  @Inject
  LicenceHandler licenceHandler;

  @Inject
  @Named("deviceId")
  String deviceId;

  LicenceApiTask(TaskExecutionFragment tTaskExecutionFragment, int taskId) {
    this.taskExecutionFragment = tTaskExecutionFragment;
    this.taskId = taskId;
    MyApplication.getInstance().getAppComponent().inject(this);
  }

  @Override
  protected void onPreExecute() {
    super.onPreExecute();
  }

  @Override
  protected Result doInBackground(Void... voids) {
    String licenceKey = PrefKey.NEW_LICENCE.getString("");
    if ("".equals(licenceKey)) {
      return Result.FAILURE;
    }

    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl("https://licencedb.myexpenses.mobi/")
        .addConverterFactory(GsonConverterFactory.create())
        .build();

    ValidationService service = retrofit.create(ValidationService.class);

    if (taskId == TaskExecutionFragment.TASK_VALIDATE_LICENCE) {
      Call<Licence> licenceCall = service.validateLicence(licenceKey, deviceId);
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
              return buildFailureResult(String.valueOf(licenceResponse.code()));
          }
        }
      } catch (IOException e) {
        return buildFailureResult(e.getMessage());
      }
    } else if (taskId == TaskExecutionFragment.TASK_REMOVE_LICENCE) {
      Call<Void> licenceCall = service.removeLicence(licenceKey, deviceId);
      try {
        Response<Void> licenceResponse = licenceCall.execute();
        if (licenceResponse.isSuccessful()) {
          licenceHandler.updateLicenceStatus(null);
          return new Result(true, R.string.licence_removal_success);
        } else {
          return buildFailureResult(String.valueOf(licenceResponse.code()));
        }
      } catch (IOException e) {
        return buildFailureResult(e.getMessage());
      }
    }
    return Result.FAILURE;
  }

  @NonNull
  private Result buildFailureResult(String s) {
    return new Result(false, R.string.error, s);
  }

  @Override
  protected void onPostExecute(Result result) {
    if (this.taskExecutionFragment.mCallbacks != null) {
      this.taskExecutionFragment.mCallbacks.onPostExecute(taskId, result);
    }
  }
}
