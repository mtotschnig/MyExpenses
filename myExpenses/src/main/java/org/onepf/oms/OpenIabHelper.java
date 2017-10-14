package org.onepf.oms;

import android.app.Activity;
import android.content.Intent;

import org.onepf.oms.appstore.googleUtils.IabHelper;
import org.onepf.oms.appstore.googleUtils.Inventory;

import java.util.List;

/**
 * Stubs for functionality not used on master branch
 */
public class OpenIabHelper {
  public static final String ITEM_TYPE_INAPP = "inapp";
  public static final String ITEM_TYPE_SUBS = "subs";
  public void startSetup(final OnIabSetupFinishedListener listener) {

  }

  public void queryInventoryAsync(final boolean querySkuDetails, final List<String> moreItemSkus,
                                  final List<String> moreSubsSkus, final QueryInventoryFinishedListener listener) {

  }

  public void dispose() {

  }

  public void launchPurchaseFlow(Activity act, String sku, String itemType, List<String> oldSkus, int requestCode,
                                 IabHelper.OnIabPurchaseFinishedListener listener, String extraData) throws IabHelper.IabAsyncInProgressException {

  }

  public boolean handleActivityResult(int requestCode, int resultCode, Intent data) {
    return false;
  }

  public static class IabResult {
    public boolean isSuccess() {
      return false;
    }
  }

  public interface OnIabSetupFinishedListener {
    void onIabSetupFinished(IabResult result);
  }

  public interface QueryInventoryFinishedListener {
    void onQueryInventoryFinished(IabResult result, Inventory inv);
  }
}
