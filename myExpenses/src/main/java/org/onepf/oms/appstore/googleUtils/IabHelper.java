package org.onepf.oms.appstore.googleUtils;

public class IabHelper {

  public interface OnIabPurchaseFinishedListener {
    void onIabPurchaseFinished(IabResult result, Purchase info);
  }

  public class IabAsyncInProgressException extends Throwable {
  }
}
