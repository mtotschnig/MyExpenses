package org.totschnig.myexpenses.dialog;

import java.util.Locale;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.GrisbiImport;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.DialogFragment;

public class GrisbiSourcesDialogFragment extends DialogFragment implements
DialogInterface.OnClickListener {
  public int sourceIndex = -1;
  AlertDialog dialog;
  public final static String[] IMPORT_SOURCES = new String[] {
    MyApplication.getInstance().getString(R.string.grisbi_import_default_source),
    Environment.getExternalStorageDirectory().getPath() + "/myexpenses/grisbi.xml"
  };
  public final static int defaultSourceResId = 
      MyApplication.getInstance().getResources().getIdentifier(
          "cat_"+ Locale.getDefault().getLanguage(),
          "raw",
          MyApplication.getInstance().getPackageName());
  
  public static final GrisbiSourcesDialogFragment newInstance() {
    return new GrisbiSourcesDialogFragment();
  }
  @Override
  public void onStart(){
    super.onStart();
    sourceIndex = dialog.getListView().getCheckedItemPosition();
    dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setEnabled(sourceIndex!=-1);
    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(sourceIndex == 1);
}
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    Context wrappedCtx = DialogUtils.wrapContext2(getActivity());
    dialog = new AlertDialog.Builder(wrappedCtx)
      .setTitle(R.string.dialog_title_select_import_source)
      .setSingleChoiceItems(IMPORT_SOURCES, -1, this)
      .setNegativeButton(android.R.string.no, this)
      .setNeutralButton(R.string.grisbi_import_button_categories_only,this)
      .setPositiveButton(R.string.grisbi_import_button_categories_and_parties, this)
      .create();
    return dialog;
  }
  @Override
  public void onCancel (DialogInterface dialog) {
    ((GrisbiImport) getActivity()).cancelDialog();
  }
  @Override
  public void onClick(DialogInterface dialog, int id) {
    switch (id) {
    case AlertDialog.BUTTON_POSITIVE:
      ((GrisbiImport) getActivity()).onSourceSelected(sourceIndex,true);
      break;
    case AlertDialog.BUTTON_NEUTRAL:
      ((GrisbiImport) getActivity()).onSourceSelected(sourceIndex,false);
      break;
    case AlertDialog.BUTTON_NEGATIVE:
      onCancel(dialog);
      break;
    default:
      sourceIndex = id;
      //we enable import of categories only for any source
      //categories and parties only for the last custom file
      ((AlertDialog)dialog).getButton(AlertDialog.BUTTON_NEUTRAL).setEnabled(true);
      ((AlertDialog)dialog).getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(
          id == 1);  
    }
  }
}
