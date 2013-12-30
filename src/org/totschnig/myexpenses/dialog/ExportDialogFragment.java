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

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.Export;
import org.totschnig.myexpenses.util.Utils;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.AggregateAccount;
import org.totschnig.myexpenses.model.DataObjectNotFoundException;
import org.totschnig.myexpenses.model.ContribFeature.Feature;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

public class ExportDialogFragment extends DialogFragment implements android.content.DialogInterface.OnClickListener, android.view.View.OnClickListener {
  CheckBox notYetExportedCB,deleteCB;
  RadioButton formatRB;
  TextView warningTV;
  EditText dateFormatET;
  AlertDialog mDialog;
  static final String PREFKEY_EXPORT_DATE_FORMAT = "export_date_format";
  
  public static final ExportDialogFragment newInstance(Long accountId) {
    ExportDialogFragment dialogFragment = new ExportDialogFragment();
    if (accountId != null) {
      Bundle bundle = new Bundle();
      bundle.putLong("accountId", accountId);
      dialogFragment.setArguments(bundle);
    }
    return dialogFragment;
  }
  
  @TargetApi(Build.VERSION_CODES.HONEYCOMB)
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    Activity ctx  = (Activity) getActivity();
    Context wrappedCtx = DialogUtils.wrapContext1(ctx);
    Bundle args = getArguments();
    Long accountId = args != null ? args.getLong("accountId") : null;
    boolean allP = false;
    String warningText;
    if (accountId == null) {
      allP = true;
      warningText = getString(R.string.warning_reset_account_all);
    } else if (accountId < 0L) {
      AggregateAccount aa = AggregateAccount.getCachedInstance(accountId);
      if (aa == null)
        throw new DataObjectNotFoundException(accountId);
      warningText = getString(R.string.warning_reset_account_all," ("+aa.currency.getCurrencyCode()+")");
    } else {
      warningText = getString(R.string.warning_reset_account);
    }
    LayoutInflater li = LayoutInflater.from(wrappedCtx);
    View view = li.inflate(R.layout.export_dialog, null);
    dateFormatET = (EditText) view.findViewById(R.id.date_format);
    String dateFormatDefault =
        ((SimpleDateFormat)DateFormat.getDateInstance(DateFormat.SHORT)).toPattern();
    String dateFormat = MyApplication.getInstance().getSettings()
        .getString(PREFKEY_EXPORT_DATE_FORMAT, "");
    if (dateFormat.equals(""))
      dateFormat = dateFormatDefault;
    else {
      try {
        new SimpleDateFormat(dateFormat,Locale.US);
      }  catch (IllegalArgumentException e) {
        dateFormat = dateFormatDefault;
      }
    }
    dateFormatET.setText(dateFormat);
    dateFormatET.addTextChangedListener(new TextWatcher(){
      public void afterTextChanged(Editable s) {
        try {
          new SimpleDateFormat(s.toString(),Locale.US);
          mDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
        } catch (IllegalArgumentException e) {
          dateFormatET.setError(getString(R.string.date_format_illegal));
          mDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
        }
      }
      public void beforeTextChanged(CharSequence s, int start, int count, int after){}
      public void onTextChanged(CharSequence s, int start, int before, int count){}
    });
    notYetExportedCB = (CheckBox) view.findViewById(R.id.export_not_yet_exported);
    deleteCB = (CheckBox) view.findViewById(R.id.export_delete);
    warningTV = (TextView) view.findViewById(R.id.warning_reset);
    formatRB = (RadioButton) view.findViewById(R.id.csv);
    String format = MyApplication.getInstance().getSettings()
        .getString(MyApplication.PREFKEY_EXPORT_FORMAT, "QIF");
    boolean deleteP = true;
    if (format.equals("CSV"))
      (formatRB).setChecked(true);
    deleteCB.setOnClickListener(this);
    if (Account.getHasExported(accountId)) {
      deleteP = false;
      deleteCB.setChecked(false);
      notYetExportedCB.setChecked(true);
      notYetExportedCB.setVisibility(View.VISIBLE);
    }
    warningTV.setText(warningText);
    if (deleteP)
      warningTV.setVisibility(View.VISIBLE);
    else
      warningTV.setVisibility(View.GONE);
    AlertDialog.Builder builder = new AlertDialog.Builder(wrappedCtx)
      .setTitle(allP ? R.string.menu_reset_all : R.string.menu_reset)
      .setView(view)
      .setPositiveButton(android.R.string.ok,this)
      .setNegativeButton(android.R.string.cancel,null);
   if (Build.VERSION.SDK_INT < 11)
     builder.setIcon(android.R.drawable.ic_dialog_alert);
   else
     builder.setIconAttribute(android.R.attr.alertDialogIcon);
   mDialog = builder.create();
   return mDialog;
  }

  @Override
  public void onClick(DialogInterface dialog, int which) {
    Bundle args = getArguments();
    Long accountId = args != null ? args.getLong("accountId") : null;
    Intent i;
    Activity ctx = getActivity();
    AlertDialog dlg = (AlertDialog) dialog;
    String format = ((RadioGroup) dlg.findViewById(R.id.format)).getCheckedRadioButtonId() == R.id.csv ?
        "CSV" : "QIF";
    String dateFormat = ((EditText) dlg.findViewById(R.id.date_format)).getText().toString();
    MyApplication.getInstance().getSettings().edit()
      .putString(MyApplication.PREFKEY_EXPORT_FORMAT, format)
      .putString(PREFKEY_EXPORT_DATE_FORMAT, dateFormat)
      .commit();
    boolean deleteP = ((CheckBox) dlg.findViewById(R.id.export_delete)).isChecked();
    boolean notYetExportedP =  ((CheckBox) dlg.findViewById(R.id.export_not_yet_exported)).isChecked();
    if (Utils.isExternalStorageAvailable()) {
      if (accountId == null)
        Feature.RESET_ALL.recordUsage();
      i = new Intent(ctx, Export.class)
        .putExtra(KEY_ROWID, accountId)
        .putExtra("format", format)
        .putExtra("deleteP", deleteP)
        .putExtra("notYetExportedP",notYetExportedP)
        .putExtra("dateFormat",dateFormat);
      ctx.startActivityForResult(i,0);
    } else {
      Toast.makeText(ctx,
          ctx.getString(R.string.external_storage_unavailable),
          Toast.LENGTH_LONG)
          .show();
    }
  }

  /* 
   * if we are in the situation, where there are already exported transactions
   * we suggest to the user the default of again exporting without deleting
   * but if the user now changes to deleting, we enforce a complete export/reset
   * since a partial deletion of only transactions not yet exported would
   * lead to an inconsistent state
   */
  @Override
  public void onClick(View view) {
   if (((CheckBox) view).isChecked()) {
     notYetExportedCB.setEnabled(false);
     notYetExportedCB.setChecked(false);
     warningTV.setVisibility(View.VISIBLE);
   } else {
     notYetExportedCB.setEnabled(true);
     notYetExportedCB.setChecked(true);
     warningTV.setVisibility(View.GONE);
   }
  }
}
