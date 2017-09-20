package org.totschnig.myexpenses.preference;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;

// the following keys are stored as string resources, so that
// they can be referenced from preferences.xml, and thus we
// can guarantee the referential integrity
public enum PrefKey {
  ROOT_SCREEN(R.string.pref_root_screen_key),
  CATEGORIES_SORT_BY_USAGES_LEGACY(R.string.pref_categories_sort_by_usages_key),
  SORT_ORDER_LEGACY(R.string.pref_sort_order_key),
  SORT_ORDER_TEMPLATES("sort_order_templates"),
  SORT_ORDER_CATEGORIES("sort_order_categories"),
  SORT_ORDER_ACCOUNTS("sort_order_accounts"),
  PERFORM_SHARE(R.string.pref_perform_share_key),
  SHARE_TARGET(R.string.pref_share_target_key),
  UI_THEME_KEY(R.string.pref_ui_theme_key),
  UI_FONTSIZE(R.string.pref_ui_fontsize_key),
  BACKUP(R.string.pref_backup_key),
  RESTORE(R.string.pref_restore_key),
  IMPORT_QIF(R.string.pref_import_qif_key),
  IMPORT_CSV(R.string.pref_import_csv_key),
  RESTORE_LEGACY(R.string.pref_restore_legacy_key),
  CONTRIB_PURCHASE(R.string.pref_contrib_purchase_key),
  ENTER_LICENCE(R.string.pref_enter_licence_key),
  NEW_LICENCE(R.string.pref_new_licence_key),
  PERFORM_PROTECTION(R.string.pref_perform_protection_key),
  PERFORM_PROTECTION_SCREEN(R.string.pref_screen_protection_key),
  SET_PASSWORD(R.string.pref_set_password_key),
  SECURITY_ANSWER(R.string.pref_security_answer_key),
  SECURITY_QUESTION(R.string.pref_security_question_key),
  PROTECTION_DELAY_SECONDS(R.string.pref_protection_delay_seconds_key),
  PROTECTION_ENABLE_ACCOUNT_WIDGET(R.string.pref_protection_enable_account_widget_key),
  PROTECTION_ENABLE_TEMPLATE_WIDGET(R.string.pref_protection_enable_template_widget_key),
  PROTECTION_ENABLE_DATA_ENTRY_FROM_WIDGET(R.string.pref_protection_enable_data_entry_from_widget_key),
  EXPORT_FORMAT(R.string.pref_export_format_key),
  SEND_FEEDBACK(R.string.pref_send_feedback_key),
  MORE_INFO_DIALOG(R.string.pref_more_info_dialog_key),
  SHORTCUT_CREATE_TRANSACTION(R.string.pref_shortcut_create_transaction_key),
  SHORTCUT_CREATE_TRANSFER(R.string.pref_shortcut_create_transfer_key),
  SHORTCUT_CREATE_SPLIT(R.string.pref_shortcut_create_split_key),
  PLANNER_CALENDAR_ID(R.string.pref_planner_calendar_id_key),
  RATE(R.string.pref_rate_key),
  UI_LANGUAGE(R.string.pref_ui_language_key),
  APP_DIR(R.string.pref_app_dir_key),
  CATEGORY_CONTRIB(R.string.pref_category_contrib_key),
  CATEGORY_MANAGE(R.string.pref_category_manage_key),
  ACCOUNT_GROUPING(R.string.pref_account_grouping_key),
  PLANNER_CALENDAR_PATH("planner_calendar_path"),
  CURRENT_VERSION("currentversion"),
  FIRST_INSTALL_VERSION("first_install_version"),
  CURRENT_ACCOUNT("current_account"),
  PLANNER_LAST_EXECUTION_TIMESTAMP("planner_last_execution_timestamp"),
  APP_FOLDER_WARNING_SHOWN("app_folder_warning_shown"),
  AUTO_FILL(R.string.pref_auto_fill_key),
  AUTO_FILL_HINT_SHOWN("auto_fill_hint_shown"),
  TEMPLATE_CLICK_DEFAULT(R.string.pref_template_click_default_key),
  TEMPLATE_CLICK_HINT_SHOWN("template_click_hint_shown"),
  NEXT_REMINDER_RATE("nextReminderRate"),
  NEXT_REMINDER_CONTRIB("nextReminderContrib"),
  DISTRIBUTION_SHOW_CHART("distributionShowChart"),
  DISTRIBUTION_AGGREGATE_TYPES("distributionAggregateTypes"),
  MANAGE_STALE_IMAGES(R.string.pref_manage_stale_images_key),
  CSV_IMPORT_HEADER_TO_FIELD_MAP(R.string.pref_import_csv_header_to_field_map_key),
  CUSTOM_DECIMAL_FORMAT(R.string.pref_custom_decimal_format_key),
  AUTO_BACKUP(R.string.pref_auto_backup_key),
  AUTO_BACKUP_TIME(R.string.pref_auto_backup_time_key),
  AUTO_BACKUP_DIRTY("auto_backup_dirty"),
  AUTO_BACUP_CLOUD(R.string.pref_auto_backup_cloud_key),
  AUTO_BACKUP_INFO(R.string.pref_auto_backup_info_key),
  UI_HOME_SCREEN_SHORTCUTS(R.string.pref_ui_home_screen_shortcuts_key),
  CALENDAR_PERMISSION_REQUESTED("calendar_permission_requested"),
  STORAGE_PERMISSION_REQUESTED("storage_permission_requested"),
  GROUPING_START_SCREEN(R.string.pref_grouping_start_key),
  GROUP_WEEK_STARTS(R.string.pref_group_week_starts_key),
  GROUP_MONTH_STARTS(R.string.pref_group_month_starts_key),
  NEW_PLAN_ENABLED("new_plan_enabled"),
  NEW_ACCOUNT_ENABLED("new_account_enabled"),
  NEW_SPLIT_TEMPLATE_ENABLED("new_split_template_enabled"),
  SYNC_FREQUCENCY(R.string.pref_sync_frequency_key),
  SYNC_UPSELL_NOTIFICATION_SHOWN("sync_upsell_notification_shown"),
  MANAGE_SYNC_BACKENDS(R.string.pref_manage_sync_backends_key),
  TRACKING(R.string.pref_tracking_key),
  WEBDAV_TIMEOUT(R.string.pref_webdav_timeout_key),
  DEBUG_SCREEN(R.string.pref_debug_key),
  DEBUG_LOGGING(R.string.pref_debug_logging_key),
  SYNC_NOTIFICATION(R.string.pref_sync_notification_key);

  private int resId = 0;
  private String key = null;

  public String getKey() {
    return resId == 0 ? key : MyApplication.getInstance().getString(resId);
  }

  public String getString(String defValue) {
    return MyApplication.getInstance().getSettings().getString(getKey(), defValue);
  }

  public void putString(String value) {
    MyApplication.getInstance().getSettings().edit().putString(getKey(), value).apply();
  }

  public boolean getBoolean(boolean defValue) {
    return MyApplication.getInstance().getSettings().getBoolean(getKey(), defValue);
  }

  public void putBoolean(boolean value) {
    MyApplication.getInstance().getSettings().edit().putBoolean(getKey(), value).apply();
  }

  public int getInt(int defValue) {
    return MyApplication.getInstance().getSettings().getInt(getKey(), defValue);
  }

  public void putInt(int value) {
    MyApplication.getInstance().getSettings().edit().putInt(getKey(), value).apply();
  }

  public long getLong(long defValue) {
    return MyApplication.getInstance().getSettings().getLong(getKey(), defValue);
  }

  public void putLong(long value) {
    MyApplication.getInstance().getSettings().edit().putLong(getKey(), value).apply();
  }

  public void remove() {
    MyApplication.getInstance().getSettings().edit().remove(getKey()).apply();
  }

  public boolean isSet() {
    return MyApplication.getInstance().getSettings().contains(getKey());
  }

  PrefKey(int resId) {
    this.resId = resId;
  }

  PrefKey(String key) {
    this.key = key;
  }
}
