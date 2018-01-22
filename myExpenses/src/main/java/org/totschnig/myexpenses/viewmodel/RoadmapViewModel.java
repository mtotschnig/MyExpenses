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
import org.totschnig.myexpenses.retrofit.Vote;
import org.totschnig.myexpenses.util.Utils;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import timber.log.Timber;

public class RoadmapViewModel extends AndroidViewModel {
  public static final String ROADMAP_URL = BuildConfig.DEBUG ?
      "https://votedb-staging.herokuapp.com"  : "https://roadmap.myexpenses.mobi/";

  @Inject
  HttpLoggingInterceptor loggingInterceptor;

  private final MutableLiveData<List<Issue>> data = new MutableLiveData<>();
  private final MutableLiveData<Boolean> voteResult = new MutableLiveData<>();
  public static final String CACHE = "issue_cache.json";
  private RoadmapService roadmapService;

  public RoadmapViewModel(Application application) {
    super(application);
    ((MyApplication) application).getAppComponent().inject(this);

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
    roadmapService = retrofit.create(RoadmapService.class);

    loadData(true);
  }

  public LiveData<List<Issue>> getData() {
    return data;
  }

  public MutableLiveData<Boolean> getVoteResult() {
    return voteResult;
  }

  public void loadData(boolean withCache) {
    new LoadTask(withCache).execute();
  }

  public void submitVote(String key, HashMap<Integer, Integer> voteWeights) {
    new VoteTask().execute(new Vote(key, voteWeights, false));
  }

  private class VoteTask extends AsyncTask<Vote, Void, Boolean> {

    @Override
    protected Boolean doInBackground(Vote... votes) {
      Call<Void> voteCall = roadmapService.createVote(votes[0]);
      try {
        Response<Void> voteResponse = voteCall.execute();
        if (voteResponse.isSuccessful()) {
         return true;
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
      return false;
    }

    @Override
    protected void onPostExecute(Boolean result) {
      voteResult.setValue(result);
    }
  }

  private class LoadTask extends AsyncTask<Void, Void, List<Issue>> {

    private final boolean withCache;

    public LoadTask(boolean withCache) {
      this.withCache = withCache;
    }

    @Override
    protected List<Issue> doInBackground(Void... voids) {
      Gson gson = new Gson();
      List<Issue> result = null;

      if (withCache) {
        try {
          FileInputStream fis = getApplication().openFileInput(CACHE);
          String issuesJson = IOUtils.streamToString(fis);
          Type listType = new TypeToken<ArrayList<Issue>>() {
          }.getType();
          result = gson.fromJson(issuesJson, listType);
          Timber.i("Loaded %d issues from cache", result.size());
        } catch (IOException e) {
          Timber.e(e);
        }
      }

      if (result == null) {

        Call<List<Issue>> issuesCall = roadmapService.getIssues();

        try {
          Response<List<Issue>> response = issuesCall.execute();
          result = response.body();
          Timber.i("Loaded %d issues from network", result.size());
          FileOutputStream fos = getApplication().openFileOutput(CACHE, Context.MODE_PRIVATE);
          fos.write(gson.toJson(result).getBytes());
          fos.close();
        } catch (IOException e) {
          Timber.e(e);
        }
      }
      if (result != null) {
        Collections.sort(result, (o1, o2) -> Utils.compare(o2.getNumber(), o1.getNumber()));
      }
      return result;
    }


    @Override
    protected void onPostExecute(List<Issue> result) {
      data.setValue(result);
    }
  }
}
