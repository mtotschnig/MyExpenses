package org.totschnig.myexpenses.viewmodel;

import android.app.Application;
import android.content.Context;
import android.os.AsyncTask;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.totschnig.myexpenses.BuildConfig;
import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.preference.PrefHandler;
import org.totschnig.myexpenses.preference.PrefKey;
import org.totschnig.myexpenses.retrofit.Issue;
import org.totschnig.myexpenses.retrofit.RoadmapService;
import org.totschnig.myexpenses.retrofit.Vote;
import org.totschnig.myexpenses.util.Result;
import org.totschnig.myexpenses.util.io.StreamReader;
import org.totschnig.myexpenses.util.licence.LicenceHandler;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import timber.log.Timber;

//TODO migrate to Coroutines, and use mock network for test
public class RoadmapViewModel extends AndroidViewModel {
  public static final String ROADMAP_URL = BuildConfig.DEBUG ?
      "https://votedb-staging.herokuapp.com/"  : "https://roadmap.myexpenses.mobi/";
  public static final int EXPECTED_MINIMAL_VERSION  = 2;
  public static final String ISSUE_CACHE = "issue_cache.json";
  public static final String ROADMAP_VOTE = "roadmap_vote.json";

  @Inject
  OkHttpClient.Builder builder;
  @Inject
  LicenceHandler licenceHandler;
  @Inject
  PrefHandler prefHandler;

  private final MutableLiveData<List<Issue>> data = new MutableLiveData<>();
  private final MutableLiveData<Vote> lastVote = new MutableLiveData<>();
  private final MutableLiveData<Result<Vote>> voteResult = new MutableLiveData<>();
  private RoadmapService roadmapService;
  private Gson gson;
  private Call currentCall;

  public RoadmapViewModel(Application application) {
    super(application);
    ((MyApplication) application).getAppComponent().inject(this);
    gson = new Gson();

    final OkHttpClient okHttpClient = builder
        .connectTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build();
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(ROADMAP_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .client(okHttpClient)
        .build();
    roadmapService = retrofit.create(RoadmapService.class);
  }

  public void loadLastVote() {
    new LoadLastVoteTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
  }

  public LiveData<List<Issue>> getData() {
    return data;
  }

  public LiveData<Result<Vote>> getVoteResult() {
    return voteResult;
  }

  public LiveData<Vote> getLastVote() {
    return lastVote;
  }

  public void loadData(boolean withCache) {
    new LoadIssuesTask(withCache).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
  }

  public void submitVote(Vote vote) {
    new VoteTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, vote);
  }

  public void cancel() {
    if (currentCall != null && !currentCall.isCanceled()) {
      currentCall.cancel();
    }
  }

  public void cacheWeights(Map<Integer, Integer> voteWeights) {
    PrefKey.ROADMAP_VOTE.putString(gson.toJson(voteWeights));
  }

  public Map<Integer, Integer> restoreWeights() {
    String stored = PrefKey.ROADMAP_VOTE.getString(null);

    return stored != null ? gson.fromJson(stored, new TypeToken<Map<Integer, Integer>>(){}.getType()) :
        new HashMap<>();
  }

  private class VoteTask extends AsyncTask<Vote, Void, Result<Vote>> {
    @Override
    protected Result<Vote> doInBackground(Vote... votes) {
      try {
        final Vote vote = votes[0];
        Call<Void> voteCall = roadmapService.createVote(vote);
        currentCall = voteCall;
        Response<Void> voteResponse = voteCall.execute();
        if (voteResponse.isSuccessful()) {
          writeToFile(ROADMAP_VOTE, gson.toJson(vote));
          return Result.ofSuccess(R.string.roadmap_vote_success, vote);
        } else if (voteResponse.code() == 452) {
          return Result.ofFailure(R.string.roadmap_vote_outdated);
        }
      } catch (IOException | SecurityException e) {
        Timber.i(e);
      }
      return Result.ofFailure(R.string.roadmap_vote_failure);
    }

    @Override
    protected void onPostExecute(Result<Vote> result) {
      currentCall = null;
      voteResult.setValue(result);
    }
  }

  private class LoadIssuesTask extends AsyncTask<Void, Void, List<Issue>> {

    private final boolean withCache;

    public LoadIssuesTask(boolean withCache) {
      this.withCache = withCache;
    }

    @Override
    protected List<Issue> doInBackground(Void... voids) {
      List<Issue> issueList = null;

      if (withCache) {
        try {
          Type listType = new TypeToken<ArrayList<Issue>>() {
          }.getType();
          issueList = gson.fromJson(readFromFile(ISSUE_CACHE), listType);
          Timber.i("Loaded %d issues from cache", issueList.size());
        } catch (IOException e) {
          Timber.i(e);
        }
      }

      if (issueList == null) {
        issueList = readIssuesFromNetwork();
      }
      return issueList;
    }

    @Override
    protected void onPostExecute(List<Issue> result) {
      currentCall = null;
      data.setValue(result);
    }
  }

  private class LoadLastVoteTask extends AsyncTask<Void, Void, Vote> {

    @Override
    protected Vote doInBackground(Void... voids) {
      return readLastVoteFromFile();
    }


    @Override
    protected void onPostExecute(Vote result) {
      lastVote.setValue(result);
    }
  }

  @Nullable
  private Vote readLastVoteFromFile() {
    Vote lastVote = null;

    try {
      lastVote = gson.fromJson(readFromFile(ROADMAP_VOTE), Vote.class);
    } catch (IOException e) {
      Timber.i(e);
    }
    return lastVote;
  }

  private List<Issue> readIssuesFromNetwork() {
    List<Issue> issueList = null;
    try {
      Call<List<Issue>> issuesCall = roadmapService.getIssues();
      currentCall = issuesCall;
      Response<List<Issue>> response = issuesCall.execute();
      @Nullable String version = response.headers().get("X-Version");
      issueList = response.body();
      if (response.isSuccessful() && issueList != null) {
        if (version != null) {
          final int versionInt;
          try {
            versionInt = Integer.parseInt(version);
            prefHandler.putInt(PrefKey.ROADMAP_VERSION, versionInt);
          } catch (NumberFormatException ignored) {}
        }
        Timber.i("Loaded %d issues (version %s) from network", issueList.size(), version);
        writeToFile(ISSUE_CACHE, gson.toJson(issueList));
      }
    } catch (IOException | SecurityException e) {
      Timber.i(e);
    }
    return issueList;
  }


  private void writeToFile(String fileName, String json) throws IOException {
    FileOutputStream fos = getApplication().openFileOutput(fileName, Context.MODE_PRIVATE);
    fos.write(json.getBytes());
    fos.close();
  }

  private String readFromFile(String filename) throws IOException {
    FileInputStream fis = getApplication().openFileInput(filename);
    return new StreamReader(fis).read();
  }
}
