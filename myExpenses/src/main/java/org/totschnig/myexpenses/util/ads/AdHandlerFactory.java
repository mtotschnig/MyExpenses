package org.totschnig.myexpenses.util.ads;

import android.view.ViewGroup;

import org.totschnig.myexpenses.activity.ProtectedFragmentActivity;

public interface AdHandlerFactory {
  boolean isRequestLocationInEeaOrUnknown();

  AdHandler create(ViewGroup adContainer);

  /**
   * @param context     context in which callback action will be taken
   * @param forceShow   if false, consent form is only shown if consent is unknown
   */
  void gdprConsent(ProtectedFragmentActivity context, boolean forceShow);
  void clearConsent();
  void setConsent(boolean personalized);
}
