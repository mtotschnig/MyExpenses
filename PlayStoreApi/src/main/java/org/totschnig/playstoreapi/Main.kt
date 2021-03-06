package org.totschnig.playstoreapi

import com.google.api.services.androidpublisher.AndroidPublisher
import com.google.api.services.androidpublisher.model.InAppProduct
import com.google.api.services.androidpublisher.model.InAppProductListing
import com.google.api.services.androidpublisher.model.InappproductsListResponse
import com.google.api.services.androidpublisher.model.Listing
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import java.io.IOException
import java.security.GeneralSecurityException


object Main {
    const val APPLICATION_NAME = "MyExpensesMobi-Application/1.0"

    private const val PACKAGE_NAME = "org.totschnig.myexpenses"

    private const val SUB_SKU = "sku_professional_monthly" //sku_professional_monthly sku_professional_yearly sku_extended2professional_yearly

    const val SERVICE_ACCOUNT_EMAIL = "fastlane@api-5950718857839288276-239934.iam.gserviceaccount.com"

    private val log: Log = LogFactory.getLog(Main::class.java)
    @JvmStatic
    fun main(args: Array<String>) {
        try {
            // Create the API service.
            val service: AndroidPublisher = AndroidPublisherHelper.init()
            val inappproducts = service.inappproducts()

            val content = InAppProduct()
                    .setPackageName(PACKAGE_NAME)
                    .setSku(SUB_SKU)
                    .setPurchaseType("subscription")
                    .setListings(mapOf(
                            "en-US" to InAppProductListing()
                                    .setTitle("Professional Key")
                                    .setDescription("Purchase access to premium features.")
                                    .setBenefits(listOf("Unlock all premium features", "Email support", "Voting right on development roadmap")),
                            "ar" to InAppProductListing()
                                    .setTitle("المفتاح شامل")
                                    .setDescription("شراء الوصول إلى الميزات الكاملة.")
                                    .setBenefits(listOf("دعم البريد الإلكتروني", "حق التصويت على  مخططات التنمية")),
                            "bg" to InAppProductListing()
                                    .setTitle("Професионален ключ")
                                    .setDescription("Закупете достъп до Допълнителни функции.")
                                    .setBenefits(listOf("Поддръжка по имейл", "Право да гласувате на карта за развитие")),
                            "ca" to InAppProductListing()
                                    .setTitle("Clau professional")
                                    .setDescription("Compreu accés a funcions premium."),
                            "cs-CZ" to InAppProductListing()
                                    .setTitle("Profesionální klíč")
                                    .setDescription("Kupte si přístup k prémiovým funkcím.")
                                    .setBenefits(listOf("Emailová podpora")),
                            "da-DK" to InAppProductListing()
                                    .setTitle("Professionel Nøgle")
                                    .setDescription("Køb adgang til premium funktioner."),
                            "de-DE" to InAppProductListing()
                                    .setTitle("Professioneller Schlüssel")
                                    .setDescription("Zugriff auf Premium-Funktionen erwerben.")
                                    .setBenefits(listOf("E-Mail-Support", "Stimmrecht zum Entwicklungsfahrplan")),
                            "el-GR" to InAppProductListing()
                                    .setTitle("Επαγγελματικό κλειδί")
                                    .setDescription("Αγοράστε πρόσβαση σε αναβαθμισμένα χαρακτηριστικά.")
                                    .setBenefits(listOf("Υποστήριξη ηλεκτρονικού ταχυδρομείου", "Δικαίωμα ψήφου για τον χάρτη πορείας")),
                            "es-ES" to InAppProductListing()
                                    .setTitle("Llave Profesional")
                                    .setDescription("Comprar acceso a las características Premium.")
                                    .setBenefits(listOf("Soporte por correo electrónico", "Participar en la ruta de desarrollo")),
                            "eu-ES" to InAppProductListing()
                                    .setTitle("Gako profesionala")
                                    .setDescription("Erosi sarbidea premium ezaugarrietara.")
                                    .setBenefits(listOf("E-mail bidezko laguntza", "Garapen ibilbidean bozkatzeko aukera")),
                            "fr-FR" to InAppProductListing()
                                    .setTitle("Clé professionnelle")
                                    .setDescription("Acheter l'accès aux fonctions Premium.")
                                    .setBenefits(listOf("Assistance par e-mail", "Voter sur la feuille de route")),
                            "hr" to InAppProductListing()
                                    .setTitle("Profesionalni ključ")
                                    .setDescription("Kupite pristup premium značajkama."),
                            "hu-HU" to InAppProductListing()
                                    .setTitle("Szakmai kulcs")
                                    .setDescription("Vásároljon hozzáférést a prémium funkciókhoz."),
                            "it-IT" to InAppProductListing()
                                    .setTitle("Licenza Professionale")
                                    .setDescription("Acquistare l'accesso a funzionalità premium.")
                                    .setBenefits(listOf("Supporto email", "Votare sulla roadmap di sviluppo")),
                            "iw-IL" to InAppProductListing()
                                    .setTitle("מפתח מקצועי")
                                    .setDescription("רכישת גישה לתכונות פרמיום מורחבות.")
                                    .setBenefits(listOf("שלח תמיכה", "זכות הצבעה על מפת דרכים לפיתוח")),
                            "ja-JP" to InAppProductListing()
                                    .setTitle("プロフェッショナルキー")
                                    .setDescription("プレミアムと拡張機能への購入アクセス。")
                                    .setBenefits(listOf("メールサポート", "開発ロードマップ の投票権")),
                            "km-KH" to InAppProductListing()
                                    .setTitle("គន្លឹះវិជ្ជាជីវៈ")
                                    .setDescription("ទិញការចូលប្រើលក្ខណៈពិសេស។"),
                            "kn-IN" to InAppProductListing()
                                    .setTitle("ವೃತ್ತಿಪರ ಕೀ")
                                    .setDescription("ಪ್ರೀಮಿಯಂ ವೈಶಿಷ್ಟ್ಯಗಳಿಗೆ ಪ್ರವೇಶವನ್ನು ಖರೀದಿಸಿ.")
                                    .setBenefits(listOf("ಮೇಲ್ ಬೆಂಬಲ", "ಅಭಿವೃದ್ಧಿ ಮಾರ್ಗಸೂಚಿಯಲ್ಲಿ ಮತದಾನ")),
                            "ko-KR" to InAppProductListing()
                                    .setTitle("프로페셔널 키")
                                    .setDescription("프리미엄 기능에 대한 액세스를 구입하십시오.")
                                    .setBenefits(listOf("이메일 지원", "개발 로드맵에 바로 투표")),
                            "ms" to InAppProductListing()
                                    .setTitle("Kunci profesional")
                                    .setDescription("Beli capaian ke fitur premium.")
                                    .setBenefits(listOf("Sokongan emel", "Hak mengundi dalam peta jalan")),
                            "pl-PL" to InAppProductListing()
                                    .setTitle("Profesjonalny Klucz")
                                    .setDescription("Kup dostęp do funkcji premium.")
                                    .setBenefits(listOf("Wysyłaj pocztę e-mail", "Głosowanie na mapie drogowej rozwoju")),
                            "pt-PT" to InAppProductListing()
                                    .setTitle("Chave Profissional")
                                    .setDescription("Adquira acesso a recursos premium.")
                                    .setBenefits(listOf("Suporte por e-mail", "Direito de voto no roteiro")),
                            "ro" to InAppProductListing()
                                    .setTitle("Cheie profesională")
                                    .setDescription("Achiziționați acces la funcții premium."),
                            "ru-RU" to InAppProductListing()
                                    .setTitle("Professional-ключ")
                                    .setDescription("Приобретение дает доступ к премиум функциям.")
                                    .setBenefits(listOf("E-mail поддержка", "Право голоса на дорожной карте")),
                            "si-LK" to InAppProductListing()
                                    .setTitle("වෘත්තීය යතුර")
                                    .setDescription("වාරික විශේෂාංග සඳහා ප්‍රවේශය මිලදී ගන්න."),
                            "ta-IN" to InAppProductListing()
                                    .setTitle("நிபுணத்துவ திறவுகோல்")
                                    .setDescription("மதிப்பு அம்சங்களுக்கான நுழைவுரிமை வாங்குதல்")
                                    .setBenefits(listOf("ஞ்சல் ஆதரவு", "வளர்ச்சித் திட்டத்தில் வாக்களியுங்கள்")),
                            "tr-TR" to InAppProductListing()
                                    .setTitle("Profesyonel Anahtar")
                                    .setDescription("Premium (ödemeli) işlevlere erişim satın al.")
                                    .setBenefits(listOf("e-posta desteği", "Geliştirme yol haritasına oy verin")),
                            "vi" to InAppProductListing()
                                    .setTitle("Khoá Cao cấp")
                                    .setDescription("Nâng cấp để dùng các tính năng trả phí.")
                                    .setBenefits(listOf("Hỗ trợ qua email", "Bỏ phiếu về phát triển ứng dụng")),
                            "zh-TW" to InAppProductListing()
                                    .setTitle("專業版金鑰")
                                    .setDescription("購買使用高級功能的權限。")
            ))

            inappproducts.patch(PACKAGE_NAME, SUB_SKU, content).execute()

        } catch (ex: IOException) {
            log.error("Exception was thrown while updating listing", ex)
        } catch (ex: GeneralSecurityException) {
            log.error("Exception was thrown while updating listing", ex)
        }
    }
}