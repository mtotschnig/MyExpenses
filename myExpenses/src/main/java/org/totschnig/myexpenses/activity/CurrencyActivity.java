package org.totschnig.myexpenses.activity;

import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;

import org.json.JSONObject;
import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;

public class CurrencyActivity extends ProtectedFragmentActivity {

    @Override
    public boolean dispatchCommand(int command, Object tag) {
        if (super.dispatchCommand(command, tag))
            return true;
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(MyApplication.getThemeId());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_currency);
        setupToolbar(true);
        getSupportActionBar().setTitle("Currencies");
        RequestQueue queue = Volley.newRequestQueue(this);
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, "http://api.fixer.io/latest?base=USD", null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Gson gson = new Gson();
                JSONCurrency currency = gson.fromJson(response.toString(), JSONCurrency.class);
                getSupportActionBar().setTitle("Currencies (" + currency.date + ")");
                Log.d("CURRENCY", currency.base);
                String[] currencies = new String[8];

                currencies[0] = "\u20AC: " + currency.rates.EUR + " / $1";
                currencies[1] = "\u20BD: " + currency.rates.RUB + " / $1";
                currencies[2] = "\u00A5: " + currency.rates.JPY + " / $1";
                currencies[3] = "R\u0024: " + currency.rates.BRL + " / $1";
                currencies[4] = "\u00A3: " + currency.rates.GBP + " / $1";
                currencies[5] = "\u20AA: " + currency.rates.ILS + " / $1";
                currencies[6] = "\u0E3F: " + currency.rates.THB + " / $1";
                currencies[7] = "R: " + currency.rates.ZAR + " / $1";

                ListView lvCurrency = (ListView) findViewById(R.id.lvCurrency);

                ArrayAdapter<String> adapter = new ArrayAdapter<String>(getApplicationContext(),
                        android.R.layout.simple_list_item_1, currencies);
                lvCurrency.setAdapter(adapter);
            }
        }
        , new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(CurrencyActivity.this, "No internet connection", Toast.LENGTH_SHORT).show();
            }
        });
        queue.add(jsonObjectRequest);
    }

    class Rates {
        public double AUD;
        public double BGN;
        public double BRL;
        public double CAD;
        public double CHF;
        public double CNY;
        public double CZK;
        public double DKK;
        public double GBP;
        public double HKD;
        public double HRK;
        public double HUF;
        public double IDR;
        public double ILS;
        public double INR;
        public double JPY;
        public double KRW;
        public double MXN;
        public double MYR;
        public double NOK;
        public double NZD;
        public double PHP;
        public double PLN;
        public double RON;
        public double RUB;
        public double SEK;
        public double SGD;
        public double THB;
        public double TRY;
        public double EUR;
        public double ZAR;
    }

    class JSONCurrency {
        public String base;
        public String date;
        public Rates rates;
    }
}
