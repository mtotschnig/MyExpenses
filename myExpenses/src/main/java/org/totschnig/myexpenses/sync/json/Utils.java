package org.totschnig.myexpenses.sync.json;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;

import java.io.Reader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class Utils {
  public static List<TransactionChange> getChanges(Gson gson, Reader reader) {
    Type listType = new TypeToken<ArrayList<TransactionChange>>(){}.getType();
    return gson.fromJson(reader, listType);
  }
}
