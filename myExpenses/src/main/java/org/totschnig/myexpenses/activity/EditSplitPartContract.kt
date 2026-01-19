package org.totschnig.myexpenses.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.os.BundleCompat
import org.totschnig.myexpenses.viewmodel.data.TransactionEditData

const val KEY_SPLIT_PART = "splitPart"
const val KEY_SPLIT_PART_LIST = "splitPartList"
/**
 * An ActivityResultContract to edit a split transaction part.
 *
 * Input: A TransactionEditData object for the split part to be edited,
 *        or null to create a new part.
 * Output: The saved TransactionEditData object, or null if the user canceled.
 */
abstract class EditSplitPartContract : ActivityResultContract<TransactionEditData, ArrayList<TransactionEditData>?>() {

    abstract fun prepareIntent(intent: Intent)

    override fun createIntent(context: Context, input: TransactionEditData): Intent {

        // Create an intent to launch the ExpenseEdit activity.
        val intent = Intent(context, ExpenseEdit::class.java).apply {

            // Pass the TransactionEditData object as a Parcelable extra.
            // This object contains all the details of the split part.
            putExtra(KEY_SPLIT_PART, input)
        }
        return intent.also { prepareIntent(it) }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): ArrayList<TransactionEditData>? {
        // If the activity did not return a successful result, return null.
        if (resultCode != Activity.RESULT_OK) {
            return null
        }
        // Extract the TransactionEditData object from the result intent's extras.
        return intent?.extras?.let {
            BundleCompat.getParcelableArrayList(it,
                KEY_SPLIT_PART_LIST, TransactionEditData::class.java)
        }
    }
}