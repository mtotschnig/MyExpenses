package org.totschnig.myexpenses.viewmodel

import org.apache.commons.lang3.StringUtils
import org.kapott.hbci.GV_Result.GVRKUms.UmsLine
import org.kapott.hbci.structures.Konto
import org.totschnig.myexpenses.db2.Repository
import org.totschnig.myexpenses.db2.requireParty
import org.totschnig.myexpenses.model.CrStatus
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.model.Transaction
import org.totschnig.myexpenses.model2.Party


/**
 * Logic borrowed from Hibiscus
 */

const val HBCI_TRANSFER_NAME_MAXLENGTH = 70
const val HBCI_TRANSFER_USAGE_DB_MAXLENGTH = 35

data class Transfer(
    val zweck: String?,
    val zweck2: String?,
    val weitereVerwendungszwecke: Array<String>,
    val tags: Map<VerwendungszweckUtil.Tag, String>
)

fun UmsLine.toTransaction(accountId: Long, currencyUnit: CurrencyUnit, repository: Repository): Transaction {

    return Transaction(accountId, Money(currencyUnit, value.longValue)).also {
        it.crStatus = CrStatus.RECONCILED
        it.setDate(bdate)
        it.setValueDate(valuta)
        // Verwendungszweck
        var lines = usage.toTypedArray() as Array<String?>
        if (lines.isEmpty()) lines = VerwendungszweckUtil.parse(additional)
        lines = VerwendungszweckUtil.rewrap(HBCI_TRANSFER_USAGE_DB_MAXLENGTH, *lines)
        val transfer = VerwendungszweckUtil.apply(lines)
        (VerwendungszweckUtil.getTag(transfer, VerwendungszweckUtil.Tag.ABWA)?.let {
            Party(name = it)
        } ?: other.takeIf { !(other.name.isNullOrBlank() && other.name2.isNullOrBlank()) }?.let {
            HBCIKonto2Party(other)
        })?.let { party ->
            it.payeeId = repository.requireParty(party)
        }
        if (transfer != null) {
            it.comment = VerwendungszweckUtil.getTag(transfer, VerwendungszweckUtil.Tag.SVWZ)
                ?: arrayOf(
                    transfer.zweck,
                    transfer.zweck2,
                    *transfer.weitereVerwendungszwecke
                ).joinToString("\n")
        }

        /*
        // Wir checken mal, ob wir eine EndToEnd-ID haben. Falls ja, tragen wir die gleich
        // in das dedizierte Feld ein. Aber nur, wenn wir noch keine haben
        var eref: String = umsatz.getEndToEndId()
        if (eref == null || eref.length == 0) {
            eref = de.willuhn.jameica.hbci.server.Converter.cleanSepaId(
                VerwendungszweckUtil.getTag(
                    umsatz,
                    Tag.EREF
                )
            )
            if (eref != null && eref.length > 0 && eref.length <= 100) umsatz.setEndToEndId(eref)
        }
*/

        /*        // Wir checken mal, ob wir eine Mandatsreferenz haben. Falls ja, tragen wir die gleich
        // in das dedizierte Feld ein. Aber nur, wenn wir noch keine haben
        var mid: String = umsatz.getMandateId()
        if (mid == null || mid.length == 0) {
            mid = de.willuhn.jameica.hbci.server.Converter.cleanSepaId(
                VerwendungszweckUtil.getTag(
                    umsatz,
                    Tag.MREF
                )
            )
            if (mid != null && mid.length > 0 && mid.length <= 100) umsatz.setMandateId(mid)
        }*/
    }

/*    umsatz.setArt(de.willuhn.jameica.hbci.server.Converter.clean(u.text))
    umsatz.setCustomerRef(de.willuhn.jameica.hbci.server.Converter.clean(u.customerref))
    umsatz.setPrimanota(de.willuhn.jameica.hbci.server.Converter.clean(u.primanota))
    umsatz.setTransactionId(u.id)
    umsatz.setPurposeCode(u.purposecode)
    umsatz.setEndToEndId(u.endToEndId)
    umsatz.setMandateId(u.mandateId)*/

/*    //BUGZILLA 67 http://www.willuhn.de/bugzilla/show_bug.cgi?id=67
    val s = u.saldo
    if (s != null) {
        val v = s.value
        if (v != null) {
            // BUGZILLA 318
            var saldo = v.doubleValue
            val curr = v.curr
            if (curr != null && "DEM" == curr) saldo /= de.willuhn.jameica.hbci.server.Converter.KURS_EUR
            umsatz.setSaldo(saldo)
        }
    }*/

/*    // Wir uebernehmen den GV-Code nur, wenn was sinnvolles drin steht.
    // "999" steht hierbei fuer unstrukturiert aka unbekannt.
    //
    if (u.gvcode != null && u.gvcode != "999" && u.gvcode.length <= HBCIProperties.HBCI_GVCODE_MAXLENGTH) umsatz.setGvCode(
        u.gvcode
    )
    if (u.addkey != null && u.addkey.length > 0 && u.addkey.length <= HBCIProperties.HBCI_ADDKEY_MAXLENGTH) umsatz.setAddKey(
        u.addkey
    )

    ////////////////////////////////////////////////////////////////////////////
    // Verwendungszweck

    // BUGZILLA 146
    // Aus einer Mail von Stefan Palme
    //    Es geht noch besser. Wenn in "umsline.gvcode" nicht der Wert "999"
    //    drinsteht, sind die Variablen "text", "primanota", "usage", "other"
    //    und "addkey" irgendwie sinnvoll gefüllt.  Steht in "gvcode" der Wert
    //    "999" drin, dann sind diese Variablen alle null, und der ungeparste
    //    Inhalt des Feldes :86: steht komplett in "additional".
    var lines = u.usage.toTypedArray() as Array<String?>
    if (u.isCamt && u.usage != null) {
        // Wenn wir nur eine Zeile haben, koennen wir die 1:1 uebernehmen
        if (u.usage.size == 1) umsatz.setZweck(u.usage[0]) else VerwendungszweckUtil.applyCamt(
            umsatz,
            u.usage
        )
    } else {
        // die Bank liefert keine strukturierten Verwendungszwecke (gvcode=999).
        // Daher verwenden wir den gesamten "additional"-Block und zerlegen ihn
        // in 27-Zeichen lange Haeppchen
        if (lines.size == 0) lines = VerwendungszweckUtil.parse(u.additional)

        // Es gibt eine erste Bank, die 40 Zeichen lange Verwendungszwecke lieferte.
        // Siehe Mail von Frank vom 06.02.2014
        lines = VerwendungszweckUtil.rewrap(HBCIProperties.HBCI_TRANSFER_USAGE_DB_MAXLENGTH, lines)
        VerwendungszweckUtil.apply(umsatz, lines)

        // Wir checken mal, ob wir eine EndToEnd-ID haben. Falls ja, tragen wir die gleich
        // in das dedizierte Feld ein. Aber nur, wenn wir noch keine haben
        var eref: String = umsatz.getEndToEndId()
        if (eref == null || eref.length == 0) {
            eref = de.willuhn.jameica.hbci.server.Converter.cleanSepaId(
                VerwendungszweckUtil.getTag(
                    umsatz,
                    Tag.EREF
                )
            )
            if (eref != null && eref.length > 0 && eref.length <= 100) umsatz.setEndToEndId(eref)
        }

        // Wir checken mal, ob wir eine Mandatsreferenz haben. Falls ja, tragen wir die gleich
        // in das dedizierte Feld ein. Aber nur, wenn wir noch keine haben
        var mid: String = umsatz.getMandateId()
        if (mid == null || mid.length == 0) {
            mid = de.willuhn.jameica.hbci.server.Converter.cleanSepaId(
                VerwendungszweckUtil.getTag(
                    umsatz,
                    Tag.MREF
                )
            )
            if (mid != null && mid.length > 0 && mid.length <= 100) umsatz.setMandateId(mid)
        }
    }
    //
    ////////////////////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////////////////////
    // Gegenkonto
    // und jetzt noch der Empfaenger (wenn er existiert)

    if (!HBCIProperties.HBCI_SEPA_PARSE_TAGS) return umsatz

    // Wenn wir noch keine Gegenkonto-Infos haben, versuchen wir mal, sie aus
    // dem Verwendungszweck zu extrahieren
    val haveIban = StringUtils.trimToNull(umsatz.getGegenkontoNummer()) != null
    val haveBic = StringUtils.trimToNull(umsatz.getGegenkontoBLZ()) != null
    val haveName = StringUtils.trimToNull(umsatz.getGegenkontoName()) != null
    if (!haveIban || !haveBic || !haveName) {
        val tags: Map<Tag, String> = VerwendungszweckUtil.parse(umsatz)
        if (!haveName) umsatz.setGegenkontoName(tags[Tag.ABWA])
        val iban = tags[Tag.IBAN]
        var bic = tags[Tag.BIC]
        var i: IBAN? = null
        if (!haveIban && StringUtils.trimToNull(iban) != null) {
            // Nur uebernehmen, wenn es eine gueltige IBAN ist
            try {
                i = HBCIProperties.getIBAN(iban)
                if (i != null) umsatz.setGegenkontoNummer(i.getIBAN())
            } catch (e: Exception) {
                Logger.error("invalid IBAN - ignoring: $iban", e)
            }
        }
        if (!haveBic) {
            bic = StringUtils.trimToNull(bic)
            if (bic != null) {
                try {
                    bic = HBCIProperties.checkBIC(bic)
                    if (bic != null) umsatz.setGegenkontoBLZ(bic)
                } catch (e: Exception) {
                    Logger.error("invalid BIC - ignoring: $bic", e)
                }
            } else if (i != null) {
                umsatz.setGegenkontoBLZ(i.getBIC())
            }
        }
    }*/
}

fun HBCIKonto2Party(konto: Konto): Party {

    var name: String = StringUtils.trimToEmpty(konto.name)
    val name2: String = StringUtils.trimToEmpty(konto.name2)
    if (name2.isNotEmpty()) name += " $name2"
    if (name.length > HBCI_TRANSFER_NAME_MAXLENGTH) name =
        StringUtils.trimToEmpty(
            name.substring(
                0,
                HBCI_TRANSFER_NAME_MAXLENGTH
            )
        )
    return Party.create(name, konto.iban, konto.bic)
}
