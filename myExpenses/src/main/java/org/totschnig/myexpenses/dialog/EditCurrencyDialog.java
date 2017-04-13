package org.totschnig.myexpenses.dialog;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.model.CurrencyEnum;

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY;

public class EditCurrencyDialog extends CommitSafeDialogFragment {

  public static EditCurrencyDialog newInstance(String currency) {
    Bundle arguments = new Bundle(1);
    arguments.putString(KEY_CURRENCY, currency);
    EditCurrencyDialog editCurrencyDialog = new EditCurrencyDialog();
    editCurrencyDialog.setArguments(arguments);
    return editCurrencyDialog;
  }

  @NonNull
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    Activity ctx  = getActivity();
    LayoutInflater li = LayoutInflater.from(ctx);
    //noinspection InflateParams
    View view = li.inflate(R.layout.edit_currency, null);
    return new AlertDialog.Builder(ctx)
        .setTitle(CurrencyEnum.valueOf(getArguments().getString(KEY_CURRENCY)).toString())
        .setView(view)
        .setNegativeButton(android.R.string.cancel, null)
        .setPositiveButton(android.R.string.ok, null)
        .create();
  }
}
