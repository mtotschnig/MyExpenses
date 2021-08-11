package org.totschnig.myexpenses.task;

import android.os.AsyncTask;
import android.os.Build;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.model.TransactionDTO;
import org.totschnig.myexpenses.retrofit.WebVersionService;
import org.totschnig.myexpenses.util.Result;
import org.totschnig.myexpenses.util.io.FileUtils;

import java.io.File;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class WebVersionApiTask extends AsyncTask<Void, Void, Result> {

    private String webservice;
    private String file;

    public WebVersionApiTask(String webservice, String file) {
        this.webservice = webservice;
        this.file = file;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected Result doInBackground(Void... voids) {

        final OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(20, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();

        if (!webservice.endsWith("/")) {
            webservice = webservice.concat("/");
        }

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(webservice)
                .addConverterFactory(GsonConverterFactory.create(new Gson()))
                .client(okHttpClient)
                .build();

        WebVersionService service = retrofit.create(WebVersionService.class);

        String fileContent = "";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            File f = new File(file);
            try {
                fileContent = FileUtils.getStringFromFile(f);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        Type listOfTransactionDTO = new TypeToken<ArrayList<TransactionDTO>>() {}.getType();
        List<TransactionDTO> transactionList = new Gson().fromJson(fileContent, listOfTransactionDTO);

        Call<Object> webServiceCall = service.sendFiles(transactionList);
        try {
            Response<Object> webserviceResponse = webServiceCall.execute();
            if (webserviceResponse.isSuccessful()) {
                return Result.ofSuccess("Successfully executed");
            } else {
                return buildFailureResult(String.valueOf(webserviceResponse.code()));
            }
        } catch (Exception e) {
            return buildFailureResult(e.getMessage());
        }
    }

    @NonNull
    private Result buildFailureResult(String s) {
        return Result.ofFailure(R.string.error, s);
    }

    @Override
    protected void onPostExecute(Result result) {

    }


}
