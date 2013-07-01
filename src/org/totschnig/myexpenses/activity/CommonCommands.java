package org.totschnig.myexpenses.activity;

import java.io.Serializable;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.dialog.ContribDialogFragment;
import org.totschnig.myexpenses.dialog.HelpDialogFragment;
import org.totschnig.myexpenses.model.ContribFeature.Feature;
import org.totschnig.myexpenses.util.Utils;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

public class CommonCommands {
  static boolean dispatchCommand(Activity ctx,int command) {
    if (ctx instanceof FragmentActivity && dispatchCommand((FragmentActivity) ctx,command))
      return true;
    Intent i;
    switch(command) {
    case R.id.FEEDBACK_COMMAND:
      i = new Intent(android.content.Intent.ACTION_SEND);
      i.setType("plain/text");
      i.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{ MyApplication.FEEDBACK_EMAIL });
      i.putExtra(android.content.Intent.EXTRA_SUBJECT,
          "[" + ctx.getString(R.string.app_name) +
          getVersionName(ctx) + "] Feedback"
      );
      i.putExtra(android.content.Intent.EXTRA_TEXT, ctx.getString(R.string.feedback_email_message));
      ctx.startActivity(i);
      break;
    }
   return true;
  }

  static boolean dispatchCommand(FragmentActivity ctx,int command) {
    switch(command) {
    case R.id.CONTRIB_PLAY_COMMAND:
      Utils.viewContribApp(ctx);
      return true;
    case R.id.WEB_COMMAND:
      Intent i = new Intent(Intent.ACTION_VIEW);
      i.setData(Uri.parse("http://" + MyApplication.HOST));
      ctx.startActivity(i);
      return true;
    case R.id.SETTINGS_COMMAND:
      ctx.startActivityForResult(new Intent(ctx, MyPreferenceActivity.class),0);
      return true;
    case R.id.HELP_COMMAND:
      ctx.startActivityForResult(new Intent(ctx,Help.class),0);
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
