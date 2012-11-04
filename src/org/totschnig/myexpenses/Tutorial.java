/*   This file is part of My Expenses.
 *   My Expenses is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   My Expenses is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with My Expenses.  If not, see <http://www.gnu.org/licenses/>.
*/

package org.totschnig.myexpenses;

import java.util.Locale;

import android.app.Activity;
//import android.util.Log;
//import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebViewClient;
import android.os.Bundle;

/**
 * opens a WebView with the tutorial stored in the assets folder
 * @author Michael Totschnig
 *
 */
public class Tutorial extends Activity {
  static final int TUTORIAL_RELEASE_VERSION = 3;
  private String startWith;
  
  protected android.webkit.WebView wv;
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.tutorial);
    setTitle(getString(R.string.app_name) + " " + getString(R.string.tutorial));
    Bundle extras = getIntent().getExtras();
    String[] supportedLangs = {"en","fr","de","it","es"};
    String lang =  Locale.getDefault().getLanguage();
    if (!java.util.Arrays.asList(supportedLangs).contains(lang)) {
      lang = "en";
    }
    switch (extras.getInt("start")) {
      case R.id.FAQ_COMMAND:
        startWith = "tutorial_r" +  TUTORIAL_RELEASE_VERSION + "/" + lang +  "/faq.html";
        break;
      case R.id.CHANGES_COMMAND:
        startWith = "versionlist.html";
        break;
      default:
        startWith = "tutorial_r" +  TUTORIAL_RELEASE_VERSION + "/" + lang +  "/introduction.html";
    }
    wv = (android.webkit.WebView) findViewById(R.id.webview);
    wv.setWebViewClient(new WebViewClient());
/*    wv.setWebChromeClient(new WebChromeClient() {
      public void onConsoleMessage(String message, int lineNumber, String sourceID) {
        Log.d("MyExpenses", message + " -- From line "
                             + lineNumber + " of "
                             + sourceID);
      }
    });*/
    WebSettings settings = wv.getSettings();
    settings.setDefaultTextEncodingName("utf-8");
    //settings.setJavaScriptEnabled(true);
    settings.setBuiltInZoomControls(true); 
    wv.loadUrl("http://myexpenses.totschnig.org/" + startWith); 
  }
  @Override
  protected void onSaveInstanceState(Bundle outState)
  {
    super.onSaveInstanceState(outState);

    // Save the state of the WebView
    wv.saveState(outState);
  }

  @Override
  protected void onRestoreInstanceState(Bundle savedInstanceState)
  {
    super.onRestoreInstanceState(savedInstanceState);

    // Restore the state of the WebView
    wv.restoreState(savedInstanceState);
  }
}
