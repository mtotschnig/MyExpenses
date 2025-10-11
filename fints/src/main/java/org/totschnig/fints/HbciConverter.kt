package org.totschnig.fints

import org.apache.commons.lang3.StringUtils
import org.kapott.hbci.GV_Result.GVRKUms.UmsLine
import org.kapott.hbci.structures.Konto
import org.totschnig.fints.VerwendungszweckUtil.Tag
import org.totschnig.fints.VerwendungszweckUtil.getTag
import org.totschnig.myexpenses.db2.Attribute
import org.totschnig.myexpenses.db2.AutoFillInfo
import org.totschnig.myexpenses.db2.FinTsAttribute
import org.totschnig.myexpenses.db2.Repository
import org.totschnig.myexpenses.db2.createParty
import org.totschnig.myexpenses.db2.entities.Transaction
import org.totschnig.myexpenses.db2.findParty
import org.totschnig.myexpenses.db2.findPaymentMethod
import org.totschnig.myexpenses.db2.writePaymentMethod
import org.totschnig.myexpenses.model.CrStatus
import org.totschnig.myexpenses.model.CurrencyContext
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.model2.Party
import java.util.zip.CRC32


/**
 * Logic borrowed from Hibiscus
 */

const val HBCI_TRANSFER_NAME_MAXLENGTH = 70
const val HBCI_TRANSFER_USAGE_DB_MAXLENGTH = 35
val TEXT_REPLACEMENTS_UMSATZ = arrayOf(arrayOf("\n", "\r"), arrayOf("", ""))

data class Transfer(
    val zweck: String?,
    val zweck2: String?,
    val weitereVerwendungszwecke: List<String>,
    val tags: Map<Tag, String>
)

fun UmsLine.checkSum(): Long {
    val s = "$bdate${value.longValue}$text${usage.joinToString("")}$other$primanota"
    val crc = CRC32()
    crc.update(s.toByteArray())
    return crc.value
}

class HbciConverter(val repository: Repository) {
    private val methodToId: MutableMap<String, Long> = HashMap()
    private val payeeCache: MutableMap<Party, Pair<Long, AutoFillInfo>> = HashMap()

    fun UmsLine.toTransaction(
        currencyContext: CurrencyContext,
        accountId: Long,
        accountTypeId: Long,
    ): Pair<Transaction, Map<out Attribute, String>> {
        val origAmount = orig_value?.let {
            with(currencyContext[it.curr]) {
                this to Money(this, it.longValue).amountMinor
            }
        }
        var lines = usage.toTypedArray()
        if (lines.isEmpty()) lines = VerwendungszweckUtil.parse(additional)
        lines = VerwendungszweckUtil.rewrap(HBCI_TRANSFER_USAGE_DB_MAXLENGTH, *lines)

        val transfer = VerwendungszweckUtil.apply(lines)

        val payeeInfo = (getTag(transfer, Tag.ABWA)?.let { Party.create(name = it) }
            ?: other?.takeIf { !(it.name.isNullOrBlank() && it.name2.isNullOrBlank()) }
                ?.toParty())?.let { party ->
            payeeCache[party] ?: run {
                val payeeId = repository.findParty(party)
                payeeId?.let { it to repository.autoFill(it) }
                    ?: repository.createParty(party)?.let {  it.id to null }
            }
        }

        return Transaction(
            accountId = accountId,
            amount = value.longValue,
            originalCurrency = origAmount?.first?.code,
            originalAmount = origAmount?.second,
            crStatus = CrStatus.RECONCILED,
            date = bdate.time / 1000,
            valueDate = valuta.time / 1000,
            methodId = text?.let { clean(it) }
                ?.let { extractMethodId(it, accountTypeId) },
            comment = transfer?.let {
                getTag(transfer, Tag.SVWZ)
                    ?: buildList {
                        add(transfer.zweck)
                        add(transfer.zweck2)
                        addAll(transfer.weitereVerwendungszwecke)
                    }.filterNotNull().joinToString("\n")
            },
            payeeId = payeeInfo?.first,
            categoryId = payeeInfo?.second?.categoryId
        ) to buildMap<FinTsAttribute, String> {
            extractAttribute(transfer, this, Tag.EREF, FinTsAttribute.EREF)
            extractAttribute(transfer, this, Tag.MREF, FinTsAttribute.MREF)
            extractAttribute(transfer, this, Tag.KREF, FinTsAttribute.KREF)
            extractAttribute(transfer, this, Tag.CRED, FinTsAttribute.CRED)
            extractAttribute(transfer, this, Tag.DBET, FinTsAttribute.DBET)
            put(FinTsAttribute.SALDO, saldo.value.bigDecimalValue.toString())
            put(FinTsAttribute.CHECKSUM, this@toTransaction.checkSum().toString())
        }
    }

    private fun extractAttribute(
        transfer: Transfer?,
        mutableMap: MutableMap<FinTsAttribute, String>,
        tag: Tag,
        attribute: FinTsAttribute
    ) {
        cleanSepaId(getTag(transfer, tag))?.takeIf { it.length in 1..100 }?.let {
            mutableMap.put(attribute, it)
        }
    }

    private fun clean(s: String) = replace(s, TEXT_REPLACEMENTS_UMSATZ)

    private fun cleanSepaId(textIn: String?): String? {
        var text = textIn?.let { clean(it) }
        if (text.isNullOrEmpty() || text.length <= 100) return text
        val pos = text.indexOf(' ')
        if (pos in 1..99) text = text.substring(0, pos)
        if (text.length > 100) text = text.substring(0, 100)
        return text
    }


    private fun replace(text: String?, replacements: Array<Array<String>>) =
        if (text.isNullOrEmpty()) text else
            StringUtils.replaceEach(text, replacements[0], replacements[1])


    private fun Konto.toParty(): Party? {

        var name: String = StringUtils.trimToEmpty(name)
        val name2: String = StringUtils.trimToEmpty(name2)
        if (name2.isNotEmpty()) name += " $name2"
        if (name.length > HBCI_TRANSFER_NAME_MAXLENGTH) name =
            StringUtils.trimToEmpty(
                name.substring(
                    0,
                    HBCI_TRANSFER_NAME_MAXLENGTH
                )
            )
        return Party.create(name= name, iban = iban, bic = bic)
    }

    private fun extractMethodId(methodLabel: String, accountTypeId: Long): Long =
        methodToId[methodLabel] ?: (repository.findPaymentMethod(methodLabel).takeIf { it != -1L }
            ?: repository.writePaymentMethod(methodLabel, accountTypeId)).also {
            methodToId[methodLabel] = it
        }
}