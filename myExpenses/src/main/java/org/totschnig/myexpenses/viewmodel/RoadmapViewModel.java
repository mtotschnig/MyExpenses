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
import org.totschnig.myexpenses.graphql.GraphQLQuery;
import org.totschnig.myexpenses.graphql.GraphQLResult;
import org.totschnig.myexpenses.retrofit.GithubService;

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
  public static final String VOTE_URL = BuildConfig.DEBUG ?
      "https://votedb-staging.herokuapp.com"  : "https://roadmap.myexpenses.mobi/";
  public static final String GIBTHUB_API = "https://api.github.com/";

  @Inject
  HttpLoggingInterceptor loggingInterceptor;

  private final MutableLiveData<List<GraphQLResult.Node>> data = new MutableLiveData<>();
  public static final String CACHE = "issue_cache.json";

  public RoadmapViewModel(Application application) {
    super(application);
    ((MyApplication) application).getAppComponent().inject(this);
    loadData();
  }
  public LiveData<List<GraphQLResult.Node>> getData() {
    return data;
  }
  private void loadData() {
    new MyTask().execute();
  }


  private class MyTask extends AsyncTask<Void, Void, List<GraphQLResult.Node>> {

    @Override
    protected List<GraphQLResult.Node> doInBackground(Void... voids) {
      Gson gson = new Gson();

      try {
        FileInputStream fis = getApplication().openFileInput(CACHE);
        String issuesJson = IOUtils.streamToString(fis);
        Type listType = new TypeToken<ArrayList<GraphQLResult.Node>>(){}.getType();
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
          .baseUrl(GIBTHUB_API)
          .addConverterFactory(GsonConverterFactory.create())
          .client(okHttpClient)
          .build();

      GithubService githubService = retrofit.create(GithubService.class);
      String endcursor = null;
      int pageCount = 0;
      List<GraphQLResult.Node> result = new ArrayList<>();

      while (true) {
        GraphQLQuery graphQLQuery = GraphQLQuery.create(endcursor);

        Call<GraphQLResult> graphQLResultCall = githubService.getIssues(
            "Bearer 64eb4c5579809e417b748eb2e5be838a726a2a89", graphQLQuery);
        try {
          Response<GraphQLResult> graphQLResultResponse = graphQLResultCall.execute();
          GraphQLResult graphQLResult = graphQLResultResponse.body();
          if (graphQLResultResponse.isSuccessful() && graphQLResult != null) {
            GraphQLResult.Issues issues = graphQLResult.getData().getRepository().getIssues();
            result.addAll(issues.getNodes());
            GraphQLResult.PageInfo pageInfo = issues.getPageInfo();
            pageCount++;
            if (!pageInfo.isHasNextPage() || pageCount >= 10) {
              break;
            }
            endcursor = pageInfo.getEndCursor();
          } else {
            break;
          }
        } catch (IOException e) {
          break;
        }
      }

      try {
        FileOutputStream fos = getApplication().openFileOutput(CACHE, Context.MODE_PRIVATE);
        fos.write(gson.toJson(result).getBytes());
        fos.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
      return result;
    }


    @Override
    protected void onPostExecute(List<GraphQLResult.Node> result) {
      data.setValue(result);
    }
  }
}
