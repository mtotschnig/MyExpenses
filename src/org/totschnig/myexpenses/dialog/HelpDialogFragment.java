package org.totschnig.myexpenses.dialog;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity;

import com.actionbarsherlock.view.MenuItem;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ImageView;

public class HelpDialogFragment extends DialogFragment {
  
  public static final HelpDialogFragment newInstance() {
    HelpDialogFragment dialogFragment = new HelpDialogFragment();
    return dialogFragment;
  }
  
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    ProtectedFragmentActivity ctx  = (ProtectedFragmentActivity) getActivity();
    Resources res = getResources();
    String pack = ctx.getPackageName();
    String activityName = ctx.getComponentName().getShortClassName();
    //trim leading .
    activityName = activityName.substring(activityName.lastIndexOf(".")+1);
    LayoutInflater li = LayoutInflater.from(ctx);
    View view = li.inflate(R.layout.help_dialog, null);
    Log.d("DEBUG","help_" +activityName + "_info");
    ((TextView) view.findViewById(R.id.screen_info)).setText(
        getString(res.getIdentifier("help_" +activityName + "_info", "string", pack)));
    ListView list=(ListView) view.findViewById(R.id.actionlist);
    final MenuItem[] items =  ctx.getMenuItems();
    list.setAdapter(new ArrayAdapter<MenuItem>(ctx, R.layout.help_dialog_action_row,R.id.help_text,items) {
      public View getView(int position, View convertView, ViewGroup parent) {
        View row=super.getView(position, convertView, parent);
        ((ImageView) row.findViewById(R.id.list_image)).setImageDrawable(items[position].getIcon());
        ((TextView) row.findViewById(R.id.title)).setText(items[position].getTitle());
        ((TextView) row.findViewById(R.id.help_text)).setText("Action description here");
        return row;
      }
    });
    return new AlertDialog.Builder(ctx)
      .setTitle(getString(res.getIdentifier("help_" +activityName + "_title", "string", pack)))
      .setIcon(android.R.drawable.ic_menu_help)
      .setView(view)
      .setPositiveButton(android.R.string.ok,null)
      .create();
    }
}
