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
import android.widget.Toast;

public class ExportDialogFragment extends DialogFragment implements android.content.DialogInterface.OnClickListener, android.view.View.OnClickListener {
  CheckBox notYetExportedCB;
  
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
    Context wrappedCtx = Build.VERSION.SDK_INT < 11 ? 
        new ContextThemeWrapper(ctx, R.style.AboutDialog) : ctx;
    Bundle args = getArguments();
    Long accountId = args != null ? args.getLong("accountId") : null;
    boolean allP = accountId == null;
    LayoutInflater li = LayoutInflater.from(wrappedCtx);
    View view = li.inflate(R.layout.export_dialog, null);
    notYetExportedCB = (CheckBox) view.findViewById(R.id.export_not_yet_exported);
    if (MyApplication.getInstance().getSettings().
          getString(MyApplication.PREFKEY_EXPORT_FORMAT, "QIF").equals("CSV"))
      ((RadioButton) view.findViewById(R.id.csv)).setChecked(true);
    if (Account.getHasExported(accountId)) {
      CheckBox deleteCB = (CheckBox) view.findViewById(R.id.export_delete);
      deleteCB.setChecked(false);
      deleteCB.setOnClickListener(this);
      notYetExportedCB.setChecked(true);
      notYetExportedCB.setVisibility(View.VISIBLE);
    }

    return new AlertDialog.Builder(wrappedCtx)
      .setTitle(allP ? R.string.menu_reset : R.string.menu_reset_all)
      .setView(view)
      .setPositiveButton(android.R.string.ok,this)
      .setNegativeButton(android.R.string.cancel,null)
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
   } else {
     notYetExportedCB.setEnabled(true);
     notYetExportedCB.setChecked(true);
   }
  }
}
