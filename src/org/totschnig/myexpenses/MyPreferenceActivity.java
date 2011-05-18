package org.totschnig.myexpenses;

import java.io.FileInputStream;
import java.util.Hashtable;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;

import android.preference.PreferenceActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import android.app.ProgressDialog;


public class MyPreferenceActivity extends PreferenceActivity {
	static final String CATEGORIES_XML = "/sdcard/myexpenses/categories.xml";
    ProgressDialog progressDialog;
    int totalCategories;

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
        }
     );
    }
    private class MyAsyncTask extends AsyncTask<Void, Integer, Integer> {
        private Context context;
        NodeList categories;
        NodeList sub_categories;
        ExpensesDbAdapter mDbHelper;
        Hashtable<String,String> Foreign2LocalIdMap;
        int totalImported;
        
        public MyAsyncTask(Context context) {
            this.context = context;
            mDbHelper = new ExpensesDbAdapter(MyPreferenceActivity.this);
            Foreign2LocalIdMap = new Hashtable<String,String>();
            totalImported = 0; 
        }
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = new ProgressDialog(MyPreferenceActivity.this);
            progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            progressDialog.setTitle(getString(R.string.categories_loading,CATEGORIES_XML));
            progressDialog.setProgress(0);
            progressDialog.show();
        }
        protected void onProgressUpdate(Integer... values) {
            progressDialog.setProgress(values[0]);
        }
        protected void onPostExecute(Integer result) {
        	progressDialog.dismiss();
            String msg;
            super.onPostExecute(result);
            if (result == -1) {
                msg = getString(R.string.import_categories_failure);
            } else {
                msg = getString(R.string.import_categories_success,result);
            }
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
        }

        
        @Override
         protected Integer doInBackground(Void... params) {
        	//first we do the parsing
        	try {
	        	DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	            DocumentBuilder builder = factory.newDocumentBuilder();
	            Document dom = builder.parse(new FileInputStream(CATEGORIES_XML));
	            Element root = dom.getDocumentElement();
	            categories = root.getElementsByTagName("Category");
	            sub_categories = root.getElementsByTagName("Sub_category");
        	} catch (Exception e) {
        		return -1;
        	}
            totalCategories = categories.getLength() + sub_categories.getLength();
            progressDialog.setMax(totalCategories);
            
            mDbHelper.open();

            importCatsMain();
            importCatsSub();
            return totalImported;
        }
        private void importCatsMain() {
        	int start = 1;
            String label;
            String id;
            long _id;

        	for (int i=0;i<categories.getLength();i++){
        		NamedNodeMap category = categories.item(i).getAttributes();
        		label = category.getNamedItem("Na").getNodeValue();
        		id =  category.getNamedItem("Nb").getNodeValue();
        		_id = mDbHelper.getCategoryId(label, "0");
        		if (_id != -1) {
        			Foreign2LocalIdMap.put(id, String.valueOf(_id));
        		} else {
        			_id = mDbHelper.createCategory(label,"0");
        			if (_id != -1) {
        				Foreign2LocalIdMap.put(id, String.valueOf(_id));
        				totalImported++;
        			} else {
        				//this should not happen
        				Log.w("MyExpenses","could neither retrieve nor store main category " + label);
        			}
        		}
        		if ((start+i) % 10 == 0) {
        			publishProgress(start+i);
        		}
        	}
        }
        private void importCatsSub() {
        	int start = categories.getLength() + 1;
            String label;
            //String id;
            String parent_id;
            String mapped_parent_id;
            long _id;
        	for (int i=0;i<sub_categories.getLength();i++){
        		NamedNodeMap sub_category = sub_categories.item(i).getAttributes();
        		label = sub_category.getNamedItem("Na").getNodeValue();
        		//id =  sub_category.getNamedItem("Nb").getNodeValue();
        		parent_id = sub_category.getNamedItem("Nbc").getNodeValue();
        		mapped_parent_id = Foreign2LocalIdMap.get(parent_id);
        		//TODO: for the moment, we do not deal with subcategories,
        		//if we were not able to import a matching category
        		//should check if the main category exists, but the subcategory is new
        		if (mapped_parent_id != null) {
	        		_id = mDbHelper.createCategory(label, Foreign2LocalIdMap.get(parent_id));
	        		if (_id != -1) {
	        			totalImported++;
	        		}
        		} else {
        			Log.w("MyExpenses","could not store sub category " + label);
        		}
        		if ((start+i) % 10 == 0) {
        			publishProgress(start+i);
        		}
        	}
        }
    }
}
