package org.totschnig.myexpenses.fragment

import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.dialog.LicenceKeyDialogFragment


@RunWith(AndroidJUnit4::class)
class LicenceKeyDialogFragmentTest {
    @Test
    fun testLicenceKeyEntry() {
        launchFragmentInContainer<LicenceKeyDialogFragment>(
            themeResId = R.style.MyTheme
        )
    }
}