//Copyright (c) 2010 Denis Solonenko (Financisto)
//made available under the terms of the GNU Public License v2.0
//adapted to My Expenses by Michael Totschnig

package org.totschnig.myexpenses.export.qif;

import org.totschnig.myexpenses.export.CategoryInfo;
import java.io.IOException;

import static org.totschnig.myexpenses.export.qif.QifUtils.trimFirstChar;

/**
 * Created by IntelliJ IDEA.
 * User: Denis Solonenko
 * Date: 2/16/11 10:08 PM
 */
public class QifCategory extends CategoryInfo {

    public QifCategory() {
    }

    public QifCategory(String name, boolean income) {
        super(name, income);
    }

//    public static QifCategory fromCategory(Category c) {
//        QifCategory qifCategory = new QifCategory();
//        qifCategory.name = buildName(c);
//        //qifCategory.isIncome = c.isIncome();
//        return qifCategory;
//    }

    public void writeTo(QifBufferedWriter qifWriter) throws IOException {
        qifWriter.write("N").write(getName()).newLine();
        qifWriter.write(isIncome ? "I" : "E").newLine();
        qifWriter.end();
    }

    public void readFrom(QifBufferedReader r) throws IOException {
        String line;
        while ((line = r.readLine()) != null) {
            if (line.startsWith("^")) {
                break;
            }
            if (line.startsWith("N")) {
                this.setName(trimFirstChar(line));
            } else if (line.startsWith("I")) {
                this.isIncome = true;
            }
        }
    }
}
