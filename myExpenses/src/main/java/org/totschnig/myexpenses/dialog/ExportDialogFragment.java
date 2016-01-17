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

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.PopupWindow;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.MyExpenses;
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment.ConfirmationDialogListener;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.preference.SharedPreferencesCompat;
import org.totschnig.myexpenses.task.ExportTask;
import org.totschnig.myexpenses.task.TaskExecutionFragment;
import org.totschnig.myexpenses.util.Result;
import org.totschnig.myexpenses.util.Utils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID;

public class ExportDialogFragment extends CommitSafeDialogFragment implements android.content.DialogInterface.OnClickListener, OnCheckedChangeListener {
  private static final String KEY_IS_FILTERED = "is_filtered";
  RadioGroup handleDeletedGroup;
  CheckBox notYetExportedCB, deleteCB;
  RadioButton formatRBCSV, separatorRBComma;
  TextView warningTV;
  EditText dateFormatET, fileNameET;
  AlertDialog mDialog;
  String currency;
  static final String PREFKEY_EXPORT_DATE_FORMAT = "export_date_format";
  static final String PREFKEY_EXPORT_ENCODING = "export_encoding";
  private int handleDeletedAction = Account.EXPORT_HANDLE_DELETED_DO_NOTHING;

  public static ExportDialogFragment newInstance(Long accountId, boolean isFiltered) {
    ExportDialogFragment dialogFragment = new ExportDialogFragment();
    if (accountId != null) {
      Bundle bundle = new Bundle();
      bundle.putLong(KEY_ACCOUNTID, accountId);
      bundle.putBoolean(KEY_IS_FILTERED, isFiltered);
      dialogFragment.setArguments(bundle);
    }
    return dialogFragment;
  }

  @TargetApi(Build.VERSION_CODES.HONEYCOMB)
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    MyExpenses ctx = (MyExpenses) getActivity();
    Bundle args = getArguments();
    Long accountId = args != null ? args.getLong(KEY_ACCOUNTID) : null;
    boolean allP = false, hasExported;
    String warningText;
    final String fileName;
    String now = new SimpleDateFormat("yyyMMdd-HHmmss", Locale.US)
        .format(new Date());

    if (accountId == null) {
      allP = true;
      warningText = getString(R.string.warning_reset_account_all);
      //potential Strict mode violation (currently exporting all accounts with different currencies is not active in the UI)
      hasExported = Account.getHasExported(null);
      fileName = "export" + "-" + now;
    } else {
      Account a = Account.getInstanceFromDb(accountId);
      hasExported = ctx.hasExported();
      if (accountId < 0L) {
        allP = true;
        currency = a.currency.getCurrencyCode();
        fileName = "export" + "-" + currency + "-" + now;
        warningText = getString(R.string.warning_reset_account_all, " (" + currency + ")");
      } else {
        fileName = Utils.escapeForFileName(a.label) + "-" + now;
        warningText = getString(R.string.warning_reset_account);
      }
    }

    LayoutInflater li = LayoutInflater.from(ctx);
    View view = li.inflate(R.layout.export_dialog, null);


    if (args.getBoolean(KEY_IS_FILTERED)) {
      view.findViewById(R.id.with_filter).setVisibility(View.VISIBLE);
      warningText = getString(R.string.warning_reset_account_matched);
    }

    dateFormatET = (EditText) view.findViewById(R.id.date_format);
    String dateFormatDefault =
        ((SimpleDateFormat) DateFormat.getDateInstance(DateFormat.SHORT)).toPattern();
    String dateFormat = MyApplication.getInstance().getSettings()
        .getString(PREFKEY_EXPORT_DATE_FORMAT, "");
    if (dateFormat.equals(""))
      dateFormat = dateFormatDefault;
    else {
      try {
        new SimpleDateFormat(dateFormat, Locale.US);
      } catch (IllegalArgumentException e) {
        dateFormat = dateFormatDefault;
      }
    }
    dateFormatET.setText(dateFormat);
    dateFormatET.addTextChangedListener(new TextWatcher() {
      public void afterTextChanged(Editable s) {
        try {
          new SimpleDateFormat(s.toString(), Locale.US);
          mDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
        } catch (IllegalArgumentException e) {
          dateFormatET.setError(getString(R.string.date_format_illegal));
          mDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
        }
      }

      public void beforeTextChanged(CharSequence s, int start, int count, int after) {
      }

      public void onTextChanged(CharSequence s, int start, int before, int count) {
      }
    });

    fileNameET = (EditText) view.findViewById(R.id.file_name);
    fileNameET.setText(fileName);
    fileNameET.addTextChangedListener(new TextWatcher() {
      public void afterTextChanged(Editable s) {
        int error = 0;
        if (s.toString().length() > 0) {
          if (s.toString().indexOf('/') > -1) {
            error = R.string.slash_forbidden_in_filename;
          }
        } else {
          error = R.string.no_title_given;
        }
        mDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(error == 0);
        if (error != 0) {
          fileNameET.setError(getString(error));
        }
      }

      public void beforeTextChanged(CharSequence s, int start, int count, int after) {
      }

      public void onTextChanged(CharSequence s, int start, int before, int count) {
      }
    });

    notYetExportedCB = (CheckBox) view.findViewById(R.id.export_not_yet_exported);
    deleteCB = (CheckBox) view.findViewById(R.id.export_delete);
    warningTV = (TextView) view.findViewById(R.id.warning_reset);

    String encoding = MyApplication.getInstance().getSettings()
        .getString(PREFKEY_EXPORT_ENCODING, "UTF-8");

    ((Spinner) view.findViewById(R.id.Encoding)).setSelection(
        Arrays.asList(getResources().getStringArray(R.array.pref_qif_export_file_encoding))
            .indexOf(encoding));

    formatRBCSV = (RadioButton) view.findViewById(R.id.csv);
    String format = MyApplication.PrefKey.EXPORT_FORMAT.getString("QIF");
    if (format.equals("CSV")) {
      formatRBCSV.setChecked(true);
    }

    separatorRBComma = (RadioButton) view.findViewById(R.id.comma);
    char separator = (char) MyApplication.getInstance().getSettings()
        .getInt(ExportTask.KEY_DECIMAL_SEPARATOR, Utils.getDefaultDecimalSeparator());
    if (separator == ',') {
      separatorRBComma.setChecked(true);
    }

    handleDeletedGroup = (RadioGroup) view.findViewById(R.id.handle_deleted);
    View.OnClickListener radioClickListener = new View.OnClickListener() {
      public void onClick(View v)
      {
        int mappedAction = v.getId() == R.id.create_helper ?
            Account.EXPORT_HANDLE_DELETED_CREATE_HELPER : Account.EXPORT_HANDLE_DELETED_UPDATE_BALANCE;
        if (handleDeletedAction == mappedAction) {
          handleDeletedAction = Account.EXPORT_HANDLE_DELETED_DO_NOTHING;
          handleDeletedGroup.clearCheck();
        } else {
          handleDeletedAction = mappedAction;
        }
      }
    };

    final RadioButton updateBalanceRadioButton = (RadioButton) view.findViewById(R.id.update_balance);
    final RadioButton createHelperRadioButton = (RadioButton) view.findViewById(R.id.create_helper);
    updateBalanceRadioButton.setOnClickListener(radioClickListener);
    createHelperRadioButton.setOnClickListener(radioClickListener);

    if (savedInstanceState == null) {
      handleDeletedAction = MyApplication.getInstance().getSettings()
          .getInt(ExportTask.KEY_EXPORT_HANDLE_DELETED, Account.EXPORT_HANDLE_DELETED_DO_NOTHING);
      if (handleDeletedAction == Account.EXPORT_HANDLE_DELETED_UPDATE_BALANCE) {
        updateBalanceRadioButton.setChecked(true);
      } else if (handleDeletedAction == Account.EXPORT_HANDLE_DELETED_CREATE_HELPER) {
        createHelperRadioButton.setChecked(true);
      }
    }

    deleteCB.setOnCheckedChangeListener(this);
    if (hasExported) {
      notYetExportedCB.setChecked(true);
      notYetExportedCB.setVisibility(View.VISIBLE);
    }

    warningTV.setText(warningText);
    if (allP) {
      ((TextView) view.findViewById(R.id.file_name_label)).setText(R.string.folder_name);
    }
    
    final View helpIcon = view.findViewById(R.id.date_format_help);
    helpIcon.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        final TextView infoText = (TextView) inflater.inflate(
            R.layout.textview_info, null);
        infoText.setText("This is my very long help text one long line\nand a short one ++++");

        final float scale = getResources().getDisplayMetrics().density;
        final PopupWindow infoWindow = new PopupWindow(infoText,
            (int)(200 * scale + 0.5f), (int)(50 * scale + 0.5f));
        infoWindow.setBackgroundDrawable(new BitmapDrawable());
        infoWindow.setOutsideTouchable(true);
        infoWindow.setFocusable(true);
        infoWindow.showAsDropDown(helpIcon);
        infoWindow.setTouchInterceptor(new View.OnTouchListener() {

          @Override
          public boolean onTouch(View v, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
              infoWindow.dismiss();
            }
            return true;
          }
        });
      }
    });

    AlertDialog.Builder builder = new AlertDialog.Builder(ctx)
        .setTitle(allP ? R.string.menu_reset_all : R.string.menu_reset)
        .setView(view)
        .setPositiveButton(android.R.string.ok, this)
        .setNegativeButton(android.R.string.cancel, null);
    if (Build.VERSION.SDK_INT < 11)
      builder.setIcon(android.R.drawable.ic_dialog_alert);
    else
      builder.setIconAttribute(android.R.attr.alertDialogIcon);

    mDialog = builder.create();
    return mDialog;
  }

  private View createToolTipView(String text, int textColor, int bgColor) {
    float density = getResources().getDisplayMetrics().density;
    int padding = (int) (8 * density);

    TextView contentView = new TextView(getActivity());
    contentView.setPadding(padding, padding, padding, padding);
    contentView.setText(text);
    contentView.setTextColor(textColor);
    contentView.setBackgroundColor(bgColor);
    return contentView;
  }

  @Override
  public void onClick(DialogInterface dialog, int which) {
    Activity ctx = getActivity();
    if (ctx == null) {
      return;
    }
    Bundle args = getArguments();
    Long accountId = args != null ? args.getLong(KEY_ACCOUNTID) : null;
    AlertDialog dlg = (AlertDialog) dialog;
    String format = ((RadioGroup) dlg.findViewById(R.id.format)).getCheckedRadioButtonId() == R.id.csv ?
        "CSV" : "QIF";
    String dateFormat = dateFormatET.getText().toString();
    char decimalSeparator = ((RadioGroup) dlg.findViewById(R.id.separator)).getCheckedRadioButtonId() == R.id.dot ?
        '.' : ',';

    int handleDeleted;
    switch (handleDeletedGroup.getCheckedRadioButtonId()) {
      case R.id.update_balance:
        handleDeleted = Account.EXPORT_HANDLE_DELETED_UPDATE_BALANCE;
        break;
      case R.id.create_helper:
        handleDeleted = Account.EXPORT_HANDLE_DELETED_CREATE_HELPER;
        break;
      default: //-1
        handleDeleted = Account.EXPORT_HANDLE_DELETED_DO_NOTHING;
    }

    String encoding = (String) ((Spinner) dlg.findViewById(R.id.Encoding)).getSelectedItem();
    SharedPreferencesCompat.apply(
        MyApplication.getInstance().getSettings().edit()
            .putString(MyApplication.PrefKey.EXPORT_FORMAT.getKey(), format)
            .putString(PREFKEY_EXPORT_DATE_FORMAT, dateFormat)
            .putString(PREFKEY_EXPORT_ENCODING, encoding)
            .putInt(ExportTask.KEY_DECIMAL_SEPARATOR, decimalSeparator)
            .putInt(ExportTask.KEY_EXPORT_HANDLE_DELETED, handleDeleted));
    boolean deleteP = deleteCB.isChecked();
    boolean notYetExportedP = notYetExportedCB.isChecked();
    String fileName = fileNameET.getText().toString();
    Result appDirStatus = Utils.checkAppDir();
    if (appDirStatus.success) {
      Bundle b = new Bundle();
      b.putInt(ConfirmationDialogFragment.KEY_COMMAND_POSITIVE,
          R.id.START_EXPORT_COMMAND);
      if (accountId == null) {
      } else if (accountId > 0) {
        b.putLong(KEY_ROWID, accountId);
      } else {
        b.putString(KEY_CURRENCY, currency);
      }
      b.putString(TaskExecutionFragment.KEY_FORMAT, format);
      b.putBoolean(ExportTask.KEY_DELETE_P, deleteP);
      b.putBoolean(ExportTask.KEY_NOT_YET_EXPORTED_P, notYetExportedP);
      b.putString(TaskExecutionFragment.KEY_DATE_FORMAT, dateFormat);
      b.putChar(ExportTask.KEY_DECIMAL_SEPARATOR, decimalSeparator);
      b.putString(TaskExecutionFragment.KEY_ENCODING, encoding);
      b.putInt(ExportTask.KEY_EXPORT_HANDLE_DELETED, handleDeleted);
      b.putString(ExportTask.KEY_FILE_NAME, fileName);
      if (Utils.checkAppFolderWarning()) {
        ((ConfirmationDialogListener) getActivity())
            .onPositive(b);
      } else {
        b.putInt(ConfirmationDialogFragment.KEY_TITLE,
            R.string.dialog_title_attention);
        b.putString(ConfirmationDialogFragment.KEY_MESSAGE,
            getString(R.string.warning_app_folder_will_be_deleted_upon_uninstall));
        b.putString(ConfirmationDialogFragment.KEY_PREFKEY,
            MyApplication.PrefKey.APP_FOLDER_WARNING_SHOWN.getKey());
        ConfirmationDialogFragment.newInstance(b)
            .show(getFragmentManager(), "APP_FOLDER_WARNING");
      }
    } else {
      Toast.makeText(ctx,
          appDirStatus.print(ctx),
          Toast.LENGTH_LONG)
          .show();
    }
  }

  @Override
  public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
    configure(isChecked);
  }

  /* 
   * if we are in the situation, where there are already exported transactions
   * we suggest to the user the default of again exporting without deleting
   * but if the user now changes to deleting, we enforce a complete export/reset
   * since a partial deletion of only transactions not yet exported would
   * lead to an inconsistent state
   */
  private void configure(boolean delete) {
    if (delete) {
      notYetExportedCB.setEnabled(false);
      notYetExportedCB.setChecked(false);
      warningTV.setVisibility(View.VISIBLE);
      handleDeletedGroup.setVisibility(View.VISIBLE);
    } else {
      notYetExportedCB.setEnabled(true);
      notYetExportedCB.setChecked(true);
      warningTV.setVisibility(View.GONE);
      handleDeletedGroup.setVisibility(View.GONE);
    }
  }

  @Override
  public void onStart() {
    super.onStart();
    configure(deleteCB.isChecked());
    int checkedId = handleDeletedGroup.getCheckedRadioButtonId();
    if (checkedId == R.id.update_balance)
      handleDeletedAction = Account.EXPORT_HANDLE_DELETED_UPDATE_BALANCE;
    else if (checkedId == R.id.create_helper)
      handleDeletedAction = Account.EXPORT_HANDLE_DELETED_CREATE_HELPER;
  }
}
