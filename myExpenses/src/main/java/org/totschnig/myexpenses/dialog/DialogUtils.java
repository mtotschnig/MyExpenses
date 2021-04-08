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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity;
import org.totschnig.myexpenses.adapter.AccountTypeAdapter;
import org.totschnig.myexpenses.adapter.CurrencyAdapter;
import org.totschnig.myexpenses.export.qif.QifDateFormat;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.AccountType;
import org.totschnig.myexpenses.preference.PrefHandler;
import org.totschnig.myexpenses.preference.PrefKey;
import org.totschnig.myexpenses.provider.DatabaseConstants;
import org.totschnig.myexpenses.util.distrib.DistributionHelper;
import org.totschnig.myexpenses.util.PermissionHelper.PermissionGroup;
import org.totschnig.myexpenses.util.Utils;

import java.util.Arrays;
import java.util.List;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import static org.totschnig.myexpenses.activity.ConstantsKt.IMPORT_FILENAME_REQUEST_CODE;
import static org.totschnig.myexpenses.util.PermissionHelper.PermissionGroup.CALENDAR;

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

  public static void showPasswordDialog(final ProtectedFragmentActivity ctx, AlertDialog dialog,
                                        PasswordDialogUnlockedCallback callback) {
    dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
    dialog.show();
    if (callback == null) {
      callback = () -> {
        MyApplication.getInstance().setLocked(false);
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
    final String securityQuestion = PrefKey.SECURITY_QUESTION.getString("");
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

  //https://developer.android.com/guide/topics/providers/document-provider.html

  /**
   * @return display name for document stored at mUri.
   * Returns null if accessing mUri raises {@link SecurityException}
   */
  @SuppressLint("NewApi")
  public static String getDisplayName(Uri uri) {

    if (!"file".equalsIgnoreCase(uri.getScheme()) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
      // The query, since it only applies to a single document, will only return
      // one row. There's no need to filter, sort, or select fields, since we want
      // all fields for one document.
      try {
        Cursor cursor = MyApplication.getInstance().getContentResolver()
            .query(uri, null, null, null, null, null);

        if (cursor != null) {
          try {
            if (cursor.moveToFirst()) {
              // Note it's called "Display Name".  This is
              // provider-specific, and might not necessarily be the file name.
              int columnIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
              if (columnIndex != -1) {
                String displayName = cursor.getString(columnIndex);
                if (displayName != null) {
                  return displayName;
                }
              }
            }
          } catch (Exception ignored) {
          } finally {
            cursor.close();
          }
        }
      } catch (SecurityException e) {
        //this can happen if the user has restored a backup and
        //we do not have a persistable permision
        //return null;
      }
    }
    List<String> filePathSegments = uri.getPathSegments();
    if (!filePathSegments.isEmpty()) {
      return filePathSegments.get(filePathSegments.size() - 1);
    } else {
      return "UNKNOWN";
    }
  }

  public interface PasswordDialogUnlockedCallback {
    void onPasswordDialogUnlocked();
  }

  static class PasswordDialogListener implements View.OnClickListener {
    private final AlertDialog dialog;
    private final ProtectedFragmentActivity ctx;
    private final PasswordDialogUnlockedCallback callback;

    public PasswordDialogListener(ProtectedFragmentActivity ctx, AlertDialog dialog,
                                  PasswordDialogUnlockedCallback callback) {
      this.dialog = dialog;
      this.ctx = ctx;
      this.callback = callback;
    }

    @Override
    public void onClick(View v) {
      final String securityQuestion = PrefKey.SECURITY_QUESTION.getString("");
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
            (isInSecurityQuestion ? PrefKey.SECURITY_ANSWER : PrefKey.SET_PASSWORD).getString(""))) {
          input.setText("");
          error.setText("");
          callback.onPasswordDialogUnlocked();
          if (isInSecurityQuestion) {
            PrefKey.PROTECTION_LEGACY.putBoolean(false);
            ctx.showDismissibleSnackbar(R.string.password_disabled_reenable);
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

  public static RadioGroup configureCalendarRestoreStrategy(View view) {
    RadioGroup restorePlanStrategie = (RadioGroup) view.findViewById(R.id.restore_calendar_handling);
    String calendarId = PrefKey.PLANNER_CALENDAR_ID.getString("-1");
    String calendarPath = PrefKey.PLANNER_CALENDAR_PATH.getString("");
    RadioButton configured = (RadioButton) view.findViewById(R.id.restore_calendar_handling_configured);
    if ((calendarId.equals("-1")) || calendarPath.equals("")) {
      configured.setVisibility(View.GONE);
    } else {
      //noinspection SetTextI18n
      configured.setText(configured.getText() + " (" + calendarPath + ")");
    }
    return restorePlanStrategie;
  }

  public static RadioGroup.OnCheckedChangeListener buildCalendarRestoreStrategyChangedListener(
      final Activity context, final CalendarRestoreStrategyChangedListener listener) {
    return (group, checkedId) -> {
      if (checkedId == R.id.restore_calendar_handling_backup ||
          checkedId == R.id.restore_calendar_handling_configured) {
        if (!CALENDAR.hasPermission(context)) {
          if (context instanceof ProtectedFragmentActivity) {
            ((ProtectedFragmentActivity) context).requestPermission(PermissionGroup.CALENDAR);
          }
        }
      }
      listener.onCheckedChanged();
    };
  }

  public interface CalendarRestoreStrategyChangedListener {
    void onCheckedChanged();

    void onCalendarPermissionDenied();
  }

  public static void configureDateFormat(Spinner spinner, Context context, PrefHandler prefHandler, String prefName) {
    ArrayAdapter<QifDateFormat> dateFormatAdapter =
        new ArrayAdapter<>(
            context, android.R.layout.simple_spinner_item, QifDateFormat.values());
    dateFormatAdapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
    spinner.setAdapter(dateFormatAdapter);
    QifDateFormat qdf;
    try {
      qdf = QifDateFormat.valueOf(prefHandler.getString(prefName, "EU"));
    } catch (IllegalArgumentException e) {
      qdf = QifDateFormat.EU;
    }
    spinner.setSelection(qdf.ordinal());
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

  public static void configureCurrencySpinner(Spinner spinner, AdapterView.OnItemSelectedListener listener) {
    CurrencyAdapter curAdapter = new CurrencyAdapter(spinner.getContext(), android.R.layout.simple_spinner_item);
    spinner.setAdapter(curAdapter);
    spinner.setOnItemSelectedListener(listener);
  }

  public static void configureTypeSpinner(Spinner spinner) {
    ArrayAdapter<AccountType> typAdapter = new AccountTypeAdapter(spinner.getContext());
    spinner.setAdapter(typAdapter);
  }

  public static void openBrowse(Uri uri, Fragment fragment) {
    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);//TODO implement preference that allows to use ACTION_OPEN_DOCUMENT
    intent.addCategory(Intent.CATEGORY_OPENABLE);

    if (Utils.hasApiLevel(Build.VERSION_CODES.KITKAT)) {
      intent.setType("*/*");
    } else {
      //setting uri does not have any affect in Storage Access Framework's file picker,
      //on Nougat it even can lead to FileURIExposedException if the uri passed is of scheme file
      intent.setDataAndType(uri, "*/*");
    }

    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

    try {
      fragment.startActivityForResult(intent, IMPORT_FILENAME_REQUEST_CODE);
    } catch (ActivityNotFoundException e) {
      ((ProtectedFragmentActivity) fragment.getActivity()).showSnackbar(R.string.no_filemanager_installed, Snackbar.LENGTH_SHORT);
    } catch (SecurityException ex) {
      ((ProtectedFragmentActivity) fragment.getActivity()).showSnackbar(String.format(
              "Sorry, this destination does not accept %s request. Please select a different one.", intent.getAction()),
          Snackbar.LENGTH_SHORT);
    }
  }

  public static void showSyncUnlinkConfirmationDialog(FragmentActivity context, Account account) {
    Bundle b = new Bundle();
    b.putString(ConfirmationDialogFragment.KEY_MESSAGE,
        context.getString(R.string.dialog_confirm_sync_unlink, account.getSyncAccountName()));
    b.putString(DatabaseConstants.KEY_UUID, account.getUuid());
    b.putInt(ConfirmationDialogFragment.KEY_COMMAND_POSITIVE, R.id.SYNC_UNLINK_COMMAND);
    b.putInt(ConfirmationDialogFragment.KEY_POSITIVE_BUTTON_LABEL, R.string.menu_sync_unlink);
    b.putInt(ConfirmationDialogFragment.KEY_NEGATIVE_BUTTON_LABEL, android.R.string.cancel);
    ConfirmationDialogFragment.newInstance(b).show(context.getSupportFragmentManager(), "SYNC_UNLINK");
  }
}
