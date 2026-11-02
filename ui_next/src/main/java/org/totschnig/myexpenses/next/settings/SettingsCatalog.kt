package org.totschnig.myexpenses.next.settings

import androidx.annotation.StringRes
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.next.NextFeatures
import org.totschnig.myexpenses.next.core.common.R as NextR
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.preference.PrefKey.*

/** Groups of the start page of the settings, in their order */
enum class SettingsGroup(val section: Section) {
    APPEARANCE(Section.SETTINGS),
    LISTS(Section.SETTINGS),
    ENTRY(Section.SETTINGS),
    DATA(Section.SETTINGS),
    SYNC(Section.SETTINGS),
    BACKUP(Section.SETTINGS),
    SECURITY(Section.SETTINGS),
    ADVANCED(Section.SETTINGS),
    HELP(Section.PROJECT),
    FEEDBACK(Section.PROJECT),
    PREMIUM(Section.PROJECT),
    SHARE(Section.PROJECT),
    ABOUT(Section.PROJECT);

    /** The settings themselves and below them the project: help, feedback, premium and about */
    enum class Section { SETTINGS, PROJECT }
}

/** Parts of a long group, each shown under its title on the page of the group */
enum class SettingsSection(val group: SettingsGroup) {
    ENTRY_QUICK(SettingsGroup.ENTRY),
    ENTRY_AUTO_FILL(SettingsGroup.ENTRY),
    ENTRY_DATE(SettingsGroup.ENTRY),
    ENTRY_ATTACHMENTS(SettingsGroup.ENTRY),
    ENTRY_RECEIPTS(SettingsGroup.ENTRY),
    DATA_MANAGE(SettingsGroup.DATA),
    DATA_CURRENCIES(SettingsGroup.DATA),
    DATA_PLANNER(SettingsGroup.DATA),
    BACKUP_BACKUP(SettingsGroup.BACKUP),
    BACKUP_AUTO(SettingsGroup.BACKUP),
    BACKUP_PURGE(SettingsGroup.BACKUP),
    BACKUP_IMPORT_EXPORT(SettingsGroup.BACKUP),
    BACKUP_PRINT(SettingsGroup.BACKUP),
    BACKUP_APP_DIR(SettingsGroup.BACKUP),
    SECURITY_LOCK(SettingsGroup.SECURITY),
    SECURITY_WIDGETS(SettingsGroup.SECURITY),
    SECURITY_PRIVACY(SettingsGroup.SECURITY),
    ADVANCED_WEBDAV(SettingsGroup.ADVANCED),
    ADVANCED_STORAGE(SettingsGroup.ADVANCED),
    ADVANCED_DEBUG(SettingsGroup.ADVANCED)
}

/**
 * A setting of ui_next, kept in the stores of an area instead of the preferences. Only those without
 * a place of their own: areas, salary, hidden contracts, features of tags and hidden saving tips are
 * set where they are shown. The areas bring theirs, see [AreaSettings].
 *
 * @param id stable name, also of its entry for the search and the back stack of the settings
 * @param summary of a switch, null for the others
 */
class InsightSetting(
    val id: String,
    val group: SettingsGroup,
    val kind: SettingKind,
    @param:StringRes val title: Int,
    @param:StringRes val summary: Int? = null,
) {
    override fun toString() = id

    companion object {
        /** Those of the areas, in the order of [NextFeatures.settings] */
        val all: List<InsightSetting> get() = NextFeatures.settings.flatMap { it.settings }
    }
}

sealed interface SettingKey {
    data class Pref(val prefKey: PrefKey) : SettingKey

    /** Keys of the old settings without a [PrefKey], they only open a screen */
    data class Plain(val key: String) : SettingKey

    data class Next(val setting: InsightSetting) : SettingKey
}

/** How a setting is changed, after the widget of the old settings */
enum class SettingKind { SWITCH, CHOICE, MULTI_CHOICE, TEXT, NUMBER, SLIDER, TIME, PASSWORD, ACTION, INFO }

/**
 * The screen of the old settings that holds a setting, opened with its [header] as initial screen
 * as long as the setting is not shown natively. [header] null is the main screen.
 */
enum class LegacyScreen(val xml: Int, val header: SettingKey?) {
    MAIN(R.xml.preference_headers, null),
    UI(R.xml.preferences_ui, SettingKey.Plain("ui")),
    DATA(R.xml.preferences_data, SettingKey.Plain("data")),
    IO(R.xml.preferences_io, SettingKey.Pref(CATEGORY_IO)),
    BACKUP_RESTORE(R.xml.preferences_backup_restore, SettingKey.Pref(CATEGORY_BACKUP_RESTORE)),
    OCR(R.xml.preferences_ocr, SettingKey.Plain("ocr")),
    SYNC(R.xml.preferences_sync, SettingKey.Plain("sync")),
    WEB_UI(R.xml.preferences_web_ui, SettingKey.Plain("web_ui")),
    ATTACH_PICTURE(R.xml.preferences_attach_picture, SettingKey.Plain("attach_picture")),
    PRINT(R.xml.preferences_print, SettingKey.Plain("print")),
    CONTRIB(0, SettingKey.Pref(CONTRIB_PURCHASE)),
    PROTECTION(R.xml.preferences_protection, SettingKey.Pref(CATEGORY_SECURITY)),
    FEEDBACK(R.xml.preferences_feedback, SettingKey.Plain("feedback")),
    ADVANCED(R.xml.preferences_advanced, SettingKey.Plain("advanced"))
}

/**
 * @param legacy null for settings that only ui_next has
 * @param section null in groups without sections
 */
data class SettingEntry(
    val key: SettingKey,
    val group: SettingsGroup,
    val kind: SettingKind,
    val legacy: LegacyScreen?,
    val section: SettingsSection? = null
)

/**
 * Every setting in exactly one [SettingsGroup], in the order of the group. Settings of the old
 * screens that are left out on purpose are in [omitted].
 */
object SettingsCatalog {

    val entries: List<SettingEntry> = buildList {
        group(SettingsGroup.APPEARANCE) {
            on(LegacyScreen.UI) {
                choice(UI_THEME)
                slider(UI_FONT_SIZE)
                choice(UI_LANGUAGE)
            }
            on(LegacyScreen.ADVANCED) {
                switch(CONTENT_BASED_COLORS)
                choice(CUSTOM_DECIMAL_FORMAT)
                choice(CUSTOM_DATE_FORMAT)
            }
            on(LegacyScreen.UI) {
                choice(UI_START_SCREEN)
                action(CUSTOMIZE_MENU_V2)
            }
        }
        group(SettingsGroup.LISTS) {
            on(LegacyScreen.UI) {
                switch(UI_ITEM_RENDERER_LEGACY)
                switch(UI_ITEM_RENDERER_ORIGINAL_AMOUNT)
                choice(TRANSACTION_AMOUNT_COLOR_SOURCE)
                choice(CRITERION_FUTURE)
                choice(GROUP_WEEK_STARTS)
                choice(GROUP_MONTH_STARTS)
            }
            on(null) {
                next(SettingsGroup.LISTS)
            }
        }
        group(SettingsGroup.ENTRY) {
            section(SettingsSection.ENTRY_QUICK) {
                on(LegacyScreen.UI) {
                    choice(DEFAULT_ACTION)
                    action(SHORTCUT_CREATE_TRANSACTION)
                    action(SHORTCUT_CREATE_TRANSFER)
                    action(SHORTCUT_CREATE_SPLIT)
                }
            }
            section(SettingsSection.ENTRY_AUTO_FILL) {
                on(LegacyScreen.UI) {
                    switch(AUTO_FILL_SWITCH)
                    switch(AUTO_FILL_AMOUNT)
                    switch(AUTO_FILL_CATEGORY)
                    switch(AUTO_FILL_COMMENT)
                    choice(AUTO_FILL_ACCOUNT)
                    switch(AUTO_FILL_METHOD)
                    switch(AUTO_FILL_DEBT)
                    switch(AUTO_FILL_FOCUS)
                }
                on(LegacyScreen.ADVANCED) {
                    action(DEFAULT_TRANSFER_CATEGORY)
                }
            }
            section(SettingsSection.ENTRY_DATE) {
                on(LegacyScreen.UI) {
                    switch(TRANSACTION_WITH_TIME)
                    switch(TRANSACTION_WITH_VALUE_DATE)
                    switch(CLONE_WITH_CURRENT_DATE)
                }
            }
            section(SettingsSection.ENTRY_ATTACHMENTS) {
                on(LegacyScreen.ADVANCED) {
                    choice(CAMERA_APP)
                    switch(CAMERA_CHOOSER)
                    add(SettingKind.MULTI_CHOICE, SettingKey.Pref(ATTACHMENT_MIME_TYPES))
                }
                on(LegacyScreen.ATTACH_PICTURE) {
                    switch(OPTIMIZE_PICTURE)
                    choice(OPTIMIZE_PICTURE_MAX_SIZE)
                    choice(OPTIMIZE_PICTURE_FORMAT)
                    slider(OPTIMIZE_PICTURE_QUALITY)
                    switch(COPY_ATTACHMENT)
                }
            }
            section(SettingsSection.ENTRY_RECEIPTS) {
                on(LegacyScreen.OCR) {
                    choice(OCR_ENGINE)
                    choice(TESSERACT_LANGUAGE)
                    choice(MLKIT_SCRIPT)
                }
            }
        }
        group(SettingsGroup.DATA) {
            section(SettingsSection.DATA_MANAGE) {
                on(LegacyScreen.DATA) {
                    action("manage_categories")
                    action("manage_tags")
                    action("manage_methods")
                    action(MANAGE_STALE_IMAGES)
                    switch(UNMAPPED_TRANSACTION_AS_TRANSFER)
                }
            }
            section(SettingsSection.DATA_CURRENCIES) {
                on(LegacyScreen.DATA) {
                    action(MANAGE_CURRENCIES)
                    choice(HOME_CURRENCY)
                    add(SettingKind.MULTI_CHOICE, SettingKey.Pref(EXCHANGE_RATE_PROVIDER))
                    text(OPEN_EXCHANGE_RATES_APP_ID)
                    text(COIN_API_API_KEY)
                    switch(AUTOMATIC_EXCHANGE_RATE_DOWNLOAD)
                    switch(EQUIVALENT_AMOUNTS_AUTOMATIC_UPDATE)
                    add(SettingKind.CHOICE, SettingKey.Plain("dynamic_exchange_rates_default"))
                }
            }
            section(SettingsSection.DATA_PLANNER) {
                on(LegacyScreen.ADVANCED) {
                    choice(PLANNER_CALENDAR_ID)
                    time(PLANNER_EXECUTION_TIME)
                    choice(PLANNER_MANUAL_TIME)
                    action(REMOVE_LOCAL_CALENDAR)
                }
            }
        }
        group(SettingsGroup.SYNC) {
            on(LegacyScreen.SYNC) {
                action(MANAGE_SYNC_BACKENDS)
                choice(SYNC_FREQUCENCY)
                switch(SYNC_WIFI_ONLY)
                switch(SYNC_CHANGES_IMMEDIATELY)
                switch(SYNC_NOTIFICATION)
                switch(SYNC_NOW_ALL)
            }
            on(LegacyScreen.MAIN) {
                action(BANKING_FINTS)
            }
            on(LegacyScreen.WEB_UI) {
                action(UI_WEB)
                password(WEBUI_PASSWORD)
                switch(WEBUI_HTTPS)
            }
        }
        group(SettingsGroup.BACKUP) {
            section(SettingsSection.BACKUP_BACKUP) {
                on(LegacyScreen.BACKUP_RESTORE) {
                    action(BACKUP)
                    action(RESTORE)
                    text(BACKUP_FILE_PREFIX)
                    password(EXPORT_PASSWORD)
                }
            }
            section(SettingsSection.BACKUP_AUTO) {
                on(LegacyScreen.BACKUP_RESTORE) {
                    switch(AUTO_BACKUP)
                    time(AUTO_BACKUP_TIME)
                    action(AUTO_BACKUP_CLOUD)
                    action(AUTO_BACKUP_CLOUD_SETUP)
                    info(AUTO_BACKUP_UNENCRYPTED_INFO)
                }
            }
            section(SettingsSection.BACKUP_PURGE) {
                on(LegacyScreen.BACKUP_RESTORE) {
                    switch(PURGE_BACKUP)
                    slider(PURGE_BACKUP_KEEP)
                    switch(PURGE_BACKUP_REQUIRE_CONFIRMATION)
                    switch(PERFORM_SHARE)
                    text(SHARE_TARGET)
                }
            }
            section(SettingsSection.BACKUP_IMPORT_EXPORT) {
                on(LegacyScreen.IO) {
                    action(IMPORT_CSV)
                    action(IMPORT_QIF)
                    action("import_from_grisbi")
                    switch(CSV_EXPORT_SPLIT_CATEGORIES)
                    text(CSV_EXPORT_CATEGORY_SEPARATOR)
                    switch(CSV_EXPORT_SPLIT_AMOUNT)
                    switch(CSV_EXPORT_SPLIT_DATE_TIME)
                    switch(CSV_EXPORT_ORIGINAL_EQUIVALENT_AMOUNTS)
                }
            }
            section(SettingsSection.BACKUP_PRINT) {
                on(LegacyScreen.PRINT) {
                    choice(PRINT_PAPER_FORMAT)
                    choice(PRINT_PAPER_ORIENTATION)
                    slider(PRINT_FONT_SIZE)
                    text(PRINT_HEADER_LEFT)
                    text(PRINT_HEADER_CENTER)
                    text(PRINT_HEADER_RIGHT)
                    text(PRINT_FOOTER_LEFT)
                    text(PRINT_FOOTER_CENTER)
                    text(PRINT_FOOTER_RIGHT)
                    slider(PRINT_MARGIN_TOP)
                    slider(PRINT_MARGIN_RIGHT)
                    slider(PRINT_MARGIN_BOTTOM)
                    slider(PRINT_MARGIN_LEFT)
                }
            }
            section(SettingsSection.BACKUP_APP_DIR) {
                on(LegacyScreen.MAIN) {
                    action(APP_DIR)
                    add(SettingKind.MULTI_CHOICE, SettingKey.Pref(MANAGE_APP_DIR_FILES))
                }
            }
        }
        group(SettingsGroup.SECURITY) {
            section(SettingsSection.SECURITY_LOCK) {
                on(LegacyScreen.PROTECTION) {
                    switch(PROTECTION_DEVICE_LOCK_SCREEN)
                    password(PROTECTION_LEGACY)
                    text(SECURITY_QUESTION)
                    choice(PROTECTION_DELAY_SECONDS)
                    switch(PROTECTION_ALLOW_SCREENSHOT)
                }
            }
            section(SettingsSection.SECURITY_WIDGETS) {
                on(LegacyScreen.PROTECTION) {
                    switch(PROTECTION_ENABLE_ACCOUNT_WIDGET)
                    switch(PROTECTION_ENABLE_TEMPLATE_WIDGET)
                    switch(PROTECTION_ENABLE_BUDGET_WIDGET)
                    switch(PROTECTION_ENABLE_DATA_ENTRY_FROM_WIDGET)
                    switch(UI_HOME_SCREEN_SHORTCUTS_SHOW_NEW_BALANCE)
                }
            }
            section(SettingsSection.SECURITY_PRIVACY) {
                on(LegacyScreen.PROTECTION) {
                    info(ENCRYPT_DATABASE_INFO)
                    switch(TRACKING)
                    switch(CRASHREPORT_ENABLED)
                    text(CRASHREPORT_USEREMAIL)
                    action(PERSONALIZED_AD_CONSENT)
                    action(NO_ADS)
                }
            }
        }
        group(SettingsGroup.ADVANCED) {
            section(SettingsSection.ADVANCED_WEBDAV) {
                on(LegacyScreen.ADVANCED) {
                    slider(WEBDAV_TIMEOUT)
                    switch(WEBDAV_ALLOW_UNVERIFIED_HOST)
                }
            }
            section(SettingsSection.ADVANCED_STORAGE) {
                on(LegacyScreen.ADVANCED) {
                    add(SettingKind.MULTI_CHOICE, SettingKey.Pref(FEATURE_UNINSTALL_FEATURES))
                    add(SettingKind.MULTI_CHOICE, SettingKey.Pref(FEATURE_UNINSTALL_LANGUAGES))
                }
            }
            section(SettingsSection.ADVANCED_DEBUG) {
                on(LegacyScreen.ADVANCED) {
                    switch(DEBUG_LOGGING)
                    action(DEBUG_LOG_SHARE)
                    switch(DEBUG_ADS)
                    switch(DB_SAFE_MODE)
                    action(DEBUG_REPAIR_987)
                }
            }
        }
        group(SettingsGroup.HELP) {
            on(LegacyScreen.FEEDBACK) {
                action("visit_website")
                action(NEWS)
            }
        }
        group(SettingsGroup.FEEDBACK) {
            on(LegacyScreen.FEEDBACK) {
                action(SEND_FEEDBACK)
                action(RATE)
                action(ROADMAP_VOTE)
            }
        }
        group(SettingsGroup.PREMIUM) {
            on(LegacyScreen.CONTRIB) {
                action(CONTRIB_PURCHASE)
            }
        }
        group(SettingsGroup.SHARE) {
            on(LegacyScreen.FEEDBACK) {
                action(TELL_A_FRIEND)
            }
        }
        group(SettingsGroup.ABOUT) {
            on(LegacyScreen.MAIN) {
                action(MORE_INFO_DIALOG)
            }
        }
    }

    /** Settings of the old screens that get no entry of their own */
    val omitted: Map<PrefKey, String> = mapOf(
        HELP to "help of a single old screen, the groups get their own help",
        FAB_STYLE to "the plus button of the new account page has one style only, with its label",
        UI_MAIN_SCREEN_VERSION to "hidden since the old main screen V1 is gone",
        ACCOUNT_LIST_DISPLAY_CONFIGURATION to "account list of the old main screen V1",
        UI_ITEM_RENDERER_CATEGORY_ICON to "only the old transaction list, the new one has its own icons",
        TAG_STYLE to "only the old transaction list, the new one shows tags as chips of its own",
        CUSTOM_DATE_FORMAT_NEW_TRANSACTION_LIST to "only the old transaction list",
        SCROLL_TO_CURRENT_DATE to "only the old transaction list",
        GROUP_HEADER to "sums in the headers of the old transaction list",
        OCR_TOTAL_INDICATORS to "only the old scan, receipts have their own parser",
        OCR_DATE_FORMATS to "only the old scan, receipts have their own parser",
        OCR_TIME_FORMATS to "only the old scan, receipts have their own parser"
    )

    fun of(group: SettingsGroup) = entries.filter { it.group == group }

    fun of(section: SettingsSection) = entries.filter { it.section == section }
}

private class ScreenScope(
    private val group: SettingsGroup,
    private val screen: LegacyScreen?,
    private val section: SettingsSection?,
    private val entries: MutableList<SettingEntry>
) {
    fun add(kind: SettingKind, key: SettingKey) {
        entries += SettingEntry(key, group, kind, screen, section)
    }

    /** The settings only ui_next has that belong to [group] */
    fun next(group: SettingsGroup) = InsightSetting.all.filter { it.group == group }
        .forEach { add(it.kind, SettingKey.Next(it)) }

    fun switch(key: PrefKey) = add(SettingKind.SWITCH, SettingKey.Pref(key))
    fun choice(key: PrefKey) = add(SettingKind.CHOICE, SettingKey.Pref(key))
    fun text(key: PrefKey) = add(SettingKind.TEXT, SettingKey.Pref(key))
    fun number(key: PrefKey) = add(SettingKind.NUMBER, SettingKey.Pref(key))
    fun slider(key: PrefKey) = add(SettingKind.SLIDER, SettingKey.Pref(key))
    fun time(key: PrefKey) = add(SettingKind.TIME, SettingKey.Pref(key))
    fun password(key: PrefKey) = add(SettingKind.PASSWORD, SettingKey.Pref(key))
    fun action(key: PrefKey) = add(SettingKind.ACTION, SettingKey.Pref(key))
    fun action(key: String) = add(SettingKind.ACTION, SettingKey.Plain(key))
    fun info(key: PrefKey) = add(SettingKind.INFO, SettingKey.Pref(key))
}

private class GroupScope(private val group: SettingsGroup, private val entries: MutableList<SettingEntry>) {
    private var section: SettingsSection? = null

    fun section(section: SettingsSection, block: GroupScope.() -> Unit) {
        this.section = section
        block()
        this.section = null
    }

    fun on(screen: LegacyScreen?, block: ScreenScope.() -> Unit) {
        ScreenScope(group, screen, section, entries).block()
    }
}

private fun MutableList<SettingEntry>.group(group: SettingsGroup, block: GroupScope.() -> Unit) {
    GroupScope(group, this).block()
}
