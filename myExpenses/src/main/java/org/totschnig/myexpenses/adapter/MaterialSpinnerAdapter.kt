package org.totschnig.myexpenses.adapter

import android.content.Context
import android.widget.ArrayAdapter
import android.widget.Filter

/**
 * https://blog.usejournal.com/there-is-no-material-design-spinner-for-android-3261b7c77da8
 */
class MaterialSpinnerAdapter<String>(context: Context, layout: Int, var values: List<String>) :
    ArrayAdapter<String>(context, layout, values) {
    private val filter_that_does_nothing = object: Filter() {
        override fun performFiltering(constraint: CharSequence?): FilterResults {
            val results = FilterResults()
            results.values = values
            results.count = values.size
            return results
        }
        override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
            notifyDataSetChanged()
        }
    }

    override fun getFilter(): Filter {
        return filter_that_does_nothing
    }
}