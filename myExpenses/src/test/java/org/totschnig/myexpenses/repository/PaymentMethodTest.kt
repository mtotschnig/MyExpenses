package org.totschnig.myexpenses.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.totschnig.myexpenses.BaseTestWithRepository
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.db2.loadPaymentMethod
import org.totschnig.myexpenses.db2.updatePaymentMethod
import org.totschnig.myexpenses.model2.PaymentMethod

@RunWith(RobolectricTestRunner::class)
class PaymentMethodTest: BaseTestWithRepository() {
    val context: Context
        get() = ApplicationProvider.getApplicationContext<MyApplication>()

    @Test
    fun savingAPredefinedMethodWithoutChangingLabelShouldKeepPredefinedInformation() {
        val pm = repository.loadPaymentMethod(context, 1)
        Truth.assertThat(isPredefined(pm)).isTrue()
        repository.updatePaymentMethod(context, pm.copy(isNumbered = !pm.isNumbered))
        Truth.assertThat(isPredefined(repository.loadPaymentMethod(context, 1))).isTrue()
    }

    @Test
    fun savingAPredefinedMethodWithChangingLabelShouldDiscardPredefinedInformation() {
        val pm = repository.loadPaymentMethod(context, 1)
        Truth.assertThat(isPredefined(pm)).isTrue()
        repository.updatePaymentMethod(context, pm.copy(label = "new label"))
        Truth.assertThat(isPredefined(repository.loadPaymentMethod(context, 1))).isFalse()
    }

    private fun isPredefined(pm: PaymentMethod) =
        pm.preDefinedPaymentMethod != null
}