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

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.CompoundButton
import androidx.activity.viewModels
import androidx.appcompat.widget.AppCompatCheckBox
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.evernote.android.state.State
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.databinding.OneMethodBinding
import org.totschnig.myexpenses.dialog.IconSelectorDialogFragment
import org.totschnig.myexpenses.dialog.OnIconSelectedListener
import org.totschnig.myexpenses.injector
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.model.PreDefinedPaymentMethod
import org.totschnig.myexpenses.model2.PaymentMethod
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.ui.SpinnerHelper
import org.totschnig.myexpenses.viewmodel.MethodViewModel
import org.totschnig.myexpenses.viewmodel.data.IIconInfo

private const val KEY_SELECTED_TYPES = "selectedTypes"

class MethodEdit : EditActivity(), CompoundButton.OnCheckedChangeListener, OnIconSelectedListener {
    lateinit var binding: OneMethodBinding
    private lateinit var typeSpinner: SpinnerHelper

    private val viewModel by viewModels<MethodViewModel>()

    private val rowId: Long
        get() = intent.extras?.getLong(DatabaseConstants.KEY_ROWID) ?: 0

    @State
    var preDefined: PreDefinedPaymentMethod? = null

    @State
    var icon: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        injector.inject(viewModel)
        binding = OneMethodBinding.inflate(layoutInflater)
        typeSpinner = SpinnerHelper(binding.Type)
        setContentView(binding.root)
        setupToolbarWithClose()
        if (savedInstanceState == null) {
            populateFields()
        } else {
            lifecycleScope.launch {
                setUpAccountTypeGrid(
                    viewModel.accountTypesRaw.first(),
                    savedInstanceState.getLongArray(KEY_SELECTED_TYPES)?.toList()
                )
            }
            linkInputsWithLabels()
        }
        setTitle(if (rowId == 0L) R.string.menu_create_method else R.string.menu_edit_method)
        binding.Icon.setOnClickListener {
            IconSelectorDialogFragment()
                .show(supportFragmentManager, "ICON_SELECTION")
        }
        binding.ClearIcon.setOnClickListener {
            icon = null
            setDirty()
            configureIcon()
        }
        floatingActionButton = binding.fab.CREATECOMMAND
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLongArray(KEY_SELECTED_TYPES, selectedTypes.toLongArray())
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        setupListeners()
        configureIcon()
    }

    override val fabActionName = "SAVE_METHOD"

    private fun populateFields() {
        if (rowId != 0L) {
            newInstance = false
            lifecycleScope.launch {
                with(viewModel.loadPaymentMethod(rowId)) {
                    binding.Label.setText(label)
                    binding.IsNumbered.isChecked = isNumbered
                    typeSpinner.setSelection(type + 1)
                    preDefined = preDefinedPaymentMethod
                    this@MethodEdit.icon = icon
                    setUpAccountTypeGrid(viewModel.accountTypesRaw.first(), this.accountTypes)
                }
                setupListeners()
                configureIcon()
            }
        } else {
            lifecycleScope.launch {
                setUpAccountTypeGrid(viewModel.accountTypesRaw.first(), emptyList())
            }
            setupListeners()
        }

        linkInputsWithLabels()
    }

    private fun setUpAccountTypeGrid(allTypes: List<AccountType>, checked: List<Long>?) {
        allTypes.forEach { accountType ->
            binding.AccountTypeGrid.addView(AppCompatCheckBox(this).also {
                it.text = accountType.localizedName(this)
                it.isChecked = checked?.contains(accountType.id) == true
                it.tag = accountType.id
                it.setOnCheckedChangeListener(this)
            })
        }
    }

    override fun saveState() {
        val label = binding.Label.text.toString()
        if (label.isEmpty()) {
            binding.Label.error = getString(R.string.required)
        } else {
            isSaving = true
            lifecycleScope.launch {
                viewModel.saveMethod(
                    PaymentMethod(
                        id = rowId,
                        label = binding.Label.text.toString(),
                        icon = icon,
                        type = typeSpinner.selectedItemPosition - 1,
                        isNumbered = binding.IsNumbered.isChecked,
                        accountTypes = selectedTypes,
                        preDefinedPaymentMethod = preDefined
                    )
                )
                finish()
            }
        }
    }

    private val selectedTypes: List<Long>
        get() = binding.AccountTypeGrid.children.mapNotNull {
            (it as? AppCompatCheckBox)?.let { checkBox ->
                if (checkBox.isChecked) checkBox.tag as Long else null
            }
        }.toList()

    private fun setupListeners() {
        binding.Label.addTextChangedListener(this)
        typeSpinner.setOnItemSelectedListener(object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                setDirty()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        })
        binding.IsNumbered.setOnCheckedChangeListener(this)
    }

    override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
        setDirty()
    }

    private fun configureIcon() {
        binding.Icon.setCompoundDrawablesRelativeWithIntrinsicBounds(
            icon?.let {
                IIconInfo.resolveIcon(it)?.asDrawable(
                    this, androidx.appcompat.R.attr.colorPrimary
                )
            },
            null, null, null
        )
        binding.ClearIcon.isVisible = icon != null
    }

    override fun onIconSelected(icon: String) {
        setDirty()
        this.icon = icon
        configureIcon()
    }
}