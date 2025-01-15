package org.totschnig.myexpenses.export

import android.content.Context
import androidx.annotation.Keep
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import org.totschnig.myexpenses.model.CrStatus
import org.totschnig.myexpenses.model.CurrencyContext
import org.totschnig.myexpenses.model.ExportFormat
import org.totschnig.myexpenses.model.TransactionDTO
import org.totschnig.myexpenses.model2.Account
import org.totschnig.myexpenses.provider.filter.Criterion
import java.lang.reflect.Type
import java.math.BigDecimal
import java.time.ZonedDateTime

/**
 * @param account          Account to print
 * @param filter           only transactions matched by filter will be considered
 * @param notYetExportedP  if true only transactions not marked as exported will be handled
 * @param dateFormat       format that can be parsed by SimpleDateFormat class
 * @param decimalSeparator , or .
 * @param encoding         the string describing the desired character encoding.
 */
class JSONExporter(
    account: Account,
    currencyContext: CurrencyContext,
    filter: Criterion?,
    notYetExportedP: Boolean,
    dateFormat: String,
    decimalSeparator: Char,
    encoding: String,
    private val preamble: String = "",
    private val appendix: String = ""
) :
    AbstractExporter(
        account, currencyContext, filter, notYetExportedP, dateFormat,
        decimalSeparator, encoding
    ) {

    val gson: Gson =  GsonBuilder()
        .registerTypeAdapter(ZonedDateTime::class.java, object: JsonSerializer<ZonedDateTime> {
            override fun serialize(
                zonedDateTime: ZonedDateTime,
                typeOfSrc: Type,
                context: JsonSerializationContext
            ) = JsonPrimitive(dateFormatter.format(zonedDateTime))
        })
        .create()

    override val format = ExportFormat.JSON

    override val useCategoryOfFirstPartForParent = false

    override fun header(context: Context) =
        "$preamble{\"uuid\":${gson.toJson(account.uuid)},\"label\":${gson.toJson(account.label)},\"currency\":${gson.toJson(account.currency)},\"openingBalance\":${gson.toJson(openingBalance)},\"transactions\": ["

    override fun TransactionDTO.marshall(categoryPaths: Map<Long, List<String>>): String =
        gson.toJson(convert(this))

    override fun recordDelimiter(isLastLine: Boolean) = if (isLastLine) null else ","

    override fun footer(): String = "]}$appendix"

    private fun convert(dto: TransactionDTO) : Transaction = with(dto) {
        Transaction(
            uuid = uuid,
            date = date,
            payee = payee,
            amount = amount,
            category = catId?.let { cat -> categoryPaths[cat] },
            transferAccount = transferAccount,
            comment = comment,
            methodLabel = methodLabel,
            status = status,
            referenceNumber = referenceNumber,
            attachments = attachmentFileNames,
            tags = tagList,
            splits = splits?.map(::convert)
        )
    }
}

@Keep
data class Transaction(
    val uuid: String,
    val date: ZonedDateTime,
    val payee: String?,
    val amount: BigDecimal,
    val category: List<String>?,
    val transferAccount: String?,
    val comment: String?,
    val methodLabel: String?,
    val status: CrStatus?,
    val referenceNumber: String?,
    val attachments: List<String>?,
    val tags: List<String>?,
    val splits: List<Transaction>?
)