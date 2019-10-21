package org.totschnig.myexpenses.ui;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.LocaleList;

import org.totschnig.myexpenses.util.Utils;

import java.util.Locale;

//https://stackoverflow.com/a/40849142/1199911
public class ContextHelper {

  private ContextHelper() { }

  public static Context wrap(Context context, Locale newLocale) {

    Resources res = context.getResources();
    Configuration configuration = res.getConfiguration();

    if (Utils.hasApiLevel(24)) {
      context = buildContext24(context, newLocale, configuration);

    } else if (Utils.hasApiLevel(17)) {
      context = buildContext17(context, newLocale, configuration);

    } else {
      configuration.locale = newLocale;
      res.updateConfiguration(configuration, res.getDisplayMetrics());
    }

    return context;
  }

  @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
  private static Context buildContext17(Context context, Locale newLocale, Configuration configuration) {
    configuration.setLocale(newLocale);
    context = context.createConfigurationContext(configuration);
    return context;
  }

  @TargetApi(Build.VERSION_CODES.N)
  private static Context buildContext24(Context context, Locale newLocale, Configuration configuration) {
    LocaleList localeList = new LocaleList(newLocale);
    LocaleList.setDefault(localeList);
    configuration.setLocales(localeList);

    context = context.createConfigurationContext(configuration);
    return context;
  }
}