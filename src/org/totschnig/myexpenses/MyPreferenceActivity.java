package org.totschnig.myexpenses;

import java.io.FileInputStream;

import org.xmlpull.v1.XmlPullParser;

import android.preference.PreferenceActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.util.Xml;

public class MyPreferenceActivity extends PreferenceActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
    	Button importButton;
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.layout.preferences);
        setContentView(R.layout.preference_screen);
        importButton = (Button) findViewById(R.id.import_cat);
        importButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
            	 new MyAsyncTask(MyPreferenceActivity.this).execute();
            }
          });
    }
    private class MyAsyncTask extends AsyncTask<Void, Void, Integer> {
        private Context context;
        public MyAsyncTask(Context context) {
            this.context = context;
        }
        protected void onPostExecute(Integer result) {
        	String msg;
            super.onPostExecute(result);
            if (result == 1) {
            	msg = getString(R.string.import_categories_success);
            } else {
            	msg = getString(R.string.import_categories_failure);
            }
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
        }
        protected void onPreExecute() {
            super.onPreExecute();
            Toast.makeText(context, getString(R.string.import_categories), Toast.LENGTH_LONG).show();
        }
		@Override
		protected Integer doInBackground(Void... params) {
			return importCats();
		}
    }
    //returns 1 upon success, 0 upon failure
    private int importCats() {
    	//TODO warn if there are already cats in the db
        XmlPullParser parser = Xml.newPullParser();
        ExpensesDbAdapter mDbHelper = new ExpensesDbAdapter(this);
        mDbHelper.open();
        String tagName;
        String label;
        String id;
        String parent_id;
        int result;
        try {
            // auto-detect the encoding from the stream
            parser.setInput(new FileInputStream("/sdcard/myexpenses/categories.xml"), null);
            int eventType = parser.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT){
                switch (eventType){
                    case XmlPullParser.START_TAG:
                        tagName = parser.getName();
                       if (tagName == "Category") {
                    	   label = parser.getAttributeValue(null, "Na");
                    	   id = parser.getAttributeValue(null, "Nb");
                    	   Log.w("MyPreferenceActivity", "Creating category with label " + 
                    			   label + " and id " + id);
                    	   mDbHelper.createCategory(label,id,null);
                       } else if (tagName == "Sub_category") {
                    	   label = parser.getAttributeValue(null, "Na");
                    	   id = parser.getAttributeValue(null, "Nb");
                    	   parent_id = parser.getAttributeValue(null, "Nbc");
                    	   mDbHelper.createCategory(label, id, parent_id);
                        }
                        break;
                }
                eventType = parser.next();
            }
            result = 1;
        } catch (Exception e) {
        	result = 0;
        }
        mDbHelper.close();
        return result;
    }
}
