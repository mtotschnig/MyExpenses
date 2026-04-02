package org.totschnig.myexpenses.dialog

import android.content.Context
import android.os.Parcelable
import android.view.View.LAYOUT_DIRECTION_RTL
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.annotation.MenuRes
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tune
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.datastore.preferences.core.stringPreferencesKey
import com.livefront.sealedenum.GenSealedEnum
import kotlinx.parcelize.Parcelize
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.BaseActivity
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.util.TextUtils

@Parcelize
@Stable
sealed class MenuItem(
    @param:IdRes val id: Int,
    @param:StringRes private val labelRes: Int,
    @param:DrawableRes val icon: Int,
    @param:MenuRes val subMenu: Int? = null,
    val isCheckable: Boolean = false,
    val isEnabledByDefault: Boolean = true,
) : Parcelable {

    open fun getLabel(context: Context) = context.getString(labelRes)

    data object Search : MenuItem(
        R.id.SEARCH_COMMAND,
        R.string.menu_search,
        R.drawable.ic_menu_search,
        isCheckable = true
    )

    data object Templates : MenuItem(
        R.id.MANAGE_TEMPLATES_COMMAND,
        R.string.menu_templates,
        R.drawable.ic_menu_template
    )

    data object Budget : MenuItem(
        R.id.BUDGET_COMMAND,
        R.string.menu_budget,
        R.drawable.ic_budget
    )

    data object Distribution : MenuItem(
        R.id.DISTRIBUTION_COMMAND,
        R.string.menu_distribution,
        R.drawable.ic_menu_chart
    )

    data object History : MenuItem(
        R.id.HISTORY_COMMAND,
        R.string.menu_history,
        R.drawable.ic_history
    )

    data object Parties : MenuItem(
        R.id.MANAGE_PARTIES_COMMAND,
        0,
        R.drawable.ic_group
    ) {
        override fun getLabel(context: Context) = TextUtils.concatResStrings(
            context, " / ", R.string.pref_manage_parties_title, R.string.debts
        )
    }

    data object ScanMode : MenuItem(
        R.id.SCAN_MODE_COMMAND,
        R.string.menu_scan_mode,
        R.drawable.ic_scan,
        isCheckable = true
    )

    data object Reset : MenuItem(
        R.id.RESET_COMMAND,
        R.string.menu_reset,
        R.drawable.ic_menu_download
    )

    data object Sync : MenuItem(
        R.id.SYNC_COMMAND,
        R.string.menu_sync_now,
        R.drawable.ic_sync
    )

    data object FinTsSync : MenuItem(
        R.id.FINTS_SYNC_COMMAND,
        0,
        R.drawable.ic_bank
    ) {
        override fun getLabel(context: Context) =
            (context as? BaseActivity)?.bankingFeature?.syncMenuTitle(context) ?: "FinTS"
    }

    data object ShowStatusHandle : MenuItem(
        R.id.SHOW_STATUS_HANDLE_COMMAND,
        R.string.status,
        R.drawable.ic_square,
        isCheckable = true
    )

    data object Balance : MenuItem(
        R.id.BALANCE_COMMAND,
        R.string.menu_balance,
        R.drawable.ic_action_balance
    )

    data object Sort : MenuItem(
        R.id.SORT_MENU,
        R.string.display_options_sort_list_by,
        R.drawable.ic_menu_sort,
        R.menu.main_sort
    )

    data object Grouping : MenuItem(
        R.id.GROUPING_COMMAND,
        R.string.menu_grouping,
        R.drawable.ic_action_group,
        R.menu.grouping
    )

    data object Print : MenuItem(
        R.id.PRINT_COMMAND,
        R.string.menu_print,
        R.drawable.ic_menu_print
    )

    data object Archive : MenuItem(
        R.id.ARCHIVE_COMMAND,
        R.string.action_archive,
        R.drawable.ic_archive,
    )

    data object Settings : MenuItem(
        R.id.SETTINGS_COMMAND,
        R.string.settings_label,
        R.drawable.ic_settings
    )

    data object Help : MenuItem(
        R.id.HELP_COMMAND,
        R.string.menu_help,
        R.drawable.ic_menu_help
    )

    data object Backup : MenuItem(
        R.id.BACKUP_COMMAND,
        R.string.menu_backup,
        R.drawable.ic_menu_save,
        isEnabledByDefault = false
    )

    data object WebUI : MenuItem(
        R.id.WEB_UI_COMMAND,
        R.string.title_webui,
        R.drawable.ic_computer,
        isEnabledByDefault = false,
        isCheckable = true
    )

    data object Restore : MenuItem(
        R.id.RESTORE_COMMAND,
        R.string.pref_restore_title,
        R.drawable.settings_backup_restore,
        isEnabledByDefault = false
    )

    //V2
    data object Tune : MenuItem(
        R.id.TUNE_COMMAND,
        R.string.options,
        0
    )

    open val painter: Painter
        @Composable get() = if (icon != 0)
            painterResource(id = icon)
        else
            rememberVectorPainter(
                when (this) {
                    Tune -> Icons.Filled.Tune
                    else -> throw IllegalArgumentException()
                }
            )

    enum class MenuContext {
        V1, V2Navigation, V2Transactions;

        val prefKey: PrefKey
            get() = when (this) {
                V1 -> PrefKey.CUSTOMIZE_MAIN_MENU
                V2Navigation -> PrefKey.CUSTOMIZE_MENU_V2_MAIN
                V2Transactions -> PrefKey.CUSTOMIZE_MENU_V2_TRANSACTIONS
            }

        fun title(context: Context) = when (this) {
            V1 -> "V1"
            V2Navigation -> context.getString(R.string.main_navigation)
            V2Transactions -> context.getString(R.string.import_select_transactions)
        }
    }

    enum class NavigationMode {
        DEFAULT,   // = Fixed_bottom on phone, Always rail on tablet
        FIXED_BOTTOM,     // Traditional
        TOGGLEABLE_RAIL,   // Data-first / Maximize vertical space
        ALWAYS_RAIL,      // Consistent sidebar
        ADAPTIVE;    // Rail in Landscape, BottomBar in Portrait

        val label: String
            @Composable get() = when (this) {
                DEFAULT -> ""
                FIXED_BOTTOM -> stringResource(R.string.bottom)
                TOGGLEABLE_RAIL, ALWAYS_RAIL -> railPosition()
                ADAPTIVE -> railPosition() +
                        " (" + stringResource(R.string.landscape) + "), " +
                        stringResource(R.string.bottom) +
                        " (" + stringResource(R.string.portrait) + ")"
            }

        @Composable
        private fun railPosition() =
            stringResource(
                if (LocalConfiguration.current.layoutDirection == LAYOUT_DIRECTION_RTL)
                    R.string.right else R.string.left
            )

        fun validate(isTablet: Boolean) = if (isTablet) {
            if (forTablet.contains(this)) this else ALWAYS_RAIL
        } else {
            if (forPhone.contains(this)) this else FIXED_BOTTOM
        }

        companion object {
            val PREFERENCE_KEY = stringPreferencesKey("navigationMode")
            val forPhone = listOf(FIXED_BOTTOM, TOGGLEABLE_RAIL)
            val forTablet = listOf(ALWAYS_RAIL, ADAPTIVE)
        }
    }

    @GenSealedEnum
    companion object {
        fun all(menuContext: MenuContext) = when (menuContext) {
            MenuContext.V1 -> listOf(
                Search,
                Templates,
                Budget,
                Distribution,
                History,
                Parties,
                ScanMode,
                Reset,
                Sync,
                FinTsSync,
                ShowStatusHandle,
                Balance,
                Sort,
                Grouping,
                Print,
                Archive,
                Settings,
                Help,
                Backup,
                WebUI,
                Restore
            )

            MenuContext.V2Navigation -> listOf(
                Templates,
                Budget,
                Parties,
                Settings,
                Backup,
                WebUI,
                Restore
            )

            MenuContext.V2Transactions -> listOf(
                Search,
                Distribution,
                History,
                Sync,
                FinTsSync,
                Balance,
                Reset,
                Print,
                Tune,
                Archive,
                ShowStatusHandle,
            )
        }

        fun getDefaultConfiguration(menuContext: MenuContext): List<MenuItem> =
            all(menuContext).filter { it.isEnabledByDefault }
    }
}