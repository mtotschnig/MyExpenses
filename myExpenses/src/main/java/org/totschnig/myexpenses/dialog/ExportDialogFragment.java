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
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.PopupWindow;
import android.widget.RadioButton;
import android.widget.TextView;

import com.google.android.material.snackbar.Snackbar;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.databinding.ExportDialogBinding;
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment.ConfirmationDialogListener;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.preference.PrefKey;
import org.totschnig.myexpenses.task.ExportTask;
import org.totschnig.myexpenses.task.TaskExecutionFragment;
import org.totschnig.myexpenses.util.AppDirHelper;
import org.totschnig.myexpenses.util.Result;
import org.totschnig.myexpenses.util.Utils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.text.HtmlCompat;

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID;

public class ExportDialogFragment extends BaseDialogFragment implements OnClickListener, OnCheckedChangeListener {
  private static final String KEY_IS_FILTERED = "is_filtered";
  private ExportDialogBinding binding;

  AlertDialog mDialog;
  String currency;
  static final String PREF_KEY_EXPORT_DATE_FORMAT = "export_date_format";
  static final String PREF_KEY_EXPORT_ENCODING = "export_encoding";
  private int handleDeletedAction = Account.EXPORT_HANDLE_DELETED_DO_NOTHING;

  public static ExportDialogFragment newInstance(long accountId, boolean isFiltered) {
    ExportDialogFragment dialogFragment = new ExportDialogFragment();
    Bundle bundle = new Bundle();
    bundle.putLong(KEY_ACCOUNTID, accountId);
    bundle.putBoolean(KEY_IS_FILTERED, isFiltered);
    dialogFragment.setArguments(bundle);
    return dialogFragment;
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    ((MyApplication) requireActivity().getApplication()).getAppComponent().inject(this);
  }

  @NonNull
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    Bundle args = getArguments();
    if (args == null) {
      throw new IllegalStateException("Cannot be used without args");
    }
    long accountId = args.getLong(KEY_ACCOUNTID);
    boolean allP = false;
    String warningText;
    final String fileName;
    String now = new SimpleDateFormat("yyyMMdd-HHmmss", Locale.US)
        .format(new Date());
    AlertDialog.Builder builder = initBuilderWithBinding(() -> {
      binding = ExportDialogBinding.inflate(materialLayoutInflater);
      return binding;
    });

    //TODO Strict mode violation
    Account a = Account.getInstanceFromDb(accountId);
    if (a == null) {
      binding.error.setVisibility(View.VISIBLE);
      binding.Table.setVisibility(View.GONE);
      binding.exportDelete.setVisibility(View.GONE);
      binding.error.setText("Unable to instantiate account " + accountId);
    } else {
      boolean canReset = !a.isSealed();
      boolean hasExported = Account.getHasExported(accountId);
      if (accountId == Account.HOME_AGGREGATE_ID) {
        allP = true;
        warningText = getString(R.string.warning_reset_account_all, "");
        fileName = "export" + "-" + now;
      } else {
        if (accountId < 0L) {
          allP = true;
          currency = a.getCurrencyUnit().getCode();
          fileName = "export" + "-" + currency + "-" + now;
          warningText = getString(R.string.warning_reset_account_all, " (" + currency + ")");
        } else {
          fileName = Utils.escapeForFileName(a.getLabel()) + "-" + now;
          warningText = getString(R.string.warning_reset_account);
        }
      }

      if (args.getBoolean(KEY_IS_FILTERED)) {
        dialogView.findViewById(R.id.with_filter).setVisibility(View.VISIBLE);
        warningText = getString(R.string.warning_reset_account_matched);
      }

      String dateFormatDefault =
          ((SimpleDateFormat) DateFormat.getDateInstance(DateFormat.SHORT)).toPattern();
      String dateFormat = prefHandler.getString(PREF_KEY_EXPORT_DATE_FORMAT, "");
      if (dateFormat.equals(""))
        dateFormat = dateFormatDefault;
      else {
        try {
          new SimpleDateFormat(dateFormat, Locale.US);
        } catch (IllegalArgumentException e) {
          dateFormat = dateFormatDefault;
        }
      }
      binding.dateFormat.setText(dateFormat);
      binding.dateFormat.addTextChangedListener(new TextWatcher() {
        public void afterTextChanged(Editable s) {
          try {
            new SimpleDateFormat(s.toString(), Locale.US);
            binding.dateFormat.setError(null);
          } catch (IllegalArgumentException e) {
            binding.dateFormat.setError(getString(R.string.date_format_illegal));
          }
          configureButton();
        }

        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }
      });

      binding.fileName.setText(fileName);
      binding.fileName.addTextChangedListener(new TextWatcher() {
        public void afterTextChanged(Editable s) {
          int error = 0;
          if (s.toString().length() > 0) {
            if (s.toString().indexOf('/') > -1) {
              error = R.string.slash_forbidden_in_filename;
            }
          } else {
            error = R.string.no_title_given;
          }
          binding.fileName.setError(error != 0 ? getString(error) : null);
          configureButton();
        }

        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }
      });
      binding.fileName.setFilters(new InputFilter[]{
          (source, start, end, dest, dstart, dend) -> {
            StringBuilder sb = new StringBuilder(end - start);
            for (int i = start; i < end; i++) {
              final char c = source.charAt(i);
              int type = Character.getType(c);
              if (type != Character.SURROGATE && type != Character.OTHER_SYMBOL) {
                sb.append(c);
              }
            }
            return sb;
          }
      });

      String encoding = prefHandler.getString(PREF_KEY_EXPORT_ENCODING, "UTF-8");

      binding.Encoding.setSelection(
          Arrays.asList(getResources().getStringArray(R.array.pref_qif_export_file_encoding))
              .indexOf(encoding));

      binding.format.setOnCheckedChangeListener((group, checkedId) ->
          binding.DelimiterRow.setVisibility(checkedId == R.id.csv ? View.VISIBLE : View.GONE));
      String format = prefHandler.getString(PrefKey.EXPORT_FORMAT, "QIF");
      binding.format.check(format.equals("CSV") ? R.id.csv : R.id.qif);

      char delimiter = (char) prefHandler.getInt(ExportTask.KEY_DELIMITER, ',');
      @IdRes final int delimiterButtonResId;
      switch (delimiter) {
        case ';':
          delimiterButtonResId = R.id.delimiter_semicolon;
          break;
        case '\t':
          delimiterButtonResId = R.id.delimiter_tab;
          break;
        case ',':
        default:
          delimiterButtonResId = R.id.delimiter_comma;
      }
      binding.Delimiter.check(delimiterButtonResId);

      char separator = (char) prefHandler.getInt(
          ExportTask.KEY_DECIMAL_SEPARATOR, Utils.getDefaultDecimalSeparator());
      binding.separator.check(separator == ',' ? R.id.comma : R.id.dot);

      View.OnClickListener radioClickListener = v -> {
        int mappedAction = v.getId() == R.id.create_helper ?
            Account.EXPORT_HANDLE_DELETED_CREATE_HELPER : Account.EXPORT_HANDLE_DELETED_UPDATE_BALANCE;
        if (handleDeletedAction == mappedAction) {
          handleDeletedAction = Account.EXPORT_HANDLE_DELETED_DO_NOTHING;
          binding.handleDeleted.clearCheck();
        } else {
          handleDeletedAction = mappedAction;
        }
      };

      final RadioButton updateBalanceRadioButton = dialogView.findViewById(R.id.update_balance);
      final RadioButton createHelperRadioButton = dialogView.findViewById(R.id.create_helper);
      updateBalanceRadioButton.setOnClickListener(radioClickListener);
      createHelperRadioButton.setOnClickListener(radioClickListener);

      if (savedInstanceState == null) {
        handleDeletedAction = prefHandler.getInt(
            ExportTask.KEY_EXPORT_HANDLE_DELETED, Account.EXPORT_HANDLE_DELETED_DO_NOTHING);
        if (handleDeletedAction == Account.EXPORT_HANDLE_DELETED_UPDATE_BALANCE) {
          updateBalanceRadioButton.setChecked(true);
        } else if (handleDeletedAction == Account.EXPORT_HANDLE_DELETED_CREATE_HELPER) {
          createHelperRadioButton.setChecked(true);
        }
      }

      if (canReset) {
        binding.exportDelete.setOnCheckedChangeListener(this);
      } else {
        binding.exportDelete.setVisibility(View.GONE);
      }
      if (hasExported) {
        binding.exportNotYetExported.setChecked(true);
        binding.exportNotYetExported.setVisibility(View.VISIBLE);
      }

      binding.warningReset.setText(warningText);
      if (allP) {
        boolean mergeAccounts = prefHandler.getBoolean(ExportTask.KEY_MERGE_P, false);
        setFileNameLabel(false);
        binding.mergeAccounts.setVisibility(View.VISIBLE);
        binding.mergeAccounts.setOnCheckedChangeListener((buttonView, isChecked) -> setFileNameLabel(isChecked));
        binding.mergeAccounts.setChecked(mergeAccounts);
      }

      final View helpIcon = dialogView.findViewById(R.id.date_format_help);
      helpIcon.setOnClickListener(v -> {
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        //noinspection InflateParams
        final TextView infoTextView = (TextView) inflater.inflate(
            R.layout.textview_info, null);
        final CharSequence infoText = buildDateFormatHelpText();
        final PopupWindow infoWindow = new PopupWindow(infoTextView);

        infoWindow.setBackgroundDrawable(new BitmapDrawable());
        infoWindow.setOutsideTouchable(true);
        infoWindow.setFocusable(true);
        chooseSize(infoWindow, infoText, infoTextView);
        infoTextView.setText(infoText);
        infoTextView.setMovementMethod(LinkMovementMethod.getInstance());
        //Linkify.addLinks(infoTextView, Linkify.WEB_URLS | Linkify.EMAIL_ADDRESSES);
        infoWindow.showAsDropDown(helpIcon);
      });
    }

    builder.setTitle(allP ? R.string.menu_reset_all : R.string.menu_reset)
        .setNegativeButton(android.R.string.cancel, null)
        .setIcon(R.drawable.ic_warning);
    if (a != null) {
        builder.setPositiveButton(android.R.string.ok, this);
    }

    mDialog = builder.create();
    return mDialog;
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    binding = null;
  }

  private void setFileNameLabel(boolean oneFile) {
    binding.fileNameLabel.setText(oneFile ? R.string.file_name : R.string.folder_name);
  }

  /* adapted from android.widget.Editor */
  private void chooseSize(PopupWindow pop, CharSequence text, TextView tv) {
    int ht = tv.getPaddingTop() + tv.getPaddingBottom();

    int widthInPixels = (int) (mDialog.getWindow().getDecorView().getWidth() * 0.75);
    Layout l = new StaticLayout(text, tv.getPaint(), widthInPixels,
        Layout.Alignment.ALIGN_NORMAL, 1, 0, true);

    ht += l.getHeight();
    pop.setWidth(widthInPixels);
    pop.setHeight(ht);
  }

  private CharSequence buildDateFormatHelpText() {
    String[] letters = getResources().getStringArray(R.array.help_ExportDialog_date_format_letters);
    String[] components = getResources().getStringArray(R.array.help_ExportDialog_date_format_components);
    StringBuilder sb = new StringBuilder();

    for (int i = 0; i < letters.length; i++) {
      sb.append(letters[i]);
      sb.append(" => ");
      sb.append(components[i]);
      if (i < letters.length - 1)
        sb.append(", ");
      else
        sb.append(". ");
    }
    return TextUtils.concat(sb, HtmlCompat.fromHtml(getString(R.string.help_ExportDialog_date_format), HtmlCompat.FROM_HTML_MODE_LEGACY));
  }


  @Override
  public void onClick(DialogInterface dialog, int which) {
    Activity ctx = getActivity();
    Bundle args = getArguments();
    Long accountId = args != null ? args.getLong(KEY_ACCOUNTID) : null;
    if (ctx == null || accountId == null || accountId == 0) {
      return;
    }
    String format = binding.format.getCheckedRadioButtonId() == R.id.csv ? "CSV" : "QIF";
    String dateFormat = binding.dateFormat.getText().toString();
    char decimalSeparator = binding.separator.getCheckedRadioButtonId() == R.id.dot ? '.' : ',';
    final char delimiter;
    int checkedRadioButtonId = binding.Delimiter.getCheckedRadioButtonId();
    if (checkedRadioButtonId == R.id.delimiter_tab) {
      delimiter = '\t';
    } else if (checkedRadioButtonId == R.id.delimiter_semicolon) {
      delimiter = ';';
    } else {
      delimiter = ',';
    }

    int handleDeleted;
    int radioButtonId = binding.handleDeleted.getCheckedRadioButtonId();//-1
    if (radioButtonId == R.id.update_balance) {
      handleDeleted = Account.EXPORT_HANDLE_DELETED_UPDATE_BALANCE;
    } else if (radioButtonId == R.id.create_helper) {
      handleDeleted = Account.EXPORT_HANDLE_DELETED_CREATE_HELPER;
    } else {
      handleDeleted = Account.EXPORT_HANDLE_DELETED_DO_NOTHING;
    }

    String encoding = (String) binding.Encoding.getSelectedItem();
    prefHandler.putString(PrefKey.EXPORT_FORMAT, format);
    prefHandler.putString(PREF_KEY_EXPORT_DATE_FORMAT, dateFormat);
    prefHandler.putString(PREF_KEY_EXPORT_ENCODING, encoding);
    prefHandler.putInt(ExportTask.KEY_DECIMAL_SEPARATOR, decimalSeparator);
    prefHandler.putInt(ExportTask.KEY_DELIMITER, delimiter);
    prefHandler.putInt(ExportTask.KEY_EXPORT_HANDLE_DELETED, handleDeleted);

    boolean deleteP = binding.exportDelete.isChecked();
    boolean notYetExportedP =  binding.exportNotYetExported.isChecked();
    String fileName = binding.fileName.getText().toString();
    Result appDirStatus = AppDirHelper.checkAppDir(getActivity());
    if (appDirStatus.isSuccess()) {
      Bundle b = new Bundle();
      b.putInt(ConfirmationDialogFragment.KEY_COMMAND_POSITIVE,
          R.id.START_EXPORT_COMMAND);
      if (accountId > 0) {
        b.putLong(KEY_ROWID, accountId);
      } else {
        b.putString(KEY_CURRENCY, currency);
        final boolean mergeAccounts = binding.mergeAccounts.isChecked();
        b.putBoolean(ExportTask.KEY_MERGE_P, mergeAccounts);
        prefHandler.putBoolean(ExportTask.KEY_MERGE_P, mergeAccounts);
      }
      b.putString(TaskExecutionFragment.KEY_FORMAT, format);
      b.putBoolean(ExportTask.KEY_DELETE_P, deleteP);
      b.putBoolean(ExportTask.KEY_NOT_YET_EXPORTED_P, notYetExportedP);
      b.putString(TaskExecutionFragment.KEY_DATE_FORMAT, dateFormat);
      b.putChar(ExportTask.KEY_DECIMAL_SEPARATOR, decimalSeparator);
      b.putString(TaskExecutionFragment.KEY_ENCODING, encoding);
      b.putInt(ExportTask.KEY_EXPORT_HANDLE_DELETED, handleDeleted);
      b.putString(ExportTask.KEY_FILE_NAME, fileName);
      b.putChar(ExportTask.KEY_DELIMITER, delimiter);
      if (AppDirHelper.checkAppFolderWarning(getActivity())) {
        ((ConfirmationDialogListener) getActivity())
            .onPositive(b);
      } else {
        b.putInt(ConfirmationDialogFragment.KEY_TITLE,
            R.string.dialog_title_attention);
        b.putCharSequence(
            ConfirmationDialogFragment.KEY_MESSAGE,
            Utils.getTextWithAppName(getContext(), R.string.warning_app_folder_will_be_deleted_upon_uninstall));
        b.putString(ConfirmationDialogFragment.KEY_PREFKEY, prefHandler.getKey(PrefKey.APP_FOLDER_WARNING_SHOWN));
        ConfirmationDialogFragment.newInstance(b)
            .show(getParentFragmentManager(), "APP_FOLDER_WARNING");
      }
    } else {
      showSnackbar(appDirStatus.print(ctx), Snackbar.LENGTH_LONG, null);
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
    binding.exportNotYetExported.setEnabled(!delete);
    binding.exportNotYetExported.setChecked(!delete);
    binding.warningReset.setVisibility(delete ? View.VISIBLE: View.GONE);
    binding.handleDeleted.setVisibility(delete ? View.VISIBLE: View.GONE);
  }

  private void configureButton() {
    mDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(binding.dateFormat.getError() == null &&
        binding.fileName.getError() == null);
  }

  @Override
  public void onStart() {
    super.onStart();
    configure(binding.exportDelete.isChecked());
    int checkedId = binding.handleDeleted.getCheckedRadioButtonId();
    if (checkedId == R.id.update_balance)
      handleDeletedAction = Account.EXPORT_HANDLE_DELETED_UPDATE_BALANCE;
    else if (checkedId == R.id.create_helper)
      handleDeletedAction = Account.EXPORT_HANDLE_DELETED_CREATE_HELPER;
  }
}
