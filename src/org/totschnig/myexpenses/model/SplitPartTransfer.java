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

package org.totschnig.myexpenses.model;

public class SplitPartTransfer extends Transfer {
  public SplitPartTransfer(long accountId, Long amount,long parentId) {
    super(accountId,amount);
    this.parentId = parentId;
  }

  public SplitPartTransfer(Account account, long amount, Long parentId) {
    super(account,amount);
    this.parentId = parentId;
  }
}
