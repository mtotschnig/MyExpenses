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

package org.totschnig.myexpenses.activity;


import java.net.URI;
import java.util.Locale;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.dialog.DialogUtils;
import org.totschnig.myexpenses.dialog.DonateDialogFragment;
import org.totschnig.myexpenses.util.Utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.Intent.ShortcutIconResource;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceManager;
import android.provider.Settings.Secure;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Present references screen defined in Layout file
 * @author Michael Totschnig
 *
 */
public class MyPreferenceActivity extends ProtectedPreferenceActivity implements
    OnPreferenceChangeListener,
    OnSharedPreferenceChangeListener,
    OnPreferenceClickListener {
  
  private static final int RESTORE_REQUEST = 1;
  @SuppressWarnings("deprecation")
  @Override
  public void onCreate(Bundle savedInstanceState) {
    setTheme(MyApplication.getThemeId(Build.VERSION.SDK_INT < 11));
    super.onCreate(savedInstanceState);
    setTitle(getString(R.string.app_name) + " " + getString(R.string.menu_settings));
    addPreferencesFromResource(R.layout.preferences);
    Preference pref = findPreference(MyApplication.PREFKEY_SHARE_TARGET);
    pref.setSummary(getString(R.string.pref_share_target_summary) + ":\n" + 
        "ftp: \"ftp://login:password@my.example.org:port/my/directory/\"\n" +
        "mailto: \"mailto:john@my.example.com\"");
    pref.setOnPreferenceChangeListener(this);
    findPreference(MyApplication.PREFKEY_CONTRIB_DONATE)
       .setOnPreferenceClickListener(this);
    findPreference(MyApplication.PREFKEY_REQUEST_LICENCE)
      .setOnPreferenceClickListener(this);
    findPreference(MyApplication.PREFKEY_SEND_FEEDBACK)
      .setOnPreferenceClickListener(this);
    findPreference(MyApplication.PREFKEY_MORE_INFO_DIALOG)
      .setOnPreferenceClickListener(this);
    findPreference(MyApplication.PREFKEY_RESTORE)
      .setOnPreferenceClickListener(this);
    findPreference(MyApplication.PREFKEY_RATE)
    .setOnPreferenceClickListener(this);

    findPreference(MyApplication.PREFKEY_ENTER_LICENCE)
      .setOnPreferenceChangeListener(this);
    setProtectionDependentsState();

    findPreference(MyApplication.PREFKEY_PERFORM_PROTECTION)
      .setOnPreferenceChangeListener(this);
  }
  private void setProtectionDependentsState() {
    boolean isProtected = MyApplication.getInstance().getSettings().getBoolean(MyApplication.PREFKEY_PERFORM_PROTECTION, false);
    findPreference(MyApplication.PREFKEY_SECURITY_QUESTION).setEnabled( MyApplication.getInstance().isContribEnabled && isProtected);
    findPreference(MyApplication.PREFKEY_PROTECTION_DELAY_SECONDS).setEnabled(isProtected);
  }

  @Override
  protected void onResume() {
    super.onResume();
    PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);
  }
  @Override
  protected void onPause() {
    super.onPause();
    PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this);
  }
  @Override
  public boolean onPreferenceChange(Preference pref, Object value) {
    String key = pref.getKey();
    if (key.equals(MyApplication.PREFKEY_SHARE_TARGET)) {
      String target = (String) value;
      URI uri;
      if (!target.equals("")) {
        uri = Utils.validateUri(target);
        if (uri == null) {
          Toast.makeText(getBaseContext(),getString(R.string.ftp_uri_malformed,target), Toast.LENGTH_LONG).show();
          return false;
        }
        String scheme = uri.getScheme();
        if (!(scheme.equals("ftp") || scheme.equals("mailto"))) {
          Toast.makeText(getBaseContext(),getString(R.string.share_scheme_not_supported,scheme), Toast.LENGTH_LONG).show();
          return false;
        }
        Intent intent;
        if (scheme.equals("ftp")) {
          intent = new Intent(android.content.Intent.ACTION_SENDTO);
          intent.setData(android.net.Uri.parse(target));
          if (!Utils.isIntentAvailable(this,intent)) {
            showDialog(R.id.FTP_DIALOG);
          }
        }
      }
    } else if (key.equals(MyApplication.PREFKEY_ENTER_LICENCE)) {
     if (Utils.verifyLicenceKey((String)value)) {
       Toast.makeText(getBaseContext(), R.string.licence_validation_success, Toast.LENGTH_LONG).show();
       MyApplication.getInstance().isContribEnabled = true;
       setProtectionDependentsState();
     } else {
       Toast.makeText(getBaseContext(), R.string.licence_validation_failure, Toast.LENGTH_LONG).show();
       MyApplication.getInstance().isContribEnabled = false;
     }
    }
    return true;
  }
  private void restart() {
    Intent intent = getIntent();
    finish();
    startActivity(intent);
  }
  @Override
  protected Dialog onCreateDialog(int id) {
    switch(id) {
    case R.id.FTP_DIALOG:
      return DialogUtils.sendWithFTPDialog((Activity) this);
    case R.id.DONATE_DIALOG:
      return DonateDialogFragment.buildDialog(this);
    case R.id.MORE_INFO_DIALOG:
      LayoutInflater li = LayoutInflater.from(this);
      View view = li.inflate(R.layout.more_info, null);
      ((TextView)view.findViewById(R.id.aboutVersionCode)).setText(CommonCommands.getVersionInfo(this));
      return new AlertDialog.Builder(this)
        .setTitle(R.string.pref_more_info_dialog_title)
        .setView(view)
        .setPositiveButton(android.R.string.ok,null)
        .create();
    }
    return null;
  }
  @Override
  public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
      String key) {
    if (key.equals(MyApplication.PREFKEY_PERFORM_PROTECTION)) {
      setProtectionDependentsState();
    } else if (key.equals(MyApplication.PREFKEY_PROTECTION_DELAY_SECONDS)) {
      MyApplication.setPasswordCheckDelayNanoSeconds() ;
    } else if (key.equals(MyApplication.PREFKEY_UI_FONTSIZE) ||
        key.equals(MyApplication.PREFKEY_UI_LANGUAGE) ||
        key.equals(MyApplication.PREFKEY_UI_THEME_KEY)) {
      restart();
    }
  }
  @Override
  public boolean onPreferenceClick(Preference preference) {
    if (preference.getKey().equals(MyApplication.PREFKEY_CONTRIB_DONATE)) {
      Utils.contribBuyDo(MyPreferenceActivity.this);
      return true;
    }
    if (preference.getKey().equals(MyApplication.PREFKEY_REQUEST_LICENCE)) {
      String androidId = Secure.getString(getContentResolver(),Secure.ANDROID_ID);
      Intent i = new Intent(android.content.Intent.ACTION_SEND);
      i.setType("plain/text");
      i.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{ MyApplication.FEEDBACK_EMAIL });
      i.putExtra(android.content.Intent.EXTRA_SUBJECT,
          "[" + getString(R.string.app_name) + "] " + getString(R.string.contrib_key));
      i.putExtra(android.content.Intent.EXTRA_TEXT,
          getString(R.string.request_licence_mail_head,androidId)
          + " \n\n[" + getString(R.string.request_licence_mail_description) + "]");
      if (!Utils.isIntentAvailable(MyPreferenceActivity.this,i)) {
        Toast.makeText(getBaseContext(),R.string.no_app_handling_email_available, Toast.LENGTH_LONG).show();
      } else {
        startActivity(i);
      }
      return true;
    }
    if (preference.getKey().equals(MyApplication.PREFKEY_SEND_FEEDBACK)) {
      CommonCommands.dispatchCommand(this, R.id.FEEDBACK_COMMAND);
      return true;
    }
    if (preference.getKey().equals(MyApplication.PREFKEY_RATE)) {
      CommonCommands.dispatchCommand(this, R.id.RATE_COMMAND);
      return true;
    }
    if (preference.getKey().equals(MyApplication.PREFKEY_MORE_INFO_DIALOG)) {
      showDialog(R.id.MORE_INFO_DIALOG);
      return true;
    }
    if (preference.getKey().equals(MyApplication.PREFKEY_RESTORE)) {
      startActivityForResult(preference.getIntent(), RESTORE_REQUEST);
      return true;
    }
/*    if (preference.getKey().equals(MyApplication.PREFKEY_SHORTCUT_ACCOUNT_LIST)) {
      addShortcut(".activity.ManageAccounts",R.string.pref_manage_accounts_title, R.drawable.icon);
      Toast.makeText(getBaseContext(),getString(R.string.pref_shortcut_added), Toast.LENGTH_LONG).show();
      return true;
    }*/
    return false;
  }
  @Override
  protected void onActivityResult(int requestCode, int resultCode, 
      Intent intent) {
    if (requestCode == RESTORE_REQUEST && resultCode == RESULT_FIRST_USER) {
      setResult(resultCode);
      finish();
    }
  }

  // credits Financisto
  // src/ru/orangesoftware/financisto/activity/PreferencesActivity.java
  private void addShortcut(String activity, int nameId, int iconId) {
    Intent intent = createShortcutIntent(activity, getString(nameId), Intent.ShortcutIconResource.fromContext(this, iconId), 
        "com.android.launcher.action.INSTALL_SHORTCUT");
    sendBroadcast(intent);
}

  private Intent createShortcutIntent(String activity, String shortcutName, ShortcutIconResource shortcutIcon, String action) {
    Intent shortcutIntent = new Intent();
    shortcutIntent.setComponent(new ComponentName(this.getPackageName(), activity));
    shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    Intent intent = new Intent();
    intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
    intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, shortcutName);
    intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, shortcutIcon);
    intent.setAction(action);
    return intent;
  }
}