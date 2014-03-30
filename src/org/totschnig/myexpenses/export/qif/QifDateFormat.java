/*
 * Copyright (c) 2011 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */
//adapted to My Expenses by Michael Totschnig

package org.totschnig.myexpenses.export.qif;

/**
 * Created by IntelliJ IDEA.
 * User: Denis Solonenko
 * Date: 11/12/11 1:57 AM
 */
public enum QifDateFormat {
    US_FORMAT("US (MM/dd/yyyy, MM.dd.yy, …)"),
    EU_FORMAT("EU (dd/MM/yyyy, dd.MM.yy, …)");

    private String displayLabel;

    private QifDateFormat(String displayLabel){
        this.displayLabel = displayLabel;
    }

    @Override public String toString(){
        return displayLabel;
    }
}
