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
package org.totschnig.myexpenses.dialog

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.text.TextUtils
import android.text.format.DateUtils
import android.widget.RatingBar
import android.widget.RatingBar.OnRatingBarChangeListener
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.FragmentActivity
import org.totschnig.myexpenses.BuildConfig
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.databinding.RemindRateBinding
import org.totschnig.myexpenses.dialog.MessageDialogFragment.MessageDialogListener
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.distrib.DistributionHelper
import org.totschnig.myexpenses.util.tracking.Tracker
import javax.inject.Inject

class RemindRateDialogFragment : BaseDialogFragment(), DialogInterface.OnClickListener, OnRatingBarChangeListener {
    private var _binding: RemindRateBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var tracker: Tracker

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (requireActivity().application as MyApplication).appComponent.inject(this)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = initBuilderWithBinding {
            RemindRateBinding.inflate(materialLayoutInflater).also { _binding = it }
        }
        binding.ratingHowMany.text = Utils.getTextWithAppName(requireContext(), R.string.dialog_remind_rate_how_many_stars)
        binding.rating.onRatingBarChangeListener = this
        setRatingRemindText(true)
        return builder.setTitle(R.string.app_name)
                .setCancelable(false)
                .setPositiveButton(R.string.dialog_remind_rate_yes, this)
                .setNeutralButton(R.string.dialog_remind_later, this)
                .create().also {
                    it.setOnShowListener(ButtonOnShowDisabler())
                }
    }

    private fun setRatingRemindText(isPositive: Boolean) {
        binding.ratingRemind.text = TextUtils.concat(
                if (isPositive) Utils.getTextWithAppName(context, R.string.dialog_remind_rate_1)
                else getString(R.string.dialog_remind_rate_1_suggest_improvement),
                " ", getString(R.string.dialog_remind_rate_2))
    }

    override fun onClick(dialog: DialogInterface, which: Int) {
        tracker.logEvent(Tracker.EVENT_RATING_DIALOG, Bundle().apply {
            putInt(Tracker.EVENT_PARAM_BUTTON_ID, which)
        })
        val nextReminderRate = if (which == AlertDialog.BUTTON_NEUTRAL) {
            System.currentTimeMillis() + DateUtils.DAY_IN_MILLIS * 30
        } else {
            if (which == AlertDialog.BUTTON_POSITIVE) {
                (activity as MessageDialogListener?)?.dispatchCommand(
                        if (binding.rating.rating >= POSITIVE_RATING) R.id.RATE_COMMAND else R.id.FEEDBACK_COMMAND, null)
            }
            -1
        }
        prefHandler.putLong(PrefKey.NEXT_REMINDER_RATE, nextReminderRate)
    }

    override fun onRatingChanged(ratingBar: RatingBar, rating: Float,
                                 fromUser: Boolean) {
        if (fromUser) {
            if (rating < 1) {
                ratingBar.rating = 1f
            }
            setRatingRemindText(rating >= POSITIVE_RATING)
            (dialog as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE).apply {
                isEnabled = true
                setText(if (rating >= POSITIVE_RATING) R.string.dialog_remind_rate_yes else R.string.pref_send_feedback_title)
                invalidate()
            }
        }
    }

    companion object {
        private const val POSITIVE_RATING = 5
        fun maybeShow(prefHandler: PrefHandler, activity: FragmentActivity) {
            if (shouldShow(prefHandler, activity)) {
                RemindRateDialogFragment().apply {
                    isCancelable = false
                    show(activity.supportFragmentManager, "REMIND_RATE")
                }
            }
        }

        private fun shouldShow(prefHandler: PrefHandler, activity: Context): Boolean {
            if (DistributionHelper.isGithub && !BuildConfig.DEBUG)
                return false
            val nextReminder = prefHandler.getLong(PrefKey.NEXT_REMINDER_RATE, Utils.getInstallTime(activity) + DateUtils.DAY_IN_MILLIS * 30)
            return nextReminder != -1L && nextReminder < System.currentTimeMillis()
        }
    }
}