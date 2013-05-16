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

package org.totschnig.myexpenses;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.text.Html;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Util class with helper methods
 * @author Michael Totschnig
 *
 */
public class DialogUtils {
  /**
   * @return Dialog to be used from Preference,
   * and from version update
   */
  public static Dialog sendWithFTPDialog(final Activity ctx) {
    String msg = ctx.getClass() == MyExpenses.class ? (ctx.getString(R.string.version_32_upgrade_info) + " ") : "";
    return new AlertDialog.Builder(ctx)
    .setMessage(msg + ctx.getString(R.string.no_app_handling_ftp_available))
    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
         public void onClick(DialogInterface dialog, int id) {
           ctx.dismissDialog(R.id.FTP_DIALOG);
           if (ctx.getClass() == MyExpenses.class)
             ctx.showDialog(R.id.VERSION_DIALOG);
           Intent intent = new Intent(Intent.ACTION_VIEW);
           intent.setData(Uri.parse("market://details?id=org.totschnig.sendwithftp"));
           if (Utils.isIntentAvailable(ctx,intent)) {
             ctx.startActivity(intent);
           } else {
             Toast.makeText(ctx.getBaseContext(),R.string.error_accessing_gplay, Toast.LENGTH_LONG).show();
           }
         }
      })
    .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int id) {
        ctx.dismissDialog(R.id.FTP_DIALOG);
        if (ctx.getClass() == MyExpenses.class)
          ctx.showDialog(R.id.VERSION_DIALOG);
      }
    }).create();
  }
  public static Dialog contribDialog(final Activity ctx,final String feature) {
    final Integer usagesLeft = Utils.usagesLeft(feature);
    CharSequence message = Html.fromHtml(String.format(ctx.getString(
      R.string.dialog_contrib_reminder,
      ctx.getString(ctx.getResources().getIdentifier("contrib_feature_" + feature + "_label", "string", ctx.getPackageName())),
      usagesLeft > 0 ? ctx.getString(R.string.dialog_contrib_usage_count,usagesLeft) : ctx.getString(R.string.dialog_contrib_no_usages_left))));
    return createMessageDialogWithCustomButtons(
      new ContextThemeWrapper(ctx, MyApplication.getThemeId()) {
        public void onDialogButtonClicked(View v) {
          if (v.getId() == R.id.CONTRIB_PLAY_COMMAND) {
            ctx.dismissDialog(R.id.CONTRIB_DIALOG);
            Utils.viewContribApp(ctx);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("market://details?id=org.totschnig.myexpenses.contrib"));
            if (Utils.isIntentAvailable(ctx,intent)) {
              ctx.startActivity(intent);
            } else {
              Toast.makeText(ctx.getBaseContext(),R.string.error_accessing_gplay, Toast.LENGTH_LONG).show();
            }
            ((ContribIFace)ctx).contribFeatureNotCalled();
          } else {
            if (usagesLeft > 0) {
              //we remove the dialog, in order to have it display updated usage count on next display
              ctx.removeDialog(R.id.CONTRIB_DIALOG);
              ((ContribIFace)ctx).contribFeatureCalled(feature);
            } else {
              ctx.dismissDialog(R.id.CONTRIB_DIALOG);
              ((ContribIFace)ctx).contribFeatureNotCalled();
            }
          }
        }
      },
      message,R.id.CONTRIB_PLAY_COMMAND,null, R.string.dialog_contrib_yes,R.string.dialog_contrib_no)
    .setOnCancelListener(new DialogInterface.OnCancelListener() {
          @Override
          public void onCancel(DialogInterface dialog) {
            ((ContribIFace)ctx).contribFeatureNotCalled();
          }
        })
    .create();
  }
  /**
   * @return an AlertDialog.Builder with R.layout.messagedialog as layout
   */
  public static AlertDialog.Builder createMessageDialog(Context ctx, int message,int command,Object tag) {
    return createMessageDialogWithCustomButtons(ctx,message,command,tag,android.R.string.yes,android.R.string.no);
  }
  /**
   * @return an AlertDialog.Builder with R.layout.messagedialog as layout
   */
  public static AlertDialog.Builder createMessageDialogWithCustomButtons(
      Context ctx, CharSequence message,int command,Object tag, int yesButton, int noButton) {
    LayoutInflater li = LayoutInflater.from(ctx);
    View view = li.inflate(R.layout.messagedialog, null);
    TextView tv = (TextView)view.findViewById(R.id.message_text);
    tv.setText(message);
    setDialogTwoButtons(view,
        yesButton,command,tag,
        noButton,0,null
    );
    return new AlertDialog.Builder(ctx)
      .setView(view);
  }
  public static AlertDialog.Builder createMessageDialogWithCustomButtons(
      Context ctx, int  message,int command,Object tag, int yesButton, int noButton) {
    return createMessageDialogWithCustomButtons(ctx,ctx.getString(message),command,tag,yesButton,noButton);
  }
  /**
   * one button centered takes up 33% width
   */
  static void setDialogOneButton(View view,
      int neutralString, int neutralCommandId,Object neutralTag) {
    setButton((Button) view.findViewById(R.id.NEUTRAL_BUTTON),neutralString,neutralCommandId,neutralTag);
    view.findViewById(R.id.POSITIVE_BUTTON).setVisibility(View.INVISIBLE);
    view.findViewById(R.id.NEGATIVE_BUTTON).setVisibility(View.INVISIBLE);
  }
  /**
   * two buttons 50% width each
   */
  static void setDialogTwoButtons(View view,
      int positiveString, int positiveCommandId,Object positiveTag,
      int negativeString, int negativeCommandId,Object negativeTag) {
    setButton((Button) view.findViewById(R.id.POSITIVE_BUTTON),positiveString,positiveCommandId,positiveTag);
    setButton((Button) view.findViewById(R.id.NEGATIVE_BUTTON),negativeString,negativeCommandId,negativeTag);
    view.findViewById(R.id.NEUTRAL_BUTTON).setVisibility(View.GONE);
  }
  /**
   * three buttons 33% width each
   */
  static void setDialogThreeButtons(View view,
      int positiveString, int positiveCommandId,Object positiveTag,
      int neutralString, int neutralCommandId,Object neutralTag,
      int negativeString, int negativeCommandId,Object negativeTag) {
    setButton((Button) view.findViewById(R.id.POSITIVE_BUTTON),positiveString,positiveCommandId,positiveTag);
    setButton((Button) view.findViewById(R.id.NEUTRAL_BUTTON),neutralString,neutralCommandId,neutralTag);
    setButton((Button) view.findViewById(R.id.NEGATIVE_BUTTON),negativeString,negativeCommandId,negativeTag);
  }
  /**
   * set String s and Command c on Button b
   * if s i null, hide button
   * @param b
   * @param s
   * @param c
   */
  private static void setButton(Button b, int s, int c,Object tag) {
    b.setText(s);
    if (c != 0) {
      b.setId(c);
      if (tag != null)
        b.setTag(tag);
    }
  }
  public static void showPasswordDialog(Activity ctx,Dialog dlg) {
    ctx.findViewById(android.R.id.content).setVisibility(View.INVISIBLE);
    dlg.show();
  }
  public static Dialog passwordDialog(final Activity ctx) {
    final SharedPreferences settings = MyApplication.getInstance().getSettings();
    final String securityQuestion = settings.getString(MyApplication.PREFKEY_SECURITY_QUESTION, "");
    Button okBtn;
    LayoutInflater li = LayoutInflater.from(new ContextThemeWrapper(ctx, MyApplication.getThemeId()));
    View view = li.inflate(R.layout.password_check, null);
    final EditText input = (EditText) view.findViewById(R.id.password);
    final TextView error = (TextView) view.findViewById(R.id.passwordInvalid);
    final AlertDialog pwDialog = new AlertDialog.Builder(ctx)
      .setTitle(R.string.password_prompt)
      .setView(view)
      .setOnCancelListener(new DialogInterface.OnCancelListener() {
          @Override
          public void onCancel(DialogInterface dialog) {
            ctx.moveTaskToBack(true);
          }
        })
      .create();
    final Button lostBtn = (Button) view.findViewById(R.id.NEGATIVE_BUTTON);
    if (MyApplication.getInstance().isContribEnabled && !securityQuestion.equals("")) {
      view.findViewById(R.id.NEUTRAL_BUTTON).setVisibility(View.GONE);
      okBtn = (Button) view.findViewById(R.id.POSITIVE_BUTTON);
      lostBtn.setText("Lost password");
      lostBtn.setOnClickListener(new View.OnClickListener() {
        public void onClick(View v) {
          input.setText("");
          error.setText("");
          if ((Boolean) input.getTag()) {
            input.setTag(Boolean.valueOf(false));
            lostBtn.setText("Lost password");
            pwDialog.setTitle(R.string.password_prompt);
          } else {
            input.setTag(Boolean.valueOf(true));
            pwDialog.setTitle(securityQuestion);
            lostBtn.setText("Cancel");
          }
        }
      });
    } else {
      view.findViewById(R.id.POSITIVE_BUTTON).setVisibility(View.INVISIBLE);
      view.findViewById(R.id.NEGATIVE_BUTTON).setVisibility(View.INVISIBLE);
      okBtn = (Button) view.findViewById(R.id.NEUTRAL_BUTTON);
    }
    okBtn.setText(android.R.string.ok);
    // we store as tag the state of the dialog: false => we check the password; true => we check the security question
    input.setTag(Boolean.valueOf(false));
    okBtn.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        String value = input.getText().toString();
        boolean isInSecurityQuestion = (Boolean) input.getTag();
        if (Utils.md5(value).equals(settings.getString(
            isInSecurityQuestion ? MyApplication.PREFKEY_SECURITY_ANSWER : MyApplication.PREFKEY_SET_PASSWORD,""))) {
          input.setText("");
          error.setText("");
          MyApplication.getInstance().isLocked = false;
          ctx.findViewById(android.R.id.content).setVisibility(View.VISIBLE);
          if (isInSecurityQuestion) {
            settings.edit().putBoolean(MyApplication.PREFKEY_PERFORM_PROTECTION, false).commit();
            Toast.makeText(ctx.getBaseContext(),"The password protection has been disabled", Toast.LENGTH_LONG).show();
            lostBtn.setText("Lost password");
            pwDialog.setTitle(R.string.password_prompt);
            input.setTag(Boolean.valueOf(false));
          }
          pwDialog.dismiss();
        } else {
          input.setText("");
          error.setText(isInSecurityQuestion ? R.string.password_security_answer_not_valid : R.string.password_not_valid);
        }
      }
    });
    return pwDialog;
  }
}
