package org.totschnig.myexpenses.next

import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import org.totschnig.myexpenses.dialog.MenuItem

/**
 * How the navigation is shown, from the mode the user chose
 *
 * @param calculated what the navigation suite suggests for the window
 * @param isNavigationVisible whether the toggleable rail is shown
 */
internal fun navigationSuiteType(
    mode: MenuItem.NavigationMode,
    calculated: NavigationSuiteType,
    isNavigationVisible: Boolean,
    isLandscape: Boolean,
): NavigationSuiteType = when (mode) {
    MenuItem.NavigationMode.DEFAULT -> calculated // should not happen
    MenuItem.NavigationMode.FIXED_BOTTOM -> calculated // which is either ShortNavigationBarCompact or ShortNavigationBarMedium for phone
    MenuItem.NavigationMode.TOGGLEABLE_RAIL -> if (isNavigationVisible) NavigationSuiteType.NavigationRail else NavigationSuiteType.None
    MenuItem.NavigationMode.ALWAYS_RAIL -> NavigationSuiteType.WideNavigationRailCollapsed
    MenuItem.NavigationMode.ADAPTIVE ->
        if (isLandscape) NavigationSuiteType.WideNavigationRailCollapsed else NavigationSuiteType.ShortNavigationBarMedium
}

internal fun NavigationSuiteType.isBar(): Boolean =
    this == NavigationSuiteType.NavigationBar ||
            this == NavigationSuiteType.ShortNavigationBarMedium ||
            this == NavigationSuiteType.ShortNavigationBarCompact

/**
 * How many actions of the account page are shown in its top bar, the others go to its overflow
 *
 * @param isExpanded / [isMedium] the width of the window
 */
internal fun visibleActionItems(
    isExpanded: Boolean,
    isMedium: Boolean,
    is2Pane: Boolean,
    fontScale: Float,
) = when {
    isExpanded -> if (is2Pane) 4 else 6
    isMedium && !is2Pane -> 4
    fontScale > 1.5f -> 0
    fontScale > 1.1f -> 1
    else -> 2
}
