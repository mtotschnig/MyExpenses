package org.totschnig.myexpenses.dialog

import android.content.DialogInterface
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringArrayResource
import com.afollestad.materialdialogs.utils.MDUtil.getStringArray
import eltos.simpledialogfragment.SimpleDialog
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity
import org.totschnig.myexpenses.compose.AppTheme
import org.totschnig.myexpenses.compose.IconSelector
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ICON
import org.totschnig.myexpenses.viewmodel.data.ExtraIcons
import org.totschnig.myexpenses.viewmodel.data.FontAwesomeIcons

class SimpleIconDialog : SimpleDialog<SimpleIconDialog>() {

    init {
        pos(null)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): AlertDialog {
        val ctx = requireContext()
        return (super.onCreateDialog(savedInstanceState) as AlertDialog).also { dialog ->
            dialog.setView(
                ComposeView(ctx).apply {
                    setContent {
                        AppTheme(activity = requireActivity() as ProtectedFragmentActivity) {
                            IconSelector(
                                categories = stringArrayResource(id = R.array.categories),
                                labelForCategory = {
                                    getString(
                                        resources.getIdentifier(
                                            "category_${it}_label",
                                            "string",
                                            requireContext().packageName
                                        )
                                    )
                                },
                                iconsForCategory = { category ->
                                    buildMap {
                                        ctx.getStringArray(
                                            resources.getIdentifier(
                                                "category_${category}_icons",
                                                "array",
                                                ctx.packageName
                                            )
                                        ).forEach {
                                            put(
                                                it,
                                                FontAwesomeIcons[it]
                                                    ?: throw IllegalArgumentException("no icon $it")
                                            )
                                        }
                                        resources.getIdentifier(
                                            "extra_${category}_icons", "array", ctx.packageName
                                        ).takeIf { it != 0 }?.let { resId ->
                                            ctx.getStringArray(resId).forEach {
                                                put(
                                                    it,
                                                    ExtraIcons[it]
                                                        ?: throw IllegalArgumentException("no icon $it")
                                                )
                                            }
                                        }
                                    }
                                },
                                iconsForSearch = { search ->
                                    (FontAwesomeIcons + ExtraIcons).filter {
                                        getString(it.value.label).contains(
                                            search,
                                            true
                                        )
                                    }
                                },
                                onIconSelected = {
                                    callResultListener(
                                        DialogInterface.BUTTON_POSITIVE,
                                        Bundle(1).apply {
                                            putSerializable(KEY_ICON, it.key to it.value)
                                        })
                                    dismiss()
                                }
                            )
                        }
                    }
                }
            )
        }
    }
}
