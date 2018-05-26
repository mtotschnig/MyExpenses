package org.totschnig.myexpenses.util.ads;

import android.view.ViewGroup;

import org.totschnig.myexpenses.activity.ProtectedFragmentActivity;
import org.totschnig.myexpenses.preference.PrefHandler;

public interface AdHandlerFactory {
  boolean isRequestLocationInEeaOrUnknown();

  AdHandler create(ViewGroup adContainer, PrefHandler prefHandler);

  /**
   * @param context     context in which callback action will be taken
   * @param forceShow   if false, consent form is only shown if consent is unknown
   * @param prefHandler
   */
  void gdprConsent(ProtectedFragmentActivity context, boolean forceShow, PrefHandler prefHandler);
}
