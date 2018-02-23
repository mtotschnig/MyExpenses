package org.totschnig.myexpenses.viewmodel;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.acra.util.StreamReader;
import org.totschnig.myexpenses.BuildConfig;
import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.model.ContribFeature;
import org.totschnig.myexpenses.preference.PrefKey;
import org.totschnig.myexpenses.retrofit.Issue;
import org.totschnig.myexpenses.retrofit.RoadmapService;
import org.totschnig.myexpenses.retrofit.Vote;
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

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import timber.log.Timber;

public class RoadmapViewModel extends AndroidViewModel {
  public static final String ROADMAP_URL = BuildConfig.DEBUG ?
      "https://votedb-staging.herokuapp.com/"  : "https://roadmap.myexpenses.mobi/";

  @Inject
  OkHttpClient.Builder builder;
  @Inject
  LicenceHandler licenceHandler;

  private final MutableLiveData<List<Issue>> data = new MutableLiveData<>();
  private final MutableLiveData<Vote> lastVote = new MutableLiveData<>();
  private final MutableLiveData<Boolean> voteResult = new MutableLiveData<>();
  public static final String ISSUE_CACHE = "issue_cache.json";
  public static final String ROADMAP_VOTE = "roadmap_vote.json";
  private RoadmapService roadmapService;
  private Gson gson;

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

    loadLastVote();
  }

  public void loadLastVote() {
    new LoadLastVoteTask().execute();
  }

  public LiveData<List<Issue>> getData() {
    return data;
  }

  public LiveData<Boolean> getVoteResult() {
    return voteResult;
  }

  public LiveData<Vote> getLastVote() {
    return lastVote;
  }

  public void loadData(boolean withCache) {
    new LoadIssuesTask(withCache).execute();
  }

  public void submitVote(String key, Map<Integer, Integer> voteWeights) {
    new VoteTask(key).execute(voteWeights);
  }

  public void cacheWeights(Map<Integer, Integer> voteWeights) {
    PrefKey.ROADMAP_VOTE.putString(gson.toJson(voteWeights));
  }

  public Map<Integer, Integer> restoreWeights() {
    String stored = PrefKey.ROADMAP_VOTE.getString(null);

    return stored != null ? gson.fromJson(stored, new TypeToken<Map<Integer, Integer>>(){}.getType()) :
        new HashMap<>();
  }

  private class VoteTask extends AsyncTask<Map<Integer, Integer>, Void, Vote> {

    @Nullable
    private final String key;

    public VoteTask(@Nullable String key) {
      this.key = key;
    }

    @Override
    protected Vote doInBackground(Map<Integer, Integer>... votes) {
      boolean isPro = ContribFeature.ROADMAP_VOTING.hasAccess();
      Vote vote = new Vote(key != null ? key : licenceHandler.buildRoadmapVoteKey(), votes[0], isPro);
      Call<Void> voteCall = roadmapService.createVote(vote);
      try {
        Response<Void> voteResponse = voteCall.execute();
        if (voteResponse.isSuccessful()) {
          writeToFile(ROADMAP_VOTE, gson.toJson(vote));
          return vote;
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
      return null;
    }

    @Override
    protected void onPostExecute(Vote result) {
      lastVote.setValue(result);
      voteResult.setValue(result != null);
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
          Timber.e(e);
        }
      }

      if (issueList == null) {

        Call<List<Issue>> issuesCall = roadmapService.getIssues();

        try {
          Response<List<Issue>> response = issuesCall.execute();
          issueList = response.body();
          Timber.i("Loaded %d issues from network", issueList.size());
          writeToFile(ISSUE_CACHE, gson.toJson(issueList));
        } catch (IOException e) {
          Timber.e(e);
        }
      }
      return issueList;
    }


    @Override
    protected void onPostExecute(List<Issue> result) {
      data.setValue(result);
    }
  }

  private class LoadLastVoteTask extends AsyncTask<Void, Void, Vote> {

    @Override
    protected Vote doInBackground(Void... voids) {
      Vote lastVote = null;

      try {
        lastVote = gson.fromJson(readFromFile(ROADMAP_VOTE), Vote.class);
      } catch (IOException e) {
        e.printStackTrace();
      }
      return lastVote;
    }


    @Override
    protected void onPostExecute(Vote result) {
      lastVote.setValue(result);
      loadData(true);
    }
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
