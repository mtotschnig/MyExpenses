package org.totschnig.myexpenses

import com.google.common.truth.FailureMetadata
import com.google.common.truth.Subject
import com.google.common.truth.Subject.Factory
import com.google.common.truth.Truth
import org.totschnig.myexpenses.db2.FLAG_EXPENSE
import org.totschnig.myexpenses.db2.FLAG_INCOME
import org.totschnig.myexpenses.export.CategoryInfo

class CategoryInfoSubject private constructor(
    failureMetadata: FailureMetadata,
    private val actual: CategoryInfo
) : Subject(failureMetadata, actual) {

    fun isIncome() {
        check("isIncome").that(actual.type).isEqualTo(FLAG_INCOME)
    }

    fun isExpense() {
        check("isIncome").that(actual.type).isEqualTo(FLAG_EXPENSE)
    }

    companion object {
        fun assertThat(cursor: CategoryInfo): CategoryInfoSubject {
            return Truth.assertAbout(FACTORY).that(cursor)
        }

        private val FACTORY = Factory { failureMetadata: FailureMetadata, subject: CategoryInfo? ->
            CategoryInfoSubject(failureMetadata, subject!!)
        }
    }
}