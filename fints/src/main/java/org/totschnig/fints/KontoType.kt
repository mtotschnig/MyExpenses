/**********************************************************************
 *
 * Copyright (c) 2004 Olaf Willuhn
 * All rights reserved.
 *
 * This software is copyrighted work licensed under the terms of the
 * Jameica License.  Please consult the file "LICENSE" for details.
 *
 */
package org.totschnig.fints

/**
 * von Hibiscus
 * Definition der verschiedenen Konto-Arten.
 * Siehe FinTS_3.0_Formals_2011-06-14_final_version.pdf - Data Dictionary "Kontoart",
 * Seite 94.
 */
enum class KontoType
    (
    /**
     * Liefert den zu verwendenden Wert, wenn diese Kontoart manuell ausgewaehlt wurde.
     * @return der zu verwendende Wert, wenn diese Kontoart manuell ausgewaehlt wurde.
     */
    val value: Int,
    private val max: Int,
    val label: String,
) {
    /**
     * Kontokorrent-/Girokonto.
     */
    GIRO(1, 9, "Kontokorrent-/Girokonto"),

    /**
     * Sparkonto.
     */
    SPAR(10, 19, "Sparkonto"),

    /**
     * Festgeldkonto (Termineinlagen).
     */
    FESTGELD(20, 29, "Festgeldkonto (Termineinlagen)"),

    /**
     * Wertpapierdepot.
     */
    WERTPAPIERDEPOT(30, 39, "Wertpapierdepot"),

    /**
     * Kredit-/Darlehenskonto.
     */
    DARLEHEN(40, 49, "Kredit-/Darlehenskonto"),

    /**
     * Kreditkartenkonto.
     */
    KREDITKARTE(50, 59, "Kreditkartenkonto"),

    /**
     * Fonds-Depot bei einer Kapitalanlagegesellschaft.
     */
    FONDSDEPOT(60, 69, "Fonds-Depot bei einer Kapitalanlagegesellschaft"),

    /**
     * Bausparvertrag.
     */
    BAUSPAR(70, 79, "Bausparvertrag"),

    /**
     * Versicherungsvertrag.
     */
    VERSICHERUNG(80, 89, "Versicherungsvertrag"),

    /**
     * Sonstige (nicht zuordenbar).
     */
    SONSTIGE(90, 99, "Sonstige (nicht zuordenbar)"),
    ;

    companion object {

        /**
         * Ermittelt die Kontoart fuer die ID.
         * @param id die ID. Kann NULL sein.
         * @return die Kontoart oder NULL, wenn die ID nicht bekannt ist.
         */
        fun find(id: Int?): KontoType? = id?.let {
            entries.firstOrNull { id in it.value..it.max }
        }
    }
}


