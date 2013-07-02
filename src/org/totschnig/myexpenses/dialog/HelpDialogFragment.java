package org.totschnig.myexpenses.dialog;

import org.totschnig.myexpenses.R;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ImageView;

public class HelpDialogFragment extends DialogFragment {
  
  public static final HelpDialogFragment newInstance(String activityName) {
    HelpDialogFragment dialogFragment = new HelpDialogFragment();
    Bundle args = new Bundle();
    args.putString("activityName", activityName);
    dialogFragment.setArguments(args);
    return dialogFragment;
  }
  
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    FragmentActivity ctx  = getActivity();
    final Resources res = getResources();
    final String pack = ctx.getPackageName();
    String activityName = getArguments().getString("activityName");
    final LayoutInflater li = LayoutInflater.from(ctx);
    View view = li.inflate(R.layout.help_dialog, null);
    LinearLayout ll = (LinearLayout) view.findViewById(R.id.help);
    ((TextView) view.findViewById(R.id.screen_info)).setText(
        getString(res.getIdentifier("help_" +activityName + "_info", "string", pack)));
    final String[] items = res.getStringArray(res.getIdentifier(activityName+"_menuitems", "array", pack));
    for (String item: items) {
      View row = li.inflate(R.layout.help_dialog_action_row, null);
      ((ImageView) row.findViewById(R.id.list_image)).setImageDrawable(
          res.getDrawable(res.getIdentifier(item+"_icon", "drawable", pack)));
      ((TextView) row.findViewById(R.id.title)).setText(
          res.getString(res.getIdentifier("menu_"+item,"string",pack)));
      ((TextView) row.findViewById(R.id.help_text)).setText(
          res.getString(res.getIdentifier("menu_"+item+"_help_text","string",pack)));
      ll.addView(row);
    }
    return new AlertDialog.Builder(ctx)
      .setTitle(getString(res.getIdentifier("help_" +activityName + "_title", "string", pack)))
      .setIcon(android.R.drawable.ic_menu_help)
      .setView(view)
      .setPositiveButton(android.R.string.ok,new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
          getActivity().finish();
        }
      })
      .create();
  }
  public void onCancel (DialogInterface dialog) {
    getActivity().finish();
  }
}
