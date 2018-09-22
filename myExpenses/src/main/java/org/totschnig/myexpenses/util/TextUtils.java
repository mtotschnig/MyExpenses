package org.totschnig.myexpenses.util;

import android.content.Context;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Iterator;

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
}
