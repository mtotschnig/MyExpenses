package org.totschnig.playstoreapi

import com.google.api.services.androidpublisher.AndroidPublisher
import com.google.api.services.androidpublisher.model.InAppProduct
import com.google.api.services.androidpublisher.model.InAppProductListing
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import java.io.IOException
import java.security.GeneralSecurityException

class Main {
    companion object {
        const val APPLICATION_NAME = "MyExpensesMobi-Application/1.0"

        private const val PACKAGE_NAME = "org.totschnig.myexpenses"

        private const val SKU = "categorytree" //sku_professional_monthly sku_professional_yearly sku_extended2professional_yearly

        private val categorytree: Map<String, Pair<String, String>> = mapOf(
            "en-US" to ("Category Tree" to "Arbitrarily deep category hierarchy"),
            "bg" to ("Дърво на категориите" to "Произволно дълбока йерархия на категориите"),
            "de-DE" to ("Kategorienbaum" to "Beliebig tiefe Kategorienhierarchie"),
            "es-ES" to ("Árbol de categorías" to "Jerarquía de categorías arbitraria"),
            "fr-FR" to ("Arborescence des catégories" to "Une hiérarchie de catégories arbitrairement profonde"),
            "hr" to ("Stablo kategorija" to "Proizvoljno duboka hijerarhija kategorija"),
            "it-IT" to ("Albero delle categorie" to "Gerarchia di categorie arbitrariamente profonda"),
            "iw-IL" to ("עץ קטגוריות" to "היררכיית קטגוריה עמוקה שרירותית"),
            "kn-IN" to ("ವರ್ಗ ಮರ" to "ನಿರಂಕುಶವಾಗಿ ಆಳವಾದ ವರ್ಗ ಕ್ರಮಾನುಗತ"),
            "pt-PT" to ("Árvore de categorias" to "Hierarquia de categorias arbitrariamente profunda"),
            "te-IN" to ("వర్గం చెట్టు" to "ఏకపక్షంగా లోతైన వర్గం సోపానక్రమం"),
            "tr-TR" to ("Kategori ağacı" to "İstenilen derinlikte kategori hiyerarşisi"),
            "zh-CN" to ("类别树" to "任意深的类别层次结构")
        )

        const val SERVICE_ACCOUNT_EMAIL =
            "fastlane@api-5950718857839288276-239934.iam.gserviceaccount.com"

        private val log: Log = LogFactory.getLog(Main::class.java)

        @JvmStatic
        fun main(args: Array<String>) {
            try {
                // Create the API service.
                val service: AndroidPublisher = AndroidPublisherHelper.init()
                val inappproducts = service.inappproducts()

                val content = InAppProduct()
                    .setPackageName(PACKAGE_NAME)
                    .setSku(SKU)
                    .setPurchaseType("managedUser")
                    .setListings(
                        buildMap {
                            categorytree.forEach {
                                put(
                                    it.key,
                                    InAppProductListing().setTitle(it.value.first)
                                        .setDescription(it.value.second)
                                )
                            }
                        }
                    )

                inappproducts.patch(PACKAGE_NAME, SKU, content).execute()

            } catch (ex: IOException) {
                log.error("Exception was thrown while updating listing", ex)
            } catch (ex: GeneralSecurityException) {
                log.error("Exception was thrown while updating listing", ex)
            }
        }
    }
}