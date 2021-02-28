package org.totschnig.myexpenses.task;

import android.content.Context;
import android.os.AsyncTask;

import com.google.gson.Gson;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.retrofit.ValidationService;
import org.totschnig.myexpenses.ui.ContextHelper;
import org.totschnig.myexpenses.util.Result;
import org.totschnig.myexpenses.util.TextUtils;
import org.totschnig.myexpenses.util.licence.Licence;
import org.totschnig.myexpenses.util.licence.LicenceHandler;
import org.totschnig.myexpenses.util.licence.LicenceStatus;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;

import androidx.annotation.NonNull;
import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import static org.totschnig.myexpenses.preference.PrefKey.LICENCE_EMAIL;
import static org.totschnig.myexpenses.preference.PrefKey.NEW_LICENCE;

public class LicenceApiTask extends AsyncTask<Void, Void, Result> {
  private final TaskExecutionFragment taskExecutionFragment;
  private final int taskId;

  @Inject
  LicenceHandler licenceHandler;

  @Inject
  OkHttpClient.Builder builder;

  @Inject
  @Named("deviceId")
  String deviceId;

  @Inject
  Gson gson;

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
    String licenceEmail = LICENCE_EMAIL.getString("");
    String licenceKey = NEW_LICENCE.getString("");
    if ("".equals(licenceKey) || "".equals(licenceEmail)) {
      return Result.FAILURE;
    }

    final OkHttpClient okHttpClient = builder
        .connectTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build();

    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(licenceHandler.getBackendUri())
        .addConverterFactory(GsonConverterFactory.create(gson))
        .client(okHttpClient)
        .build();

    ValidationService service = retrofit.create(ValidationService.class);

    final MyApplication application = MyApplication.getInstance();
    final Context context = ContextHelper.wrap(application, application.getAppComponent().userLocaleProvider().getUserPreferredLocale());

    if (taskId == TaskExecutionFragment.TASK_VALIDATE_LICENCE) {
      Call<Licence> licenceCall = service.validateLicence(licenceEmail, licenceKey, deviceId);
      try {
        Response<Licence> licenceResponse = licenceCall.execute();
        Licence licence = licenceResponse.body();
        if (licenceResponse.isSuccessful() && licence != null) {
          licenceHandler.updateLicenceStatus(licence);
          final LicenceStatus type = licence.getType();
          String successMessage = context.getString(R.string.licence_validation_success);
          successMessage += (type == null ? TextUtils.concatResStrings(context, ", ", licence.featureListAsResIDs(context)) :
              " " + context.getString(type.getResId()));
          return Result.ofSuccess(successMessage);
        } else {
          switch (licenceResponse.code()) {
            case 452:
              licenceHandler.voidLicenceStatus(true);
              return Result.ofFailure(R.string.licence_validation_error_expired);
            case 453:
              licenceHandler.voidLicenceStatus(false);
              return Result.ofFailure(R.string.licence_validation_error_device_limit_exceeded);
            case 404:
              licenceHandler.voidLicenceStatus(false);
              return Result.ofFailure(R.string.licence_validation_failure);
            default:
              return buildFailureResult(String.valueOf(licenceResponse.code()));
          }
        }
      } catch (IOException e) {
        return buildFailureResult(e.getMessage());
      }
    } else if (taskId == TaskExecutionFragment.TASK_REMOVE_LICENCE) {
      Call<Void> licenceCall = service.removeLicence(licenceEmail, licenceKey, deviceId);
      try {
        Response<Void> licenceResponse = licenceCall.execute();
        if (licenceResponse.isSuccessful() || licenceResponse.code() == 404) {
          NEW_LICENCE.remove();
          LICENCE_EMAIL.remove();
          licenceHandler.voidLicenceStatus(false);
          return Result.ofSuccess(R.string.licence_removal_success);
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
    return Result.ofFailure(R.string.error, s);
  }

  @Override
  protected void onPostExecute(Result result) {
    if (this.taskExecutionFragment.mCallbacks != null) {
      this.taskExecutionFragment.mCallbacks.onPostExecute(taskId, result);
    }
  }
}
