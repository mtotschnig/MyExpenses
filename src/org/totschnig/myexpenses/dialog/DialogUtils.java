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

package org.totschnig.myexpenses.dialog;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.util.Utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.support.v4.app.FragmentActivity;
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
    return new AlertDialog.Builder(ctx)
    .setMessage(R.string.no_app_handling_ftp_available)
    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
         public void onClick(DialogInterface dialog, int id) {
           ctx.dismissDialog(R.id.FTP_DIALOG);
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
      }
    }).create();
  }
  /**
   * @param ctx
   * @param accountId if null all accounts will be reset
   */
  public static void showWarningResetDialog(FragmentActivity ctx,Long accountId) {
    ExportDialogFragment.newInstance(accountId)
      .show(ctx.getSupportFragmentManager(),"WARNING_RESET");
  }

  public static void showPasswordDialog(Activity ctx,AlertDialog dialog) {
    ctx.findViewById(android.R.id.content).setVisibility(View.INVISIBLE);
    dialog.show();
    PasswordDialogListener l = new PasswordDialogListener(ctx,dialog);
    Button b = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
    if (b != null)
      b.setOnClickListener(l);
    b = dialog.getButton(DialogInterface.BUTTON_NEGATIVE);
    if (b != null)
      b.setOnClickListener(l);
  }
  public static AlertDialog passwordDialog(final Activity ctx) {
    final SharedPreferences settings = MyApplication.getInstance().getSettings();
    final String securityQuestion = settings.getString(MyApplication.PREFKEY_SECURITY_QUESTION, "");
    LayoutInflater li = LayoutInflater.from(ctx);
    View view = li.inflate(R.layout.password_check, null);
    view.findViewById(R.id.password).setTag(Boolean.valueOf(false));
    AlertDialog.Builder builder = new AlertDialog.Builder(ctx)
      .setTitle(R.string.password_prompt)
      .setView(view)
      .setOnCancelListener(new DialogInterface.OnCancelListener() {
          @Override
          public void onCancel(DialogInterface dialog) {
            ctx.moveTaskToBack(true);
          }
        });
    if (MyApplication.getInstance().isContribEnabled && !securityQuestion.equals("")) {
      builder.setNegativeButton(R.string.password_lost, new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int id) {}
      });
    }
    builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int id) {}
    });
    return builder.create();
  }
  static class PasswordDialogListener implements View.OnClickListener {
    private final AlertDialog dialog;
    private final Activity ctx;
    public PasswordDialogListener(Activity ctx, AlertDialog dialog) {
        this.dialog = dialog;
        this.ctx = ctx;
    }
    @Override
    public void onClick(View v) {
      final SharedPreferences settings = MyApplication.getInstance().getSettings();
      final String securityQuestion = settings.getString(MyApplication.PREFKEY_SECURITY_QUESTION, "");
      EditText input = (EditText) dialog.findViewById(R.id.password);
      TextView error = (TextView) dialog.findViewById(R.id.passwordInvalid);
      if (v == dialog.getButton(AlertDialog.BUTTON_NEGATIVE)) {
        if ((Boolean) input.getTag()) {
          input.setTag(Boolean.valueOf(false));
          ((Button) v).setText(R.string.password_lost);
          dialog.setTitle(R.string.password_prompt);
        } else {
          input.setTag(Boolean.valueOf(true));
          dialog.setTitle(securityQuestion);
          ((Button) v).setText(android.R.string.cancel);
        }
      } else {
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
            Toast.makeText(ctx.getBaseContext(),R.string.password_disabled_reenable, Toast.LENGTH_LONG).show();
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setText(R.string.password_lost);
            dialog.setTitle(R.string.password_prompt);
            input.setTag(Boolean.valueOf(false));
          }
          dialog.dismiss();
        } else {
          input.setText("");
          error.setText(isInSecurityQuestion ? R.string.password_security_answer_not_valid : R.string.password_not_valid);
        }
      }
    }
  }
}
