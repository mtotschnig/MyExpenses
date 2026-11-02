package org.totschnig.myexpenses.next.core.common.icons

import androidx.annotation.StringRes
import org.totschnig.myexpenses.next.core.common.R
import org.totschnig.myexpenses.next.core.model.words.Words

/**
 * An icon of Font Awesome, the font the categories take their icons from
 *
 * @param name as in [org.totschnig.myexpenses.viewmodel.data.FontAwesomeIcons]
 * @param words that the icon stands for, in German and English; one with at least
 * [IconCatalog.COMPOUND_LENGTH] letters is also found inside a word, e.g. "versicherung" in
 * "Hausratversicherung", a shorter one only at the start of a word, one starting with "=" only as
 * a whole word
 */
class CatalogIcon(val name: String, val words: List<String>)

/** Icons that belong together, shown as one filter in the picker */
class IconTheme(@param:StringRes val labelRes: Int, val icons: List<CatalogIcon>)

/**
 * The icons offered for pots, contracts and payees, chosen so that they say something about money
 * matters, with words to find them in German, which the labels of Font Awesome lack. All other
 * icons of Font Awesome can still be found by their English label, see [search].
 */
object IconCatalog {
    const val COMPOUND_LENGTH = 6

    /** [words] separated by spaces, "_" joins the words of a phrase, e.g. "ice_cream" */
    private fun i(name: String, words: String) =
        CatalogIcon(name, words.split(' ').filter { it.isNotEmpty() }.map { it.replace('_', ' ') })

    val themes: List<IconTheme> = listOf(
        IconTheme(
            R.string.next_icons_food, listOf(
                i("cart-shopping", "lebensmittel supermarkt einkauf discounter groceries supermarket"),
                i("basket-shopping", "wochenmarkt markt korb täglicher_bedarf basket market"),
                i("utensils", "essen restaurant gastro mittagessen abendessen kantine mensa dining meal"),
                i("burger", "imbiss fastfood fast_food burger snack"),
                i("pizza-slice", "pizza lieferdienst lieferando delivery takeaway"),
                i("mug-hot", "kaffee coffee café cafe tee =tea"),
                i("mug-saucer", "frühstück breakfast espresso"),
                i("bread-slice", "bäcker bäckerei brot backwaren bakery bread"),
                i("drumstick-bite", "metzger fleisch metzgerei butcher meat"),
                i("fish", "fisch fischhändler seafood fish"),
                i("cheese", "käse milchprodukte dairy cheese"),
                i("carrot", "gemüse obst bio vegetables organic"),
                i("apple-whole", "obst frucht fruit"),
                i("cookie", "süßigkeiten süßes kekse snacks sweets candy"),
                i("ice-cream", "eis eisdiele ice_cream"),
                i("cake-candles", "kuchen konditorei geburtstag torte cake birthday"),
                i("bowl-food", "asiatisch sushi bowl"),
                i("bottle-water", "getränke wasser drinks beverages water"),
                i("wine-glass", "wein restaurant wine"),
                i("beer-mug-empty", "bier kneipe =bar =bars ausgehen =pub beer"),
                i("martini-glass", "cocktail cocktails ausgehen nightlife"),
                i("champagne-glasses", "feier party sekt celebration"),
            )
        ),
        IconTheme(
            R.string.next_icons_shopping, listOf(
                i("bag-shopping", "shopping einkaufen kaufhaus bummel"),
                i("store", "laden geschäft händler einzelhandel shop retail"),
                i("shirt", "kleidung =mode textil clothing fashion clothes"),
                i("person-dress", "kleid damenmode dress"),
                i("shoe-prints", "schuhe shoes"),
                i("socks", "socken wäsche unterwäsche socks underwear"),
                i("glasses", "brille optiker glasses optician"),
                i("gem", "schmuck juwelier jewelry"),
                i("gift", "geschenk geschenke präsent gift gifts"),
                i("gifts", "weihnachten weihnachtsgeschenke christmas presents"),
                i("box", "paket versand bestellung online parcel order"),
                i("truck-fast", "lieferung versandhandel shipping"),
                i("couch", "möbel einrichtung sofa furniture"),
                i("screwdriver-wrench", "baumarkt werkzeug heimwerken diy hardware tools"),
                i("seedling", "garten gartencenter pflanzen garden plants"),
                i("pump-soap", "drogerie körperpflege kosmetik drugstore cosmetics"),
                i("spray-can-sparkles", "putzmittel reinigung cleaning"),
                i("receipt", "kassenzettel beleg quittung receipt"),
                i("cash-register", "kasse kiosk tankstellenshop checkout"),
            )
        ),
        IconTheme(
            R.string.next_icons_home, listOf(
                i("house", "wohnen haus miete wohnung zuhause =rent housing home"),
                i("house-chimney", "eigenheim nebenkosten hausgeld homeowner"),
                i("building", "wohnung vermieter hausverwaltung apartment landlord"),
                i("key", "kaution schlüssel deposit key"),
                i("bolt", "strom elektrizität stadtwerke electricity power"),
                i("fire-flame-simple", "=gas heizung heizkosten heating"),
                i("droplet", "wasser abwasser water"),
                i("temperature-half", "fernwärme heizkosten heating"),
                i("plug", "steckdose ökostrom energie energy"),
                i("lightbulb", "lampe beleuchtung licht light"),
                i("trash", "müll abfall entsorgung waste garbage"),
                i("broom", "putzhilfe reinigung haushaltshilfe cleaning"),
                i("bed", "bett schlafzimmer matratze bed"),
                i("bath", "bad badezimmer bathroom"),
                i("hammer", "handwerker reparatur renovierung repair renovation"),
                i("paint-roller", "renovierung streichen farbe malern painting"),
                i("toolbox", "werkzeug instandhaltung maintenance"),
                i("tower-broadcast", "rundfunk rundfunkbeitrag gez rundfunkgebühr broadcasting"),
            )
        ),
        IconTheme(
            R.string.next_icons_mobility, listOf(
                i("car", "auto kfz pkw wagen =car vehicle"),
                i("car-side", "leasing autokredit car_loan"),
                i("gas-pump", "tanken tankstelle kraftstoff benzin diesel sprit fuel petrol"),
                i("charging-station", "laden ladestrom e-auto elektroauto charging ev"),
                i("oil-can", "werkstatt inspektion ölwechsel garage service"),
                i("car-burst", "unfall kfz-versicherung kasko accident"),
                i("square-parking", "parken parkhaus parkplatz parking"),
                i("road", "maut autobahn vignette toll"),
                i("bus", "=bus öpnv nahverkehr deutschlandticket public_transport"),
                i("train", "bahn =zug =db fernverkehr train railway"),
                i("train-subway", "u-bahn s-bahn metro subway"),
                i("train-tram", "straßenbahn tram"),
                i("taxi", "taxi uber fahrdienst =cab =ride"),
                i("bicycle", "fahrrad =rad bike bicycle"),
                i("motorcycle", "motorrad roller scooter motorcycle"),
                i("van-shuttle", "carsharing mietwagen autovermietung car_rental"),
                i("plane", "flug flugzeug airline flight"),
                i("ship", "fähre schiff kreuzfahrt ferry cruise"),
                i("ticket", "fahrkarte ticket fahrschein"),
            )
        ),
        IconTheme(
            R.string.next_icons_health, listOf(
                i("heart-pulse", "gesundheit krankenkasse krankenversicherung health"),
                i("stethoscope", "arzt ärztin praxis doctor"),
                i("user-doctor", "facharzt behandlung physician"),
                i("tooth", "zahnarzt zahn zahnreinigung dentist dental"),
                i("eye", "augenarzt sehtest eye"),
                i("pills", "apotheke medikamente pharmacy medicine"),
                i("prescription-bottle-medical", "rezept zuzahlung prescription"),
                i("syringe", "impfung vaccination"),
                i("hospital", "krankenhaus klinik hospital"),
                i("hand-holding-medical", "pflege pflegeversicherung care nursing"),
                i("spa", "wellness massage sauna =spa"),
                i("scissors", "friseur haarschnitt hairdresser haircut barber"),
                i("dumbbell", "fitness fitnessstudio gym training sport"),
                i("person-running", "laufen joggen running"),
                i("person-swimming", "schwimmen schwimmbad hallenbad swimming pool"),
                i("person-biking", "radsport radfahren cycling"),
                i("futbol", "fußball verein sportverein football soccer club"),
                i("table-tennis-paddle-ball", "tischtennis tennis"),
                i("golf-ball-tee", "golf"),
                i("person-skiing", "ski skifahren skiing"),
                i("person-hiking", "wandern bergsport hiking"),
                i("person-praying", "yoga meditation"),
            )
        ),
        IconTheme(
            R.string.next_icons_leisure, listOf(
                i("film", "kino film filme cinema movies"),
                i("tv", "fernsehen streaming serien =tv television"),
                i("music", "musik musikstreaming music"),
                i("headphones", "hörbuch podcast audio audiobook"),
                i("radio", "radio"),
                i("guitar", "instrument musikschule musikunterricht"),
                i("microphone", "konzert konzerte karaoke concert"),
                i("masks-theater", "theater =oper kultur theatre opera culture"),
                i("ticket", "eintritt tickets veranstaltung event admission"),
                i("gamepad", "gaming spiele games konsole videospiele console"),
                i("dice", "brettspiele spieleabend board_games"),
                i("puzzle-piece", "hobby hobbys puzzle"),
                i("book", "bücher buch buchhandlung books bookstore"),
                i("book-open", "bibliothek ebook library"),
                i("newspaper", "zeitung zeitschrift magazin presse abo newspaper magazine"),
                i("camera", "foto fotografie kamera photo camera"),
                i("palette", "kunst malen basteln =art crafts"),
                i("chess", "schach verein chess"),
                i("bowling-ball", "bowling kegeln"),
                i("trophy", "wettkampf turnier tournament"),
            )
        ),
        IconTheme(
            R.string.next_icons_travel, listOf(
                i("umbrella-beach", "urlaub strand ferien vacation holiday beach"),
                i("suitcase-rolling", "reise reisen gepäck travel trip luggage"),
                i("plane-departure", "flughafen abflug flugreise airport"),
                i("hotel", "hotel unterkunft übernachtung accommodation"),
                i("bed", "ferienwohnung pension hostel"),
                i("passport", "reisepass visum passport visa"),
                i("map-location-dot", "ausflug sightseeing tour"),
                i("earth-europe", "ausland fremdwährung abroad"),
                i("campground", "camping zelt campingplatz"),
                i("caravan", "wohnmobil wohnwagen camper"),
                i("mountain", "berge alpen mountains"),
                i("snowflake", "skiurlaub winterurlaub ski_holiday"),
                i("person-walking-luggage", "geschäftsreise dienstreise business_trip"),
            )
        ),
        IconTheme(
            R.string.next_icons_family, listOf(
                i("children", "kinder kind nachwuchs children kids"),
                i("baby", "baby windeln diapers"),
                i("baby-carriage", "kinderwagen babyausstattung stroller"),
                i("child", "taschengeld kinderbetreuung pocket_money"),
                i("school", "kita kindergarten schule krippe daycare school"),
                i("people-roof", "familie haushalt family household"),
                i("hands-holding-child", "unterhalt kindesunterhalt elterngeld child_support"),
                i("paw", "haustier haustiere tierbedarf =pet =pets"),
                i("dog", "hund hundesteuer hundefutter dog"),
                i("cat", "katze katzenfutter =cat"),
                i("horse", "pferd reiten reitstall horse riding"),
                i("bone", "tierfutter futter pet_food"),
                i("house-medical", "tierarzt =vet veterinarian"),
                i("cake-candles", "kindergeburtstag geburtstag party"),
            )
        ),
        IconTheme(
            R.string.next_icons_money, listOf(
                i("wallet", "geld portemonnaie bargeld spielraum wallet cash"),
                i("money-bill-wave", "gehalt lohn einkommen salary income wage"),
                i("coins", "kleingeld münzen trinkgeld coins tips"),
                i("piggy-bank", "sparen rücklage sparziel notgroschen savings reserve"),
                i("sack-dollar", "vermögen =erbe wealth"),
                i("chart-line", "investieren depot aktien etf sparplan invest stocks"),
                i("building-columns", "bank kontoführung finanzen sparkasse volksbank banking finance"),
                i("credit-card", "kreditkarte karte kartengebühr credit_card"),
                i("hand-holding-dollar", "kredit darlehen raten ratenkauf loan installment"),
                i("house-user", "hypothek baufinanzierung immobilienkredit mortgage"),
                i("percent", "zinsen gebühren interest fees"),
                i("file-invoice-dollar", "rechnung rechnungen nebenkostenabrechnung invoice bill"),
                i("landmark", "steuer steuern finanzamt behörde amt =tax government"),
                i("scale-balanced", "anwalt rechtsschutz gericht lawyer legal"),
                i("shield-halved", "versicherung haftpflicht absicherung insurance"),
                i("shield-heart", "lebensversicherung risikoleben altersvorsorge rente life_insurance pension"),
                i("umbrella", "hausrat hausratversicherung schutz cover"),
                i("file-contract", "vertrag verträge abo abonnement contract subscription"),
                i("id-card", "mitgliedschaft mitgliedsbeitrag ausweis membership"),
                i("hand-holding-heart", "spende spenden patenschaft wohltätigkeit donation charity"),
                i("handshake", "verein gewerkschaft partei beitrag union association"),
                i("vault", "tresor schließfach safe_deposit"),
                i("arrow-right-arrow-left", "umbuchung überweisung transfer"),
            )
        ),
        IconTheme(
            R.string.next_icons_tech, listOf(
                i("mobile-screen-button", "handy mobilfunk smartphone handyvertrag mobile phone"),
                i("phone", "telefon festnetz telephone landline"),
                i("wifi", "internet dsl glasfaser wlan router broadband"),
                i("sim-card", "prepaid =sim tarif"),
                i("laptop", "computer laptop notebook elektronik electronics"),
                i("desktop", "=pc monitor"),
                i("tablet-screen-button", "tablet ipad"),
                i("cloud", "cloud speicher online-speicher software saas storage"),
                i("server", "hosting webhosting domain server"),
                i("code", "=app =apps lizenz software license"),
                i("envelope", "=post briefe porto mail postage"),
                i("at", "e-mail email mailbox"),
                i("satellite-dish", "satellit kabel kabelfernsehen satellite cable"),
                i("print", "drucker druckerpatronen printer =ink"),
                i("headset", "support kundenservice service"),
            )
        ),
        IconTheme(
            R.string.next_icons_work, listOf(
                i("briefcase", "arbeit beruf beruflich job work business"),
                i("user-tie", "selbstständig freiberuf kunde freelance client"),
                i("graduation-cap", "studium =uni universität studiengebühren bildung education university"),
                i("chalkboard-user", "kurs kurse weiterbildung seminar nachhilfe course training"),
                i("pen", "schreibwaren büro bürobedarf stationery office"),
                i("people-group", "team kollegen firma company"),
                i("helmet-safety", "baustelle handwerk construction"),
            )
        ),
        IconTheme(
            R.string.next_icons_brands, listOf(
                i("amazon", "amazon amzn prime"),
                i("paypal", "paypal"),
                i("apple", "apple icloud itunes"),
                i("google", "google"),
                i("google-pay", "google_pay"),
                i("microsoft", "microsoft office xbox"),
                i("spotify", "spotify"),
                i("deezer", "deezer"),
                i("audible", "audible hörbücher"),
                i("youtube", "youtube"),
                i("twitch", "twitch"),
                i("steam", "steam"),
                i("playstation", "playstation psn sony"),
                i("xbox", "xbox game_pass"),
                i("dropbox", "dropbox"),
                i("ebay", "ebay kleinanzeigen"),
                i("airbnb", "airbnb"),
                i("uber", "uber"),
                i("patreon", "patreon"),
                i("cc-visa", "visa"),
                i("cc-mastercard", "mastercard"),
                i("dhl", "dhl"),
                i("ups", "=ups"),
                i("fedex", "fedex"),
            )
        ),
        IconTheme(
            R.string.next_icons_other, listOf(
                i("star", "favorit besonders favorite"),
                i("heart", "partner liebe beziehung love"),
                i("calendar-days", "jährlich monatlich termin yearly monthly"),
                i("flag", "ziel sparziel goal target"),
                i("crown", "luxus gönnen treat luxury"),
                i("face-smile", "spaß =fun"),
                i("church", "kirche kirchensteuer church"),
                i("hands-praying", "religion gemeinde"),
                i("smoking", "rauchen zigaretten tabak smoking tobacco"),
                i("wine-bottle", "alkohol spirituosen alcohol liquor"),
                i("recycle", "pfand recycling deposit"),
                i("wrench", "reparatur reparaturen repair"),
                i("bell", "erinnerung reminder"),
                i("tree", "natur umwelt nature"),
                i("sun", "sommer solar photovoltaik summer"),
                i("medal", "auszeichnung prämie bonus"),
            )
        ),
    )

    /** Every icon of the catalog once, in the order of [themes] */
    val icons: List<CatalogIcon> = themes.flatMap { it.icons }.distinctBy { it.name }

    private val wordsByName: Map<String, List<String>> =
        themes.flatMap { it.icons }.groupBy({ it.name }, { it.words }).mapValues { (_, words) -> words.flatten().distinct() }

    /** Words of [name] in the catalog, empty if it is not part of it */
    fun wordsOf(name: String): List<String> = wordsByName[name].orEmpty()

    /**
     * Icons that fit [texts], e.g. the name of a pot and the labels of its categories, the icons
     * that fit an earlier text first, within one text those with the longest matching word first,
     * then in the order of the catalog
     */
    fun suggest(vararg texts: String?, limit: Int = 12): List<String> = buildList {
        texts.filterNotNull().filter { it.isNotBlank() }.forEach { text ->
            val normalized = Words.textOf(text)
            icons.mapNotNull { icon ->
                icon.words.filter { matches(normalized, it) }.maxOfOrNull { it.length }?.let { icon.name to it }
            }
                // The longest matching word is the most specific, e.g. "kfz-versicherung" before "kfz"
                .sortedByDescending { it.second }
                .forEach { (name, _) -> if (name !in this) add(name) }
        }
    }.take(limit)

    /** The first of [suggest], null if no icon fits */
    fun bestFor(vararg texts: String?): String? = suggest(*texts, limit = 1).firstOrNull()

    /**
     * Icons whose words or label contain [query]: first those of the catalog, then all others of
     * Font Awesome
     *
     * @param all names and English labels of all icons of Font Awesome
     */
    fun search(query: String, all: Map<String, String>): List<String> {
        val pattern = Words.normalize(query).trim()
        if (pattern.isEmpty()) return emptyList()
        val curated = icons.filter { icon ->
            icon.words.any { it.removePrefix("=").startsWith(pattern) || pattern.length >= 3 && pattern in it } ||
                    all[icon.name]?.let { Words.normalize(it).contains(pattern) } == true
        }.map { it.name }
        val others = all.filter { (name, label) ->
            name !in curated && (Words.normalize(label).contains(pattern) || name.contains(pattern))
        }.keys.sortedBy { all[it] }
        return curated + others
    }

    internal fun matches(text: String, word: String): Boolean = when {
        word.startsWith("=") -> Words.isWord(text, word.substring(1))
        word.length >= COMPOUND_LENGTH -> Words.contains(text, word)
        else -> Words.startsWord(text, word)
    }
}
