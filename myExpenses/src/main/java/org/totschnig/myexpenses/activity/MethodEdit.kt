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
import android.widget.CheckBox
import android.widget.CompoundButton
import androidx.activity.viewModels
import androidx.annotation.IdRes
import androidx.appcompat.widget.AppCompatCheckBox
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.evernote.android.state.State
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
            setUpAccountTypeGrid { false }
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
                    setUpAccountTypeGrid { isValidForAccountType(it) }
                    preDefined = preDefinedPaymentMethod
                    this@MethodEdit.icon = icon
                }
                setupListeners()
                configureIcon()
            }
        } else {
            setUpAccountTypeGrid { false }
            setupListeners()
        }

        linkInputsWithLabels()
    }

    private fun setUpAccountTypeGrid(isValid: (AccountType) -> Boolean) {
        AccountType.values().forEach { accountType ->
            binding.AccountTypeGrid.addView(AppCompatCheckBox(this).apply {
                setText(accountType.toStringRes())
                tag = accountType
                isChecked = isValid(accountType)
                //setting Id makes state be retained on orientation change
                id = getCheckBoxId(accountType)
                setOnCheckedChangeListener(this@MethodEdit)
            })
        }
    }

    @IdRes
    private fun getCheckBoxId(accountType: AccountType): Int {
        return when (accountType) {
            AccountType.CASH -> R.id.AccountTypeCheckboxCash
            AccountType.BANK -> R.id.AccountTypeCheckboxBank
            AccountType.CCARD -> R.id.AccountTypeCheckboxCcard
            AccountType.ASSET -> R.id.AccountTypeCheckboxAsset
            AccountType.LIABILITY -> R.id.AccountTypeCheckboxLiability
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
                        accountTypes = AccountType.values().filter {
                            binding.AccountTypeGrid.findViewWithTag<CheckBox>(it).isChecked
                        },
                        preDefinedPaymentMethod = preDefined
                    )
                )
                finish()
            }
        }
    }

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