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

package org.totschnig.myexpenses.dialog;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.RatingBar;
import android.widget.RatingBar.OnRatingBarChangeListener;
import android.widget.TextView;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.MyExpenses;
import org.totschnig.myexpenses.dialog.MessageDialogFragment.MessageDialogListener;
import org.totschnig.myexpenses.preference.PrefHandler;
import org.totschnig.myexpenses.preference.PrefKey;
import org.totschnig.myexpenses.util.Utils;
import org.totschnig.myexpenses.util.tracking.Tracker;

import javax.inject.Inject;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import static android.text.format.DateUtils.DAY_IN_MILLIS;
import static org.totschnig.myexpenses.util.tracking.Tracker.EVENT_RATING_DIALOG;

public class RemindRateDialogFragment extends BaseDialogFragment implements OnClickListener, OnRatingBarChangeListener {

  private RatingBar mRating;
  private TextView mRatingRemind;
  private int POSITIVE_RATING = 5;

  @Inject
  protected Tracker tracker;

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    ((MyApplication) requireActivity().getApplication()).getAppComponent().inject(this);
  }

  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    AlertDialog.Builder builder = initBuilderWithView(R.layout.remind_rate);
    MyExpenses ctx = (MyExpenses) getActivity();
    ((TextView) dialogView.findViewById(R.id.rating_how_many)).setText(Utils.getTextWithAppName(ctx, R.string.dialog_remind_rate_how_many_stars));
    mRating = dialogView.findViewById(R.id.rating);
    mRating.setOnRatingBarChangeListener(this);
    mRatingRemind = dialogView.findViewById(R.id.rating_remind);
    setRatingRemindText(true);
    AlertDialog dialog = builder.setTitle(R.string.app_name)
        .setCancelable(false)
        .setPositiveButton(R.string.dialog_remind_rate_yes, this)
        .setNeutralButton(R.string.dialog_remind_later, this)
        .setNegativeButton(R.string.dialog_remind_no, this)
        .create();
    dialog.setOnShowListener(new ButtonOnShowDisabler());
    return dialog;
  }

  private void setRatingRemindText(boolean isPositive) {
    CharSequence rate1 = isPositive ? Utils.getTextWithAppName(getContext(), R.string.dialog_remind_rate_1) :
        getString(R.string.dialog_remind_rate_1_suggest_improvement);
    mRatingRemind.setText(TextUtils.concat(rate1, " ", getString(R.string.dialog_remind_rate_2)));
  }

  @Override
  public void onClick(DialogInterface dialog, int which) {
    if (getActivity() == null) {
      return;
    }
    Bundle bundle = new Bundle();
    bundle.putInt(Tracker.EVENT_PARAM_BUTTON_ID, which);
    tracker.logEvent(EVENT_RATING_DIALOG, bundle);
    long nextReminderRate = -1;
    if (which == AlertDialog.BUTTON_POSITIVE) {
      ((MessageDialogListener) getActivity())
          .dispatchCommand(mRating.getRating() >= POSITIVE_RATING ? R.id.RATE_COMMAND : R.id.FEEDBACK_COMMAND, null);
    } else if (which == AlertDialog.BUTTON_NEUTRAL) {
       nextReminderRate = System.currentTimeMillis() + DAY_IN_MILLIS * 30;
    }
    prefHandler.putLong(PrefKey.NEXT_REMINDER_RATE, nextReminderRate);
  }

  @Override
  public void onRatingChanged(RatingBar ratingBar, float rating,
                              boolean fromUser) {
    if (fromUser) {
      if (rating < 1) {
        ratingBar.setRating(1);
      }
      setRatingRemindText(rating >= POSITIVE_RATING);
      Button b = ((AlertDialog) getDialog()).getButton(AlertDialog.BUTTON_POSITIVE);
      b.setEnabled(true);
      b.setText(rating >= POSITIVE_RATING ? R.string.dialog_remind_rate_yes : R.string.pref_send_feedback_title);
      b.invalidate();
    }
  }
}