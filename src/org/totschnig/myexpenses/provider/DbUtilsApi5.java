package org.totschnig.myexpenses.provider;

import org.totschnig.myexpenses.MyApplication;
import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.ContentProviderClient;
import android.os.Build;



public class DbUtilsApi5 {
  @TargetApi(Build.VERSION_CODES.ECLAIR)
  public static void postRestore() {
    MyApplication app = MyApplication.getInstance();
    ContentResolver resolver = app.getContentResolver();
    ContentProviderClient client = resolver.acquireContentProviderClient(TransactionProvider.AUTHORITY);
    TransactionProvider provider = (TransactionProvider) client.getLocalContentProvider();
    provider.resetDatabase();
    client.release();
  }
}
