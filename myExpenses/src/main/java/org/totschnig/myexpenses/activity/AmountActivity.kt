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
package org.totschnig.myexpenses.activity

import android.content.Intent
import android.view.ViewGroup
import android.widget.TextView
import org.totschnig.myexpenses.databinding.TagRowBinding
import org.totschnig.myexpenses.fragment.TagList.Companion.KEY_TAG_LIST
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.filter.KEY_SELECTION
import org.totschnig.myexpenses.ui.AmountInput
import org.totschnig.myexpenses.ui.ExchangeRateEdit
import org.totschnig.myexpenses.viewmodel.TagBaseViewModel
import org.totschnig.myexpenses.viewmodel.TagHandlingViewModel
import org.totschnig.myexpenses.viewmodel.data.Tag

abstract class AmountActivity<T: TagHandlingViewModel> : EditActivity() {
    abstract val amountLabel: TextView
    abstract val amountRow: ViewGroup
    abstract val exchangeRateRow: ViewGroup
    abstract val amountInput: AmountInput
    abstract val exchangeRateEdit: ExchangeRateEdit
    lateinit var viewModel: T

    /**
     * @return true for income, false for expense
     */
    protected val isIncome: Boolean
        get() = amountInput.type

    protected open fun onTypeChanged(isChecked: Boolean) {
        setDirty()
        configureType()
    }

    protected open fun configureType() {}

    protected fun validateAmountInput(showToUser: Boolean) = amountInput.getAmount(showToUser)

    open fun setupListeners() {
        amountInput.addTextChangedListener(this)
        amountInput.setTypeChangedListener { isChecked: Boolean -> onTypeChanged(isChecked) }
    }

    fun TagRowBinding.bindListener() {
        TagSelection.setOnClickListener {
            val i = Intent(this@AmountActivity, ManageTags::class.java).apply {
                forwardDataEntryFromWidget(this)
                viewModel.tagsLiveData.value?.let { tagList ->
                    putExtra(KEY_SELECTION, tagList.map { it.id }.toLongArray())
                }
                putExtra(DatabaseConstants.KEY_COLOR, color)
            }
            startActivityForResult(i, SELECT_TAGS_REQUEST)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        data?.getLongArrayExtra(TagBaseViewModel.KEY_DELETED_IDS)?.let {
            handleDeletedTagIds(it)
        }
        when (requestCode) {
            SELECT_TAGS_REQUEST -> data?.also {
                if (resultCode == RESULT_OK) {
                    (data.getParcelableArrayListExtra<Tag>(KEY_TAG_LIST))?.let {
                        viewModel.updateTags(it, true)
                        setDirty()
                    }
                }
            }
        }
    }

    open fun handleDeletedTagIds(ids: LongArray) {
        viewModel.removeTags(*ids)
    }
}