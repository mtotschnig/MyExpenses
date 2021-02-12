package org.totschnig.myexpenses.preference;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import org.totschnig.myexpenses.R;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

public class PrefHandlerImpl implements PrefHandler {
  private final Application context;
  private final SharedPreferences sharedPreferences;

  public PrefHandlerImpl(Application context, SharedPreferences sharedPreferences) {
    this.context = context;
    this.sharedPreferences = sharedPreferences;
  }

  @NonNull
  @Override
  public String getKey(PrefKey key) {
    return key.resId == 0 ? key.key : context.getString(key.resId);
  }

  @Override
  @Nullable
  public String getString(PrefKey key, String defValue) {
    return getString(getKey(key), defValue);
  }

  @Override
  @Nullable
  public String getString(String key, String defValue) {
    return sharedPreferences.getString(key, defValue);
  }

  @Override
  public void putString(PrefKey key, String value) {
    putString(getKey(key), value);
  }

  @Override
  public void putString(String key, String value) {
    sharedPreferences.edit().putString(key, value).apply();
  }

  @Override
  public boolean getBoolean(PrefKey key, boolean defValue) {
    return getBoolean(getKey(key), defValue);
  }

  @Override
  public boolean getBoolean(String key, boolean defValue) {
    return sharedPreferences.getBoolean(key, defValue);
  }

  @Override
  public void putBoolean(PrefKey key, boolean value) {
    putBoolean(getKey(key), value);
  }

  @Override
  public void putBoolean(String key, boolean value) {
    sharedPreferences.edit().putBoolean(key, value).apply();
  }

  @Override
  public int getInt(PrefKey key, int defValue) {
    return getInt(getKey(key), defValue);
  }

  @Override
  public int getInt(String key, int defValue) {
    return sharedPreferences.getInt(key, defValue);
  }

  @Override
  public void putInt(PrefKey key, int value) {
    putInt(getKey(key), value);
  }

  @Override
  public void putInt(String key, int value) {
    sharedPreferences.edit().putInt(key, value).apply();
  }

  @Override
  public long getLong(PrefKey key, long defValue) {
    return getLong(getKey(key), defValue);
  }

  @Override
  public long getLong(String key, long defValue) {
    return sharedPreferences.getLong(key, defValue);
  }

  @Override
  public void putLong(PrefKey key, long value) {
    putLong(getKey(key), value);
  }

  @Override
  public void putLong(String key, long value) {
    sharedPreferences.edit().putLong(key, value).apply();
  }

  @Override
  public void remove(PrefKey key) {
    remove(getKey(key));
  }

  @Override
  public void remove(String key) {
    sharedPreferences.edit().remove(key).apply();
  }

  @Override
  public boolean isSet(PrefKey key) {
    return isSet(getKey(key));
  }

  @Override
  public boolean isSet(String key) {
    return sharedPreferences.contains(key);
  }

  @Override
  public boolean matches(@NonNull String key, PrefKey... prefKeys) {
    for (PrefKey prefKey: prefKeys) {
      if (key.equals(getKey(prefKey)))
        return true;
    }
    return false;
  }

  @Override
  public void setDefaultValues(Context context) {
    PreferenceManager.setDefaultValues(context, R.xml.preferences, false);
  }

  @Override
  public void preparePreferenceFragment(PreferenceFragmentCompat preferenceFragmentCompat) {
    //NOOP overriden in test
  }
}
