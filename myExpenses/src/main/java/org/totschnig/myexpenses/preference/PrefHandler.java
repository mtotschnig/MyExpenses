package org.totschnig.myexpenses.preference;

import android.support.annotation.NonNull;

public interface PrefHandler {
  String getKey(PrefKey key);

  String getString(PrefKey key, String defValue);

  void putString(PrefKey key, String value);

  boolean getBoolean(PrefKey key, boolean defValue);

  void putBoolean(PrefKey key, boolean value);

  int getInt(PrefKey key, int defValue);

  void putInt(PrefKey key, int value);

  long getLong(PrefKey key, long defValue);

  void putLong(PrefKey key, long value);

  void remove(PrefKey key);

  boolean isSet(PrefKey key);

  boolean matches(@NonNull String key, PrefKey... prefKeys);
}
