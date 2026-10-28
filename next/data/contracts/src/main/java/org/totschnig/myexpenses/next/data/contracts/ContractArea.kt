package org.totschnig.myexpenses.next.data.contracts

/**
 * Category of a contract, shown to the user as "area" ("Bereich"). Each has its own tab on the contracts
 * screen; contracts without one are only listed under all contracts.
 *
 * Called area in code, to not confuse it with the categories of transactions.
 */
sealed interface ContractArea {
    /** Stable key, used to store the choice of the user */
    val key: String
}

/**
 * Categories that come with the app. They are suggested from the category of the transactions
 * of a contract.
 *
 * @param keywords matched anywhere in a part of the category path, e.g. "versicherung" in "Kfz-Versicherung"
 * @param prefixes matched at the start of a word, e.g. "strom" in "Stromkosten"
 * @param words matched as whole word only, for short words like "tv" or "gas"
 */
enum class BuiltInArea(
    private val keywords: List<String> = emptyList(),
    private val prefixes: List<String> = emptyList(),
    private val words: List<String> = emptyList(),
) : ContractArea {
    INSURANCE(
        keywords = listOf("versicherung", "insurance"),
        prefixes = listOf("haftpflicht")
    ),
    HOUSING(
        prefixes = listOf(
            "wohn", "haushalt", "miete", "nebenkosten", "strom", "heizung", "wasser", "rundfunk",
            "housing", "household", "rent", "utilit", "electricity", "heating", "water"
        ),
        words = listOf("gas")
    ),
    TELECOM(
        prefixes = listOf(
            "internet", "telefon", "mobilfunk", "handy", "festnetz", "telekommunikation", "kommunikation",
            "phone", "telecom", "communication"
        ),
        words = listOf("dsl", "mobile")
    ),
    STREAMING(
        prefixes = listOf("streaming", "fernseh", "video", "television"),
        words = listOf("tv")
    ),
    FITNESS(
        prefixes = listOf("fitness", "sport", "gym", "yoga", "schwimm")
    ),
    MOBILITY(
        prefixes = listOf(
            "mobilität", "auto", "kfz", "fahrzeug", "leasing", "öpnv", "nahverkehr", "bahn", "ticket",
            "carsharing", "parken", "mobility", "vehicle", "transport", "parking"
        ),
        words = listOf("car", "cars")
    ),
    FINANCE(
        prefixes = listOf(
            "kredit", "darlehen", "finanz", "bank", "kontoführung", "tilgung", "hypothek",
            "loan", "credit", "mortgage", "finance"
        )
    ),
    SOFTWARE(
        prefixes = listOf("software", "cloud", "computer", "hosting", "lizenz", "license"),
        words = listOf("app", "apps")
    ),
    PRESS(
        prefixes = listOf("zeitung", "zeitschrift", "magazin", "presse", "newspaper", "news")
    ),
    MEMBERSHIP(
        prefixes = listOf("verein", "mitglied", "gewerkschaft", "partei", "club", "membership", "union")
    ),
    DONATIONS(
        prefixes = listOf("spende", "patenschaft", "donation", "charity")
    ),
    EDUCATION(
        prefixes = listOf(
            "kinder", "kita", "schule", "musikschule", "bildung", "kurs", "unterricht", "nachhilfe", "studium",
            "education", "school", "tuition", "childcare"
        )
    );

    override val key: String get() = name

    private fun matches(part: String): Boolean {
        if (keywords.any { it in part }) return true
        val partWords = part.split(NON_LETTERS).filter { it.isNotEmpty() }
        return partWords.any { word -> prefixes.any { word.startsWith(it) } || word in words }
    }

    companion object {
        private val NON_LETTERS = Regex("[^\\p{L}]+")

        /**
         * Insurance first, so that e.g. "Kfz-Versicherung" is not taken as mobility,
         * housing last, since it is the broadest.
         */
        private val MATCH_ORDER = listOf(INSURANCE) + entries.filter { it != INSURANCE && it != HOUSING } + HOUSING

        /**
         * Suggests a category from the category path of a transaction, e.g. "Wohnen > Internet & Telefon".
         * The most specific part of the path wins, so this example counts as [TELECOM], not [HOUSING].
         * Category names are chosen by the user and depend on the language of the setup,
         * so this is only a best guess the user can correct.
         */
        fun suggest(categoryPath: String?): BuiltInArea? =
            categoryPath?.lowercase()?.split(CATEGORY_SEPARATOR)?.asReversed()?.firstNotNullOfOrNull { part ->
                MATCH_ORDER.firstOrNull { it.matches(part) }
            }

        fun fromKey(key: String) = entries.find { it.key == key }

        private const val CATEGORY_SEPARATOR = '>'
    }
}

/**
 * Category created by the user
 */
data class CustomArea(val id: String, val name: String) : ContractArea {
    override val key: String get() = "$KEY_PREFIX$id"

    companion object {
        const val KEY_PREFIX = "custom:"
    }
}

/**
 * Choice of the user for the category of a contract
 */
sealed interface AreaChoice {
    /** Use the category suggested by the category of the transactions */
    data object Automatic : AreaChoice

    /** @param area null for no category */
    data class Fixed(val area: ContractArea?) : AreaChoice
}

/** The icon of the catalog for a built-in area */
val BuiltInArea.icon: String
    get() = when (this) {
        BuiltInArea.INSURANCE -> "shield-halved"
        BuiltInArea.HOUSING -> "house"
        BuiltInArea.TELECOM -> "wifi"
        BuiltInArea.STREAMING -> "tv"
        BuiltInArea.FITNESS -> "dumbbell"
        BuiltInArea.MOBILITY -> "car"
        BuiltInArea.FINANCE -> "building-columns"
        BuiltInArea.SOFTWARE -> "cloud"
        BuiltInArea.PRESS -> "newspaper"
        BuiltInArea.MEMBERSHIP -> "id-card"
        BuiltInArea.DONATIONS -> "hand-holding-heart"
        BuiltInArea.EDUCATION -> "graduation-cap"
    }
