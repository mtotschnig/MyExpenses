package org.totschnig.myexpenses.dialog

import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringArrayResource
import com.afollestad.materialdialogs.utils.MDUtil.getStringArray
import eltos.simpledialogfragment.SimpleDialog
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity
import org.totschnig.myexpenses.compose.AppTheme
import org.totschnig.myexpenses.compose.IconInfo
import org.totschnig.myexpenses.compose.IconSelector

class SimpleIconDialog : SimpleDialog<SimpleIconDialog>() {

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
                                    ctx.getString(
                                        ctx.resources.getIdentifier(
                                            "category_${it}_label",
                                            "string",
                                            ctx.packageName
                                        )
                                    )
                                },
                                iconsForCategory = { category ->
                                    ctx.getStringArray(
                                        ctx.resources.getIdentifier(
                                            "category_${category}_icons", "array", ctx.packageName
                                        )
                                    ).map { icon ->
                                        IconInfo(
                                            unicode = ctx.getString(
                                                ctx.resources.getIdentifier(
                                                    "fa_${icon}_unicode",
                                                    "string",
                                                    ctx.packageName
                                                )
                                            ),
                                            label = ctx.getString(
                                                ctx.resources.getIdentifier(
                                                    "fa_${icon}_label",
                                                    "string",
                                                    ctx.packageName
                                                )
                                            ),
                                            isBrand = ctx.resources.getInteger(
                                                ctx.resources.getIdentifier(
                                                    "fa_${icon}_style",
                                                    "integer",
                                                    ctx.packageName
                                                )
                                            ) == 1
                                        )
                                    }.toTypedArray()
                                })
                        }
                    }
                }
            )
        }
    }
}
