package org.totschnig.myexpenses.util;

import java.util.EnumSet;
import java.util.Iterator;

public class TextUtils {
  public static <E extends Enum<E>> String joinEnum(Class<E> enumClass) {
    String result = "";
    Iterator<E> iterator = EnumSet.allOf(enumClass).iterator();
    while (iterator.hasNext()) {
      result += "'" + iterator.next().name() + "'";
      if (iterator.hasNext())
        result += ",";
    }
    return result;
  }
}
