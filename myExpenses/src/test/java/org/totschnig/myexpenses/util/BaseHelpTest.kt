package org.totschnig.myexpenses.util

import android.app.Application
import android.content.res.Resources
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertWithMessage

abstract class BaseHelpTest {
    protected val context: Application
        get() = ApplicationProvider.getApplicationContext()
    protected val resources: Resources
        get() = context.resources
    private val helper: HelpDialogHelper by lazy {
        HelpDialogHelper(context)
    }
    val packageName: String
        get() = context.packageName

    protected fun getArrayIdentifier(name: String) = getIdentifier(name, "array")

    protected fun getStringIdentifier(name: String) = getIdentifier(name, "string")

    private fun getIdentifier(name: String, defType: String) =
        resources.getIdentifier(name, defType, packageName)

    fun testMenuItems(
        context: String,
        variant: String?,
        menuItems: Array<String>,
        prefix: String
    ) {
        for (item in menuItems) {
            val description =
                "Context: $context, variant: $variant, prefix: $prefix, item: $item"
            assertWithMessage("title not found for $description")
                .that(helper.resolveTitle(item, prefix)).isNotEmpty()
            assertWithMessage("help text not found for $description")
                .that(
                    variant?.let {
                        resolveStringOrArray(prefix + "_" + context + "_" + variant + "_" + item + "_help_text")
                    }
                        ?: resolveStringOrArray(prefix + "_" + context + "_" + item + "_help_text")
                        ?: resolveStringOrArray(prefix + "_" + item + "_help_text")
                ).isNotNull()
        }
    }

    fun resolveStringOrArray(resString: String) =
        helper.resolveStringOrArray(resString, false)?.takeIf { it.isNotEmpty() }
}