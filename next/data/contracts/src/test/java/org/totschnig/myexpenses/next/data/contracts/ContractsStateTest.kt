package org.totschnig.myexpenses.next.data.contracts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ContractsStateTest {

    private val today: LocalDate = LocalDate.of(2026, 9, 25)

    private fun contract(
        signature: String,
        amount: Long,
        isActive: Boolean = true,
        last: LocalDate = today,
        category: String? = null,
        isConfirmed: Boolean = true,
    ) = Contract(
        signature = signature,
        name = signature,
        interval = ContractInterval.MONTHLY,
        transactions = listOf(
            ContractTransaction(id = 1, date = last, amount = -amount, accountId = 1, categoryPath = category)
        ),
        nextExpectedDate = last.plusMonths(1),
        isActive = isActive,
        isConfirmed = isConfirmed
    )

    @Test
    fun partitionsIntoActiveEndedAndDismissed() {
        val state = buildContractsState(
            listOf(
                contract("small", 100),
                contract("big", 900),
                contract("oldEnded", 500, isActive = false, last = today.minusMonths(6)),
                contract("newEnded", 500, isActive = false, last = today.minusMonths(3)),
                contract("wrong", 300),
            ),
            ContractSettings(dismissed = setOf("wrong", "notDetectedAnyMore"))
        )
        assertEquals(listOf("big", "small"), state.active.map { it.signature })
        assertEquals(listOf("newEnded", "oldEnded"), state.ended.map { it.signature })
        assertEquals(listOf("wrong"), state.dismissed.map { it.signature })
    }

    @Test
    fun appliesCustomNames() {
        val state = buildContractsState(
            listOf(contract("a", 100), contract("b", 200)),
            ContractSettings(names = mapOf("a" to "Strom"))
        )
        assertEquals(setOf("Strom", "b"), state.active.map { it.displayName }.toSet())
        assertEquals("a", state.active.single { it.signature == "a" }.name)
    }

    @Test
    fun assignsAreasByCategoryAndUserChoice() {
        val gym = CustomArea("1", "Gym")
        val state = buildContractsState(
            listOf(
                contract("liability", 100, category = "Versicherungen > Haftpflicht"),
                contract("rent", 900, category = "Wohnen > Miete"),
                contract("phone", 300, category = "Kommunikation"),
                contract("notInsurance", 200, category = "Versicherungen"),
                contract("studio", 50, category = "Freizeit"),
                contract("deletedArea", 40, category = "Wohnen"),
            ),
            ContractSettings(
                areas = mapOf(
                    "phone" to BuiltInArea.HOUSING.key,
                    "notInsurance" to ContractSettings.AREA_NONE,
                    "studio" to gym.key,
                    "deletedArea" to "custom:gone"
                ),
                customAreas = listOf(gym)
            )
        )
        assertEquals(listOf("liability"), state.forArea(BuiltInArea.INSURANCE).active.map { it.signature })
        assertEquals(listOf("rent", "phone", "deletedArea"), state.forArea(BuiltInArea.HOUSING).active.map { it.signature })
        assertEquals(listOf("studio"), state.forArea(gym).active.map { it.signature })
        assertEquals(6, state.active.size)
    }

    @Test
    fun showsTabsForUsedBuiltInAndAllCustomAreas() {
        val empty = CustomArea("1", "Empty")
        val state = buildContractsState(
            listOf(contract("rent", 900, category = "Wohnen > Miete"), contract("tv", 100, category = "Streaming")),
            ContractSettings(customAreas = listOf(empty))
        )
        assertEquals(listOf(BuiltInArea.HOUSING, BuiltInArea.STREAMING, empty), state.areas)
    }

    @Test
    fun separatesSuggestionsFromConfirmedContracts() {
        val state = buildContractsState(
            listOf(
                contract("confirmed", 100),
                contract("small", 50, isConfirmed = false),
                contract("big", 500, isConfirmed = false, category = "Wohnen"),
                contract("endedSuggestion", 300, isActive = false, isConfirmed = false),
            ),
            ContractSettings()
        )
        assertEquals(listOf("confirmed"), state.active.map { it.signature })
        assertEquals(listOf("big", "small"), state.suggestions.map { it.signature })
        assertTrue(state.ended.isEmpty())
        // Suggestions neither make a category appear nor show up in its tab
        assertEquals(emptyList<ContractArea>(), state.areas)
        assertTrue(state.forArea(BuiltInArea.HOUSING).suggestions.isEmpty())
    }

    @Test
    fun separatesCancelledContracts() {
        val state = buildContractsState(
            listOf(
                contract("running", 100),
                contract("cancelledEarlier", 200, isActive = false).copy(cancelledOn = today.minusMonths(2)),
                contract("cancelledNow", 300, isActive = false).copy(cancelledOn = today),
            ),
            ContractSettings()
        )
        assertEquals(listOf("running"), state.active.map { it.signature })
        assertEquals(listOf("cancelledNow", "cancelledEarlier"), state.cancelled.map { it.signature })
        assertTrue(state.ended.isEmpty())
    }
}
