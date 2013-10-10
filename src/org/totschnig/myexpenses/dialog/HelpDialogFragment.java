/*   This file is part of My Expenses.
 *   My Expenses is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   My Expenses is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with My Expenses.  If not, see <http://www.gnu.org/licenses/>.
*/

package org.totschnig.myexpenses.dialog;

import java.util.ArrayList;
import java.util.Arrays;

import org.totschnig.myexpenses.R;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ImageView;

public class HelpDialogFragment extends DialogFragment {
  
  public static final HelpDialogFragment newInstance(String activityName, Enum<?> variant) {
    HelpDialogFragment dialogFragment = new HelpDialogFragment();
    Bundle args = new Bundle();
    args.putString("activityName", activityName);
    if (variant != null)
      args.putString("variant", variant.name());
    dialogFragment.setArguments(args);
    return dialogFragment;
  }
  
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    FragmentActivity ctx  = getActivity();
    Context wrappedCtx = DialogUtils.wrapContext2(ctx);
    final Resources res = getResources();
    final String pack = ctx.getPackageName();
    String title,screenInfo="";
    Bundle args = getArguments();
    String activityName = args.getString("activityName");
    String variant = args.getString("variant");
    final LayoutInflater li = LayoutInflater.from(wrappedCtx);
    View view = li.inflate(R.layout.help_dialog, null);
    LinearLayout ll = (LinearLayout) view.findViewById(R.id.help);
    try {
      int resId = res.getIdentifier("help_" +activityName + "_info", "string", pack);
      if (resId != 0)
        screenInfo = getString(res.getIdentifier("help_" +activityName + "_info", "string", pack));
      else if (variant == null)
        throw new NotFoundException();
      if (variant != null)
        screenInfo += "\n" + getString(res.getIdentifier("help_" +activityName + "_" + variant + "_info", "string", pack));
      ((TextView) view.findViewById(R.id.screen_info)).setText(screenInfo);
      resId = res.getIdentifier(activityName+"_menuitems", "array", pack);
      ArrayList<String> menuItems= new ArrayList<String>();
      if (resId != 0)
        menuItems.addAll(Arrays.asList(res.getStringArray(resId)));
      if (variant != null &&
            (resId = res.getIdentifier(activityName + "_" + variant +"_menuitems", "array", pack)) != 0)
          menuItems.addAll(Arrays.asList(res.getStringArray(resId)));
      if (menuItems.size() == 0)
        view.findViewById(R.id.menu_commands_heading).setVisibility(View.GONE);
      else
        for (String item: menuItems) {
          View row = li.inflate(R.layout.help_dialog_action_row, null);
          ((ImageView) row.findViewById(R.id.list_image)).setImageDrawable(
              res.getDrawable(res.getIdentifier(item+"_icon", "drawable", pack)));
          ((TextView) row.findViewById(R.id.title)).setText(
              res.getString(res.getIdentifier("menu_"+item,"string",pack)));
          //we look for a help text specific to the variant first, thank to the activity
          //and last a generic one
          resId = res.getIdentifier("menu_" +activityName + "_" + variant + "_" + item + "_help_text","string",pack);
          if (resId == 0)
            resId = res.getIdentifier("menu_" +activityName + "_" + item + "_help_text","string",pack);
          if (resId == 0)
            resId = res.getIdentifier("menu_" + item + "_help_text","string",pack);
          if (resId == 0)
            throw new NotFoundException();
          ((TextView) row.findViewById(R.id.help_text)).setText(
              res.getString(resId));
          ll.addView(row);
        }
      resId = variant != null ? res.getIdentifier("help_" +activityName + "_" + variant + "_title", "string", pack) : 0;
      if (resId == 0)
        resId = res.getIdentifier("help_" +activityName + "_title", "string", pack);
      title = getString(resId);
    } catch (NotFoundException e) {
      return new AlertDialog.Builder(wrappedCtx)
          .setMessage("Error generating Help dialog")
          .setIcon(android.R.drawable.ic_dialog_alert)
          .create();
    }
    return new AlertDialog.Builder(wrappedCtx)
      .setTitle(title)
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
