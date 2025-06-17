package org.totschnig.myexpenses.repository

import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.totschnig.myexpenses.BaseTestWithRepository
import org.totschnig.myexpenses.db2.createParty
import org.totschnig.myexpenses.db2.requireParty
import org.totschnig.myexpenses.model2.Party


@RunWith(RobolectricTestRunner::class)
class PartyTest : BaseTestWithRepository() {
    private lateinit var party: Party

    @Before
    fun setupPayee() {
        party = repository.createParty(Party(name = "N.N."))!!
        assertThat(party.id).isGreaterThan(0L)
    }

    @Test
    fun requireNewPayee() {
        val id = repository.requireParty("X")
        assertThat(id).isGreaterThan(0L)
        assertThat(id).isNotEqualTo(party.id)
    }

    @Test
    fun requireNewPayeeWithSpace() {
        val id = repository.requireParty(" X ")
        assertThat(id).isGreaterThan(0L)
        assertThat(id).isNotEqualTo(party.id)
        assertThat(id).isEqualTo(repository.requireParty("X"))
    }

    @Test
    fun requireExistingPayee() {
        val id = repository.requireParty("N.N.")
        assertThat(id).isEqualTo(party.id)
    }

    @Test
    fun requireExistingPayeeWithSpace() {
        val id = repository.requireParty(" N.N. ")
        assertThat(id).isEqualTo(party.id)
    }
}