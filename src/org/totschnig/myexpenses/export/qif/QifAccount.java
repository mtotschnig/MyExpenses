//Copyright (c) 2010 Denis Solonenko (Financisto)
//made available under the terms of the GNU Public License v2.0
//adapted to My Expenses by Michael Totschnig

package org.totschnig.myexpenses.export.qif;

import org.totschnig.myexpenses.model.Account;

import android.text.TextUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;

import static org.totschnig.myexpenses.export.qif.QifUtils.trimFirstChar;

/**
 * Created by IntelliJ IDEA.
 * User: Denis Solonenko
 * Date: 2/7/11 8:03 PM
 */
public class QifAccount {

    public String type = "";
    public String memo = "";
    public String desc = "";

    public Account dbAccount;
    public final List<QifTransaction> transactions = new ArrayList<QifTransaction>();

    public static QifAccount fromAccount(Account account) {
        QifAccount qifAccount = new QifAccount();
        qifAccount.type = account.type.toQifName();
        qifAccount.memo = account.label;
        qifAccount.desc = account.description;
        return qifAccount;
    }

    public Account toAccount(Currency currency) {
        Account a = new Account();
        a.currency = currency;
        a.label = TextUtils.isEmpty(memo) ? "QIF Import" : memo;
        a.type = Account.Type.fromQifName(type);
        a.description = desc;
        return a;
    }

    public void writeTo(QifBufferedWriter w) throws IOException {
        w.writeAccountsHeader();
        w.write("N").write(memo).newLine();
        w.write("T").write(type).newLine();
        w.end();
    }

    public void readFrom(QifBufferedReader r) throws IOException {
        String line;
        while ((line = r.readLine()) != null) {
            if (line.startsWith("^")) {
                break;
            }
            if (line.startsWith("N")) {
                this.memo = trimFirstChar(line);
            } else if (line.startsWith("T")) {
                this.type = trimFirstChar(line);
            } else if (line.startsWith("D")) {
              this.desc = trimFirstChar(line);
            }
        }
    }
}
