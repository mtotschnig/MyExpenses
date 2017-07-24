package org.totschnig.myexpenses.ui;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.LocaleList;

import org.totschnig.myexpenses.util.Utils;

import java.util.Locale;

//https://stackoverflow.com/a/40849142/1199911
public class ContextWrapper extends android.content.ContextWrapper {

  public ContextWrapper(Context base) {
    super(base);
  }

  public static ContextWrapper wrap(Context context, Locale newLocale) {

    Resources res = context.getResources();
    Configuration configuration = res.getConfiguration();

    if (Utils.hasApiLevel(24)) {
      configuration.setLocale(newLocale);

      LocaleList localeList = new LocaleList(newLocale);
      LocaleList.setDefault(localeList);
      configuration.setLocales(localeList);

      context = context.createConfigurationContext(configuration);

    } else if (Utils.hasApiLevel(17)) {
      configuration.setLocale(newLocale);
      context = context.createConfigurationContext(configuration);

    } else {
      configuration.locale = newLocale;
      res.updateConfiguration(configuration, res.getDisplayMetrics());
    }

    return new ContextWrapper(context);
  }}