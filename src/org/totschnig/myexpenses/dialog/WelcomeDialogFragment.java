package org.totschnig.myexpenses.dialog;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.dialog.MessageDialogFragment.MessageDialogListener;
import org.totschnig.myexpenses.util.Utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

public class WelcomeDialogFragment extends DialogFragment {
  
  public static final WelcomeDialogFragment newInstance(String versionInfo) {
    WelcomeDialogFragment dialogFragment = new WelcomeDialogFragment();
    Bundle bundle = new Bundle();
    bundle.putString("versionInfo", versionInfo);
    dialogFragment.setArguments(bundle);
    return dialogFragment;
  }
  
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    Activity ctx  = (Activity) getActivity();
    LayoutInflater li = LayoutInflater.from(ctx);
    View view = li.inflate(R.layout.welcome, null);
    return new AlertDialog.Builder(ctx)
      .setTitle(getResources().getString(R.string.app_name) + " " + getResources().getString(R.string.menu_help))
      .setIcon(R.drawable.icon)
      .setView(view)
      .setPositiveButton(android.R.string.ok,null)
      .create();
    }
}
