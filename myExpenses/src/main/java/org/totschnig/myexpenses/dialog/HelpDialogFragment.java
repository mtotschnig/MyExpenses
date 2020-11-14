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

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Html;
import android.text.Html.ImageGetter;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.util.DistributionHelper;
import org.totschnig.myexpenses.util.crashreporting.CrashHandler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import androidx.annotation.ArrayRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;

/**
 * A Dialog Fragment that displays help information. The content is constructed from resources
 * based on the activity and an optional variant passed in.
 *
 * @author Michael Totschnig
 */
public class HelpDialogFragment extends CommitSafeDialogFragment implements ImageGetter {

  public static final String KEY_VARIANT = "variant";
  private static final String KEY_CONTEXT = "context";
  private static final HashMap<String, Integer> iconMap = new HashMap<>();

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
    iconMap.put("distribution", R.drawable.ic_menu_chart);
    iconMap.put("edit_plan_instance", R.drawable.ic_menu_edit);
    iconMap.put("forward", R.drawable.ic_menu_forward);
    iconMap.put("invert_transfer", R.drawable.ic_menu_move);
    iconMap.put("manage_plans", R.drawable.ic_menu_template);
    iconMap.put("templates", R.drawable.ic_menu_template);
    iconMap.put("reset", R.drawable.ic_menu_download);
    iconMap.put("reset_plan_instance", R.drawable.ic_menu_revert);
    iconMap.put("save", R.drawable.ic_menu_done);
    iconMap.put("search", R.drawable.ic_menu_search);
    iconMap.put("select", R.drawable.ic_menu_done);
    iconMap.put("print", R.drawable.ic_menu_print);
    iconMap.put("create_template_from_transaction", R.drawable.ic_action_template_add);
    iconMap.put("create_folder", R.drawable.ic_menu_add);
    iconMap.put("up", R.drawable.ic_arrow_upward);
    iconMap.put("categories_export", R.drawable.ic_menu_download);
    iconMap.put("split_transaction", R.drawable.ic_menu_split_transaction);
    iconMap.put("ungroup_split_transaction", R.drawable.ic_menu_split);
    iconMap.put("move", R.drawable.ic_menu_move);
    iconMap.put("sort", R.drawable.ic_menu_sort);
    iconMap.put("sort_direction", R.drawable.ic_menu_sort);
    iconMap.put("sort_up", R.drawable.ic_arrow_upward);
    iconMap.put("sort_down", R.drawable.ic_arrow_downward);
    iconMap.put("grouping", R.drawable.ic_action_group);
    iconMap.put("create_sync_backend", R.drawable.ic_menu_add);
    iconMap.put("sync_now", null);
    iconMap.put("remove", null);
    iconMap.put("sync_download", null);
    iconMap.put("sync_link", null);
    iconMap.put("sync_unlink", null);
    iconMap.put("vote", null);
    iconMap.put("refresh", R.drawable.ic_sync);
    iconMap.put("result", R.drawable.ic_web);
    iconMap.put("learn_more", null);
    iconMap.put("set_weight", null);
    iconMap.put("original_amount", null);
    iconMap.put("equivalent_amount", null);
    iconMap.put("color", R.drawable.ic_color);
    iconMap.put("history", R.drawable.ic_history);
    iconMap.put("budget", R.drawable.ic_budget);
    iconMap.put("manage_categories", null);
    iconMap.put("show_transactions", null);
    iconMap.put("back.forward", null);
    iconMap.put("hidden_accounts", R.drawable.design_ic_visibility_off);
    iconMap.put("hide", R.drawable.design_ic_visibility_off);
    iconMap.put("close.reopen", R.drawable.ic_lock);
    iconMap.put("remap", null);
    iconMap.put("scan_mode", R.drawable.ic_scan);
  }

  private String context;
  private String variant;
  private LinearLayout linearLayout;

  public static HelpDialogFragment newInstance(String context, String variant) {
    HelpDialogFragment dialogFragment = new HelpDialogFragment();
    Bundle args = new Bundle();
    args.putString(KEY_CONTEXT, context);
    if (variant != null) {
      args.putString(KEY_VARIANT, variant);
    }
    dialogFragment.setArguments(args);
    return dialogFragment;
  }

  @NonNull
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    FragmentActivity ctx = getActivity();
    final Resources res = getResources();
    String title;
    Bundle args = getArguments();
    context = args.getString(KEY_CONTEXT);
    variant = args.getString(KEY_VARIANT);
    AlertDialog.Builder builder = initBuilderWithView(R.layout.help_dialog);

    linearLayout = dialogView.findViewById(R.id.help);

    try {
      String resIdString = "help_" + context + "_info";
      CharSequence screenInfo = resolveStringOrArray(resIdString, true);
      if (variant != null) {
        resIdString = "help_" + context + "_" + variant + "_info";
        CharSequence variantInfo = resolveStringOrArray(resIdString, true);
        if (!TextUtils.isEmpty(variantInfo)) {
          if (!TextUtils.isEmpty(screenInfo)) {
            screenInfo = TextUtils.concat(screenInfo, Html.fromHtml("<br>"), variantInfo);
          } else {
            screenInfo = variantInfo;
          }
        }
      }
      final TextView infoView = dialogView.findViewById(R.id.screen_info);
      if (TextUtils.isEmpty(screenInfo)) {
        infoView.setVisibility(View.GONE);
      } else {
        infoView.setText(screenInfo);
        infoView.setMovementMethod(LinkMovementMethod.getInstance());
      }

      // Form entries
      final int formResId = resolveArray(buildComponentName("formfields"));
      ArrayList<String> menuItems = new ArrayList<>();
      if (formResId != 0) {
        menuItems.addAll(Arrays.asList(res.getStringArray(formResId)));
      }
      if (menuItems.isEmpty()) {
        dialogView.findViewById(R.id.form_fields_heading).setVisibility(View.GONE);
      } else {
        handleMenuItems(menuItems, "form", dialogView.findViewById(R.id.form_fields_container));
      }

      // Menu items
      final int menuResId = resolveArray(buildComponentName("menuitems"));
      menuItems.clear();
      if (menuResId != 0)
        menuItems.addAll(Arrays.asList(res.getStringArray(menuResId)));
      if (menuItems.isEmpty())
        dialogView.findViewById(R.id.menu_commands_heading).setVisibility(View.GONE);
      else {
        handleMenuItems(menuItems, "menu", dialogView.findViewById(R.id.menu_commands_container));
      }

      // Contextual action bar
      final String componentName = buildComponentName("cabitems");
      final int cabResId = resolveArray(componentName);
      menuItems.clear();
      if (cabResId != 0)
        menuItems.addAll(Arrays.asList(res.getStringArray(cabResId)));
      if (menuItems.isEmpty()) {
        dialogView.findViewById(R.id.cab_commands_heading).setVisibility(View.GONE);
      } else {
        handleMenuItems(menuItems, "cab", dialogView.findViewById(R.id.cab_commands_container));
      }
      if (menuItems.isEmpty() || !showLongTapHint(componentName)) {
        dialogView.findViewById(R.id.cab_commands_help).setVisibility(View.GONE);
      }

      final int titleResId = variant != null ? resolveString("help_" + context + "_" + variant + "_title") : 0;
      if (titleResId == 0) {
        title = resolveStringOrThrowIf0("help_" + context + "_title");
      } else {
        title = getString(titleResId);
      }
    } catch (NotFoundException e) {
      CrashHandler.report(e);
      return new MaterialAlertDialogBuilder(ctx)
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
    return builder.setTitle(title)
        .setIcon(R.drawable.ic_menu_help)
        .setPositiveButton(android.R.string.ok, (dialog, which) -> {
          if (getActivity() == null) {
            return;
          }
          getActivity().finish();
        })
        .create();
  }

  private boolean showLongTapHint(String componentName) {
    return !componentName.equals("ManageTemplates_planner_cabitems");
  }

  private String buildComponentName(String type) {
    return variant != null ? (context + "_" + variant + "_" + type) : (context + "_" + type);
  }

  /**
   * @param menuItems list of menuitems to be displayed
   * @param prefix    "form", "menu" or "cab"
   * @param container items will be added to this container
   * @throws NotFoundException
   */
  protected void handleMenuItems(ArrayList<String> menuItems, String prefix, ViewGroup container)
      throws NotFoundException {
    String resIdString;
    Integer resId;
    for (String item : menuItems) {
      View row = layoutInflater.inflate(R.layout.help_dialog_action_row, linearLayout, false);

      String title = "";
      //this allows us to map an item like "date.time" to the concatenation of translations for date and for time
      for (String resIdPart : item.split("\\.")) {
        if (!title.equals(""))
          title += "/";
        title += resolveStringOrThrowIf0((prefix.equals("form") ? "" : "menu_") + resIdPart);
      }

      ((TextView) row.findViewById(R.id.title)).setText(title);

      if (prefix.equals("form")) {
        row.findViewById(R.id.list_image_container).setVisibility(View.GONE);
      } else if (iconMap.containsKey(item)) {
        resId = iconMap.get(item);
        if (resId != null) {
          //null value in the map indicates no icon
          final ImageView icon = row.findViewById(R.id.list_image);
          icon.setVisibility(View.VISIBLE);
          icon.setImageResource(resId);
          icon.setContentDescription(title);
        }
      } else {
        //menu entries without entries in the map are checkable
        row.findViewById(R.id.list_checkbox).setVisibility(View.VISIBLE);
      }

      //we look for a help text specific to the variant first, then to the activity
      //and last a generic one
      //We look for an array first, which allows us to compose messages of parts

      CharSequence helpText;

      helpText = resolveStringOrArray(prefix + "_" + context + "_" + variant + "_" + item + "_help_text", false);
      if (TextUtils.isEmpty(helpText)) {
        helpText = resolveStringOrArray(prefix + "_" + context + "_" + item + "_help_text", false);
        if (TextUtils.isEmpty(helpText)) {
          resIdString = prefix + "_" + item + "_help_text";
          helpText = resolveStringOrArray(resIdString, false);
          if (TextUtils.isEmpty(helpText)) {
            throw new NotFoundException(resIdString);
          }
        }
      }

      ((TextView) row.findViewById(R.id.help_text)).setText(helpText);
      container.addView(row);
    }
  }

  private CharSequence resolveStringOrArray(String resString, boolean separateComponentsByLinefeeds) {
    final String resIdString = resString.replace('.', '_');
    int arrayId = resolveArray(resIdString);
    if (arrayId == 0) {
      int stringId = resolveString(resIdString);
      if (stringId == 0) {
        return null;
      } else {
        return Html.fromHtml(getStringSafe(stringId), this, null);
      }
    } else {
      CharSequence linefeed = Html.fromHtml("<br>");
      List<String> components = Stream.of(getResources().getStringArray(arrayId))
          .filter(component -> !shouldSkip(component))
          .map(this::resolveString)
          .map(this::getStringSafe).collect(Collectors.toList());
      ArrayList<CharSequence> resolvedComponents = new ArrayList<>();
      for (int i = 0; i < components.size(); i++) {
        resolvedComponents.add(Html.fromHtml(components.get(i), this, null));
        if (i < components.size() - 1) {
          resolvedComponents.add(separateComponentsByLinefeeds ? linefeed : " ");
        }
      }
      return TextUtils.concat(resolvedComponents.toArray(new CharSequence[resolvedComponents.size()]));
    }
  }

  private boolean shouldSkip(String component) {
    switch (component) {
      case "form_plan_help_text_advanced":
        return !DistributionHelper.shouldUseAndroidPlatformCalendar();
      case "help_ManageSyncBackends_drive":
        return DistributionHelper.isGithub();
    }
    return false;
  }

  private
  @StringRes
  int resolveString(String resIdString) {
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

  private
  @ArrayRes
  int resolveArray(String resIdString) {
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
      //Keeping the legacy attribute reference in order to not have to update all translations, where
      //it appears
      if (name.startsWith("?")) {
        name = name.substring(1);
        //noinspection SwitchStatementWithTooFewBranches
        switch (name) {
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
      Drawable d = null;
      if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
        d = getResources().getDrawable(resId, getActivity().getTheme());
      } else {
        d = getResources().getDrawable(resId);
      }
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
