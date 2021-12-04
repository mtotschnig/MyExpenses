package org.totschnig.myexpenses.activity

import org.totschnig.myexpenses.provider.CheckSealedHandler

class TestMyExpenses: MyExpenses() {
    lateinit var decoratedCheckSealeHandler: CheckSealedHandler

    override fun buildCheckSealedHander() = decoratedCheckSealeHandler
}