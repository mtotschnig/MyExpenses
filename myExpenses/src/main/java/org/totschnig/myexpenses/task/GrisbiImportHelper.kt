package org.totschnig.myexpenses.task

import org.totschnig.myexpenses.db2.Repository
import org.totschnig.myexpenses.db2.findCategory
import org.totschnig.myexpenses.db2.saveCategory
import org.totschnig.myexpenses.model2.Category
import org.totschnig.myexpenses.util.CategoryTree
import timber.log.Timber

object GrisbiImportHelper {
    private fun writeCategory(repository: Repository, label: String, parentId: Long? = null) =
        repository.saveCategory(Category(label = label, parentId = parentId)) ?: -1

    fun importCats(catTree: CategoryTree, task: GrisbiImportTask): Int {
        var count = 0
        var total = 0
        var label: String
        var main_id: Long
        var sub_id: Long
        val size = catTree.children().size()
        for (i in 0 until size) {
            val mainCat = catTree.children().valueAt(i)
            label = mainCat.label
            count++
            main_id = task.repository.findCategory(label, null)
            if (main_id != -1L) {
                Timber.i("category with label %s already defined", label)
            } else {
                main_id = writeCategory(task.repository, label)
                if (main_id != -1L) {
                    total++
                    if (count % 10 == 0) {
                        task.publishProgress(count)
                    }
                } else {
                    // this should not happen
                    Timber.w("could neither retrieve nor store main category %s", label)
                    continue
                }
            }
            val subSize = mainCat.children().size()
            for (j in 0 until subSize) {
                label = mainCat.children().valueAt(j).label
                count++
                sub_id = writeCategory(task.repository, label, main_id)
                if (sub_id != -1L) {
                    total++
                } else {
                    Timber.i("could not store sub category %s", label)
                }
                if (count % 10 == 0) {
                    task.publishProgress(count)
                }
            }
        }
        return total
    }
}