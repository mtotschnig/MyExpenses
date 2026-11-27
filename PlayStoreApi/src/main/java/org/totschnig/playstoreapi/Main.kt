package org.totschnig.playstoreapi

import com.google.api.services.androidpublisher.AndroidPublisher
import com.google.api.services.androidpublisher.model.OneTimeProduct
import com.google.api.services.androidpublisher.model.OneTimeProductListing
import com.google.api.services.androidpublisher.model.Subscription
import com.google.api.services.androidpublisher.model.SubscriptionListing
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element

const val APPLICATION_NAME = "MyExpensesMobi-Application/1.0"
private const val PACKAGE_NAME = "org.totschnig.myexpenses"

// Configured list of target languages (populated from resConfig in root build.gradle)
val TARGET_LANGUAGES = listOf(
    "en", "ar", "bg", "bn", "ca", "cs", "da", "de", "el", "es",
    "eu", "fr", "hr", "hu", "in", "it", "iw", "ja", "km", "kn", "ko", "ms",
    "nl", "pl", "pt-rBR", "pt-rPT", "ro", "ru", "si", "sv", "ta", "te", "th", "tr", "uk",
    "ur", "vi", "zh-rCN", "zh-rHK", "zh-rTW"
)

val ALL_SUBSCRIPTION_SKUS = listOf(
    "sku_professional_yearly",
    "sku_professional_monthly"
)

val ALL_IN_APP_SKUS = listOf(
    "sku_premium",
    "sku_extended",
    "sku_premium2extended",
    "categorytree",
    "csvimport",
    "distribution",
    "adfree",
    "accountsunlimited",
    "splittemplate",
    "history",
    "budget",
    "ocr",
    "webui",
    "plansunlimited",
    "print",
    "splittransaction",
    "synchronization"
)

private val RES_CONFIG_TO_PLAY_LOCALE_MAP = mapOf(
    "en" to "en-US",
    "en-rGB" to "en-GB",
    "bn" to "bn-BD",
    "cs" to "cs-CZ",
    "da" to "da-DK",
    "de" to "de-DE",
    "de-rDE" to "de-DE",
    "el" to "el-GR",
    "es" to "es-ES",
    "eu" to "eu-ES",
    "fr" to "fr-FR",
    "fr-rCA" to "fr-CA",
    "hu" to "hu-HU",
    "in" to "id",
    "it" to "it-IT",
    "iw" to "iw-IL",
    "ja" to "ja-JP",
    "km" to "km-KH",
    "kn" to "kn-IN",
    "ko" to "ko-KR",
    "nl" to "nl-NL",
    "pl" to "pl-PL",
    "pt-rBR" to "pt-BR",
    "pt-rPT" to "pt-PT",
    "ru" to "ru-RU",
    "si" to "si-LK",
    "sv" to "sv-SE",
    "ta" to "ta-IN",
    "te" to "te-IN",
    "tr" to "tr-TR",
    "zh-rCN" to "zh-CN",
    "zh-rHK" to "zh-HK",
    "zh-rTW" to "zh-TW"
)

fun mapResConfigToPlayLocale(resConfigTag: String): String =
    RES_CONFIG_TO_PLAY_LOCALE_MAP[resConfigTag] ?: resConfigTag.replace("-r", "-")

fun locateCatalogDir(): File {
    val candidates = listOf(
        File("business/ProductCatalog"),
        File("../business/ProductCatalog"),
        File("../../business/ProductCatalog")
    )
    return candidates.firstOrNull { it.isDirectory }
        ?: error("Could not locate business/ProductCatalog directory.")
}

fun resolveCatalogDirForTag(catalogDir: File, tag: String): File? {
    val directName = if (tag == "en") "values" else "values-$tag"
    val directDir = File(catalogDir, directName)
    if (directDir.exists() && directDir.isDirectory) return directDir

    if (tag.contains("-r")) {
        val baseTag = tag.substringBefore("-r")
        val baseName = if (baseTag == "en") "values" else "values-$baseTag"
        val baseDir = File(catalogDir, baseName)
        if (baseDir.exists() && baseDir.isDirectory) return baseDir
    }

    val defaultDir = File(catalogDir, "values")
    return if (defaultDir.exists() && defaultDir.isDirectory) defaultDir else null
}

fun parseCatalog(
    catalogDir: File = locateCatalogDir(),
    enabledLanguages: List<String> = TARGET_LANGUAGES
): Map<String, Map<String, String>> {
    val result = mutableMapOf<String, MutableMap<String, String>>()
    val factory = DocumentBuilderFactory.newInstance()
    factory.isExpandEntityReferences = false
    val builder = factory.newDocumentBuilder()

    enabledLanguages.forEach { tag ->
        val playLocale = mapResConfigToPlayLocale(tag)
        val targetDir = resolveCatalogDirForTag(catalogDir, tag)
        if (targetDir != null) {
            val stringsXml = File(targetDir, "strings.xml")
            if (stringsXml.exists()) {
                try {
                    val doc = builder.parse(stringsXml)
                    val stringNodes = doc.getElementsByTagName("string")
                    val map = result.getOrPut(playLocale) { mutableMapOf() }
                    for (i in 0 until stringNodes.length) {
                        val node = stringNodes.item(i) as Element
                        val name = node.getAttribute("name")
                        val text = node.textContent.trim()
                        if (name.isNotEmpty() && text.isNotEmpty()) {
                            map[name] = text
                        }
                    }
                } catch (e: Exception) {
                    println(
                        "Warning: Failed to parse ${stringsXml.absolutePath} for tag '$tag': ${e.message}"
                    )
                }
            }
        }
    }
    return result
}

fun parseSelectedSkus(args: Array<String>): List<String> {
    val prefixArg = args.firstOrNull { it.startsWith("--skus=") || it.startsWith("--sku=") }
    if (prefixArg != null) {
        val raw = prefixArg.substringAfter("=")
        return raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }
    val index = args.indexOfFirst { it == "--skus" || it == "--sku" }
    if (index != -1) {
        require(index + 1 < args.size && !args[index + 1].startsWith("--")) {
            "--skus/--sku requires a value"
        }
        return args[index + 1].split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }
    return emptyList()
}

class Main {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val selectedSkus = parseSelectedSkus(args)
            if (selectedSkus.isEmpty()) {
                println("No SKUs specified. Specify SKUs to update using --skus=<sku1,sku2,...>")
                println("Available Subscriptions: ${ALL_SUBSCRIPTION_SKUS.joinToString()}")
                println("Available In-App Products: ${ALL_IN_APP_SKUS.joinToString()}")
                println("Add --dry-run for a dry run.")
                return
            }

            val targetSubSkus = ALL_SUBSCRIPTION_SKUS.filter { selectedSkus.contains(it) }
            val targetInAppSkus = ALL_IN_APP_SKUS.filter { selectedSkus.contains(it) }

            val unknownSkus = selectedSkus.filter {
                !ALL_SUBSCRIPTION_SKUS.contains(it) && !ALL_IN_APP_SKUS.contains(it)
            }
            if (unknownSkus.isNotEmpty()) {
                println(
                    "Warning: The following requested SKUs are not recognized: ${unknownSkus.joinToString()}"
                )
            }

            val dryRun = args.contains("--dry-run") || args.contains("--dump")
            val catalogData = parseCatalog()
            println("Parsed ProductCatalog for ${catalogData.size} target locales.")

            if (dryRun) {
                println("=== DRY RUN / DUMP MODE ===")
                dumpParsedListings(catalogData, targetInAppSkus, targetSubSkus)
                return
            }

            println("=== UPDATING PLAY STORE VIA GOOGLE PLAY DEVELOPER API ===")
            val failedSkus = mutableListOf<String>()

            if (targetSubSkus.isNotEmpty()) {
                failedSkus.addAll(updateSubscriptions(catalogData, targetSubSkus))
            }
            if (targetInAppSkus.isNotEmpty()) {
                failedSkus.addAll(updateInAppProducts(catalogData, targetInAppSkus))
            }

            if (failedSkus.isNotEmpty()) {
                error("Failed to update ${failedSkus.size} SKU(s): ${failedSkus.joinToString()}")
            }
        }
    }
}

fun dumpParsedListings(
    catalogData: Map<String, Map<String, String>>,
    targetInAppSkus: List<String> = ALL_IN_APP_SKUS,
    targetSubSkus: List<String> = ALL_SUBSCRIPTION_SKUS
) {
    if (targetSubSkus.isNotEmpty()) {
        println("\n--- SUBSCRIPTION LISTINGS (${targetSubSkus.joinToString()}) ---")
        val subListings = buildSubscriptionListings(catalogData)
        subListings.forEach { sub ->
            println(
                """[${sub.languageCode}]
                    |Title: "${sub.title}"
                    |Desc: "${sub.description}"
                    |Benefits: ${sub.benefits}"""
                    .trimMargin()
            )
        }
    }

    if (targetInAppSkus.isNotEmpty()) {
        println("\n--- IN-APP PRODUCT LISTINGS ---")
        targetInAppSkus.forEach { sku ->
            val listings = buildOneTimeProductListings(sku, catalogData)
            println("SKU '$sku' (${listings.size} locales):")
            listings.forEach { listing ->
                println(
                    """[${listing.languageCode}]
                        |Title: "${listing.title}"
                        |Desc: "${listing.description}""""
                        .trimMargin()
                )
            }
        }
    }
}

fun buildSubscriptionListings(catalogData: Map<String, Map<String, String>>) =
    catalogData.mapNotNull { (locale, strings) ->
        val b1 = strings["professional_benefit_1"]
        val b2 = strings["professional_benefit_2"]
        val b3 = strings["professional_benefit_3"]
        val b4 = strings["professional_benefit_4"]
        val desc = strings["professional_description"]
        val title = strings["professional_title"] ?: "Professional Key"

        val benefits = listOfNotNull(b1, b2, b3, b4)
        if (benefits.isNotEmpty()) {
            SubscriptionListing()
                .setLanguageCode(locale)
                .setTitle(title)
                .setDescription(desc)
                .setBenefits(benefits)
        } else {
            null
        }
    }

fun buildOneTimeProductListings(
    sku: String,
    catalogData: Map<String, Map<String, String>>
): List<OneTimeProductListing> {
    val titleKey = "${sku}_title"
    val descKey = "${sku}_description"
    return catalogData.mapNotNull { (locale, strings) ->
        val title = strings[titleKey]
        val desc = strings[descKey]
        if (title != null && desc != null) {
            OneTimeProductListing()
                .setLanguageCode(locale)
                .setTitle(title)
                .setDescription(desc)
        } else {
            null
        }
    }
}

fun updateSubscriptions(
    catalogData: Map<String, Map<String, String>>,
    targetSubSkus: List<String> = ALL_SUBSCRIPTION_SKUS
): List<String> {
    val service: AndroidPublisher = AndroidPublisherHelper.init()
    val subs = service.monetization().subscriptions()
    val listings = buildSubscriptionListings(catalogData)

    val failedSkus = mutableListOf<String>()
    if (listings.isEmpty()) {
        println("Error updating subscriptions: no localized listings found in ProductCatalog.")
        failedSkus.addAll(targetSubSkus)
    } else {
        targetSubSkus.forEach { sku ->
            try {
                println(
                    "Patching subscription '$sku' with ${listings.size} localized listings..."
                )
                subs.patch(
                    PACKAGE_NAME,
                    sku,
                    Subscription().setListings(listings)
                ).also {
                    it.updateMask = "listings"
                    it.regionsVersionVersion = "2022/02"
                }.execute()
                println("Subscription $sku updated successfully.")
            } catch (e: Exception) {
                println("Error updating subscription $sku: ${e.message}")
                failedSkus.add(sku)
            }
        }
    }
    return failedSkus
}

fun updateInAppProducts(
    catalogData: Map<String, Map<String, String>>,
    targetSkus: List<String> = ALL_IN_APP_SKUS
): List<String> {
    val service: AndroidPublisher = AndroidPublisherHelper.init()
    val oneTimeProducts = service.monetization().onetimeproducts()
    val failedSkus = mutableListOf<String>()

    targetSkus.forEach { sku ->
        val listings = buildOneTimeProductListings(sku, catalogData)
        if (listings.isEmpty()) {
            println("Error updating one-time product $sku: no localized listings found in ProductCatalog.")
            failedSkus.add(sku)
            return@forEach
        }

        val content = OneTimeProduct()
            .setPackageName(PACKAGE_NAME)
            .setProductId(sku)
            .setListings(listings)

        try {
            println(
                "Patching one-time product '$sku' with ${listings.size} localized listings..."
            )
            oneTimeProducts.patch(PACKAGE_NAME, sku, content).also {
                it.updateMask = "listings"
                it.regionsVersionVersion = "2022/02"
            }.execute()
            println("One-time product '$sku' updated successfully.")
        } catch (e: Exception) {
            println("Error updating one-time product $sku: ${e.message}")
            failedSkus.add(sku)
        }
    }
    return failedSkus
}
