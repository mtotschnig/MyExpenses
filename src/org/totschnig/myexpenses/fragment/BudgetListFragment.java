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

package org.totschnig.myexpenses.fragment;

import org.totschnig.myexpenses.R;

import android.content.res.Resources;
import android.util.TypedValue;

import com.actionbarsherlock.app.SherlockFragment;

/**
 * Helper that factors out commen properties
 *
 */
public class BudgetListFragment extends SherlockFragment {

  protected int colorExpense;
  protected int colorIncome;

  public BudgetListFragment() {
    super();
  }

  protected void setColors() {
    Resources.Theme theme = getActivity().getTheme();
    TypedValue color = new TypedValue();
    theme.resolveAttribute(R.attr.colorExpense, color, true);
    colorExpense = color.data;
    theme.resolveAttribute(R.attr.colorIncome,color, true);
    colorIncome = color.data;
  }

}