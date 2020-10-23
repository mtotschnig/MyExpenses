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
import android.widget.ProgressBar;
import android.widget.TextView;

import org.totschnig.myexpenses.R;

import butterknife.BindView;
import butterknife.ButterKnife;

@Deprecated
public class ScrollableProgressDialog extends AlertDialog {

  private CharSequence message;
  @BindView(R.id.message)
  TextView messageView;
  @BindView(R.id.progress)
  ProgressBar progressBar;

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
    LayoutInflater inflater = LayoutInflater.from(getContext());
    //noinspection InflateParams
    View view = inflater.inflate(R.layout.scrollable_progress_dialog, null);
    ButterKnife.bind(this, view);
    if (message != null) {
      setMessage(message);
    }
    setView(view);
    super.onCreate(savedInstanceState);
  }

  @Override
  public void setMessage(CharSequence message) {
    if (messageView != null) {
      messageView.setText(message);
    }
    this.message = message;
  }

  public void unsetIndeterminateDrawable() {
    progressBar.setVisibility(View.INVISIBLE);
  }
}
