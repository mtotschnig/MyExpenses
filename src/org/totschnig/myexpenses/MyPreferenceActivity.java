package org.totschnig.myexpenses;

import java.io.FileInputStream;
import java.util.Hashtable;

import org.xmlpull.v1.XmlPullParser;

import android.preference.PreferenceActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.util.Xml;
import android.app.Dialog;
import android.app.ProgressDialog;


public class MyPreferenceActivity extends PreferenceActivity {
	static final int PROGRESS_DIALOG = 0;
	static final String CATEGORIES_XML = "/sdcard/myexpenses/categories.xml";
    ProgressDialog progressDialog;
    ProgressThread progressThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
    	Button importButton;
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.layout.preferences);
        setContentView(R.layout.preference_screen);
        importButton = (Button) findViewById(R.id.import_cat);
        importButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
            	 //new MyAsyncTask(MyPreferenceActivity.this).execute();
            	showDialog(PROGRESS_DIALOG);
          }
        }
     );
    }
    protected Dialog onCreateDialog(int id) {
        switch(id) {
        case PROGRESS_DIALOG:
            progressDialog = new ProgressDialog(MyPreferenceActivity.this);
            progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            progressDialog.setMessage("Loading categories from " + CATEGORIES_XML);
            progressDialog.setProgress(0);
            return progressDialog;
        default:
            return null;
        }
    }
    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
        switch(id) {
        case PROGRESS_DIALOG:
            progressDialog.setProgress(0);
            progressThread = new ProgressThread(handler);
            progressThread.start();
        }
    }
        // Define the Handler that receives messages from the thread and update the progress
        final Handler handler = new Handler() {
            public void handleMessage(Message msg) {
                int total = msg.arg1;
                progressDialog.setProgress(total);
                if (total >= 100){
                    dismissDialog(PROGRESS_DIALOG);
                }
            }
        };

        private class ProgressThread extends Thread {
            Handler mHandler;
            final static int STATE_DONE = 0;
            final static int STATE_RUNNING = 1;
            int mState;
            int total;
           
            ProgressThread(Handler h) {
                mHandler = h;
            }
           
            public void run() {
                int result = importCats();
	        	Message msg = mHandler.obtainMessage();
                msg.arg1 = 100;
                mHandler.sendMessage(msg);
            }
            
		    //returns 1 upon success, 0 upon failure
		    private int importCats() {
		    	//TODO check for uniqueness of parent_id + name
		    	//TODO correlate progress with number of cats
		        XmlPullParser parser = Xml.newPullParser();
		        ExpensesDbAdapter mDbHelper = new ExpensesDbAdapter(MyPreferenceActivity.this);
		        mDbHelper.open();
		        String tagName;
		        String label;
		        String id;
		        String parent_id;
		        long _id;
		        int result;
		        int total = 0;
		        
		        Hashtable<String,String> Foreign2LocalIdMap = new Hashtable<String,String>();
		        try {
		            // auto-detect the encoding from the stream
		            parser.setInput(new FileInputStream(CATEGORIES_XML), null);
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
		                    	   _id = mDbHelper.createCategory(label,null);
		                    	   Foreign2LocalIdMap.put(id, String.valueOf(_id));
		                    	   
		                       } else if (tagName == "Sub_category") {
		                    	   label = parser.getAttributeValue(null, "Na");
		                    	   id = parser.getAttributeValue(null, "Nb");
		                    	   parent_id = parser.getAttributeValue(null, "Nbc");
		                    	   mDbHelper.createCategory(label, Foreign2LocalIdMap.get(parent_id));
		                        }
				                Log.i("DEBUG","Imported category " + total);
		                       	total++;
		                       	if (total % 10 == 0) {
			                       	Message msg = mHandler.obtainMessage();
	                                msg.arg1 = total / 2;
	                                mHandler.sendMessage(msg);
	                                Log.i("DEBUG","Sent message");
		                       	}
		                        break;
		                }
		                eventType = parser.next();
		            }
		            result = 1;
		        } catch (Exception e) {
		        	Log.e("MyExpenses",e.toString());
		        	result = 0;
		        }
		        mDbHelper.close();
		        return result;
		    }
        }
}
