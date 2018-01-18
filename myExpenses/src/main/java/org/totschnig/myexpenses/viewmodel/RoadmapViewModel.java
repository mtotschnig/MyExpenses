package org.totschnig.myexpenses.viewmodel;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.Context;
import android.os.AsyncTask;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.acra.util.IOUtils;
import org.totschnig.myexpenses.BuildConfig;
import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.retrofit.Issue;
import org.totschnig.myexpenses.retrofit.RoadmapService;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RoadmapViewModel extends AndroidViewModel {
  public static final String ROADMAP_URL = BuildConfig.DEBUG ?
      "https://votedb-staging.herokuapp.com"  : "https://roadmap.myexpenses.mobi/";

  @Inject
  HttpLoggingInterceptor loggingInterceptor;

  private final MutableLiveData<List<Issue>> data = new MutableLiveData<>();
  public static final String CACHE = "issue_cache.json";

  public RoadmapViewModel(Application application) {
    super(application);
    ((MyApplication) application).getAppComponent().inject(this);
    loadData();
  }
  public LiveData<List<Issue>> getData() {
    return data;
  }
  private void loadData() {
    new MyTask().execute();
  }


  private class MyTask extends AsyncTask<Void, Void, List<Issue>> {

    @Override
    protected List<Issue> doInBackground(Void... voids) {
      Gson gson = new Gson();

      try {
        FileInputStream fis = getApplication().openFileInput(CACHE);
        String issuesJson = IOUtils.streamToString(fis);
        Type listType = new TypeToken<ArrayList<Issue>>() {
        }.getType();
        return gson.fromJson(issuesJson, listType);
      } catch (IOException e) {
        e.printStackTrace();
      }

      final OkHttpClient okHttpClient = new OkHttpClient.Builder()
          .connectTimeout(20, TimeUnit.SECONDS)
          .writeTimeout(20, TimeUnit.SECONDS)
          .readTimeout(30, TimeUnit.SECONDS)
          .addInterceptor(loggingInterceptor)
          .build();

      Retrofit retrofit = new Retrofit.Builder()
          .baseUrl(ROADMAP_URL)
          .addConverterFactory(GsonConverterFactory.create())
          .client(okHttpClient)
          .build();

      RoadmapService githubService = retrofit.create(RoadmapService.class);


      Call<List<Issue>> issuesCall = githubService.getIssues();
      List<Issue> result = null;
      try {
        Response<List<Issue>> response = issuesCall.execute();
        result = response.body();
        FileOutputStream fos = getApplication().openFileOutput(CACHE, Context.MODE_PRIVATE);
        fos.write(gson.toJson(result).getBytes());
        fos.close();
      } catch (IOException ignore) {
      }
      return result;
    }


    @Override
    protected void onPostExecute(List<Issue> result) {
      data.setValue(result);
    }
  }
}
