package org.totschnig.myexpenses.util;

import android.content.Context;

import org.totschnig.myexpenses.model.CurrencyUnit;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Locale;

import androidx.annotation.Nullable;

public class TextUtils {
  public static <E extends Enum<E>> String joinEnum(Class<E> enumClass) {
    StringBuilder result = new StringBuilder();
    Iterator<E> iterator = EnumSet.allOf(enumClass).iterator();
    while (iterator.hasNext()) {
      result.append("'").append(iterator.next().name()).append("'");
      if (iterator.hasNext())
        result.append(",");
    }
    return result.toString();
  }

  public static String concatResStrings(Context ctx, String separator, Integer... resIds) {
    StringBuilder result = new StringBuilder();
    Iterator<Integer> itemIterator = Arrays.asList(resIds).iterator();
    if (itemIterator.hasNext()) {
      result.append(ctx.getString(itemIterator.next()));
      while (itemIterator.hasNext()) {
        result.append(separator).append(ctx.getString(itemIterator.next()));
      }
    }
    return result.toString();
  }

  public static String appendCurrencySymbol(Context context, int resId, CurrencyUnit currency) {
    return appendText(context, resId, currency.getSymbol());
  }

  public static String appendCurrencyDescription(Context context, int resId, CurrencyUnit currency) {
    return appendText(context, resId, currency.getDescription());
  }

  public static String appendText(Context context, int resId, String symbol) {
    return String.format(Locale.ROOT, "%s (%s)", context.getString(resId), symbol);
  }

  public static String formatQifCategory(String mainLabel, String subLabel) {
    mainLabel = sanitizeQifCategory(mainLabel);
    subLabel = sanitizeQifCategory(subLabel);
    return android.text.TextUtils.isEmpty(subLabel) ? mainLabel : String.format("%s:%s", mainLabel, subLabel);
  }

  private static String sanitizeQifCategory(@Nullable String label) {
    if (label == null) {
      return null;
    }
    final char substitute = '|';
    return label.replace('/', substitute).replace(':', substitute);
  }
}
