package org.totschnig.myexpenses.sync.json;

import com.google.gson.TypeAdapterFactory;
import com.ryanharter.auto.value.gson.GsonTypeAdapterFactory;

@GsonTypeAdapterFactory
public abstract class AdapterFactory implements TypeAdapterFactory {

  public static AdapterFactory create() {
    return new AutoValueGson_AdapterFactory();
  }
}