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

package org.totschnig.myexpenses.util;

import android.content.Context;

import org.totschnig.myexpenses.R;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import kotlin.Unit;

/**
 * represents a tuple of success flag, and message as an R id
 *
 * @author Michael Totschnig
 */
public class Result<T> {

  public static final Result<Unit> SUCCESS = new Result<>(true);
  public static final Result<Unit> FAILURE = new Result<>(false);

  /**
   * true represents success, false failure
   */
  private final boolean success;
  /**
   * a string id from {@link R} for i18n and joining with an argument
   */
  private final int message;
  private final String messageString;

  /**
   * optional argument to be passed to getString when resolving message id
   */
  @Nullable
  private final T extra;

  private final Object[] messageArguments;

  public int getMessage() {
    return message;
  }

  public boolean isSuccess() {
    return success;
  }

  @Nullable
  public T getExtra() {
    return extra;
  }

  private Result(boolean success) {
    this(success, 0);
  }

  private Result(boolean success, int message) {
    this(success, message, null);
  }

  private Result(boolean success, int message, @Nullable T extra) {
    this(success, message, extra, (Object[]) null);
  }

  private Result(boolean success, int message, @Nullable T extra, Object... messageArguments) {
    this.success = success;
    this.message = message;
    this.extra = extra;
    this.messageArguments = messageArguments;
    this.messageString = null;
  }

  private Result(boolean success, String messageString) {
    this.success = success;
    this.message = 0;
    this.messageArguments = null;
    this.extra = null;
    this.messageString = messageString;
  }

  public static <T> Result<T> ofSuccess(int message) {
    return new Result<>(true, message, null);
  }

  public static <T> Result<T> ofSuccess(String messageString) {
    return new Result<>(true, messageString);
  }

  public static <T> Result<T> ofSuccess(int message, T extra) {
    return new Result<>(true, message, extra);
  }

  public static <T> Result<T> ofSuccess(int message, T extra, Object... messageArguments) {
    return new Result<>(true, message, extra, messageArguments);
  }

  public static <T> Result<T> ofFailure(int message) {
    return new Result<>(false, message);
  }

  public static <T> Result<T> ofFailure(String messageString) {
    return new Result<>(false, messageString);
  }

  public static <T> Result<T> ofFailure(int message, Object... messageArguments) {
    return new Result<>(false, message, null, messageArguments);
  }

  @NonNull
  public String print(Context ctx) {
    return message == 0 ? (messageString == null ? "" : messageString) :
        (ctx.getString(message, messageArguments));
  }

  public String print0(Context ctx) {
    return message == 0 ? messageString :
        (ctx.getString(message, messageArguments));
  }
}