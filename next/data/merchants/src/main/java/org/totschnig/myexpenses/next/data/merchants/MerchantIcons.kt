package org.totschnig.myexpenses.next.data.merchants

/**
 * Icons of the [org.totschnig.myexpenses.next.icons.IconCatalog] for [Merchants], shown without
 * consent to load logos, so that nothing is sent: the brand where Font Awesome has one, else what
 * the merchant is for, e.g. a petrol pump for Shell
 */
object MerchantIcons {
    private val byName = mapOf(
        // Supermarkets and drugstores
        "REWE" to "cart-shopping", "EDEKA" to "cart-shopping", "ALDI" to "cart-shopping", "Lidl" to "cart-shopping",
        "Netto" to "cart-shopping", "Penny" to "cart-shopping", "Kaufland" to "cart-shopping", "tegut" to "cart-shopping",
        "Norma" to "cart-shopping", "Globus" to "cart-shopping", "famila" to "cart-shopping", "Alnatura" to "carrot",
        "denn's" to "carrot", "Tesco" to "cart-shopping", "Sainsbury's" to "cart-shopping", "Asda" to "cart-shopping",
        "Waitrose" to "cart-shopping", "Walmart" to "cart-shopping", "Kroger" to "cart-shopping",
        "dm" to "pump-soap", "Rossmann" to "pump-soap", "Budni" to "pump-soap",
        // Online shops and payments
        "Amazon" to "amazon", "PayPal" to "paypal", "eBay" to "ebay", "Zalando" to "shirt", "Otto" to "box",
        "Apple" to "apple", "Google" to "google", "Klarna" to "credit-card", "Temu" to "box", "AliExpress" to "box",
        // Streaming
        "Netflix" to "tv", "Spotify" to "spotify", "Disney+" to "tv", "DAZN" to "tv", "Audible" to "audible",
        "YouTube" to "youtube", "Sky" to "satellite-dish",
        // Phone and internet
        "Telekom" to "wifi", "Vodafone" to "mobile-screen-button", "O2" to "mobile-screen-button", "1&1" to "wifi",
        "congstar" to "mobile-screen-button", "freenet" to "mobile-screen-button",
        // Travel and mobility
        "Deutsche Bahn" to "train", "Shell" to "gas-pump", "Aral" to "gas-pump", "Esso" to "gas-pump",
        "TotalEnergies" to "gas-pump", "JET" to "gas-pump", "Uber" to "uber", "FlixBus" to "bus",
        "Lufthansa" to "plane", "Ryanair" to "plane", "Eurowings" to "plane", "ADAC" to "car", "Sixt" to "van-shuttle",
        // Home, electronics, clothes
        "IKEA" to "couch", "OBI" to "screwdriver-wrench", "Bauhaus" to "screwdriver-wrench",
        "Hornbach" to "screwdriver-wrench", "toom" to "screwdriver-wrench", "MediaMarkt" to "laptop",
        "Saturn" to "laptop", "Decathlon" to "futbol", "H&M" to "shirt", "Primark" to "shirt", "TK Maxx" to "shirt",
        "Deichmann" to "shoe-prints", "Thalia" to "book", "Douglas" to "spray-can-sparkles", "Fressnapf" to "paw",
        "TEDi" to "store", "Woolworth" to "store",
        // Eating out
        "McDonald's" to "burger", "Burger King" to "burger", "Starbucks" to "mug-hot", "Lieferando" to "pizza-slice",
        "Wolt" to "pizza-slice", "KFC" to "drumstick-bite",
        // Insurance and health insurance
        "Allianz" to "shield-halved", "HUK-COBURG" to "shield-halved", "AXA" to "shield-halved",
        "ERGO" to "shield-halved", "DEVK" to "shield-halved", "Generali" to "shield-halved",
        "Techniker Krankenkasse" to "heart-pulse", "AOK" to "heart-pulse", "BARMER" to "heart-pulse",
        "DAK-Gesundheit" to "heart-pulse",
        // Energy and fees
        "Vattenfall" to "bolt", "E.ON" to "bolt", "EnBW" to "bolt", "LichtBlick" to "bolt",
        "Rundfunkbeitrag" to "tower-broadcast",
        // Fitness
        "McFit" to "dumbbell", "Urban Sports Club" to "dumbbell",
    )

    /** The icon of [merchant], null if it has none */
    fun of(merchant: Merchant): String? = byName[merchant.name]
}
