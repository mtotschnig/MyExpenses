package org.totschnig.myexpenses;

import java.io.FileInputStream;

import org.xmlpull.v1.XmlPullParser;

import android.preference.PreferenceActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;
import android.content.Context;
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
            	Context context = getApplicationContext();
            	CharSequence text = "Importing Categories now!";
            	int duration = Toast.LENGTH_SHORT;
            	Toast toast = Toast.makeText(context, text, duration);
            	toast.show();
            	importCats();
            }
          });
    }
    private void importCats() {
        XmlPullParser parser = Xml.newPullParser();
        try {
            // auto-detect the encoding from the stream
            parser.setInput(new FileInputStream("/sdcard/categories.xml"), null);
            int eventType = parser.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT){
                String name = null;
                switch (eventType){
                    case XmlPullParser.START_TAG:
                        name = parser.getName();
                       if (name == "Category") {
                    	   //String getAttributeValue
                       } else if (name == "Sub_category") {
                    	   //TODOgetAttributeValue
                        }
                        break;
                }
                eventType = parser.next();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }
}