package org.totschnig.myexpenses.dialog;

import java.util.ArrayList;
import java.util.Iterator;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.CommonCommands;
import org.totschnig.myexpenses.dialog.MessageDialogFragment.MessageDialogListener;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.TextView;

public class VersionDialogFragment extends DialogFragment implements OnClickListener {
  public static final VersionDialogFragment newInstance(ArrayList<CharSequence> versionInfo) {
    VersionDialogFragment dialogFragment = new VersionDialogFragment();
    Bundle bundle = new Bundle();
    bundle.putSerializable("versionInfo", versionInfo);
    dialogFragment.setArguments(bundle);
    return dialogFragment;
  }
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    Bundle bundle = getArguments();
    Activity ctx  = (Activity) getActivity();
    LayoutInflater li = LayoutInflater.from(ctx);
    ArrayList<CharSequence> versionInfo = (ArrayList<CharSequence>) bundle.getSerializable("versionInfo");
    View view = li.inflate(R.layout.versiondialog, null);
    ((TextView) view.findViewById(R.id.versionInfoChanges))
      .setText(R.string.help_whats_new);
    if (versionInfo.size() > 0) {
      View divider;
      LinearLayout main = (LinearLayout) view.findViewById(R.id.layoutMain);
      ((TextView) view.findViewById(R.id.versionInfoImportantHeading)).setVisibility(View.VISIBLE);
      TextView tv;
      for(Iterator<CharSequence> i = versionInfo.iterator();i.hasNext();) {
        tv = new TextView(ctx);
        tv.setText(i.next());
        tv.setTextAppearance(ctx, R.style.form_label);
        tv.setPadding(15, 0, 0, 0);
        main.addView(tv);
        if (i.hasNext()) {
          divider = new View(ctx);
          divider.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,1));
          divider.setBackgroundColor(getResources().getColor(R.color.appDefault));
          main.addView(divider);
        }
      }
    }
    return new AlertDialog.Builder(ctx)
      .setTitle(getString(R.string.new_version) + " : "
          + CommonCommands.getVersionName(getActivity()))
      .setIcon(R.drawable.icon)
      .setView(view)
      .setPositiveButton(android.R.string.ok, this)
      .setNeutralButton(R.string.menu_help,this)
      .setNegativeButton( R.string.menu_contrib, this)
      .create();
  }
  @Override
  public void onClick(DialogInterface dialog, int which) {
    if (which != AlertDialog.BUTTON_POSITIVE)
      ((MessageDialogListener) getActivity()).dispatchCommand(
          which == AlertDialog.BUTTON_NEUTRAL ? R.id.HELP_COMMAND : R.id.CONTRIB_PLAY_COMMAND,
          null);
  }
}
