package org.totschnig.myexpenses.preference;

import org.totschnig.myexpenses.MyApplication;

public class PrefHandlerImpl implements PrefHandler {
  private MyApplication context;

  public PrefHandlerImpl(MyApplication context) {
    this.context = context;
  }

  @Override
  public String getKey(PrefKey key) {
    return key.resId == 0 ? key.key : context.getString(key.resId);
  }

  @Override
  public String getString(PrefKey key, String defValue) {
    return context.getSettings().getString(getKey(key), defValue);
  }

  @Override
  public void putString(PrefKey key, String value) {
    context.getSettings().edit().putString(getKey(key), value).apply();
  }

  @Override
  public boolean getBoolean(PrefKey key, boolean defValue) {
    return context.getSettings().getBoolean(getKey(key), defValue);
  }

  @Override
  public void putBoolean(PrefKey key, boolean value) {
    context.getSettings().edit().putBoolean(getKey(key), value).apply();
  }

  @Override
  public int getInt(PrefKey key, int defValue) {
    return context.getSettings().getInt(getKey(key), defValue);
  }

  @Override
  public void putInt(PrefKey key, int value) {
    context.getSettings().edit().putInt(getKey(key), value).apply();
  }

  @Override
  public long getLong(PrefKey key, long defValue) {
    return context.getSettings().getLong(getKey(key), defValue);
  }

  @Override
  public void putLong(PrefKey key, long value) {
    context.getSettings().edit().putLong(getKey(key), value).apply();
  }

  @Override
  public void remove(PrefKey key) {
    context.getSettings().edit().remove(getKey(key)).apply();
  }

  @Override
  public boolean isSet(PrefKey key) {
    return context.getSettings().contains(getKey(key));
  }
}
