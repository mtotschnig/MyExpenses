package org.totschnig.myexpenses.activity

import android.app.Activity
import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.os.BundleCompat
import org.totschnig.myexpenses.fragment.TagList.Companion.KEY_TAG_LIST
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID
import org.totschnig.myexpenses.provider.filter.CategoryCriterion
import org.totschnig.myexpenses.provider.filter.IdCriterion
import org.totschnig.myexpenses.provider.filter.KEY_SELECTION
import org.totschnig.myexpenses.provider.filter.NULL_ITEM_ID
import org.totschnig.myexpenses.provider.filter.PayeeCriterion
import org.totschnig.myexpenses.provider.filter.TagCriterion
import org.totschnig.myexpenses.util.enumValueOrDefault
import org.totschnig.myexpenses.viewmodel.data.Tag

enum class Action {
    SELECT_MAPPING, SELECT_FILTER, MANAGE
}

const val HELP_VARIANT_MANGE = "manage"
const val HELP_VARIANT_SELECT_MAPPING = "select_mapping"
const val HELP_VARIANT_SELECT_FILTER = "select_filter"

val Intent.asAction get() = enumValueOrDefault(action, Action.SELECT_MAPPING)

abstract class PickObjectContract : ActivityResultContract<Pair<Long?, IdCriterion?>, IdCriterion?>() {
    abstract val activityClass: Class<out Activity>
    override fun createIntent(context: Context, input: Pair<Long?, IdCriterion?>) =
        Intent(context, activityClass).apply {
            action = Action.SELECT_FILTER.name
            putExtra(KEY_ACCOUNTID, input.first)
            putExtra(KEY_SELECTION, input.second?.values?.toLongArray())
        }

    protected fun extractObjects(resultCode: Int, extras: Bundle): Pair<String?, LongArray?> =
        extras.getString(KEY_LABEL) to if (resultCode == RESULT_OK) {
            longArrayOf(extras.getLong(KEY_ROWID))
        } else {
            extras.getLongArray(KEY_ROWID)
        }
}

class PickCategoryContract : PickObjectContract() {
    override val activityClass = ManageCategories::class.java

    override fun parseResult(resultCode: Int, intent: Intent?) = intent?.extras?.let {
        val (label, catIds) = extractObjects(resultCode, it)
        if (catIds != null && label != null) {
            if (catIds.size == 1 && catIds[0] == NULL_ITEM_ID) CategoryCriterion() else
                CategoryCriterion(label, *catIds)
        } else null
    }
}

class PickPayeeContract : PickObjectContract() {
    override val activityClass = ManageParties::class.java

    override fun parseResult(resultCode: Int, intent: Intent?) = intent?.extras?.let {
        val (label, catIds) = extractObjects(resultCode, it)
        if (catIds != null && label != null) {
            if (catIds.size == 1 && catIds[0] == NULL_ITEM_ID) PayeeCriterion() else
                PayeeCriterion(label, *catIds)
        } else null
    }
}

class PickTagContract : PickObjectContract() {
    override val activityClass = ManageTags::class.java

    override fun parseResult(resultCode: Int, intent: Intent?) = intent?.extras
        .takeIf { resultCode == RESULT_OK }
        ?.let { bundle ->
            BundleCompat.getParcelableArrayList(bundle, KEY_TAG_LIST, Tag::class.java)
                ?.takeIf { it.isNotEmpty() }
                ?.let { tagList ->
                    TagCriterion(
                        tagList.joinToString { it.label },
                        *tagList.map { it.id }.toLongArray()
                    )
                }
        }
}