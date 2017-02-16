package org.totschnig.myexpenses.sync;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link java.util.ServiceLoader} unfortunately does not work from sync process
 */
public class ServiceLoader {
  public static List<SyncBackendProviderFactory> load(Context context) {
    List<SyncBackendProviderFactory> services = new ArrayList<>();
    tryToInstantiate(services, "org.totschnig.myexpenses.sync.LocalFileBackendProviderFactory", context);
    tryToInstantiate(services, "org.totschnig.myexpenses.sync.GoogleDriveBackendProviderFactory", context);
    services.add(new WebDavBackendProviderFactory());
    return services;
  }

  private static void tryToInstantiate(List<SyncBackendProviderFactory> services, String className, Context context) {
    try {
      SyncBackendProviderFactory factory = (SyncBackendProviderFactory) Class.forName(className).newInstance();
      if (factory.isEnabled(context)) {
        services.add(factory);
      }
    } catch (InstantiationException | ClassNotFoundException | IllegalAccessException | ClassCastException ignored) {
    }
  }
}
