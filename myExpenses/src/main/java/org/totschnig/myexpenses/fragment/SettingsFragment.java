package org.totschnig.myexpenses.fragment;

import static org.totschnig.myexpenses.preference.PrefKey.AUTO_BACKUP;
import static org.totschnig.myexpenses.preference.PrefKey.AUTO_FILL_SWITCH;
import static org.totschnig.myexpenses.preference.PrefKey.OPTIMIZE_PICTURE;
import static org.totschnig.myexpenses.preference.PrefKey.PERFORM_SHARE;
import static org.totschnig.myexpenses.preference.PrefKey.PLANNER_CALENDAR_ID;
import static org.totschnig.myexpenses.preference.PrefKey.PURGE_BACKUP;
import static org.totschnig.myexpenses.preference.PrefKey.ROOT_SCREEN;
import static org.totschnig.myexpenses.preference.PrefKey.SECURITY_QUESTION;
import static org.totschnig.myexpenses.preference.PrefKey.UI_WEB;
import static org.totschnig.myexpenses.util.PermissionHelper.PermissionGroup.CALENDAR;
import static org.totschnig.myexpenses.util.TextUtils.concatResStrings;

import android.os.Bundle;

import androidx.appcompat.app.ActionBar;
import androidx.fragment.app.DialogFragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.MyPreferenceActivity;
import org.totschnig.myexpenses.di.AppComponent;
import org.totschnig.myexpenses.preference.CalendarListPreferenceDialogFragmentCompat;
import org.totschnig.myexpenses.preference.FontSizeDialogFragmentCompat;
import org.totschnig.myexpenses.preference.FontSizeDialogPreference;
import org.totschnig.myexpenses.preference.SecurityQuestionDialogFragmentCompat;
import org.totschnig.myexpenses.preference.SimplePasswordDialogFragmentCompat;
import org.totschnig.myexpenses.preference.SimplePasswordPreference;
import org.totschnig.myexpenses.preference.TimePreference;
import org.totschnig.myexpenses.preference.TimePreferenceDialogFragmentCompat;

public class SettingsFragment extends BaseSettingsFragment {

  @Override
  public void onCreate(Bundle savedInstanceState) {
    final AppComponent appComponent = requireApplication().getAppComponent();
    appComponent.inject(this);
    super.onCreate(savedInstanceState);
    prefHandler.preparePreferenceFragment(this);
  }
}
