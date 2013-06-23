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

public class HelpDialogFragment extends DialogFragment implements OnClickListener {
  
  public static final HelpDialogFragment newInstance(String versionInfo) {
    HelpDialogFragment dialogFragment = new HelpDialogFragment();
    Bundle bundle = new Bundle();
    bundle.putString("versionInfo", versionInfo);
    dialogFragment.setArguments(bundle);
    return dialogFragment;
  }
  
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    Activity ctx  = (Activity) getActivity();
    DisplayMetrics displayMetrics = new DisplayMetrics();
    ctx.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
    int minWidth = (int) (displayMetrics.widthPixels*0.9f);
    if (minWidth / displayMetrics.density > 650)
      minWidth = (int) (650 * displayMetrics.density);
  
    LayoutInflater li = LayoutInflater.from(ctx);
    View view = li.inflate(R.layout.aboutview, null);
    view.setMinimumWidth(minWidth);
    ((TextView)view.findViewById(R.id.aboutVersionCode)).setText(getArguments().getString("versionInfo"));
    ((TextView)view.findViewById(R.id.help_contrib)).setText(
        Html.fromHtml(getString(R.string.dialog_contrib_text,Utils.getContribFeatureLabelsAsFormattedList(ctx))));
    ((TextView)view.findViewById(R.id.help_quick_guide)).setMovementMethod(LinkMovementMethod.getInstance());
    return new AlertDialog.Builder(ctx)
      .setTitle(getResources().getString(R.string.app_name) + " " + getResources().getString(R.string.menu_help))
      .setIcon(R.drawable.icon)
      .setView(view)
      .setPositiveButton(android.R.string.ok,this)
      .setNegativeButton(R.string.menu_contrib, this)
      .create();
    }

  @Override
  public void onClick(DialogInterface dialog, int which) {
    if (which == AlertDialog.BUTTON_NEGATIVE)
      ((MessageDialogListener) getActivity())
      .dispatchCommand(R.id.CONTRIB_PLAY_COMMAND,null);
  }
}
