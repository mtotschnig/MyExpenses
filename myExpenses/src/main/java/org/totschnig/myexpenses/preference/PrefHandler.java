package org.totschnig.myexpenses.preference;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceFragmentCompat;

public interface PrefHandler {
  @NonNull
  String getKey(PrefKey key);

  @Nullable
  String getString(PrefKey key, String defValue);

  @Nullable
  String getString(String key, String defValue);

  void putString(PrefKey key, String value);

  void putString(String key, String value);

  boolean getBoolean(PrefKey key, boolean defValue);

  boolean getBoolean(String key, boolean defValue);

  void putBoolean(PrefKey key, boolean value);

  void putBoolean(String key, boolean value);

  int getInt(PrefKey key, int defValue);

  int getInt(String key, int defValue);

  void putInt(PrefKey key, int value);

  void putInt(String key, int value);

  long getLong(PrefKey key, long defValue);

  long getLong(String key, long defValue);

  void putLong(PrefKey key, long value);

  void putLong(String key, long value);

  void remove(PrefKey key);

  void remove(String key);

  boolean isSet(PrefKey key);

  boolean isSet(String key);

  boolean matches(@NonNull String key, PrefKey... prefKeys);

  void setDefaultValues(Context context);

  void preparePreferenceFragment(PreferenceFragmentCompat preferenceFragmentCompat);
}
