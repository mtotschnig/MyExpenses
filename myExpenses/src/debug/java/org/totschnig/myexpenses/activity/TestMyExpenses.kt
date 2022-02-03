package org.totschnig.myexpenses.activity

import org.totschnig.myexpenses.provider.CheckSealedHandler

class TestMyExpenses: MyExpenses() {
    lateinit var decoratedCheckSealedHandler: CheckSealedHandler

    override fun buildCheckSealedHandler() = decoratedCheckSealedHandler
}