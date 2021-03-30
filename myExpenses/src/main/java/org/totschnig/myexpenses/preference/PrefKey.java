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
  SORT_ORDER_BUDGET_CATEGORIES("sort_order_budget_categories"),
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
  LICENCE_LEGACY(R.string.pref_enter_licence_key),
  NEW_LICENCE(R.string.pref_new_licence_key),
  LICENCE_EMAIL("licence_email"),
  PROTECTION_LEGACY(R.string.pref_protection_password_key),
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
  CATEGORY_UI(R.string.pref_category_ui_key),
  CATEGORY_CONTRIB(R.string.pref_category_contrib_key),
  CATEGORY_MANAGE(R.string.pref_category_manage_key),
  CATEGORY_PRIVACY(R.string.pref_category_privacy_key),
  CATEGORY_BACKUP(R.string.pref_category_backup_key),
  CATEGORY_ADVANCED(R.string.pref_category_advanced_key),
  ACCOUNT_GROUPING(R.string.pref_account_grouping_key),
  PLANNER_CALENDAR_PATH("planner_calendar_path"),
  CURRENT_VERSION("currentversion"),
  FIRST_INSTALL_VERSION("first_install_version"),
  FIRST_INSTALL_DB_SCHEMA_VERSION("first_install_db_schema_version"),
  CURRENT_ACCOUNT("current_account"),
  PLANNER_LAST_EXECUTION_TIMESTAMP("planner_last_execution_timestamp"),
  APP_FOLDER_WARNING_SHOWN("app_folder_warning_shown"),
  AUTO_FILL_LEGACY("auto_fill"),
  AUTO_FILL_ACCOUNT(R.string.pref_auto_fill_account_key),
  AUTO_FILL_AMOUNT(R.string.pref_auto_fill_amount_key),
  AUTO_FILL_CATEGORY(R.string.pref_auto_fill_category_key),
  AUTO_FILL_COMMENT(R.string.pref_auto_fill_comment_key),
  AUTO_FILL_METHOD(R.string.pref_auto_fill_method_key),
  AUTO_FILL_HINT_SHOWN("auto_fill_hint_shown"),
  TEMPLATE_CLICK_DEFAULT(R.string.pref_template_click_default_key),
  NEXT_REMINDER_RATE("nextReminderRate"),
  DISTRIBUTION_SHOW_CHART("distributionShowChart"),
  DISTRIBUTION_AGGREGATE_TYPES("distributionAggregateTypes"),
  BUDGET_AGGREGATE_TYPES("budgetAggregateTypes"),
  MANAGE_STALE_IMAGES(R.string.pref_manage_stale_images_key),
  CSV_IMPORT_HEADER_TO_FIELD_MAP(R.string.pref_import_csv_header_to_field_map_key),
  CUSTOM_DECIMAL_FORMAT(R.string.pref_custom_decimal_format_key),
  CUSTOM_DATE_FORMAT(R.string.pref_custom_date_format_key),
  AUTO_BACKUP(R.string.pref_auto_backup_key),
  AUTO_BACKUP_TIME(R.string.pref_auto_backup_time_key),
  AUTO_BACKUP_DIRTY("auto_backup_dirty"),
  AUTO_BACKUP_CLOUD(R.string.pref_auto_backup_cloud_key),
  AUTO_BACKUP_INFO(R.string.pref_auto_backup_info_key),
  UI_HOME_SCREEN_SHORTCUTS(R.string.pref_ui_home_screen_shortcuts_key),
  CALENDAR_PERMISSION_REQUESTED("calendar_permission_requested"),
  STORAGE_PERMISSION_REQUESTED("storage_permission_requested"),
  GROUPING_START_SCREEN(R.string.pref_grouping_start_key),
  GROUP_WEEK_STARTS(R.string.pref_group_week_starts_key),
  GROUP_MONTH_STARTS(R.string.pref_group_month_starts_key),
  NEW_PLAN_ENABLED("new_plan_enabled"),
  INTERSTITIAL_LAST_SHOWN("interstitialLastShown"),
  ENTRIES_CREATED_SINCE_LAST_INTERSTITIAL("entriesCreatedSinceLastInterstitial"),
  NEW_ACCOUNT_ENABLED("new_account_enabled"),
  NEW_SPLIT_TEMPLATE_ENABLED("new_split_template_enabled"),
  SYNC_FREQUCENCY(R.string.pref_sync_frequency_key),
  SYNC_UPSELL_NOTIFICATION_SHOWN("sync_upsell_notification_shown"),
  MANAGE_SYNC_BACKENDS(R.string.pref_manage_sync_backends_key),
  TRACKING(R.string.pref_tracking_key),
  WEBDAV_TIMEOUT(R.string.pref_webdav_timeout_key),
  DEBUG_SCREEN(R.string.pref_debug_key),
  DEBUG_LOGGING(R.string.pref_debug_logging_key),
  SYNC_NOTIFICATION(R.string.pref_sync_notification_key),
  SYNC_WIFI_ONLY(R.string.pref_sync_wifi_only_key),
  DEBUG_ADS(R.string.pref_debug_show_ads_key),
  PROTECTION_DEVICE_LOCK_SCREEN(R.string.pref_protection_device_lock_screen_key),
  HISTORY_SHOW_BALANCE("history_show_balance"),
  HISTORY_SHOW_TOTALS("history_show_totals"),
  HISTORY_INCLUDE_TRANSFERS("history_include_transfers"),
  ROADMAP_VOTE("roadmap_vote"),
  ROADMAP_VERSION("roadmap_version"),
  CRASHREPORT_SCREEN(R.string.pref_crash_reports_key),
  CRASHREPORT_ENABLED(R.string.pref_crashreport_enabled_key),
  CRASHREPORT_USEREMAIL(R.string.pref_crashreport_useremail_key),
  HOME_CURRENCY(R.string.pref_home_currency_key),
  LAST_ORIGINAL_CURRENCY("last_original_currency"),
  TRANSACTION_WITH_TIME(R.string.pref_transaction_time_key),
  TRANSACTION_WITH_VALUE_DATE(R.string.pref_value_date_key),
  TRANSACTION_LAST_ACCOUNT_FROM_WIDGET("transactionLastAccountFromWidget"),
  TRANSFER_LAST_ACCOUNT_FROM_WIDGET("transferLastAccountFromWidget"),
  TRANSFER_LAST_TRANSFER_ACCOUNT_FROM_WIDGET("transferLastTransferAccountFromWidget"),
  SPLIT_LAST_ACCOUNT_FROM_WIDGET("splitLastAccountFromWidget"),
  PROFESSIONAL_EXPIRATION_REMINDER_LAST_SHOWN("professionalExpirationReminderLastShown"),
  PROFESSIONAL_UPSELL_SNACKBAR_SHOWN("professionalUpsellSnackbarShown"),
  PERSONALIZED_AD_CONSENT(R.string.pref_ad_consent_key),
  SCROLL_TO_CURRENT_DATE(R.string.pref_scroll_to_current_date_key),
  EXPORT_PASSWORD(R.string.pref_security_export_password_key),
  ACCOUNT_LIST_FAST_SCROLL(R.string.pref_account_list_fast_scroll_key),
  TRANSLATION(R.string.pref_translation_key),
  SYNC_CHANGES_IMMEDIATELY(R.string.pref_sync_changes_immediately_key),
  EXCHANGE_RATE_PROVIDER(R.string.pref_exchange_rate_provider_key),
  OPEN_EXCHANGE_RATES_APP_ID(R.string.pref_openexchangerates_app_id_key),
  PLANNER_EXECUTION_TIME(R.string.pref_plan_executor_time_key),
  WEBDAV_ALLOW_UNVERIFIED_HOST(R.string.pref_webdav_allow_unverified_host_key),
  CLONE_WITH_CURRENT_DATE(R.string.pref_clone_with_current_date_key),
  PLANNER_MANUAL_TIME(R.string.pref_planner_manual_time_key),
  OCR(R.string.pref_ocr_key),
  EXCHANGE_RATES(R.string.pref_exchange_rates_key),
  OCR_TOTAL_INDICATORS(R.string.pref_ocr_total_indicators_key),
  OCR_TIME_FORMATS(R.string.pref_ocr_time_formats_key),
  OCR_DATE_FORMATS(R.string.pref_ocr_date_formats_key),
  CRITERION_FUTURE(R.string.pref_criterion_future_key),
  SYNC(R.string.pref_sync_key),
  FEATURE_UNINSTALL(R.string.pref_feature_uninstall_key),
  FEATURE_UNINSTALL_FEATURES(R.string.pref_feature_uninstall_features_key),
  FEATURE_UNINSTALL_LANGUAGES(R.string.pref_feature_uninstall_languages_key),
  EXPENSE_EDIT_SAVE_AND_NEW("expense_edit_save_and_new"),
  EXPENSE_EDIT_SAVE_AND_NEW_SPLIT_PART("expense_edit_save_and_new_split_part"),
  ACRA_INFO(R.string.pre_acra_info_key),
  OCR_ENGINE(R.string.pref_ocr_engine_key),
  TESSERACT_LANGUAGE(R.string.pref_tesseract_language_key),
  GROUP_HEADER(R.string.pref_group_header_show_details_key),
  UI_WEB(R.string.pref_web_ui_key),
  DATES_ARE_LINKED("dates_are_linked"),
  VOTE_REMINDER_LAST_CHECK("vote_reminder_last_check");

  int resId = 0;
  String key = null;

  @Deprecated
  public String getKey() {
    return resId == 0 ? key : MyApplication.getInstance().getString(resId);
  }

  @Deprecated
  public String getString(String defValue) {
    return MyApplication.getInstance().getSettings().getString(getKey(), defValue);
  }

  @Deprecated
  public void putString(String value) {
    MyApplication.getInstance().getSettings().edit().putString(getKey(), value).apply();
  }

  @Deprecated
  public boolean getBoolean(boolean defValue) {
    return MyApplication.getInstance().getSettings().getBoolean(getKey(), defValue);
  }

  @Deprecated
  public void putBoolean(boolean value) {
    MyApplication.getInstance().getSettings().edit().putBoolean(getKey(), value).apply();
  }

  @Deprecated
  public int getInt(int defValue) {
    return MyApplication.getInstance().getSettings().getInt(getKey(), defValue);
  }

  @Deprecated
  public void putInt(int value) {
    MyApplication.getInstance().getSettings().edit().putInt(getKey(), value).apply();
  }

  @Deprecated
  public long getLong(long defValue) {
    return MyApplication.getInstance().getSettings().getLong(getKey(), defValue);
  }

  @Deprecated
  public void putLong(long value) {
    MyApplication.getInstance().getSettings().edit().putLong(getKey(), value).apply();
  }

  @Deprecated
  public void remove() {
    MyApplication.getInstance().getSettings().edit().remove(getKey()).apply();
  }

  @Deprecated
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
