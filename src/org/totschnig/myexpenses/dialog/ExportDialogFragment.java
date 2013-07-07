package org.totschnig.myexpenses.dialog;

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.Export;
import org.totschnig.myexpenses.util.Utils;
import org.totschnig.myexpenses.model.Account;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

public class ExportDialogFragment extends DialogFragment implements OnClickListener {
  
  public static final ExportDialogFragment newInstance(Long accountId) {
    ExportDialogFragment dialogFragment = new ExportDialogFragment();
    Bundle bundle = new Bundle();
    bundle.putLong("accountId", accountId);
    dialogFragment.setArguments(bundle);
    return dialogFragment;
  }
  
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    Long accountId = getArguments().getLong("accountId");
    boolean allP = accountId == null;
    Activity ctx  = (Activity) getActivity();
    LayoutInflater li = LayoutInflater.from(ctx);
    View view = li.inflate(R.layout.export_dialog, null);
    if (MyApplication.getInstance().getSettings().
          getString(MyApplication.PREFKEY_EXPORT_FORMAT, "QIF").equals("CSV"))
      ((RadioButton) view.findViewById(R.id.csv)).setChecked(true);
    if (!Account.getHasExported(accountId))
      view.findViewById(R.id.export_not_yet_exported).setVisibility(View.GONE);
    
    return new AlertDialog.Builder(ctx)
      .setTitle(allP ? R.string.dialog_title_warning_reset_all : R.string.dialog_title_warning_reset_one)
      .setView(view)
      .setPositiveButton(android.R.string.ok,this)
      .setNegativeButton(android.R.string.cancel,null)
      .create();
  }

  @Override
  public void onClick(DialogInterface dialog, int which) {
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
      Toast.makeText(ctx,
          "deleteP: " + deleteP + "\nonlyExported: " + notYetExportedP,
          Toast.LENGTH_LONG)
          .show();
      i = new Intent(ctx, Export.class);
      i.putExtra(KEY_ROWID, getArguments().getLong("accountId"));
      i.putExtra("format", format);
      i.putExtra("deleteP", deleteP);
      i.putExtra("notYetExportedP",notYetExportedP);
      ctx.startActivityForResult(i,0);
    } else {
      Toast.makeText(ctx,
          ctx.getString(R.string.external_storage_unavailable),
          Toast.LENGTH_LONG)
          .show();
    }
  }
}
