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

package org.totschnig.myexpenses.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import org.totschnig.myexpenses.databinding.ScrollableProgressDialogBinding;

import androidx.annotation.Nullable;

@Deprecated
public class ScrollableProgressDialog extends AlertDialog {
  @Nullable
  private ScrollableProgressDialogBinding binding;
  private CharSequence message;

  /**
   * Creates a Progress dialog.
   *
   * @param context the parent context
   */
  public ScrollableProgressDialog(Context context) {
    super(context);
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    binding = ScrollableProgressDialogBinding.inflate(LayoutInflater.from(getContext()));
    if (message != null) {
      setMessage(message);
    }
    setView(binding.getRoot());
    super.onCreate(savedInstanceState);
  }

  @Override
  public void setMessage(CharSequence message) {
    if (binding != null) {
      binding.message.setText(message);
    }
    this.message = message;
  }

  public void unsetIndeterminateDrawable() {
    binding.progress.setVisibility(View.INVISIBLE);
  }
}
