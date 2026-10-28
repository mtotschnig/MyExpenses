package org.totschnig.myexpenses.next.data.merchants

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

/**
 * The logos of [Merchants], loaded once per domain and kept as files in [dir]. A domain the icon
 * service has no usable logo for is remembered for [RETRY_AFTER_MS], so that it is not asked
 * again on every start. Asking only with consent is up to the caller, see [MerchantLogos].
 * The files are not part of backups.
 *
 * @param fetch the icon of a domain, [Fetched.Missing] if there is none, [Fetched.Failed] if it
 * could not be asked, e.g. without network
 * @param prepare a logo of the size shown from what [fetch] returned, null if it is unusable
 */
class MerchantLogoStore(
    private val dir: File,
    private val fetch: suspend (domain: String) -> Fetched = ::fetchFromDuckDuckGo,
    private val prepare: (ByteArray) -> ByteArray? = ::preparePng,
    private val now: () -> Long = System::currentTimeMillis,
) {
    sealed interface Fetched {
        class Icon(val bytes: ByteArray) : Fetched
        data object Missing : Fetched
        data object Failed : Fetched
    }

    /**
     * The prepared logo of [domain], loaded if not stored yet, null if there is none. Callers ask
     * once per domain, see [MerchantLogos].
     *
     * @param retry asks again also for a domain that had no logo recently, when the user asks for it
     */
    suspend fun logo(domain: String, retry: Boolean = false): ByteArray? = withContext(Dispatchers.IO) {
        val logo = File(dir, "$domain.png")
        val missing = File(dir, "$domain.missing")
        when {
            logo.exists() -> logo.readBytes()
            !retry && missing.exists() && now() - missing.lastModified() < RETRY_AFTER_MS -> null
            else -> when (val fetched = fetch(domain)) {
                is Fetched.Icon -> prepare(fetched.bytes)?.also {
                    dir.mkdirs()
                    logo.writeBytes(it)
                    missing.delete()
                } ?: null.also { markMissing(missing) }
                Fetched.Missing -> null.also { markMissing(missing) }
                Fetched.Failed -> null
            }
        }
    }

    private fun markMissing(file: File) {
        dir.mkdirs()
        file.writeText("")
        file.setLastModified(now())
    }

    /**
     * Removes the logos, when the user withdraws the consent, except those of [keep], which the
     * user asked for explicitly
     */
    suspend fun clear(keep: Set<String> = emptySet()) {
        withContext(Dispatchers.IO) {
            val kept = keep.mapTo(HashSet()) { "$it.png" }
            dir.listFiles()?.filterNot { it.name in kept }?.forEach { it.deleteRecursively() }
        }
    }

    companion object {
        val RETRY_AFTER_MS = TimeUnit.DAYS.toMillis(30)

        /** Smaller icons look blurred on the tile, the monogram looks better */
        const val MIN_SIZE = 32

        /** The size of the large tile in the details on a dense screen */
        const val MAX_SIZE = 128
    }
}

/**
 * Asks the icon service of DuckDuckGo, which answers 404 with a placeholder for domains without
 * an icon. Only the domain is sent.
 */
private suspend fun fetchFromDuckDuckGo(domain: String): MerchantLogoStore.Fetched = withContext(Dispatchers.IO) {
    try {
        val connection = URL("https://icons.duckduckgo.com/ip3/$domain.ico").openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            when (connection.responseCode) {
                HttpURLConnection.HTTP_OK -> MerchantLogoStore.Fetched.Icon(connection.inputStream.use { it.readBytes() })
                HttpURLConnection.HTTP_NOT_FOUND -> MerchantLogoStore.Fetched.Missing
                else -> MerchantLogoStore.Fetched.Failed
            }
        } finally {
            connection.disconnect()
        }
    } catch (_: IOException) {
        MerchantLogoStore.Fetched.Failed
    }
}

/** Decodes an icon of any format Android reads, e.g. ICO, PNG, WebP, and stores it as PNG */
private fun preparePng(bytes: ByteArray): ByteArray? {
    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
    if (minOf(bitmap.width, bitmap.height) < MerchantLogoStore.MIN_SIZE) return null
    val scale = MerchantLogoStore.MAX_SIZE.toFloat() / maxOf(bitmap.width, bitmap.height)
    val scaled = if (scale < 1) Bitmap.createScaledBitmap(
        bitmap, (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true
    ) else bitmap
    return ByteArrayOutputStream().use {
        scaled.compress(Bitmap.CompressFormat.PNG, 100, it)
        it.toByteArray()
    }
}
