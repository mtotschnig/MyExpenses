/*   This file is part of My Expenses.
 *   My Expenses is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   My Expenses is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with My Expenses.  If not, see <http://www.gnu.org/licenses/>.
 */
package eltos.simpledialogfragment.form

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import arrow.core.Tuple4
import butterknife.BindView
import butterknife.ButterKnife
import eltos.simpledialogfragment.SimpleDialog.OnDialogResultListener
import eltos.simpledialogfragment.form.SimpleFormDialog.DialogActions
import eltos.simpledialogfragment.form.SimpleFormDialog.FocusActions
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.dialog.SimpleIconDialog
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.viewmodel.data.ExtraIcon
import org.totschnig.myexpenses.viewmodel.data.IIconInfo
import org.totschnig.myexpenses.viewmodel.data.IIconInfo.Companion.resolveIcon
import org.totschnig.myexpenses.viewmodel.data.IconInfo

internal class IconViewHolder(field: SelectIconField) :
    FormElementViewHolder<SelectIconField>(field), OnDialogResultListener {
    @BindView(R.id.label)
    lateinit var label: TextView

    @BindView(R.id.icon_extra)
    lateinit var iconExtra: ImageView

    @BindView(R.id.icon)
    lateinit var icon: TextView

    @BindView(R.id.select)
    lateinit var select: Button

    var selected: Pair<String, IIconInfo>? = null
    override fun getContentViewLayout(): Int {
        return R.layout.simpledialogfragment_form_item_icon
    }

    override fun setUpView(
        view: View, context: Context, savedInstanceState: Bundle?,
        actions: DialogActions
    ) {
        ButterKnife.bind(this, view)
        label.text = field.getText(context)
        val selected =
            if (savedInstanceState != null) savedInstanceState.getString(SAVED_ICON) else field.preset
        val onClickListener = View.OnClickListener { onClick(actions) }
        select.setOnClickListener(onClickListener)
        icon.setOnClickListener(onClickListener)
        iconExtra.setOnClickListener(onClickListener)
        updateIcon(selected?.let { resolveIcon(it) })
    }

    private fun updateIcon(result: IIconInfo?) {
        val (iconVisibility, iconExtraVisibility, selectVisibility, labelFor) =
            if (result == null) {
                Tuple4(View.GONE, View.GONE, View.VISIBLE, R.id.select)
            } else {
                if (result is ExtraIcon) {
                    val (drawable, label1) = result
                    iconExtra.contentDescription = iconExtra.context.getString(label1)
                    iconExtra.setImageResource(drawable)
                    Tuple4(View.GONE, View.VISIBLE, View.GONE, R.id.icon_extra)
                } else {
                    val (unicode, label1, isBrand) = result as IconInfo
                    icon.typeface = ResourcesCompat.getFont(
                        icon.context,
                        if (isBrand) R.font.fa_brands_400 else R.font.fa_solid_900
                    )
                    icon.text = unicode.toString()
                    icon.contentDescription = icon.context.getString(label1)
                    Tuple4(View.VISIBLE, View.GONE, View.GONE, R.id.icon)
                }
            }
        icon.visibility = iconVisibility
        iconExtra.visibility = iconExtraVisibility
        select.visibility = selectVisibility
        label.labelFor = labelFor
    }

    override fun saveState(outState: Bundle) {
        outState.putString(SAVED_ICON, selected?.first)
    }

    override fun putResults(results: Bundle, key: String) {
        results.putString(key, selected?.first)
    }

    override fun focus(actions: FocusActions): Boolean {
        return label.requestFocus()
    }

    override fun posButtonEnabled(context: Context): Boolean {
        return true
    }

    override fun validate(context: Context): Boolean {
        return true
    }

    override fun onResult(dialogTag: String, which: Int, extras: Bundle): Boolean {
        if (ICON_PICKER_DIALOG_TAG + field.resultKey == dialogTag) {
            if (which == OnDialogResultListener.BUTTON_POSITIVE) {
                selected =
                    extras.getSerializable(DatabaseConstants.KEY_ICON) as Pair<String, IconInfo>
                updateIcon(selected!!.second)
            } else if (which == OnDialogResultListener.BUTTON_NEGATIVE) {
                selected = null
                updateIcon(null)
            }
            return true
        }
        return false
    }

    private fun onClick(actions: DialogActions) {
        val iconDialog = SimpleIconDialog()
            .neut()
        if (selected != null) {
            iconDialog.neg(R.string.menu_remove)
        }
        actions.showDialog(
            iconDialog,
            ICON_PICKER_DIALOG_TAG + field.resultKey
        )
    }

    companion object {
        private const val SAVED_ICON = "color"
        private const val ICON_PICKER_DIALOG_TAG = "iconPickerDialogTag"
    }
}