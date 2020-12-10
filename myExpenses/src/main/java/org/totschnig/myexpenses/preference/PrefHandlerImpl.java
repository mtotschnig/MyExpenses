package org.totschnig.myexpenses.preference;

import org.totschnig.myexpenses.MyApplication;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class PrefHandlerImpl implements PrefHandler {
  private final MyApplication context;

  public PrefHandlerImpl(MyApplication context) {
    this.context = context;
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
    return context.getSettings().getString(key, defValue);
  }

  @Override
  public void putString(PrefKey key, String value) {
    putString(getKey(key), value);
  }

  @Override
  public void putString(String key, String value) {
    context.getSettings().edit().putString(key, value).apply();
  }

  @Override
  public boolean getBoolean(PrefKey key, boolean defValue) {
    return getBoolean(getKey(key), defValue);
  }

  @Override
  public boolean getBoolean(String key, boolean defValue) {
    return context.getSettings().getBoolean(key, defValue);
  }

  @Override
  public void putBoolean(PrefKey key, boolean value) {
    putBoolean(getKey(key), value);
  }

  @Override
  public void putBoolean(String key, boolean value) {
    context.getSettings().edit().putBoolean(key, value).apply();
  }

  @Override
  public int getInt(PrefKey key, int defValue) {
    return getInt(getKey(key), defValue);
  }

  @Override
  public int getInt(String key, int defValue) {
    return context.getSettings().getInt(key, defValue);
  }

  @Override
  public void putInt(PrefKey key, int value) {
    putInt(getKey(key), value);
  }

  @Override
  public void putInt(String key, int value) {
    context.getSettings().edit().putInt(key, value).apply();
  }

  @Override
  public long getLong(PrefKey key, long defValue) {
    return getLong(getKey(key), defValue);
  }

  @Override
  public long getLong(String key, long defValue) {
    return context.getSettings().getLong(key, defValue);
  }

  @Override
  public void putLong(PrefKey key, long value) {
    putLong(getKey(key), value);
  }

  @Override
  public void putLong(String key, long value) {
    context.getSettings().edit().putLong(key, value).apply();
  }

  @Override
  public void remove(PrefKey key) {
    remove(getKey(key));
  }

  @Override
  public void remove(String key) {
    context.getSettings().edit().remove(key).apply();
  }

  @Override
  public boolean isSet(PrefKey key) {
    return isSet(getKey(key));
  }

  @Override
  public boolean isSet(String key) {
    return context.getSettings().contains(key);
  }

  @Override
  public boolean matches(@NonNull String key, PrefKey... prefKeys) {
    for (PrefKey prefKey: prefKeys) {
      if (key.equals(getKey(prefKey)))
        return true;
    }
    return false;
  }
}
