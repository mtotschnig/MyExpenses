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

import android.app.Activity;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.BaseActivity;
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity;
import org.totschnig.myexpenses.adapter.CurrencyAdapter;
import org.totschnig.myexpenses.adapter.IdAdapter;
import org.totschnig.myexpenses.model.AccountType;
import org.totschnig.myexpenses.preference.PrefHandler;
import org.totschnig.myexpenses.preference.PrefKey;
import org.totschnig.myexpenses.provider.DatabaseConstants;
import org.totschnig.myexpenses.util.Utils;
import org.totschnig.myexpenses.util.distrib.DistributionHelper;

import java.util.Arrays;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import static org.totschnig.myexpenses.activity.ConstantsKt.IMPORT_FILENAME_REQUEST_CODE;

public class DialogUtils {
  private DialogUtils() {
  }

  /**
   * @return Dialog to be used from Preference,
   * and from version update
   */
  public static Dialog sendWithFTPDialog(final ProtectedFragmentActivity ctx) {
    return new MaterialAlertDialogBuilder(ctx)
        .setMessage(R.string.no_app_handling_ftp_available)
        .setPositiveButton(R.string.response_yes, (dialog, id) -> {
          ctx.dismissDialog(R.id.FTP_DIALOG);
          Intent intent = new Intent(Intent.ACTION_VIEW);
          intent.setData(Uri.parse(DistributionHelper.getMarketPrefix() + "org.totschnig.sendwithftp"));
          ctx.startActivity(intent, R.string.error_accessing_market, null);
        })
        .setNegativeButton(R.string.response_no, (dialog, id) -> ctx.dismissDialog(R.id.FTP_DIALOG)).create();
  }

  public static void showPasswordDialog(final BaseActivity ctx, AlertDialog dialog,
                                        PasswordDialogUnlockedCallback callback) {
    dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
    dialog.show();
    if (callback == null) {
      callback = () -> {
        MyApplication.Companion.getInstance().unlock();
        ctx.showWindow();
      };
    }
    PasswordDialogListener passwordDialogListener = new PasswordDialogListener(ctx, dialog, callback);
    Button b = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
    if (b != null)
      b.setOnClickListener(passwordDialogListener);
    b = dialog.getButton(DialogInterface.BUTTON_NEUTRAL);
    if (b != null)
      b.setOnClickListener(passwordDialogListener);
  }

  public static AlertDialog passwordDialog(final Activity ctx, final boolean cancelable) {
    PrefHandler prefHandler = ((MyApplication) ctx.getApplication()).getAppComponent().prefHandler();
    final String securityQuestion = prefHandler.getString(PrefKey.SECURITY_QUESTION,"");
    AlertDialog.Builder builder = new MaterialAlertDialogBuilder(ctx);
    //noinspection InflateParams
    View view = LayoutInflater.from(builder.getContext()).inflate(R.layout.password_check, null);
    view.findViewById(R.id.password).setTag(Boolean.FALSE);
    builder.setTitle(R.string.password_prompt)
        .setView(view)
        .setOnCancelListener(dialog -> {
          if (cancelable) {
            dialog.dismiss();
          } else {
            ctx.moveTaskToBack(true);
          }
        });
    if (!securityQuestion.equals("")) {
      builder.setNeutralButton(R.string.password_lost, (dialog, id) -> {
      });
    }
    builder.setPositiveButton(android.R.string.ok, (dialog, id) -> {
    });
    if (cancelable) {
      builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.dismiss());
    }
    return builder.create();
  }

  public interface PasswordDialogUnlockedCallback {
    void onPasswordDialogUnlocked();
  }

  static class PasswordDialogListener implements View.OnClickListener {
    private final AlertDialog dialog;
    private final BaseActivity ctx;
    private final PasswordDialogUnlockedCallback callback;

    public PasswordDialogListener(BaseActivity ctx, AlertDialog dialog,
                                  PasswordDialogUnlockedCallback callback) {
      this.dialog = dialog;
      this.ctx = ctx;
      this.callback = callback;
    }

    @Override
    public void onClick(View v) {
      PrefHandler prefHandler = ((MyApplication) ctx.getApplication()).getAppComponent().prefHandler();
      final String securityQuestion = prefHandler.getString(PrefKey.SECURITY_QUESTION,"");
      EditText input = dialog.findViewById(R.id.password);
      TextView error = dialog.findViewById(R.id.passwordInvalid);
      if (v == dialog.getButton(AlertDialog.BUTTON_NEUTRAL)) {
        if ((Boolean) input.getTag()) {
          input.setTag(Boolean.FALSE);
          ((Button) v).setText(R.string.password_lost);
          dialog.setTitle(R.string.password_prompt);
        } else {
          input.setTag(Boolean.TRUE);
          dialog.setTitle(securityQuestion);
          ((Button) v).setText(android.R.string.cancel);
        }
      } else {
        String value = input.getText().toString();
        boolean isInSecurityQuestion = (Boolean) input.getTag();
        if (Utils.md5(value).equals(
                prefHandler.getString(isInSecurityQuestion ? PrefKey.SECURITY_ANSWER : PrefKey.SET_PASSWORD, ""))) {
          input.setText("");
          error.setText("");
          callback.onPasswordDialogUnlocked();
          if (isInSecurityQuestion) {
            prefHandler.putBoolean(PrefKey.PROTECTION_LEGACY, false);
            ctx.showDismissibleSnackBar(R.string.password_disabled_reenable);
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setText(R.string.password_lost);
            dialog.setTitle(R.string.password_prompt);
            input.setTag(Boolean.FALSE);
          }
          dialog.dismiss();
        } else {
          input.setText("");
          error.setText(isInSecurityQuestion ? R.string.password_security_answer_not_valid : R.string.password_not_valid);
        }
      }
    }
  }

  public static void configureEncoding(Spinner spinner, Context context, PrefHandler prefHandler, String prefName) {
    spinner.setSelection(
        Arrays.asList(context.getResources().getStringArray(R.array.pref_qif_export_file_encoding))
            .indexOf(prefHandler.getString(prefName, "UTF-8")));
  }

  public static void configureDelimiter(Spinner spinner, Context context, PrefHandler prefHandler, String prefName) {
    spinner.setSelection(
        Arrays.asList(context.getResources().getStringArray(R.array.pref_csv_import_delimiter_values))
            .indexOf(prefHandler.getString(prefName, ",")));
  }

  public static void openBrowse(Uri uri, Fragment fragment) {
    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);//TODO implement preference that allows to use ACTION_OPEN_DOCUMENT
    intent.addCategory(Intent.CATEGORY_OPENABLE);

    intent.setType("*/*");

    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

    try {
      fragment.startActivityForResult(intent, IMPORT_FILENAME_REQUEST_CODE);
    } catch (ActivityNotFoundException e) {
      ((ProtectedFragmentActivity) fragment.getActivity()).showSnackBar(R.string.no_filemanager_installed, Snackbar.LENGTH_SHORT);
    } catch (SecurityException ex) {
      ((ProtectedFragmentActivity) fragment.getActivity()).showSnackBar(String.format(
              "Sorry, this destination does not accept %s request. Please select a different one.", intent.getAction()),
          Snackbar.LENGTH_SHORT);
    }
  }

  public static void showSyncUnlinkConfirmationDialog(FragmentActivity context, String syncAccountName, String uuid) {
    Bundle b = new Bundle();
    b.putString(ConfirmationDialogFragment.KEY_MESSAGE,
        context.getString(R.string.dialog_confirm_sync_unlink, syncAccountName));
    b.putString(DatabaseConstants.KEY_UUID, uuid);
    b.putInt(ConfirmationDialogFragment.KEY_COMMAND_POSITIVE, R.id.SYNC_UNLINK_COMMAND);
    b.putInt(ConfirmationDialogFragment.KEY_POSITIVE_BUTTON_LABEL, R.string.menu_sync_unlink);
    ConfirmationDialogFragment.newInstance(b).show(context.getSupportFragmentManager(), "SYNC_UNLINK");
  }
}
