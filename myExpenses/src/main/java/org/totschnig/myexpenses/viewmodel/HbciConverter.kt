package org.totschnig.myexpenses.viewmodel

import org.apache.commons.lang3.StringUtils
import org.kapott.hbci.GV_Result.GVRKUms.UmsLine
import org.kapott.hbci.structures.Konto
import org.totschnig.myexpenses.db2.Attribute
import org.totschnig.myexpenses.db2.Repository
import org.totschnig.myexpenses.db2.findPaymentMethod
import org.totschnig.myexpenses.db2.requireParty
import org.totschnig.myexpenses.db2.writePaymentMethod
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.model.CrStatus
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.model.Transaction
import org.totschnig.myexpenses.model2.Party
import org.totschnig.myexpenses.viewmodel.VerwendungszweckUtil.Tag
import org.totschnig.myexpenses.viewmodel.VerwendungszweckUtil.getTag


/**
 * Logic borrowed from Hibiscus
 */

const val HBCI_TRANSFER_NAME_MAXLENGTH = 70
const val HBCI_TRANSFER_USAGE_DB_MAXLENGTH = 35
val TEXT_REPLACEMENTS_UMSATZ = arrayOf(arrayOf("\n", "\r"), arrayOf("", ""))

data class Transfer(
    val zweck: String?,
    val zweck2: String?,
    val weitereVerwendungszwecke: Array<String>,
    val tags: Map<Tag, String>
)

class HbciConverter(val repository: Repository, val eur: CurrencyUnit) {
    private val methodToId: MutableMap<String, Long> = HashMap()

    fun UmsLine.toTransaction(accountId: Long): Pair<Transaction, Map<out Attribute, String>> {
        val transaction = Transaction(accountId, Money(eur, value.longValue))
        transaction.crStatus = CrStatus.RECONCILED
        transaction.setDate(bdate)
        transaction.setValueDate(valuta)

        var lines = usage.toTypedArray() as Array<String?>
        if (lines.isEmpty()) lines = VerwendungszweckUtil.parse(additional)
        lines = VerwendungszweckUtil.rewrap(HBCI_TRANSFER_USAGE_DB_MAXLENGTH, *lines)
        val transfer = VerwendungszweckUtil.apply(lines)

        (getTag(transfer, Tag.ABWA)?.let {
            Party(name = it)
        } ?: other.takeIf { !(other.name.isNullOrBlank() && other.name2.isNullOrBlank()) }
            ?.let {
                other.toParty()
            })?.let { party ->
            transaction.payeeId = repository.requireParty(party)
        }

        if (transfer != null) {
            transaction.comment =
                getTag(transfer, Tag.SVWZ)
                    ?: arrayOf(
                        transfer.zweck,
                        transfer.zweck2,
                        *transfer.weitereVerwendungszwecke
                    ).joinToString("\n")
        }

        transaction.methodId = clean(text)?.let { extractMethodId(it) }

        val attributes = buildMap {
            extractAttribute(transfer, this, Tag.EREF, FinTsAttribute.EREF)
            extractAttribute(transfer, this, Tag.MREF, FinTsAttribute.MREF)
            extractAttribute(transfer, this, Tag.KREF, FinTsAttribute.KREF)
            extractAttribute(transfer, this, Tag.CRED, FinTsAttribute.CRED)
            extractAttribute(transfer, this, Tag.DBET, FinTsAttribute.DBET)
            put(FinTsAttribute.SALDO, saldo.value.bigDecimalValue.toString())
        }

        return transaction to attributes
    }

    private fun extractAttribute(
        transfer: Transfer,
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


    fun replace(text: String?, replacements: Array<Array<String>>) =
        if (text.isNullOrEmpty()) text else
            StringUtils.replaceEach(text, replacements[0], replacements[1])


    private fun Konto.toParty(): Party {

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
        return Party.create(name, iban, bic)
    }

    private fun extractMethodId(methodLabel: String): Long =
        methodToId[methodLabel] ?: (repository.findPaymentMethod(methodLabel).takeIf { it != -1L }
            ?: repository.writePaymentMethod(methodLabel, AccountType.BANK)).also {
            methodToId[methodLabel] = it
        }
}