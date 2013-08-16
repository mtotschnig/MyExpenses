package org.totschnig.myexpenses.dialog;

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.Export;
import org.totschnig.myexpenses.util.Utils;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.ContribFeature.Feature;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

public class ExportDialogFragment extends DialogFragment implements android.content.DialogInterface.OnClickListener, android.view.View.OnClickListener {
  CheckBox notYetExportedCB,deleteCB;
  RadioButton formatRB;
  TextView warningTV;
  
  public static final ExportDialogFragment newInstance(Long accountId) {
    ExportDialogFragment dialogFragment = new ExportDialogFragment();
    if (accountId != null) {
      Bundle bundle = new Bundle();
      bundle.putLong("accountId", accountId);
      dialogFragment.setArguments(bundle);
    }
    return dialogFragment;
  }
  
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    Activity ctx  = (Activity) getActivity();
    Context wrappedCtx = DialogUtils.wrapContext1(ctx);
    Bundle args = getArguments();
    Long accountId = args != null ? args.getLong("accountId") : null;
    boolean allP = accountId == null;
    LayoutInflater li = LayoutInflater.from(wrappedCtx);
    View view = li.inflate(R.layout.export_dialog, null);
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
    warningTV.setText(getString(
        allP ? R.string.warning_reset_account_all : R.string.warning_reset_account));
    if (deleteP)
      warningTV.setVisibility(View.VISIBLE);
    else
      warningTV.setVisibility(View.GONE);
    return new AlertDialog.Builder(wrappedCtx)
      .setTitle(allP ? R.string.menu_reset_all : R.string.menu_reset)
      .setView(view)
      .setPositiveButton(android.R.string.ok,this)
      .setNegativeButton(android.R.string.cancel,null)
      .setIcon(android.R.drawable.ic_dialog_alert)
      .create();
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
    MyApplication.getInstance().getSettings().edit()
      .putString(MyApplication.PREFKEY_EXPORT_FORMAT, format).commit();
    boolean deleteP = ((CheckBox) dlg.findViewById(R.id.export_delete)).isChecked();
    boolean notYetExportedP =  ((CheckBox) dlg.findViewById(R.id.export_not_yet_exported)).isChecked();
    if (Utils.isExternalStorageAvailable()) {
      if (accountId == null)
        Feature.RESET_ALL.recordUsage();
      i = new Intent(ctx, Export.class)
        .putExtra(KEY_ROWID, accountId)
        .putExtra("format", format)
        .putExtra("deleteP", deleteP)
        .putExtra("notYetExportedP",notYetExportedP);
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
