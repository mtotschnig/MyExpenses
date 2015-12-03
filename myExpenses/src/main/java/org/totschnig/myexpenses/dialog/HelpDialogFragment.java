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

import android.support.v7.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.text.Html;
import android.text.Html.ImageGetter;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ImageView;

/**
 * A Dialog Fragment that displays help information. The content is constructed from resources
 * based on the activity and an optional variant passed in.
 *
 * @author Michael Totschnig
 */
public class HelpDialogFragment extends CommitSafeDialogFragment implements ImageGetter {

  public static final String KEY_VARIANT = "variant";
  public static final String KEY_ACTIVITY_NAME = "activityName";
  public static final HashMap<String, Integer> iconMap = new HashMap<String, Integer>();

  static {
    iconMap.put("create_transaction", android.R.drawable.ic_menu_add);
    iconMap.put("edit", android.R.drawable.ic_menu_edit);
    iconMap.put("back", R.drawable.ic_menu_back);
    iconMap.put("balance", R.drawable.ic_action_balance);
    iconMap.put("cancel_plan_instance", android.R.drawable.ic_menu_close_clear_cancel);
    iconMap.put("categories_setup_default", android.R.drawable.ic_menu_upload);
    iconMap.put("clone_transaction", R.drawable.ic_menu_copy);
    iconMap.put("create_instance_edit", R.drawable.create_instance_edit_icon);
    iconMap.put("create_instance_save", R.drawable.create_instance_save_icon);
    iconMap.put("create_main_cat", android.R.drawable.ic_menu_add);
    iconMap.put("create_method", android.R.drawable.ic_menu_add);
    iconMap.put("create_party", android.R.drawable.ic_menu_add);
    iconMap.put("create_split", R.drawable.ic_menu_split);
    iconMap.put("create_split_part_category", android.R.drawable.ic_menu_add);
    iconMap.put("create_split_part_transfer", R.drawable.ic_menu_forward);
    iconMap.put("create_sub_cat", android.R.drawable.ic_menu_add);
    iconMap.put("create_template_for_transfer", R.drawable.ic_menu_forward);
    iconMap.put("create_template_for_transaction", android.R.drawable.ic_menu_add);
    iconMap.put("create_transaction", android.R.drawable.ic_menu_add);
    iconMap.put("create_transfer", R.drawable.ic_menu_forward);
    iconMap.put("delete", android.R.drawable.ic_menu_delete);
    iconMap.put("edit", android.R.drawable.ic_menu_edit);
    iconMap.put("distribution", android.R.drawable.ic_menu_today);
    iconMap.put("edit_plan_instance", android.R.drawable.ic_menu_edit);
    iconMap.put("forward", R.drawable.ic_menu_forward);
    iconMap.put("grouping", android.R.drawable.ic_menu_sort_by_size);
    iconMap.put("invert_transfer", R.drawable.ic_menu_refresh);
    iconMap.put("manage_plans", android.R.drawable.ic_menu_set_as);
    iconMap.put("reset", android.R.drawable.ic_menu_revert);
    iconMap.put("reset_plan_instance", android.R.drawable.ic_menu_revert);
    iconMap.put("save_and_new", Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1 ?
        android.R.drawable.ic_menu_save : R.drawable.save_and_new_icon);
    iconMap.put("save", android.R.drawable.ic_menu_save);
    iconMap.put("search", android.R.drawable.ic_menu_search);
    iconMap.put("select_category", R.drawable.ic_menu_goto);
    iconMap.put("set_sort_key", android.R.drawable.ic_menu_sort_by_size);
    iconMap.put("print", R.drawable.print_icon);
    iconMap.put("create_template_from_transaction", R.drawable.create_template_from_transaction_icon);
    iconMap.put("create_folder", android.R.drawable.ic_menu_add);
    iconMap.put("select_folder", R.drawable.ic_menu_goto);
    iconMap.put("up", R.drawable.ic_action_up);
    iconMap.put("categories_export", R.drawable.ic_menu_download);
    iconMap.put("split_transaction", R.drawable.ic_menu_split);
    iconMap.put("move",R.drawable.ic_menu_forward);
  }

  private LayoutInflater layoutInflater;
  private String activityName;
  private String variant;
  private LinearLayout linearLayout;

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
    FragmentActivity ctx = getActivity();
    final Resources res = getResources();
    String title;
    String screenInfo = "";
    Bundle args = getArguments();
    activityName = args.getString(KEY_ACTIVITY_NAME);
    variant = args.getString(KEY_VARIANT);
    layoutInflater = LayoutInflater.from(ctx);
    View view = layoutInflater.inflate(R.layout.help_dialog, null);
    linearLayout = (LinearLayout) view.findViewById(R.id.help);

    try {
      String resIdString = "help_" + activityName + "_info";
      int resId = resolveString(resIdString);
      if (resId != 0) {
        screenInfo = getStringSafe(resId);
      }
      if (variant != null) {
        resIdString = "help_" + activityName + "_" + variant + "_info";
        resId = resolveString(resIdString);
        if (resId != 0) {
          String variantInfo = getStringSafe(resId);
          if (!TextUtils.isEmpty(variantInfo)) {
            if (!TextUtils.isEmpty(screenInfo)) {
              screenInfo += "<br>";
            }
            screenInfo += variantInfo;
          }
        }
      }
      final TextView infoView = (TextView) view.findViewById(R.id.screen_info);
      if (TextUtils.isEmpty(screenInfo)) {
        infoView.setVisibility(View.GONE);
      } else {
        infoView.setText(Html.fromHtml(screenInfo, this, null));
      }

      // Form entries
      resId = variant != null ? resolveArray(activityName + "_" + variant + "_formfields") :
          resolveArray(activityName + "_formfields");
      ArrayList<String> menuItems = new ArrayList<String>();
      if (resId != 0) {
        menuItems.addAll(Arrays.asList(res.getStringArray(resId)));
      }
      if (menuItems.size() == 0) {
        view.findViewById(R.id.form_fields_heading).setVisibility(View.GONE);
      } else {
        handleMenuItems(menuItems, "form", 2);
      }

      // Menu items
      resId = resolveArray(activityName + "_menuitems");
      menuItems.clear();
      if (resId != 0)
        menuItems.addAll(Arrays.asList(res.getStringArray(resId)));
      if (variant != null &&
          (resId = resolveArray(activityName + "_" + variant + "_menuitems")) != 0)
        menuItems.addAll(Arrays.asList(res.getStringArray(resId)));
      if (menuItems.size() == 0)
        view.findViewById(R.id.menu_commands_heading).setVisibility(View.GONE);
      else {
        handleMenuItems(menuItems, "menu", 1);
      }

      // Contextual action bar
      resId = resolveArray(activityName + "_cabitems");
      menuItems.clear();
      if (resId != 0)
        menuItems.addAll(Arrays.asList(res.getStringArray(resId)));
      if (variant != null &&
          (resId = resolveArray(activityName + "_" + variant + "_cabitems")) != 0)
        menuItems.addAll(Arrays.asList(res.getStringArray(resId)));
      if (menuItems.size() == 0)
        view.findViewById(R.id.cab_commands_heading).setVisibility(View.GONE);
      else {
        handleMenuItems(menuItems, "cab", 0);
      }

      resId = variant != null ? resolveString("help_" + activityName + "_" + variant + "_title") : 0;
      if (resId == 0) {
        title = resolveStringOrThrowIf0("help_" + activityName + "_title");
      } else {
        title = getString(resId);
      }
    } catch (NotFoundException e) {
      Utils.reportToAcra(e);
      return new AlertDialog.Builder(ctx)
          .setMessage("Error generating Help dialog")
          .create();
    }
    return new AlertDialog.Builder(ctx)
        .setTitle(title)
        .setIcon(android.R.drawable.ic_menu_help)
        .setView(view)
        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            if (getActivity() == null) {
              return;
            }
            getActivity().finish();
          }
        })
        .create();
  }

  /**
   * @param menuItems
   * @throws NotFoundException
   */

  protected void handleMenuItems(ArrayList<String> menuItems, String prefix, int offset)
      throws NotFoundException {
    final Resources res = getResources();
    String resIdString;
    int resId;
    for (String item : menuItems) {
      View row = layoutInflater.inflate(R.layout.help_dialog_action_row, null);
      if (prefix.equals("form")) {
        row.findViewById(R.id.list_image_container).setVisibility(View.GONE);
      } else if (iconMap.containsKey(item)) {
        resId = iconMap.get(item);
        final ImageView icon = (ImageView) row.findViewById(R.id.list_image);
        icon.setVisibility(View.VISIBLE);
        icon.setImageDrawable(res.getDrawable(resId));
      } else {
        //for the moment we assume that menu entries without icon are checkable
        row.findViewById(R.id.list_checkbox).setVisibility(View.VISIBLE);
      }

      String title = "";
      if (prefix.equals("form")) {
        //this allows us to map an item like "date.time" to the concatenation of translations for date and for time
        for (String resIdPart : item.split("\\.")) {
          if (!title.equals(""))
            title += "/";
          title += resolveStringOrThrowIf0(resIdPart);
        }
      } else {
        title = resolveStringOrThrowIf0("menu_" + item);
      }

      ((TextView) row.findViewById(R.id.title)).setText(title);

      //we look for a help text specific to the variant first, then to the activity
      //and last a generic one
      //We look for an array first, which allows us to compose messages of parts

      CharSequence helpText;

      helpText = resolveStringOrArray(prefix + "_" + activityName + "_" + variant + "_" + item + "_help_text");
      if (TextUtils.isEmpty(helpText)) {
        helpText = resolveStringOrArray(prefix + "_" + activityName + "_" + item + "_help_text");
        if (TextUtils.isEmpty(helpText)) {
          resIdString = prefix + "_" + item + "_help_text";
          helpText = resolveStringOrArray(resIdString);
          if (TextUtils.isEmpty(helpText)) {
            throw new NotFoundException(resIdString);
          }
        }
      }

      ((TextView) row.findViewById(R.id.help_text)).setText(helpText);
      linearLayout.addView(row, linearLayout.getChildCount() - offset);
    }
  }

  private CharSequence resolveStringOrArray(String resString) {
    int resId = resolveArray(resString);
    if (resId == 0) {
      resId = resolveString(resString);
      if (resId == 0) {
        return null;
      } else {
        return Html.fromHtml(getStringSafe(resId), this, null);
      }
    } else {
      String[] components = getResources().getStringArray(resId);
      CharSequence[] resolvedComponents = new CharSequence[components.length];
      for (int i = 0; i < components.length; i++) {
        String component = getStringSafe(resolveString(components[i]));
        if (i<components.length-1) component += " ";
        resolvedComponents[i] = Html.fromHtml(component, this, null);
      }
      return TextUtils.concat(resolvedComponents);
    }
  }

  private int resolveString(String resIdString) {
    return resolve(resIdString, "string");
  }

  /**
   * @param resIdString
   * @return
   * @throws NotFoundException if there is no ressource for the given String. On the contrary, if the
   *                           String does exist in an alternate locale, but not in the default one, the resulting exception is caught
   *                           and empty String is returned.
   */
  private String resolveStringOrThrowIf0(String resIdString) throws NotFoundException {
    int resId = resolveString(resIdString);
    if (resId == 0) {
      throw new NotFoundException(resIdString);
    }
    return getStringSafe(resId);
  }

  private String getStringSafe(int resId) {
    try {
      return getResources().getString(resId);
    } catch (NotFoundException e) {//if resource does exist in an alternate locale, but not in the default one
      return "";
    }
  }

  private int resolveArray(String resIdString) {
    return resolve(resIdString, "array");
  }

  private int resolve(String resIdString, String defType) {
    return resolve(getResources(), resIdString, defType, getActivity().getPackageName());
  }

  private int resolveSystem(String resIdString, String defType) {
    return resolve(getResources().getSystem(), resIdString, defType, "android");
  }

  private int resolve(Resources resources, String resIdString, String defType, String packageName) {
    return resources.getIdentifier(resIdString, defType, packageName);
  }

  public void onCancel(DialogInterface dialog) {
    getActivity().finish();
  }

  @Override
  public Drawable getDrawable(String name) {
    Drawable d = null;
    int resId;
    Resources.Theme theme = getActivity().getTheme();
    try {
      if (name.startsWith("?")) {
        name = name.substring(1);
        TypedValue value = new TypedValue();
        theme.resolveAttribute(resolve(name, "attr"), value, true);
        resId = value.resourceId;
      } else {
        if (name.startsWith("android:")) {
          name = name.substring(8);
          resId = resolveSystem(name, "drawable");
        } else {
          resId = resolve(name, "drawable");
        }
      }
      d = getResources().getDrawable(resId);
    } catch (NotFoundException e) {
      return null;
    }
    d.setBounds(0, 0, d.getIntrinsicWidth() / 2,
        d.getIntrinsicHeight() / 2);
    return d;
  }
}
