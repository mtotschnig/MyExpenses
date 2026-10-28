package org.totschnig.myexpenses.next.icons

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.totschnig.myexpenses.viewmodel.data.FontAwesomeIcons
import org.totschnig.myexpenses.next.core.common.icons.IconCatalog

class IconCatalogTest {

    @Test
    fun everyIconExistsInFontAwesome() {
        val names = IconCatalog.themes.flatMap { theme -> theme.icons.map { it.name } }
        assertEquals(emptyList<String>(), names.filter { it !in FontAwesomeIcons })
    }

    @Test
    fun suggestsFromGermanWordsAndCompounds() {
        assertEquals("umbrella", IconCatalog.bestFor("Hausratversicherung"))
        assertEquals("car-burst", IconCatalog.bestFor("Kfz-Versicherung"))
        assertEquals("car", IconCatalog.bestFor("Kfz"))
        assertEquals("dumbbell", IconCatalog.bestFor("Fitnessstudio"))
        assertEquals("gas-pump", IconCatalog.bestFor("Tanken"))
        assertEquals("bread-slice", IconCatalog.bestFor("Bäcker"))
    }

    @Test
    fun earlierTextsWin() {
        val suggested = IconCatalog.suggest("Haustier", "Lebensmittel")
        assertEquals("paw", suggested.first())
        assertTrue(suggested.indexOf("house") < suggested.indexOf("cart-shopping"))
    }

    @Test
    fun shortWordsOnlyAsWholeWords() {
        // "car" must not be found in "card", "pet" not in "petrol", "bus" not in "business"
        assertEquals(listOf("credit-card"), IconCatalog.suggest("credit card"))
        assertEquals(listOf("gas-pump"), IconCatalog.suggest("Petrol"))
        assertFalse("bus" in IconCatalog.suggest("Business"))
        assertFalse("beer-mug-empty" in IconCatalog.suggest("Barbershop"))
        assertTrue("beer-mug-empty" in IconCatalog.suggest("Bar"))
    }

    @Test
    fun noSuggestionForUnknownText() {
        assertEquals(emptyList<String>(), IconCatalog.suggest("Xyzzy", null, " "))
    }

    @Test
    fun searchFindsCuratedFirstThenAllByLabel() {
        val all = mapOf("dumbbell" to "Dumbbell", "weight-hanging" to "Hanging Weight", "house" to "House")
        assertEquals(listOf("dumbbell"), IconCatalog.search("fitness", all))
        assertEquals(listOf("weight-hanging"), IconCatalog.search("weight", all))
        assertEquals(emptyList<String>(), IconCatalog.search("", all))
    }
}
