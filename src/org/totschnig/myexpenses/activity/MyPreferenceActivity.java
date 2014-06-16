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


import java.io.File;
import java.net.URI;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.dialog.DialogUtils;
import org.totschnig.myexpenses.dialog.DonateDialogFragment;
import org.totschnig.myexpenses.preference.SharedPreferencesCompat;
import org.totschnig.myexpenses.preference.CalendarListPreference;
import org.totschnig.myexpenses.util.Utils;
import org.totschnig.myexpenses.widget.AbstractWidget;
import org.totschnig.myexpenses.widget.AccountWidget;
import org.totschnig.myexpenses.widget.TemplateWidget;

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
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.provider.Settings.Secure;
import android.text.TextUtils;
import android.util.Log;
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
  private static final int PICK_FOLDER_REQUEST = 1;
  public static final String KEY_OPEN_PREF_KEY = "openPrefKey";
  @SuppressWarnings("deprecation")
  @Override
  public void onCreate(Bundle savedInstanceState) {
    setTheme(MyApplication.getThemeId(Build.VERSION.SDK_INT < 11));
    super.onCreate(savedInstanceState);
    setTitle(Utils.concatResStrings(this,R.string.app_name,R.string.menu_settings));
    addPreferencesFromResource(R.layout.preferences);
    Preference pref = findPreference(MyApplication.PREFKEY_SHARE_TARGET);
    pref.setSummary(getString(R.string.pref_share_target_summary) + ":\n" + 
        "ftp: \"ftp://login:password@my.example.org:port/my/directory/\"\n" +
        "mailto: \"mailto:john@my.example.com\"");
    pref.setOnPreferenceChangeListener(this);
    configureContribPrefs();
    findPreference(MyApplication.PREFKEY_SEND_FEEDBACK)
      .setOnPreferenceClickListener(this);
    findPreference(MyApplication.PREFKEY_MORE_INFO_DIALOG)
      .setOnPreferenceClickListener(this);

    pref = findPreference(MyApplication.PREFKEY_RESTORE);
    pref.setTitle(getString(R.string.pref_restore_title) + " (ZIP)");
    pref.setOnPreferenceClickListener(this);

    pref = findPreference(MyApplication.PREFKEY_RESTORE_LEGACY);
    pref.setTitle(getString(R.string.pref_restore_title) + " (" + getString(R.string.pref_restore_legacy_data) + ")");
    pref.setOnPreferenceClickListener(this);

    findPreference(MyApplication.PREFKEY_RATE)
    .setOnPreferenceClickListener(this);

    findPreference(MyApplication.PREFKEY_ENTER_LICENCE)
      .setOnPreferenceChangeListener(this);
    setProtectionDependentsState();

    findPreference(MyApplication.PREFKEY_PERFORM_PROTECTION)
      .setOnPreferenceChangeListener(this);
    
    findPreference(MyApplication.PREFKEY_PROTECTION_ENABLE_ACCOUNT_WIDGET)
    .setOnPreferenceChangeListener(this);
    
    findPreference(MyApplication.PREFKEY_PROTECTION_ENABLE_TEMPLATE_WIDGET)
    .setOnPreferenceChangeListener(this);
    
    findPreference(MyApplication.PREFKEY_APP_DIR)
    .setOnPreferenceClickListener(this);
    setAppDirSummary();
    if (savedInstanceState == null &&
        TextUtils.equals(
            getIntent().getStringExtra(KEY_OPEN_PREF_KEY),
            MyApplication.PREFKEY_PLANNER_CALENDAR_ID)) {
      ((CalendarListPreference) findPreference(MyApplication.PREFKEY_PLANNER_CALENDAR_ID)).show();
    }
    
    findPreference(MyApplication.PREFKEY_SHORTCUT_CREATE_TRANSACTION).setOnPreferenceClickListener(this);
    findPreference(MyApplication.PREFKEY_SHORTCUT_CREATE_TRANSFER).setOnPreferenceClickListener(this);
    findPreference(MyApplication.PREFKEY_SHORTCUT_CREATE_SPLIT).setOnPreferenceClickListener(this);
    findPreference(MyApplication.PREFKEY_SECURITY_QUESTION).setSummary(
        Utils.concatResStrings(this, R.string.pref_security_question_summary,R.string.contrib_key_requires));
    findPreference(MyApplication.PREFKEY_SHORTCUT_CREATE_SPLIT).setSummary(
        Utils.concatResStrings(this, R.string.pref_shortcut_summary,R.string.contrib_key_requires));
  }
  private void configureContribPrefs() {
    Preference pref1 = findPreference(MyApplication.PREFKEY_REQUEST_LICENCE),
        pref2 = findPreference(MyApplication.PREFKEY_CONTRIB_DONATE);
    if (MyApplication.getInstance().isContribEnabled) {
      ((PreferenceCategory) findPreference(MyApplication.PREFKEY_CATEGORY_CONTRIB)).removePreference(pref1);
      pref2.setSummary(Utils.concatResStrings(this, R.string.thank_you, R.string.pref_contrib_donate_summary_already_contrib));
    } else {
      pref1.setOnPreferenceClickListener(this);
      pref1.setSummary(getString(R.string.pref_request_licence_summary,Secure.getString(getContentResolver(),Secure.ANDROID_ID)));
      pref2.setSummary(R.string.pref_contrib_donate_summary);
    }
    pref2.setOnPreferenceClickListener(this);
    findPreference(MyApplication.PREFKEY_SHORTCUT_CREATE_SPLIT).setEnabled(MyApplication.getInstance().isContribEnabled);
  }
  private void setProtectionDependentsState() {
    boolean isProtected = MyApplication.getInstance().getSettings().getBoolean(MyApplication.PREFKEY_PERFORM_PROTECTION, false);
    findPreference(MyApplication.PREFKEY_SECURITY_QUESTION).setEnabled( MyApplication.getInstance().isContribEnabled && isProtected);
    findPreference(MyApplication.PREFKEY_PROTECTION_DELAY_SECONDS).setEnabled(isProtected);
    findPreference(MyApplication.PREFKEY_PROTECTION_ENABLE_ACCOUNT_WIDGET).setEnabled(isProtected);
    findPreference(MyApplication.PREFKEY_PROTECTION_ENABLE_TEMPLATE_WIDGET).setEnabled(isProtected);
    findPreference(MyApplication.PREFKEY_PROTECTION_ENABLE_DATA_ENTRY_FROM_WIDGET).setEnabled(isProtected);
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
     configureContribPrefs();
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
      AbstractWidget.updateWidgets(this, AccountWidget.class);
      AbstractWidget.updateWidgets(this, TemplateWidget.class);
    } else if (key.equals(MyApplication.PREFKEY_PROTECTION_DELAY_SECONDS)) {
      MyApplication.setPasswordCheckDelayNanoSeconds() ;
    } else if (key.equals(MyApplication.PREFKEY_UI_FONTSIZE) ||
        key.equals(MyApplication.PREFKEY_UI_LANGUAGE) ||
        key.equals(MyApplication.PREFKEY_UI_THEME_KEY)) {
      restart();
    } else if (key.equals(MyApplication.PREFKEY_PROTECTION_ENABLE_ACCOUNT_WIDGET)) {
      Log.d("DEBUG","shared preference changed: Account Widget");
      AbstractWidget.updateWidgets(this, AccountWidget.class);
    } else if (key.equals(MyApplication.PREFKEY_PROTECTION_ENABLE_TEMPLATE_WIDGET)) {
      Log.d("DEBUG","shared preference changed: Template Widget");
      AbstractWidget.updateWidgets(this, TemplateWidget.class);
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
      CommonCommands.dispatchCommand(this, R.id.FEEDBACK_COMMAND, null);
      return true;
    }
    if (preference.getKey().equals(MyApplication.PREFKEY_RATE)) {
      CommonCommands.dispatchCommand(this, R.id.RATE_COMMAND, null);
      return true;
    }
    if (preference.getKey().equals(MyApplication.PREFKEY_MORE_INFO_DIALOG)) {
      showDialog(R.id.MORE_INFO_DIALOG);
      return true;
    }
    if (preference.getKey().equals(MyApplication.PREFKEY_RESTORE) ||
        preference.getKey().equals(MyApplication.PREFKEY_RESTORE_LEGACY)) {
      startActivityForResult(preference.getIntent(), RESTORE_REQUEST);
      return true;
    }
    if (preference.getKey().equals(MyApplication.PREFKEY_APP_DIR)) {
      File appDir = Utils.requireAppDir();
      Preference pref = findPreference(MyApplication.PREFKEY_APP_DIR);
      if (appDir == null) {
        pref.setSummary(R.string.external_storage_unavailable);
        pref.setEnabled(false);
      } else {
        Intent intent = new Intent(this, FolderBrowser.class);
        intent.putExtra(FolderBrowser.PATH, appDir.getPath());
        startActivityForResult(intent,PICK_FOLDER_REQUEST);
      }
      return true;
    }
    if (preference.getKey().equals(MyApplication.PREFKEY_SHORTCUT_CREATE_TRANSACTION)) {
      Bundle extras = new Bundle();
      extras.putBoolean(AbstractWidget.EXTRA_START_FROM_WIDGET, true);
      extras.putBoolean(AbstractWidget.EXTRA_START_FROM_WIDGET_DATA_ENTRY, true);
      addShortcut(".activity.ExpenseEdit",R.string.transaction, R.drawable.shortcut_create_transaction_icon,extras);
      return true;
    }
    if (preference.getKey().equals(MyApplication.PREFKEY_SHORTCUT_CREATE_TRANSFER)) {
      Bundle extras = new Bundle();
      extras.putBoolean(AbstractWidget.EXTRA_START_FROM_WIDGET, true);
      extras.putBoolean(AbstractWidget.EXTRA_START_FROM_WIDGET_DATA_ENTRY, true);
      extras.putInt(MyApplication.KEY_OPERATION_TYPE, MyExpenses.TYPE_TRANSFER);
      addShortcut(".activity.ExpenseEdit",R.string.transfer, R.drawable.shortcut_create_transfer_icon,extras);
      return true;
    }
    if (preference.getKey().equals(MyApplication.PREFKEY_SHORTCUT_CREATE_SPLIT)) {
      Bundle extras = new Bundle();
      extras.putBoolean(AbstractWidget.EXTRA_START_FROM_WIDGET, true);
      extras.putBoolean(AbstractWidget.EXTRA_START_FROM_WIDGET_DATA_ENTRY, true);
      extras.putInt(MyApplication.KEY_OPERATION_TYPE, MyExpenses.TYPE_SPLIT);
      addShortcut(".activity.ExpenseEdit",R.string.split_transaction, R.drawable.shortcut_create_split_icon,extras);
      return true;
    }
    return false;
  }
  @Override
  protected void onActivityResult(int requestCode, int resultCode, 
      Intent intent) {
    if (requestCode == RESTORE_REQUEST && resultCode == RESULT_FIRST_USER) {
      setResult(resultCode);
      finish();
    } else if (requestCode == PICK_FOLDER_REQUEST) {
      if (resultCode == RESULT_OK) {
        String databaseBackupFolder = intent.getStringExtra(FolderBrowser.PATH);
        SharedPreferencesCompat.apply(
            MyApplication.getInstance().getSettings().edit()
            .putString(MyApplication.PREFKEY_APP_DIR, databaseBackupFolder));
      }
      setAppDirSummary();
    }
  }
  private void setAppDirSummary() {
    File appDir = Utils.requireAppDir();
    Preference pref = findPreference(MyApplication.PREFKEY_APP_DIR);
    if (appDir == null) {
      pref.setSummary(R.string.external_storage_unavailable);
      pref.setEnabled(false);
    } else {
      pref.setSummary(appDir.getPath());
    }
  }

  // credits Financisto
  // src/ru/orangesoftware/financisto/activity/PreferencesActivity.java
  private void addShortcut(String activity, int nameId, int iconId, Bundle extra) {
    Intent shortcutIntent = createShortcutIntent(activity);
    if (extra != null) {
      shortcutIntent.putExtras(extra);
    }
    Intent intent = new Intent();
    intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
    intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, getString(nameId));
    intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource.fromContext(this, iconId));
    intent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");

    if (Utils.isIntentReceiverAvailable(this, intent)) {
      sendBroadcast(intent);
      Toast.makeText(getBaseContext(),getString(R.string.pref_shortcut_added), Toast.LENGTH_LONG).show();
    } else {
      Toast.makeText(getBaseContext(),getString(R.string.pref_shortcut_not_added), Toast.LENGTH_LONG).show();
    }
}

  private Intent createShortcutIntent(String activity) {
    Intent shortcutIntent = new Intent();
    shortcutIntent.setComponent(new ComponentName(this.getPackageName(), activity));
    shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    return shortcutIntent;
  }
  private Intent findDirPicker() {
    Intent intent = new Intent("com.estrongs.action.PICK_DIRECTORY ");
    intent.putExtra("com.estrongs.intent.extra.TITLE", "Select Directory");
    if (Utils.isIntentAvailable(this, intent)) {
      return intent;
    }
    return null;
  }
  public void onCalendarListPreferenceSet() {
    if (TextUtils.equals(
        getIntent().getStringExtra(KEY_OPEN_PREF_KEY),
        MyApplication.PREFKEY_PLANNER_CALENDAR_ID)) {
      setResult(RESULT_OK);
      finish();
    }
  }
}