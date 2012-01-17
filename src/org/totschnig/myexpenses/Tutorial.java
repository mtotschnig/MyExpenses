package org.totschnig.myexpenses;

import android.app.Activity;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.os.Bundle;

public class Tutorial extends Activity {
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.tutorial);
    WebView wv;
    wv = (WebView) findViewById(R.id.webview);
    WebSettings settings = wv.getSettings();
    settings.setDefaultTextEncodingName("utf-8");
    wv.loadUrl("file:///android_asset/tutorial/tutorial1.html"); 
  }
}
