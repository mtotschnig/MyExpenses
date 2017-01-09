package org.totschnig.myexpenses.sync;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link java.util.ServiceLoader} unfortunately does not work from sync process
 */
public class ServiceLoader {
  public static List<SyncBackendProviderFactory> load() {
    List<SyncBackendProviderFactory> services = new ArrayList<>();
    try {
      services.add((SyncBackendProviderFactory) Class.forName("org.totschnig.myexpenses.sync.LocalFileBackendProviderFactory").newInstance());
    } catch (InstantiationException | ClassNotFoundException | IllegalAccessException | ClassCastException e) {
    }
    services.add(new WebDavBackendProviderFactory());
    return services;
  }
}
