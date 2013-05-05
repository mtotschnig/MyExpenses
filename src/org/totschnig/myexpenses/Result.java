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

package org.totschnig.myexpenses;

/**
 * represents a tuple of success flag, and message as an R id
 * @author Michael Totschnig
 *
 */
public class Result {
  /**
   * true represents success, false failure
   */
  public boolean success;
  /**
   * a string id from {@link R} for i18n and joining with an argument
   */
  public int message;
  
  /**
   * optional argument to be passed to getString when resolving message id
   */
  public Object[] extra;
  
  public Result(boolean success) {
    this.success = success;
  }

  public Result(boolean success,int message) {
    this.success = success;
    this.message = message;
  }

  public Result(boolean success,int message,Object... extra) {
    this.success = success;
    this.message = message;
    this.extra = extra;
  }
}