package org.totschnig.myexpenses.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AdditionalHelpTest: BaseHelpTest() {

    @Test
    fun testHelpWebUi() {
        assertThat(resolveStringOrArray("help_WebUI_info")).isNotNull()
    }

    @Test
    fun testHelpSetupSync() {
        assertThat(resolveStringOrArray("help_SetupSync_info")).isNotNull()
    }

    @Test
    fun testHelpNavigationDrawer() {
        val context = "NavigationDrawer"
        assertThat(resolveStringOrArray("help_${context}_title")).isNotNull()
        assertThat(resolveStringOrArray("help_${context}_info")).isNotNull()
        testMenuItems(context, null, resources.getStringArray(getArrayIdentifier(context + "_cabitems")), "cab")
        testMenuItems(context, null, resources.getStringArray(getArrayIdentifier(context + "_menuitems")), "menu")
    }
}