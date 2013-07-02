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
    ((TextView) view.findViewById(R.id.screen_info)).setText(
        getString(res.getIdentifier("help_" +activityName + "_info", "string", pack)));
    final String[] items = res.getStringArray(res.getIdentifier(activityName+"_menuitems", "array", pack));
    ListView list=(ListView) view.findViewById(R.id.actionlist);
    list.setAdapter(new BaseAdapter() {
      @Override
      public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView == null ?
          li.inflate(R.layout.help_dialog_action_row, null) :
          convertView;
        ((ImageView) row.findViewById(R.id.list_image)).setImageDrawable(
            res.getDrawable(res.getIdentifier(items[position]+"_icon", "drawable", pack)));
        ((TextView) row.findViewById(R.id.title)).setText(
            res.getString(res.getIdentifier("menu_"+items[position],"string",pack)));
        ((TextView) row.findViewById(R.id.help_text)).setText(
            res.getString(res.getIdentifier("menu_"+items[position]+"_help_text","string",pack)));
        return row;
      }

      @Override
      public int getCount() {
        // TODO Auto-generated method stub
        return items.length;
      }

      @Override
      public Object getItem(int position) {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public long getItemId(int position) {
        // TODO Auto-generated method stub
        return 0;
      }
    });
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
