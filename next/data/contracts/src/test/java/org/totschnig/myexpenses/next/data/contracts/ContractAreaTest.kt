package org.totschnig.myexpenses.next.data.contracts

import org.junit.Assert.assertEquals
import org.junit.Test

class ContractAreaTest {

    private fun assertSuggests(expected: BuiltInArea?, vararg paths: String?) {
        paths.forEach { assertEquals(it, expected, BuiltInArea.suggest(it)) }
    }

    @Test
    fun suggestsInsurance() {
        assertSuggests(
            BuiltInArea.INSURANCE,
            "Versicherung", "Versicherungen > Haftpflicht", "Auto > Kfz-Versicherung", "Insurance",
            "Wohnen > Haushaltsversicherung"
        )
    }

    @Test
    fun suggestsHousing() {
        assertSuggests(
            BuiltInArea.HOUSING,
            "Wohnen", "Wohnen > Miete", "Haushalt", "Nebenkosten > Strom", "Stromkosten", "Energie > Gas", "Housing > Rent"
        )
    }

    /** Contracts in the German default categories, see xml-de/categories.xml of :myExpenses */
    @Test
    fun suggestsFromGermanDefaultCategories() {
        assertSuggests(
            BuiltInArea.INSURANCE,
            "Wohnen > Hausrat- & Gebäudeversicherung", "Mobilität > Kfz-Versicherung", "Versicherungen > Berufsunfähigkeit",
            "Gesundheit & Pflege > Krankenversicherung & Zusatzversicherungen", "Haustiere > Tierversicherung"
        )
        assertSuggests(BuiltInArea.HOUSING, "Wohnen > Strom", "Wohnen > Rundfunkbeitrag", "Wohnen > Heizung/Gas")
        assertSuggests(BuiltInArea.TELECOM, "Telefon & Digitales > Handyvertrag", "Telefon & Digitales > Internet & Festnetz")
        assertSuggests(BuiltInArea.STREAMING, "Telefon & Digitales > Streaming (Video & Musik)")
        assertSuggests(BuiltInArea.SOFTWARE, "Telefon & Digitales > Software & Apps", "Telefon & Digitales > Cloud-Speicher")
        assertSuggests(BuiltInArea.FITNESS, "Freizeit & Hobbys > Fitnessstudio & Sport")
        assertSuggests(BuiltInArea.MOBILITY, "Mobilität > ÖPNV & Deutschlandticket", "Mobilität > Kfz-Steuer")
        assertSuggests(BuiltInArea.FINANCE, "Finanzen & Gebühren > Kontoführung", "Finanzen & Gebühren > Kreditraten")
        assertSuggests(BuiltInArea.PRESS, "Freizeit & Hobbys > Bücher & Zeitschriften")
        assertSuggests(
            BuiltInArea.MEMBERSHIP,
            "Freizeit & Hobbys > Vereinsbeiträge", "Bildung & Beruf > Gewerkschaft & Berufsverband"
        )
        assertSuggests(BuiltInArea.DONATIONS, "Geschenke & Spenden > Spenden")
        assertSuggests(BuiltInArea.EDUCATION, "Kinder > Kita & Kinderbetreuung", "Kinder > Nachhilfe")
    }

    @Test
    fun mostSpecificPartWins() {
        assertSuggests(BuiltInArea.TELECOM, "Wohnen > Internet & Telefon", "Kommunikation > Handy")
        assertSuggests(BuiltInArea.STREAMING, "Freizeit > Streaming", "Unterhaltung > Pay-TV")
        assertSuggests(BuiltInArea.FITNESS, "Freizeit > Fitnessstudio", "Sport", "Sportverein")
        assertSuggests(BuiltInArea.MOBILITY, "Mobilität > Deutschlandticket", "Auto > Leasing")
        assertSuggests(BuiltInArea.FINANCE, "Finanzen > Kredit", "Bankgebühren")
        assertSuggests(BuiltInArea.SOFTWARE, "Computer > Cloud-Speicher", "Apps")
        assertSuggests(BuiltInArea.PRESS, "Medien > Zeitung")
        assertSuggests(BuiltInArea.MEMBERSHIP, "Vereine", "Mitgliedsbeiträge")
        assertSuggests(BuiltInArea.DONATIONS, "Spenden")
        assertSuggests(BuiltInArea.EDUCATION, "Kinder > Kita", "Musikschule")
        // The main category is used, if the sub category does not tell
        assertSuggests(BuiltInArea.HOUSING, "Wohnen > Sonstiges")
    }

    @Test
    fun suggestsNothingForOtherCategories() {
        assertSuggests(null, null, "", "Essen > Gastronomie", "Lebensmittel", "Kleidung")
    }
}
