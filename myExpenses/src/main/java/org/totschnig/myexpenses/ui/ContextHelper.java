package org.totschnig.myexpenses.ui;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.LocaleList;

import java.util.Locale;

//https://stackoverflow.com/a/40849142/1199911
public class ContextHelper {

  private ContextHelper() { }

  public static Context wrap(Context context, Locale newLocale) {

    Resources res = context.getResources();
    if (res != null) {
      Configuration configuration = res.getConfiguration();

      if (Build.VERSION.SDK_INT >= 24) {
        context = buildContext24(context, newLocale, configuration);

      } else {
        context = buildContext17(context, newLocale, configuration);
      }
    }
    return context;
  }

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