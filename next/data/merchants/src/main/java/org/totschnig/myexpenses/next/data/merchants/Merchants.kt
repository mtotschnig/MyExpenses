package org.totschnig.myexpenses.next.data.merchants

import org.totschnig.myexpenses.next.core.model.words.Words

/**
 * A well-known shop or company that payees are recognized as, e.g. "REWE Markt GmbH Fil. 4711"
 *
 * @param domain of its website, its logo is loaded for it, see [MerchantLogos]
 * @param patterns start a word of the payee, e.g. "rewe", may span several words
 * @param words are whole words of the payee, for short ones like "dm" that start other words
 */
data class Merchant(
    val name: String,
    val domain: String,
    val patterns: List<String> = emptyList(),
    val words: List<String> = emptyList(),
    val kind: Kind = Kind.OTHER,
) {
    enum class Kind { SUPERMARKET, OTHER }

    /** Where the merchant is named first in [text] of [Words.textOf], -1 if it is not */
    internal fun indexIn(text: String): Int =
        (patterns.map { Words.indexOfWordStart(text, it) } + words.map { Words.indexOfWord(text, it) })
            .filter { it >= 0 }
            .minOrNull() ?: -1
}

/** A payee as stored, see [Merchants.ofPayees] */
class RawPayee(val id: Long, val name: String, val shortName: String?, val parentId: Long?)

object Merchants {
    private val SUPERMARKET = Merchant.Kind.SUPERMARKET

    /** Supermarkets in the order the saving tips check them, then the others */
    val all = listOf(
        Merchant("REWE", "rewe.de", listOf("rewe"), kind = SUPERMARKET),
        Merchant("EDEKA", "edeka.de", listOf("edeka", "e center", "marktkauf"), kind = SUPERMARKET),
        Merchant("ALDI", "aldi-sued.de", listOf("aldi"), kind = SUPERMARKET),
        Merchant("Lidl", "lidl.de", listOf("lidl"), kind = SUPERMARKET),
        Merchant("Netto", "netto-online.de", listOf("netto"), kind = SUPERMARKET),
        Merchant("Penny", "penny.de", listOf("penny"), kind = SUPERMARKET),
        Merchant("Kaufland", "kaufland.de", listOf("kaufland"), kind = SUPERMARKET),
        Merchant("tegut", "tegut.com", listOf("tegut"), kind = SUPERMARKET),
        Merchant("Norma", "norma-online.de", listOf("norma"), kind = SUPERMARKET),
        Merchant("Globus", "globus.de", listOf("globus"), kind = SUPERMARKET),
        Merchant("famila", "famila.de", listOf("famila"), kind = SUPERMARKET),
        Merchant("Alnatura", "alnatura.de", listOf("alnatura"), kind = SUPERMARKET),
        Merchant("denn's", "denns-biomarkt.de", listOf("denns", "denn s"), kind = SUPERMARKET),
        Merchant("Tesco", "tesco.com", listOf("tesco"), kind = SUPERMARKET),
        Merchant("Sainsbury's", "sainsburys.co.uk", listOf("sainsbury"), kind = SUPERMARKET),
        Merchant("Asda", "asda.com", listOf("asda"), kind = SUPERMARKET),
        Merchant("Waitrose", "waitrose.com", listOf("waitrose"), kind = SUPERMARKET),
        Merchant("Walmart", "walmart.com", listOf("walmart"), kind = SUPERMARKET),
        Merchant("Kroger", "kroger.com", listOf("kroger"), kind = SUPERMARKET),
        // Drugstores
        Merchant("dm", "dm.de", listOf("dm drogerie", "dm fil"), words = listOf("dm")),
        Merchant("Rossmann", "rossmann.de", listOf("rossmann")),
        Merchant("Budni", "budni.de", listOf("budni")),
        // Online shops and payments
        Merchant("Amazon", "amazon.de", listOf("amazon", "amzn")),
        Merchant("PayPal", "paypal.com", listOf("paypal")),
        Merchant("eBay", "ebay.de", listOf("ebay")),
        Merchant("Zalando", "zalando.de", listOf("zalando")),
        Merchant("Otto", "otto.de", listOf("otto gmbh", "otto versand")),
        Merchant("Apple", "apple.com", listOf("apple com", "itunes")),
        Merchant("Google", "google.com", listOf("google")),
        Merchant("Klarna", "klarna.com", listOf("klarna")),
        Merchant("Temu", "temu.com", listOf("temu")),
        Merchant("AliExpress", "aliexpress.com", listOf("aliexpress")),
        // Streaming
        Merchant("Netflix", "netflix.com", listOf("netflix")),
        Merchant("Spotify", "spotify.com", listOf("spotify")),
        Merchant("Disney+", "disneyplus.com", listOf("disney")),
        Merchant("DAZN", "dazn.com", listOf("dazn")),
        Merchant("Audible", "audible.de", listOf("audible")),
        Merchant("YouTube", "youtube.com", listOf("youtube")),
        Merchant("Sky", "sky.de", listOf("sky deutschland")),
        // Phone and internet
        Merchant("Telekom", "telekom.com", listOf("telekom")),
        Merchant("Vodafone", "vodafone.de", listOf("vodafone")),
        Merchant("O2", "o2online.de", listOf("telefonica"), words = listOf("o2")),
        Merchant("1&1", "1und1.de", listOf("1und1", "1 1 telecom", "1 1 internet")),
        Merchant("congstar", "congstar.de", listOf("congstar")),
        Merchant("freenet", "freenet.de", listOf("freenet")),
        // Travel and mobility
        Merchant("Deutsche Bahn", "bahn.de", listOf("deutsche bahn", "db fernverkehr", "db vertrieb", "db regio")),
        Merchant("Shell", "shell.com", words = listOf("shell")),
        Merchant("Aral", "aral.de", words = listOf("aral")),
        Merchant("Esso", "esso.de", words = listOf("esso")),
        Merchant("TotalEnergies", "totalenergies.de", listOf("totalenergies", "total energies")),
        Merchant("JET", "jet.de", listOf("jet tankstelle")),
        Merchant("Uber", "uber.com", words = listOf("uber")),
        Merchant("FlixBus", "flixbus.de", listOf("flixbus", "flixtrain")),
        Merchant("Lufthansa", "lufthansa.com", listOf("lufthansa")),
        Merchant("Ryanair", "ryanair.com", listOf("ryanair")),
        Merchant("Eurowings", "eurowings.com", listOf("eurowings")),
        Merchant("ADAC", "adac.de", words = listOf("adac")),
        Merchant("Sixt", "sixt.de", words = listOf("sixt")),
        // Home, electronics, clothes
        Merchant("IKEA", "ikea.com", listOf("ikea")),
        Merchant("OBI", "obi.de", words = listOf("obi")),
        Merchant("Bauhaus", "bauhaus.info", listOf("bauhaus")),
        Merchant("Hornbach", "hornbach.de", listOf("hornbach")),
        Merchant("toom", "toom.de", listOf("toom baumarkt"), words = listOf("toom")),
        Merchant("MediaMarkt", "mediamarkt.de", listOf("mediamarkt", "media markt")),
        Merchant("Saturn", "saturn.de", listOf("saturn electro"), words = listOf("saturn")),
        Merchant("Decathlon", "decathlon.de", listOf("decathlon")),
        Merchant("H&M", "hm.com", listOf("hennes mauritz"), words = listOf("h m")),
        Merchant("Primark", "primark.com", listOf("primark")),
        Merchant("TK Maxx", "tkmaxx.de", listOf("tk maxx")),
        Merchant("Deichmann", "deichmann.de", listOf("deichmann")),
        Merchant("Thalia", "thalia.de", listOf("thalia")),
        Merchant("Douglas", "douglas.de", listOf("parfumerie douglas", "douglas gmbh")),
        Merchant("Fressnapf", "fressnapf.de", listOf("fressnapf")),
        Merchant("TEDi", "tedi.com", words = listOf("tedi")),
        Merchant("Woolworth", "woolworth.de", listOf("woolworth")),
        // Eating out
        Merchant("McDonald's", "mcdonalds.com", listOf("mcdonald", "mc donald")),
        Merchant("Burger King", "burgerking.de", listOf("burger king", "burgerking")),
        Merchant("Starbucks", "starbucks.de", listOf("starbucks")),
        Merchant("Lieferando", "lieferando.de", listOf("lieferando")),
        Merchant("Wolt", "wolt.com", words = listOf("wolt")),
        Merchant("KFC", "kfc.com", words = listOf("kfc")),
        // Insurance and health insurance
        Merchant("Allianz", "allianz.de", listOf("allianz")),
        Merchant("HUK-COBURG", "huk.de", listOf("huk coburg", "huk24")),
        Merchant("AXA", "axa.de", words = listOf("axa")),
        Merchant("ERGO", "ergo.de", words = listOf("ergo")),
        Merchant("DEVK", "devk.de", words = listOf("devk")),
        Merchant("Generali", "generali.it", listOf("generali")),
        Merchant("Techniker Krankenkasse", "tk.de", listOf("techniker krankenkasse")),
        Merchant("AOK", "aok.de", words = listOf("aok")),
        Merchant("BARMER", "barmer.de", listOf("barmer")),
        Merchant("DAK-Gesundheit", "dak.de", listOf("dak gesundheit")),
        // Energy and fees
        Merchant("Vattenfall", "vattenfall.de", listOf("vattenfall")),
        Merchant("E.ON", "eon.de", words = listOf("e on", "eon")),
        Merchant("EnBW", "enbw.com", words = listOf("enbw")),
        Merchant("LichtBlick", "lichtblick.de", listOf("lichtblick")),
        Merchant("Rundfunkbeitrag", "rundfunkbeitrag.de", listOf("rundfunk ard", "beitragsservice")),
        // Fitness
        Merchant("McFit", "mcfit.com", listOf("mcfit")),
        Merchant("Urban Sports Club", "urbansportsclub.com", listOf("urban sports")),
    )

    val supermarkets = all.filter { it.kind == SUPERMARKET }

    /**
     * The merchant named first in [names], e.g. the name of a payee and of the payee it belongs to.
     * "PayPal Europe … Netflix" is PayPal, which takes the money.
     */
    fun of(vararg names: String?): Merchant? {
        val text = Words.textOf(*names)
        return all.map { it to it.indexIn(text) }
            .filter { it.second >= 0 }
            .minByOrNull { it.second }
            ?.first
    }

    /**
     * The merchant of each payee that is recognized: by its name and short name, else by the
     * payee it belongs to, e.g. "REWE" for "REWE Markt GmbH Fil. 4711"
     */
    fun ofPayees(payees: List<RawPayee>): Map<Long, Merchant> {
        val byId = payees.associateBy { it.id }
        val result = HashMap<Long, Merchant>()
        payees.forEach { payee ->
            val parent = payee.parentId?.let(byId::get)
            (of(payee.name, payee.shortName) ?: parent?.let { of(it.name, it.shortName) })
                ?.let { result[payee.id] = it }
        }
        return result
    }
}

/** Restaurants and delivery services, matched at the start of a word of payee, purpose or category */
val EATING_OUT = listOf(
    "restaurant", "gastronomie", "gaststätte", "pizzeria", "imbiss", "döner", "doener", "sushi", "burger",
    "mcdonald", "kfc", "subway", "vapiano", "lieferando", "lieferdienst", "wolt", "uber eats", "deliveroo",
    "just eat", "domino", "takeaway", "take away", "fast food", "essen gehen", "eating out", "dining"
)

/** Label to the patterns matched at the start of a word of the payee, from [Merchants] */
val SUPERMARKETS = Merchants.supermarkets.map { it.name to it.patterns }

/** The supermarket chain of a payee, null for other payees */
fun supermarketOf(payeeName: String?): String? {
    val text = Words.textOf(payeeName)
    return SUPERMARKETS.firstOrNull { (_, patterns) -> patterns.any { Words.startsWord(text, it) } }?.first
}

/**
 * A payee as the dialogs about its image show it
 *
 * @param id null for a payee that is not stored yet
 */
data class Payee(val id: Long?, val displayName: String)
