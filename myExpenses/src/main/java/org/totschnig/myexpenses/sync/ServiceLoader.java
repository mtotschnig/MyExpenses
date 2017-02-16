package org.totschnig.myexpenses.sync;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link java.util.ServiceLoader} unfortunately does not work from sync process
 */
public class ServiceLoader {
  public static List<SyncBackendProviderFactory> load() {
    List<SyncBackendProviderFactory> services = new ArrayList<>();
    tryToInstantiate(services, "org.totschnig.myexpenses.sync.LocalFileBackendProviderFactory");
    tryToInstantiate(services, "org.totschnig.myexpenses.sync.GoogleDriveBackendProviderFactory");
    services.add(new WebDavBackendProviderFactory());
    return services;
  }

  private static void tryToInstantiate(List<SyncBackendProviderFactory> services, String className) {
    try {
      SyncBackendProviderFactory factory = (SyncBackendProviderFactory) Class.forName(className).newInstance();
      if (factory.isEnabled()) {
        services.add(factory);
      }
    } catch (InstantiationException | ClassNotFoundException | IllegalAccessException | ClassCastException ignored) {
    }
  }
}
