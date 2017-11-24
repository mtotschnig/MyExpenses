package org.totschnig.myexpenses.sync;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link java.util.ServiceLoader} unfortunately does not work from sync process
 */
public class ServiceLoader {

  private static final String GOOGLE = "org.totschnig.myexpenses.sync.GoogleDriveBackendProviderFactory";
  private static final String LOCAL = "org.totschnig.myexpenses.sync.LocalFileBackendProviderFactory";

  public static List<SyncBackendProviderFactory> load(Context context) {
    List<SyncBackendProviderFactory> services = new ArrayList<>();
    tryToInstantiate(services, LOCAL, context);
    tryToInstantiate(services, GOOGLE, context);
    services.add(new WebDavBackendProviderFactory());
    services.add(new DropboxProviderFactory());
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
