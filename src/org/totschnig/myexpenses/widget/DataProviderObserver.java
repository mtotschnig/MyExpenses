package org.totschnig.myexpenses.widget;

import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.util.Log;

class DataProviderObserver extends ContentObserver {
  /**
   * 
   */
  private Context mContext;
  private Class<? extends AbstractWidget<?>> mProvider;

  DataProviderObserver(Context ctx, Handler h, Class<? extends AbstractWidget<?>> provider) {
      super(h);
      mContext = ctx;
      mProvider = provider;
  }

  @Override
  public void onChange(boolean selfChange) {
      super.onChange(selfChange);
      Log.d("DEBUG", "Data Changed");
      Log.d("DEBUG", "creating intent for class "+ mProvider);
      AbstractWidget.updateWidgets(mContext,mProvider);
  }
}