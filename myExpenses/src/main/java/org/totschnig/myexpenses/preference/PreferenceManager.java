package org.totschnig.myexpenses.preference;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by elena on 27/09/16.
 */
public class PreferenceManager {
    int MODE = 0;
    Context _context;
    SharedPreferences pref;
    SharedPreferences.Editor editor;

    private static final String FIRST_LAUNCH = "FirstLaunch";
    private static final String PREFERENCE_NAME = "androidhive-welcome";

    public PreferenceManager(Context context) {
        this._context = context;
        pref = _context.getSharedPreferences(PREFERENCE_NAME, MODE);
        editor = pref.edit();
    }

    public boolean isFirstTimeLaunch() {
        return pref.getBoolean(FIRST_LAUNCH, true);
    }

    public void setFirstTimeLaunch(boolean isFirstTime) {
        editor.putBoolean(FIRST_LAUNCH, isFirstTime);
        editor.commit();
    }
}
