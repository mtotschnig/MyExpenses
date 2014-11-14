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
import java.util.HashMap;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.util.Utils;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.text.Html;
import android.text.Html.ImageGetter;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ImageView;

/**A Dialog Fragment that displays help information. The content is constructed from resources
 * based on the activity and an optional variant passed in.
 * @author Michael Totschnig
 */
public class HelpDialogFragment extends CommitSafeDialogFragment implements ImageGetter {
  
  public static final String KEY_VARIANT = "variant";
  public static final String KEY_ACTIVITY_NAME = "activityName";
  public static final HashMap<String,Integer> iconMap = new HashMap<String,Integer>();
  static {
    iconMap.put("create_transaction", android.R.drawable.ic_menu_add);
    iconMap.put("edit", android.R.drawable.ic_menu_edit);
    iconMap.put("back", R.drawable.ic_menu_back);
    iconMap.put("balance", R.drawable.ic_action_balance);
    iconMap.put("cancel_plan_instance", android.R.drawable.ic_menu_close_clear_cancel);
    iconMap.put("categories_setup_default",android.R.drawable.ic_menu_upload);
    iconMap.put("clone_transaction",R.drawable.ic_menu_copy);
    iconMap.put("create_instance_edit",R.drawable.create_instance_edit_icon);
    iconMap.put("create_instance_save",R.drawable.create_instance_save_icon);
    iconMap.put("create_main_cat",android.R.drawable.ic_menu_add);
    iconMap.put("create_method",android.R.drawable.ic_menu_add);
    iconMap.put("create_party",android.R.drawable.ic_menu_add);
    iconMap.put("create_split",R.drawable.ic_menu_split);
    iconMap.put("create_split_part_category",android.R.drawable.ic_menu_add);
    iconMap.put("create_split_part_transfer",R.drawable.ic_menu_forward);
    iconMap.put("create_sub_cat",android.R.drawable.ic_menu_add);
    iconMap.put("create_template_for_transfer",R.drawable.ic_menu_forward);
    iconMap.put("create_template_for_transaction",android.R.drawable.ic_menu_add);
    iconMap.put("create_transaction",android.R.drawable.ic_menu_add);
    iconMap.put("create_transfer",R.drawable.ic_menu_forward);
    iconMap.put("delete",android.R.drawable.ic_menu_delete);
    iconMap.put("edit", android.R.drawable.ic_menu_edit);
    iconMap.put("distribution", android.R.drawable.ic_menu_today);
    iconMap.put("edit_plan_instance", android.R.drawable.ic_menu_edit);
    iconMap.put("exclude_from_totals",android.R.drawable.ic_menu_close_clear_cancel);
    iconMap.put("forward", R.drawable.ic_menu_forward);
    iconMap.put("grouping", android.R.drawable.ic_menu_sort_by_size);
    iconMap.put("invert_transfer",R.drawable.ic_menu_refresh);
    iconMap.put("manage_plans",android.R.drawable.ic_menu_set_as);
    iconMap.put("reset", android.R.drawable.ic_menu_revert);
    iconMap.put("reset_plan_instance", android.R.drawable.ic_menu_revert);
    iconMap.put("save_and_new", Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1 ?
        android.R.drawable.ic_menu_save : R.drawable.save_and_new_icon);
    iconMap.put("save",android.R.drawable.ic_menu_save);
    iconMap.put("search", android.R.drawable.ic_menu_search);
    iconMap.put("select_category", R.drawable.ic_menu_goto);
    iconMap.put("set_sort_key", android.R.drawable.ic_menu_sort_by_size);
    iconMap.put("print", R.drawable.print_icon);
    iconMap.put("create_template_from_transaction", R.drawable.create_template_from_transaction_icon);
  }
  public static final HelpDialogFragment newInstance(String activityName, Enum<?> variant) {
    HelpDialogFragment dialogFragment = new HelpDialogFragment();
    Bundle args = new Bundle();
    args.putString(KEY_ACTIVITY_NAME, activityName);
    if (variant != null)
      args.putString(KEY_VARIANT, variant.name());
    dialogFragment.setArguments(args);
    return dialogFragment;
  }
  
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    FragmentActivity ctx  = getActivity();
    Context wrappedCtx = DialogUtils.wrapContext2(ctx);
    final Resources res = getResources();
    final String pack = ctx.getPackageName();
    String title;
    String screenInfo="";
    Bundle args = getArguments();
    String activityName = args.getString(KEY_ACTIVITY_NAME);
    String variant = args.getString(KEY_VARIANT);
    final LayoutInflater li = LayoutInflater.from(wrappedCtx);
    View view = li.inflate(R.layout.help_dialog, null);
    LinearLayout ll = (LinearLayout) view.findViewById(R.id.help);

    try {
      String resIdString = "help_" +activityName + "_info";
      int resId = res.getIdentifier(resIdString, "string", pack);
      if (resId != 0) {
        screenInfo = getString(resId);
      }
      else if (variant == null) {
        throw new NotFoundException(resIdString);
      }
      if (variant != null) {
        resIdString = "help_" +activityName + "_" + variant + "_info";
        resId = res.getIdentifier(resIdString, "string", pack);
        if (resId == 0) {
          throw new NotFoundException(resIdString);
        }
        screenInfo += "<br>";
        screenInfo +=  getString(resId);
      }
      ((TextView) view.findViewById(R.id.screen_info)).setText(Html.fromHtml(screenInfo, this, null));
      resId = res.getIdentifier(activityName+"_menuitems", "array", pack);
      ArrayList<String> menuItems= new ArrayList<String>();
      if (resId != 0)
        menuItems.addAll(Arrays.asList(res.getStringArray(resId)));
      if (variant != null &&  
            (resId = res.getIdentifier(activityName + "_" + variant +"_menuitems", "array", pack)) != 0)
          menuItems.addAll(Arrays.asList(res.getStringArray(resId)));
      if (menuItems.size() == 0)
        view.findViewById(R.id.menu_commands_heading).setVisibility(View.GONE);
      else {
        handleMenuItems(activityName, variant, li, ll, menuItems,"menu",1);
      }
      resId = res.getIdentifier(activityName+"_cabitems", "array", pack);
      menuItems.clear();
      if (resId != 0)
        menuItems.addAll(Arrays.asList(res.getStringArray(resId)));
      if (variant != null &&
            (resId = res.getIdentifier(activityName + "_" + variant +"_cabitems", "array", pack)) != 0)
          menuItems.addAll(Arrays.asList(res.getStringArray(resId)));
      if (menuItems.size() == 0)
        view.findViewById(R.id.cab_commands_heading).setVisibility(View.GONE);
      else {
        handleMenuItems(activityName, variant, li, ll, menuItems,"cab",0);
      }
      resId = variant != null ? res.getIdentifier("help_" +activityName + "_" + variant + "_title", "string", pack) : 0;
      if (resId == 0) {
        resIdString = "help_" +activityName + "_title";
        resId = res.getIdentifier(resIdString, "string", pack);
        if (resId == 0) {
          throw new NotFoundException(resIdString);
        }
      }
      title = getString(resId);
    } catch (NotFoundException e) {
      Utils.reportToAcra(e);
      return new AlertDialog.Builder(wrappedCtx)
          .setMessage("Error generating Help dialog")
          .create();
    }
    return new AlertDialog.Builder(wrappedCtx)
      .setTitle(title)
      .setIcon(android.R.drawable.ic_menu_help)
      .setView(view)
      .setPositiveButton(android.R.string.ok,new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
          if (getActivity()==null) {
            return;
          }
          getActivity().finish();
        }
      })
      .create();
  }

  /**
   * @param res
   * @param pack
   * @param activityName
   * @param variant
   * @param li
   * @param ll
   * @param menuItems
   * @throws NotFoundException
   */
  protected void handleMenuItems(String activityName, String variant, final LayoutInflater li,
      LinearLayout ll, ArrayList<String> menuItems,String prefix,int offset) throws NotFoundException {
    final Resources res = getResources();
    final String pack = getActivity().getPackageName();
    String resIdString;
    int resId;
    for (String item: menuItems) {
      View row = li.inflate(R.layout.help_dialog_action_row, null);
      if (iconMap.containsKey(item)) {
        resId = iconMap.get(item);
        ((ImageView) row.findViewById(R.id.list_image)).setImageDrawable(
            res.getDrawable(resId));
      } else {
        throw new NotFoundException(item + " icon");
      }
      resIdString = "menu_"+item;
      resId = res.getIdentifier(resIdString,"string",pack);
      if (resId == 0) {
        throw new NotFoundException(resIdString);
      }
      ((TextView) row.findViewById(R.id.title)).setText(
          res.getString(resId));
      //we look for a help text specific to the variant first, then to the activity
      //and last a generic one
      resId = res.getIdentifier(prefix + "_" +activityName + "_" + variant + "_" + item + "_help_text","string",pack);
      if (resId == 0) {
        resId = res.getIdentifier(prefix + "_" +activityName + "_" + item + "_help_text","string",pack);
        if (resId == 0) {
          resIdString = prefix + "_"  + item + "_help_text";
          resId = res.getIdentifier(resIdString,"string",pack);
          if (resId == 0) {
            throw new NotFoundException(resIdString);
          }
        }
      }
      ((TextView) row.findViewById(R.id.help_text)).setText(
          res.getString(resId));
      ll.addView(row,ll.getChildCount()-offset);
    }
  }
  public void onCancel (DialogInterface dialog) {
    getActivity().finish();
  }
  @Override
  public Drawable getDrawable(String name) {
      Drawable d = getResources().getDrawable(
          getResources().getIdentifier(
              name,
              "drawable",
              getActivity().getPackageName()));
      d.setBounds(0, 0, d.getIntrinsicWidth(),
          d.getIntrinsicHeight());
      return d;
  }
}
