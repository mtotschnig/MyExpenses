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

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AlertDialog;
import android.text.Html;
import android.text.Html.ImageGetter;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.util.AcraHelper;
import org.totschnig.myexpenses.util.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

/**
 * A Dialog Fragment that displays help information. The content is constructed from resources
 * based on the activity and an optional variant passed in.
 *
 * @author Michael Totschnig
 */
public class HelpDialogFragment extends CommitSafeDialogFragment implements ImageGetter {

  public static final String KEY_VARIANT = "variant";
  public static final String KEY_CONTEXT = "context";
  public static final HashMap<String, Integer> iconMap = new HashMap<>();

  static {
    iconMap.put("edit", R.drawable.ic_menu_edit);
    iconMap.put("back", R.drawable.ic_menu_back);
    iconMap.put("balance", R.drawable.ic_action_balance);
    iconMap.put("cancel_plan_instance", R.drawable.ic_menu_close_clear_cancel);
    iconMap.put("categories_setup_default", R.drawable.ic_menu_add_list);
    iconMap.put("clone_transaction", R.drawable.ic_menu_copy);
    iconMap.put("create_instance_edit", R.drawable.ic_action_apply_edit);
    iconMap.put("create_instance_save", R.drawable.ic_action_apply_save);
    iconMap.put("create_account", R.drawable.ic_menu_add);
    iconMap.put("create_split", R.drawable.ic_menu_split);
    iconMap.put("create_sub_cat", R.drawable.ic_menu_add);
    iconMap.put("delete", R.drawable.ic_menu_delete);
    iconMap.put("edit", R.drawable.ic_menu_edit);
    iconMap.put("distribution", R.drawable.ic_menu_chart);
    iconMap.put("edit_plan_instance", R.drawable.ic_menu_edit);
    iconMap.put("forward", R.drawable.ic_menu_forward);
    iconMap.put("invert_transfer", R.drawable.ic_menu_move);
    iconMap.put("manage_plans", R.drawable.ic_menu_template);
    iconMap.put("reset", R.drawable.ic_menu_download);
    iconMap.put("reset_plan_instance", R.drawable.ic_menu_revert);
    iconMap.put("save_and_new", R.drawable.ic_action_save_new);
    iconMap.put("save", R.drawable.ic_menu_done);
    iconMap.put("search", R.drawable.ic_menu_search);
    iconMap.put("select_category", R.drawable.ic_menu_done);
    iconMap.put("print", R.drawable.ic_menu_print);
    iconMap.put("create_template_from_transaction", R.drawable.ic_action_template_add);
    iconMap.put("create_folder", R.drawable.ic_menu_add);
    iconMap.put("select_folder", R.drawable.ic_menu_done);
    iconMap.put("up", R.drawable.ic_arrow_upward);
    iconMap.put("categories_export", R.drawable.ic_menu_download);
    iconMap.put("split_transaction", R.drawable.ic_menu_split);
    iconMap.put("move",R.drawable.ic_menu_move);
    iconMap.put("sort",R.drawable.ic_menu_sort);
    iconMap.put("sort_up",R.drawable.ic_arrow_upward);
    iconMap.put("sort_down",R.drawable.ic_arrow_downward);
    iconMap.put("grouping",R.drawable.ic_action_group);
  }

  private LayoutInflater layoutInflater;
  private String context;
  private String variant;
  private LinearLayout linearLayout;

  public static HelpDialogFragment newInstance(String context, Enum<?> variant) {
    HelpDialogFragment dialogFragment = new HelpDialogFragment();
    Bundle args = new Bundle();
    args.putString(KEY_CONTEXT, context);
    if (variant != null)
      args.putString(KEY_VARIANT, variant.name());
    dialogFragment.setArguments(args);
    return dialogFragment;
  }

  @NonNull
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    FragmentActivity ctx = getActivity();
    final Resources res = getResources();
    String title;
    String screenInfo = "";
    Bundle args = getArguments();
    context = args.getString(KEY_CONTEXT);
    variant = args.getString(KEY_VARIANT);
    layoutInflater = LayoutInflater.from(ctx);
    @SuppressLint("InflateParams")
    final View view = layoutInflater.inflate(R.layout.help_dialog, null);
    linearLayout = (LinearLayout) view.findViewById(R.id.help);

    try {
      String resIdString = "help_" + context + "_info";
      int resId = resolveString(resIdString);
      if (resId != 0) {
        screenInfo = getStringSafe(resId);
      }
      if (variant != null) {
        resIdString = "help_" + context + "_" + variant + "_info";
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
      resId = variant != null ? resolveArray(context + "_" + variant + "_formfields") :
          resolveArray(context + "_formfields");
      ArrayList<String> menuItems = new ArrayList<>();
      if (resId != 0) {
        menuItems.addAll(Arrays.asList(res.getStringArray(resId)));
      }
      if (menuItems.isEmpty()) {
        view.findViewById(R.id.form_fields_heading).setVisibility(View.GONE);
      } else {
        handleMenuItems(menuItems, "form", 2);
      }

      // Menu items
      resId = variant != null ? resolveArray(context + "_" + variant + "_menuitems") :
          resolveArray(context + "_menuitems");
      menuItems.clear();
      if (resId != 0)
        menuItems.addAll(Arrays.asList(res.getStringArray(resId)));
      if (menuItems.isEmpty())
        view.findViewById(R.id.menu_commands_heading).setVisibility(View.GONE);
      else {
        handleMenuItems(menuItems, "menu", 1);
      }

      // Contextual action bar
      resId = variant != null ? resolveArray(context + "_" + variant + "_cabitems") :
          resolveArray(context + "_cabitems");
      menuItems.clear();
      if (resId != 0)
        menuItems.addAll(Arrays.asList(res.getStringArray(resId)));
      if (menuItems.isEmpty())
        view.findViewById(R.id.cab_commands_heading).setVisibility(View.GONE);
      else {
        handleMenuItems(menuItems, "cab", 0);
      }

      resId = variant != null ? resolveString("help_" + context + "_" + variant + "_title") : 0;
      if (resId == 0) {
        title = resolveStringOrThrowIf0("help_" + context + "_title");
      } else {
        title = getString(resId);
      }
    } catch (NotFoundException e) {
      AcraHelper.report(e);
      return new AlertDialog.Builder(ctx)
          .setMessage("Error generating Help dialog")
          .create();
    }
/*    view.setOnTouchListener(new View.OnTouchListener() {
      @Override
      public boolean onTouch(View v, MotionEvent event) {
        final ObjectAnimator animScrollToTop = ObjectAnimator.ofInt(view, "scrollY", 4000);
        animScrollToTop.setDuration(4000);
        animScrollToTop.start();
        return true;
      }
    });*/
    return new AlertDialog.Builder(ctx)
        .setTitle(title)
        .setIcon(R.drawable.ic_menu_help)
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
   * @param menuItems list of menuitems to be displayed
   * @param prefix "form", "menu" or "cab"
   * @param offset items will be added with this offset at the bottom
   * @throws NotFoundException
   */
  protected void handleMenuItems(ArrayList<String> menuItems, String prefix, int offset)
      throws NotFoundException {
    String resIdString;
    int resId;
    for (String item : menuItems) {
      View row = layoutInflater.inflate(R.layout.help_dialog_action_row, linearLayout,false);

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

      if (prefix.equals("form")) {
        row.findViewById(R.id.list_image_container).setVisibility(View.GONE);
      } else if (iconMap.containsKey(item)) {
        resId = iconMap.get(item);
        final ImageView icon = (ImageView) row.findViewById(R.id.list_image);
        icon.setVisibility(View.VISIBLE);
        icon.setImageResource(resId);
        icon.setContentDescription(title);
      } else {
        //for the moment we assume that menu entries without icon are checkable
        row.findViewById(R.id.list_checkbox).setVisibility(View.VISIBLE);
      }

      //we look for a help text specific to the variant first, then to the activity
      //and last a generic one
      //We look for an array first, which allows us to compose messages of parts

      CharSequence helpText;

      helpText = resolveStringOrArray(prefix + "_" + context + "_" + variant + "_" + item + "_help_text");
      if (TextUtils.isEmpty(helpText)) {
        helpText = resolveStringOrArray(prefix + "_" + context + "_" + item + "_help_text");
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
      ArrayList<CharSequence> resolvedComponents = new ArrayList<>();
      for (int i = 0; i < components.length; i++) {
        if (shouldSkip(components[i])) {
          continue;
        }
        String component = getStringSafe(resolveString(components[i]));
        if (i<components.length-1) component += " ";
        resolvedComponents.add(Html.fromHtml(component, this, null));
      }
      return TextUtils.concat(resolvedComponents.toArray(new CharSequence[resolvedComponents.size()]));
    }
  }

  private boolean shouldSkip(String component) {
    switch (component) {
      case "form_plan_help_text_advanced":
        return !Utils.shouldUseAndroidPlatformCalendar();
    }
    return false;
  }

  private int resolveString(String resIdString) {
    return resolve(resIdString, "string");
  }

  /**
   * @throws NotFoundException if there is no ressource for the given String. On the contrary, if the
   *                           String does exist in an alternate locale, but not in the default one,
   *                           the resulting exception is caught and empty String is returned.
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
    return resolve(Resources.getSystem(), resIdString, defType, "android");
  }

  private int resolve(Resources resources, String resIdString, String defType, String packageName) {
    return resources.getIdentifier(resIdString, defType, packageName);
  }

  public void onCancel(DialogInterface dialog) {
    getActivity().finish();
  }

  @Override
  public Drawable getDrawable(String name) {
    int resId;
    Resources.Theme theme = getActivity().getTheme();
    try {
      if (name.startsWith("?")) {
        name = name.substring(1);
        switch(name) {
          case "calcIcon":
            resId = R.drawable.ic_action_equal;
            break;
          default:
            TypedValue value = new TypedValue();
            theme.resolveAttribute(resolve(name, "attr"), value, true);
            resId = value.resourceId;
        }
      } else {
        if (name.startsWith("android:")) {
          name = name.substring(8);
          resId = resolveSystem(name, "drawable");
        } else {
          resId = resolve(name, "drawable");
        }
      }
      @SuppressWarnings("deprecation")
      Drawable d = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP ?
          getResources().getDrawable(resId,getActivity().getTheme()):
          getResources().getDrawable(resId);
      if (d != null) {
        d.setBounds(0, 0, d.getIntrinsicWidth() / 2,
            d.getIntrinsicHeight() / 2);
      }
      return d;
    } catch (NotFoundException e) {
      return null;
    }

  }
}
