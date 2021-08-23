/***
 * Copyright (c) 2015 CommonsWare, LLC
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain	a copy
 * of the License at http://www.apache.org/licenses/LICENSE-2.0. Unless required
 * by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS,	WITHOUT	WARRANTIES OR CONDITIONS
 * OF ANY KIND, either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * Covered in detail in the book _The Busy Coder's Guide to Android Development_
 * https://commonsware.com/Android
 */
package org.totschnig.myexpenses.adapter

import android.os.Bundle
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import org.totschnig.myexpenses.util.SparseBooleanArrayParcelable
import org.totschnig.myexpenses.util.asTrueSequence

abstract class ChoiceCapableAdapter<T, VH : RecyclerView.ViewHolder>(
    private val choiceMode: ChoiceMode,
    diffCallBack: DiffUtil.ItemCallback<T>
) :
    ListAdapter<T, VH>(diffCallBack) {
    fun onChecked(position: Int, isChecked: Boolean) {
        choiceMode.setChecked(position, isChecked)
    }

    fun isChecked(position: Int): Boolean {
        return choiceMode.isChecked(position)
    }

    fun onSaveInstanceState(state: Bundle) {
        choiceMode.onSaveInstanceState(state)
    }

    fun onRestoreInstanceState(state: Bundle) {
        choiceMode.onRestoreInstanceState(state)
    }

    val checkedCount: Int
        get() = choiceMode.checkedCount

    val checkedPositions: List<Int>
        get() = choiceMode.checkedPositions

    fun clearChecks() {
        choiceMode.clearChecks()
    }
}

interface ChoiceMode {
    fun setChecked(position: Int, isChecked: Boolean)
    fun isChecked(position: Int): Boolean
    fun onSaveInstanceState(state: Bundle)
    fun onRestoreInstanceState(state: Bundle)
    val checkedCount: Int

    fun clearChecks()
    val checkedPositions: List<Int>
}

class MultiChoiceMode : ChoiceMode {
    private var checkStates = SparseBooleanArrayParcelable()
    override fun setChecked(position: Int, isChecked: Boolean) {
        if (isChecked) {
            checkStates.put(position, isChecked)
        } else {
            checkStates.delete(position)
        }
    }

    override fun isChecked(position: Int): Boolean {
        return checkStates.get(position, false)
    }

    override fun onSaveInstanceState(state: Bundle) {
        state.putParcelable(STATE_CHECK_STATES, checkStates)
    }

    override fun onRestoreInstanceState(state: Bundle) {
        checkStates = state.getParcelable(STATE_CHECK_STATES)!!
    }

    override val checkedCount: Int
        get() = checkStates.size()

    override fun clearChecks() {
        checkStates.clear()
    }

    override val checkedPositions: List<Int>
        get() = checkStates.asTrueSequence().toList()

    companion object {
        private const val STATE_CHECK_STATES = "checkStates"
    }
}