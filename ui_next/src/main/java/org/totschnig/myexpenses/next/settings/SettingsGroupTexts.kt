package org.totschnig.myexpenses.next.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.ui.graphics.vector.ImageVector
import org.totschnig.myexpenses.next.core.common.R

val SettingsGroup.title: Int
    get() = when (this) {
        SettingsGroup.APPEARANCE -> R.string.next_settings_appearance
        SettingsGroup.LISTS -> R.string.next_settings_lists
        SettingsGroup.ENTRY -> R.string.next_settings_entry
        SettingsGroup.DATA -> R.string.next_settings_data
        SettingsGroup.INSIGHTS -> R.string.next_settings_insights
        SettingsGroup.SYNC -> R.string.next_settings_sync
        SettingsGroup.BACKUP -> R.string.next_settings_backup
        SettingsGroup.SECURITY -> R.string.next_settings_security
        SettingsGroup.ADVANCED -> R.string.next_settings_advanced
        SettingsGroup.HELP -> R.string.next_settings_help
        SettingsGroup.FEEDBACK -> R.string.next_settings_feedback
        SettingsGroup.PREMIUM -> R.string.next_settings_premium
        SettingsGroup.SHARE -> R.string.next_settings_share
        SettingsGroup.ABOUT -> R.string.next_settings_about
    }

/** A few of the settings in the group, below its title on the start page, null for none */
val SettingsGroup.summary: Int?
    get() = when (this) {
        SettingsGroup.APPEARANCE -> R.string.next_settings_appearance_summary
        SettingsGroup.LISTS -> R.string.next_settings_lists_summary
        SettingsGroup.ENTRY -> R.string.next_settings_entry_summary
        SettingsGroup.DATA -> R.string.next_settings_data_summary
        SettingsGroup.INSIGHTS -> R.string.next_settings_insights_summary
        SettingsGroup.SYNC -> R.string.next_settings_sync_summary
        SettingsGroup.BACKUP -> R.string.next_settings_backup_summary
        SettingsGroup.SECURITY -> R.string.next_settings_security_summary
        SettingsGroup.ADVANCED -> R.string.next_settings_advanced_summary
        else -> null
    }

val SettingsGroup.icon: ImageVector
    get() = when (this) {
        SettingsGroup.APPEARANCE -> Icons.Outlined.Palette
        SettingsGroup.LISTS -> Icons.AutoMirrored.Outlined.ViewList
        SettingsGroup.ENTRY -> Icons.Outlined.EditNote
        SettingsGroup.DATA -> Icons.Outlined.Folder
        SettingsGroup.INSIGHTS -> Icons.Outlined.Insights
        SettingsGroup.SYNC -> Icons.Outlined.Sync
        SettingsGroup.BACKUP -> Icons.Outlined.Backup
        SettingsGroup.SECURITY -> Icons.Outlined.Lock
        SettingsGroup.ADVANCED -> Icons.Outlined.Tune
        SettingsGroup.HELP -> Icons.AutoMirrored.Outlined.HelpOutline
        SettingsGroup.FEEDBACK -> Icons.Outlined.Forum
        SettingsGroup.PREMIUM -> Icons.Outlined.FavoriteBorder
        SettingsGroup.SHARE -> Icons.Outlined.Share
        SettingsGroup.ABOUT -> Icons.Outlined.Info
    }

val SettingsSection.title: Int
    get() = when (this) {
        SettingsSection.ENTRY_QUICK -> R.string.next_settings_section_entry_quick
        SettingsSection.ENTRY_AUTO_FILL -> R.string.next_settings_section_entry_auto_fill
        SettingsSection.ENTRY_DATE -> R.string.next_settings_section_entry_date
        SettingsSection.ENTRY_ATTACHMENTS -> R.string.next_settings_section_entry_attachments
        SettingsSection.ENTRY_RECEIPTS -> R.string.next_settings_section_entry_receipts
        SettingsSection.DATA_MANAGE -> R.string.next_settings_section_data_manage
        SettingsSection.DATA_CURRENCIES -> R.string.next_settings_section_data_currencies
        SettingsSection.DATA_PLANNER -> R.string.next_settings_section_data_planner
        SettingsSection.BACKUP_BACKUP -> R.string.next_settings_section_backup_backup
        SettingsSection.BACKUP_AUTO -> R.string.next_settings_section_backup_auto
        SettingsSection.BACKUP_PURGE -> R.string.next_settings_section_backup_purge
        SettingsSection.BACKUP_IMPORT_EXPORT -> R.string.next_settings_section_backup_import_export
        SettingsSection.BACKUP_PRINT -> R.string.next_settings_section_backup_print
        SettingsSection.BACKUP_APP_DIR -> R.string.next_settings_section_backup_app_dir
        SettingsSection.SECURITY_LOCK -> R.string.next_settings_section_security_lock
        SettingsSection.SECURITY_WIDGETS -> R.string.next_settings_section_security_widgets
        SettingsSection.SECURITY_PRIVACY -> R.string.next_settings_section_security_privacy
        SettingsSection.ADVANCED_WEBDAV -> R.string.next_settings_section_advanced_webdav
        SettingsSection.ADVANCED_STORAGE -> R.string.next_settings_section_advanced_storage
        SettingsSection.ADVANCED_DEBUG -> R.string.next_settings_section_advanced_debug
    }
