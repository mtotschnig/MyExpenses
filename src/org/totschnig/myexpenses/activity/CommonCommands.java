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

import java.io.Serializable;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.dialog.ContribDialogFragment;
import org.totschnig.myexpenses.dialog.ContribInfoDialogFragment;
import org.totschnig.myexpenses.model.ContribFeature.Feature;
import org.totschnig.myexpenses.preference.SharedPreferencesCompat;
import org.totschnig.myexpenses.util.Utils;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.widget.Toast;

public class CommonCommands {
  static boolean dispatchCommand(Activity ctx,int command, Object tag) {
    Intent i;
    switch(command) {
    case R.id.RATE_COMMAND:
      SharedPreferencesCompat.apply(
          MyApplication.getInstance().getSettings().edit().putLong("nextReminderRate", -1));
      i = new Intent(Intent.ACTION_VIEW);
      i.setData(Uri.parse(MyApplication.MARKET_PREFIX + "org.totschnig.myexpenses"));
      if (Utils.isIntentAvailable(ctx,i)) {
        ctx.startActivity(i);
      } else {
        Toast.makeText(
            ctx,
            R.string.error_accessing_market,
            Toast.LENGTH_LONG)
          .show();
      }
      return true;
    case R.id.SETTINGS_COMMAND:
      i = new Intent(ctx, MyPreferenceActivity.class);
      i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
      if (tag != null) {
        i.putExtra(MyPreferenceActivity.KEY_OPEN_PREF_KEY,(String) tag);
      }
      ctx.startActivityForResult(i,ProtectedFragmentActivity.PREFERENCES_REQUEST);
      return true;
    case R.id.FEEDBACK_COMMAND:
      i = new Intent(android.content.Intent.ACTION_SEND);
      i.setType("plain/text");
      i.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{ MyApplication.FEEDBACK_EMAIL });
      i.putExtra(android.content.Intent.EXTRA_SUBJECT,
          "[" + ctx.getString(R.string.app_name) +
          getVersionName(ctx) + "] Feedback"
      );
      i.putExtra(android.content.Intent.EXTRA_TEXT, ctx.getString(R.string.feedback_email_message));
      if (!Utils.isIntentAvailable(ctx,i)) {
        Toast.makeText(ctx,R.string.no_app_handling_email_available, Toast.LENGTH_LONG).show();
      } else {
        ctx.startActivity(i);
      }
      break;
    case R.id.CONTRIB_INFO_COMMAND:
      CommonCommands.showContribInfoDialog((FragmentActivity) ctx,false);
      return true;
    case R.id.CONTRIB_BUY_COMMAND:
      Utils.contribBuyDo(ctx);
      return true;
    case R.id.WEB_COMMAND:
      i = new Intent(Intent.ACTION_VIEW);
      i.setData(Uri.parse(ctx.getString(R.string.website)));
      ctx.startActivity(i);
      return true;
    case R.id.HELP_COMMAND:
      i = new Intent(ctx,Help.class);
      i.putExtra("variant",((ProtectedFragmentActivity)ctx).helpVariant);
      //for result is needed since it allows us to inspect the calling activity
      ctx.startActivityForResult(i,0);
      return true;
    case android.R.id.home:
      ctx.setResult(FragmentActivity.RESULT_CANCELED);
      ctx.finish();
      return true;
    }
   return false;
  }
  public static void showContribDialog(FragmentActivity ctx,Feature feature, Serializable tag) {
    ContribDialogFragment.newInstance(feature, tag).show(ctx.getSupportFragmentManager(),"CONTRIB");
  }
  public static void showContribInfoDialog(FragmentActivity ctx,boolean reminderP) {
    ContribInfoDialogFragment.newInstance(reminderP).show(ctx.getSupportFragmentManager(),"CONTRIB_INFO");
  }
  /**
   * retrieve information about the current version
   * @return concatenation of versionName, versionCode and buildTime
   * buildTime is automatically stored in property file during build process
   */
  public static String getVersionInfo(Activity ctx) {
    String version = "";
    String versionname = "";
    try {
      PackageInfo pi = ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0);
      version = " (revision " + pi.versionCode + ") ";
      versionname = pi.versionName;
      //versiontime = ", " + R.string.installed + " " + sdf.format(new Date(pi.lastUpdateTime));
    } catch (Exception e) {
      Log.e("MyExpenses", "Package info not found", e);
    }
    return versionname + version  + MyApplication.BUILD_DATE;
  }
  /**
   * @return version name
   */
  public static String getVersionName(Activity ctx) {
    String version = "";
    try {
      PackageInfo pi = ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0);
      version = pi.versionName;
    } catch (Exception e) {
      Log.e("MyExpenses", "Package name not found", e);
    }
    return version;
  }
  /**
   * @return version number (versionCode)
   */
  public static int getVersionNumber(Activity ctx) {
    int version = -1;
    try {
      PackageInfo pi = ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0);
      version = pi.versionCode;
    } catch (Exception e) {
      Log.e("MyExpenses", "Package name not found", e);
    }
    return version;
  }
}
