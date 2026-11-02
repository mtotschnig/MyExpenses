package org.totschnig.myexpenses.next.settings

import org.totschnig.myexpenses.next.core.common.R
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.preference.PrefKey.*

/**
 * Texts of the new settings for those whose texts on the old screens are technical or missing,
 * in everyday words
 */
object SettingTexts {

    /** Instead of the titles of the old screens */
    val titles: Map<PrefKey, Int> = mapOf(
        HOME_CURRENCY to R.string.next_settings_title_home_currency,
        MANAGE_SYNC_BACKENDS to R.string.next_settings_title_sync_backends,
        DEFAULT_ACTION to R.string.next_settings_title_default_action,
        OCR_ENGINE to R.string.next_settings_title_ocr_engine,
        TESSERACT_LANGUAGE to R.string.next_settings_title_tesseract_language,
        MLKIT_SCRIPT to R.string.next_settings_title_mlkit_script,
        EXPORT_PASSWORD to R.string.next_settings_title_export_password,
        BACKUP_FILE_PREFIX to R.string.next_settings_title_backup_file_prefix,
        SHARE_TARGET to R.string.next_settings_title_share_target,
        FEATURE_UNINSTALL_FEATURES to R.string.next_settings_title_uninstall_features,
        FEATURE_UNINSTALL_LANGUAGES to R.string.next_settings_title_uninstall_languages,
        PLANNER_EXECUTION_TIME to R.string.next_settings_title_planner_execution_time,
        PLANNER_MANUAL_TIME to R.string.next_settings_title_planner_manual_time,
        CRITERION_FUTURE to R.string.next_settings_title_criterion_future,
        EQUIVALENT_AMOUNTS_AUTOMATIC_UPDATE to R.string.next_settings_title_equivalent_amounts_update,
        UI_ITEM_RENDERER_ORIGINAL_AMOUNT to R.string.next_settings_title_original_amount,
        CAMERA_CHOOSER to R.string.next_settings_title_camera_chooser,
        ATTACHMENT_MIME_TYPES to R.string.next_settings_title_mime_types,
        OPTIMIZE_PICTURE_MAX_SIZE to R.string.next_settings_title_picture_max_size,
        SYNC_FREQUCENCY to R.string.next_settings_title_sync_frequency,
        PROTECTION_DELAY_SECONDS to R.string.next_settings_title_protection_delay,
        WEBDAV_TIMEOUT to R.string.next_settings_title_webdav_timeout,
        WEBDAV_ALLOW_UNVERIFIED_HOST to R.string.next_settings_title_webdav_unverified_host,
        DEBUG_LOGGING to R.string.next_settings_title_debug_logging,
        DEBUG_ADS to R.string.next_settings_title_debug_ads,
        DB_SAFE_MODE to R.string.next_settings_title_db_safe_mode,
        DEBUG_REPAIR_987 to R.string.next_settings_title_repair_987,
    )

    /**
     * Below the title of a switch or an action, or above the options of a list and in the dialog
     * of a text
     */
    val explanations: Map<PrefKey, Int> = mapOf(
        HOME_CURRENCY to R.string.next_settings_explain_home_currency,
        CRITERION_FUTURE to R.string.next_settings_explain_criterion_future,
        PLANNER_MANUAL_TIME to R.string.next_settings_explain_planner_manual_time,
        EQUIVALENT_AMOUNTS_AUTOMATIC_UPDATE to R.string.next_settings_explain_equivalent_amounts_update,
        UI_ITEM_RENDERER_ORIGINAL_AMOUNT to R.string.next_settings_explain_original_amount,
        OCR_ENGINE to R.string.next_settings_explain_ocr_engine,
        MLKIT_SCRIPT to R.string.next_settings_explain_mlkit_script,
        BACKUP_FILE_PREFIX to R.string.next_settings_explain_backup_file_prefix,
        PROTECTION_ALLOW_SCREENSHOT to R.string.next_settings_explain_allow_screenshot,
        PROTECTION_ENABLE_ACCOUNT_WIDGET to R.string.next_settings_explain_widget_protection,
        PROTECTION_ENABLE_TEMPLATE_WIDGET to R.string.next_settings_explain_widget_protection,
        PROTECTION_ENABLE_BUDGET_WIDGET to R.string.next_settings_explain_widget_protection,
        MANAGE_SYNC_BACKENDS to R.string.next_settings_explain_sync_backends,
        DEFAULT_TRANSFER_CATEGORY to R.string.next_settings_explain_default_transfer_category,
        FEATURE_UNINSTALL_FEATURES to R.string.next_settings_explain_uninstall,
        FEATURE_UNINSTALL_LANGUAGES to R.string.next_settings_explain_uninstall,
        CAMERA_CHOOSER to R.string.next_settings_explain_camera_chooser,
        OPTIMIZE_PICTURE_MAX_SIZE to R.string.next_settings_explain_picture_max_size,
        PROTECTION_DELAY_SECONDS to R.string.next_settings_explain_protection_delay,
        WEBDAV_ALLOW_UNVERIFIED_HOST to R.string.next_settings_explain_webdav_unverified_host,
        DEBUG_LOGGING to R.string.next_settings_explain_debug_logging,
        DEBUG_ADS to R.string.next_settings_explain_debug_ads,
        DB_SAFE_MODE to R.string.next_settings_explain_db_safe_mode,
        DEBUG_REPAIR_987 to R.string.next_settings_explain_repair_987,
        REMOVE_LOCAL_CALENDAR to R.string.next_settings_explain_remove_local_calendar,
    )
}
