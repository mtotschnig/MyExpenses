/*   This file is part of My Expenses.
 *   My Expenses is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 2 of the License, or
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
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.os.Bundle;

/**
 * opens a WebView with the tutorial stored in the assets folder
 * @author Michael Totschnig
 *
 */
public class Tutorial extends Activity {
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.tutorial);
    setTitle(getString(R.string.app_name) + " " + getString(R.string.tutorial));
    WebView wv;
    wv = (WebView) findViewById(R.id.webview);
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
    //we abuse user agent string, since the language is not reported correctly 
    String[] supportedLangs = {"en","fr","de"};
    String lang =  Locale.getDefault().getLanguage();
    if (!java.util.Arrays.asList(supportedLangs).contains(lang))
      lang = "en";
    wv.loadUrl("http://myexpenses.totschnig.org/tutorial/" + lang +  "/tutorial1.html"); 
  }
}
