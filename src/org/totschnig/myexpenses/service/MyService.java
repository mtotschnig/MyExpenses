package org.totschnig.myexpenses.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.Messenger;
import android.util.Log;

public class MyService extends Service {

  /**
   * Target we publish for clients to send messages to IncomingHandler.
   */
  final Messenger mMessenger = new Messenger(new UnlockHandler());

  /**
   * When binding to the service, we return an interface to our messenger
   * for sending messages to the service.
   */
  @Override
  public IBinder onBind(Intent intent) {
    Log.i("DEBUG","binding");
    return mMessenger.getBinder();
  }
}
