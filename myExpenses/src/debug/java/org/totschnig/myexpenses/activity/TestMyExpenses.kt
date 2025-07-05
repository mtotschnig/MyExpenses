package org.totschnig.myexpenses.activity

import org.totschnig.myexpenses.provider.CheckSealedHandler

class TestMyExpenses: MyExpenses() {

    override val helpContext = "MyExpenses"

    lateinit var decoratedCheckSealedHandler: CheckSealedHandler

    override val checkSealedHandler
        get() = decoratedCheckSealedHandler
}