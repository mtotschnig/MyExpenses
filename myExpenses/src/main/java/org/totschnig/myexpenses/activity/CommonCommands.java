/*   This file is part of My Expenses.
 *   My Expenses is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   My Expenses is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with My Expenses.  If not, see <http://www.gnu.org/licenses/>.
*/

package org.totschnig.myexpenses.activity;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.dialog.HelpDialogFragment;
import org.totschnig.myexpenses.model.ContribFeature;
import org.totschnig.myexpenses.preference.PrefKey;
import org.totschnig.myexpenses.util.DistribHelper;
import org.totschnig.myexpenses.util.Utils;
import org.totschnig.myexpenses.util.licence.LicenceHandler;
import org.totschnig.myexpenses.util.licence.LicenceStatus;

import java.io.Serializable;
import java.util.Locale;

import static org.totschnig.myexpenses.util.DistribHelper.getMarketSelfUri;
import static org.totschnig.myexpenses.util.DistribHelper.getVersionInfo;

public class CommonCommands {
  private CommonCommands() {
  }

  public static boolean dispatchCommand(ProtectedFragmentActivity ctx, int command, Object tag) {
    Intent i;
    switch (command) {
      case R.id.RATE_COMMAND:
        i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse(getMarketSelfUri()));
        if (Utils.isIntentAvailable(ctx, i)) {
          ctx.startActivity(i);
        } else {
          ctx.showSnackbar(R.string.error_accessing_market, Snackbar.LENGTH_LONG);
        }
        return true;
      case R.id.SETTINGS_COMMAND:
        i = new Intent(ctx, MyPreferenceActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        if (tag != null) {
          i.putExtra(MyPreferenceActivity.KEY_OPEN_PREF_KEY, (String) tag);
        }
        ctx.startActivityForResult(i, ProtectedFragmentActivity.PREFERENCES_REQUEST);
        return true;
      case R.id.FEEDBACK_COMMAND: {
        LicenceHandler licenceHandler = MyApplication.getInstance().getLicenceHandler();
        LicenceStatus licenceStatus = licenceHandler.getLicenceStatus();
        String licenceInfo = "";
        if (licenceStatus != null) {
          licenceInfo = "\nLICENCE: " + licenceStatus.name();
          String purchaseExtraInfo = licenceHandler.getPurchaseExtraInfo();
          if (!TextUtils.isEmpty(purchaseExtraInfo)) {
            licenceInfo += " (" + purchaseExtraInfo + ")";
          }
        }
        i = new Intent(android.content.Intent.ACTION_SEND);
        i.setType("plain/text");
        i.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{MyApplication.FEEDBACK_EMAIL});
        i.putExtra(android.content.Intent.EXTRA_SUBJECT,
            "[" + ctx.getString(R.string.app_name) + "] Feedback"
        );
        String messageBody = String.format(Locale.ROOT,
            "APP_VERSION:%s\nANDROID_VERSION:%s\nBRAND:%s\nMODEL:%s\nLANGUAGE:%s%s\n\n%s\n\n",
            getVersionInfo(ctx),
            Build.VERSION.RELEASE,
            Build.BRAND,
            Build.MODEL,
            Locale.getDefault().toString(),
            licenceInfo,
            ctx.getString(R.string.feedback_email_message));
        i.putExtra(android.content.Intent.EXTRA_TEXT, messageBody);
        if (!Utils.isIntentAvailable(ctx, i)) {
          ctx.showSnackbar(R.string.no_app_handling_email_available, Snackbar.LENGTH_LONG);
        } else {
          ctx.startActivity(i);
        }
        break;
      }
      case R.id.CONTRIB_INFO_COMMAND:
        CommonCommands.showContribDialog(ctx, null, null);
        return true;
      case R.id.WEB_COMMAND:
        i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse(ctx.getString(R.string.website)));
        ctx.startActivity(i);
        return true;
      case R.id.HELP_COMMAND:
        i = new Intent(ctx, Help.class);
        i.putExtra(HelpDialogFragment.KEY_VARIANT,
            tag != null ? (Enum<?>) tag : ((ProtectedFragmentActivity) ctx).helpVariant);
        //for result is needed since it allows us to inspect the calling activity
        ctx.startActivityForResult(i, 0);
        return true;
      case R.id.REQUEST_LICENCE_MIGRATION_COMMAND:
        LicenceHandler licenceHandler = MyApplication.getInstance().getLicenceHandler();
        String androidId = Settings.Secure.getString(ctx.getContentResolver(), Settings.Secure.ANDROID_ID);
        i = new Intent(android.content.Intent.ACTION_SEND);
        i.setType("plain/text");
        i.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{MyApplication.FEEDBACK_EMAIL});
        i.putExtra(android.content.Intent.EXTRA_SUBJECT,
            "[" + ctx.getString(R.string.app_name) + "] " +  ctx.getString(licenceHandler.getLicenceStatus().getResId()));
        String extraText = String.format(
            "Please send me a new licence key. Current key is %1$s for Android-Id %2$s\nLANGUAGE:%3$s\nVERSION:%4$s",
            PrefKey.LICENCE_LEGACY.getString(null), androidId,
            Locale.getDefault().toString(), DistribHelper.getVersionInfo(ctx));
        i.putExtra(android.content.Intent.EXTRA_TEXT, extraText);
        if (!Utils.isIntentAvailable(ctx, i)) {
          ctx.showSnackbar(R.string.no_app_handling_email_available, Snackbar.LENGTH_LONG);
        } else {
          ctx.startActivity(i);
        }
        return true;
      case android.R.id.home:
        ctx.setResult(FragmentActivity.RESULT_CANCELED);
        ctx.finish();
        return true;
    }
    return false;
  }

  public static void showContribDialog(Activity ctx, @Nullable ContribFeature feature, Serializable tag) {
    Intent i = ContribInfoDialogActivity.getIntentFor(ctx, feature);
    i.putExtra(ContribInfoDialogActivity.KEY_TAG, tag);
    ctx.startActivityForResult(i, ProtectedFragmentActivity.CONTRIB_REQUEST);
  }

}
