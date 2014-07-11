/*
 * Copyright (c) 2011 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */
//adapted to My Expenses by Michael Totschnig

package org.totschnig.myexpenses.export.qif;

import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.text.TextUtils;

/**
 * Created by IntelliJ IDEA.
 * User: Denis Solonenko
 * Date: 10/11/11 10:32 PM
 */
public class QifParser {

    private final QifBufferedReader r;
    private final QifDateFormat dateFormat;

    public final List<QifAccount> accounts = new ArrayList<QifAccount>();
    public final Set<QifCategory> categories = new HashSet<QifCategory>();
    public final Set<QifCategory> categoriesFromTransactions = new HashSet<QifCategory>();
    public final Set<String> payees = new HashSet<String>();
    public final Set<String> classes = new HashSet<String>();

    public QifParser(QifBufferedReader r, QifDateFormat dateFormat) {
        this.r = r;
        this.dateFormat = dateFormat;
    }

    public void parse() throws IOException {
        String peek;
        while ((peek = r.peekLine()) != null) {
            if (peek.startsWith("!Option:AutoSwitch")) {
              String line = r.readLine();
              outer:
              while (true) {
                line = r.readLine();
                if (line == null) {
                  return;
                }
                if (line.equals("!Account")) {
                  inner:
                  while (true) {
                    peek = r.peekLine();
                    if (peek == null) {
                      return;
                    }
                    if (peek.equals("!Clear:AutoSwitch")) {
                      r.readLine();
                      break outer;
                    }
                    QifAccount a = parseAccount();
                    accounts.add(a);
                  }
                }
              }
            } else if (peek.startsWith("!Account")) {
                r.readLine();
                parseTransactions(parseAccount());
            } else if (peek.startsWith("!Type:Cat")) {
                r.readLine();
                parseCategories();
            } else if (peek.startsWith("!Type") && !peek.startsWith("!Type:Class")) {
              parseTransactions(new QifAccount());
            } else {
              r.readLine();
            }
        }
        categories.addAll(categoriesFromTransactions);
    }

    private void parseCategories() throws IOException {
        while (true) {
            QifCategory category = new QifCategory();
            category.readFrom(r);
            if (category.name != null) {
              categories.add(category);
            }
            if (shouldBreakCurrentBlock()) {
                break;
            }
        }
    }
    private void parseTransactions(QifAccount account) throws IOException {
      accounts.add(account);
      String peek = r.peekLine();
      if (peek != null) {
          if (peek.startsWith("!Type:")) {
              applyAccountType(account, peek);
              r.readLine();
              while (true) {
                  QifTransaction t = new QifTransaction();
                  t.readFrom(r, dateFormat);
                  addPayeeFromTransaction(t);
                  addCategoryFromTransaction(t);
                  account.transactions.add(t);
                  if (shouldBreakCurrentBlock()) {
                      break;
                  }
              }
          }
      }
    }
    private QifAccount parseAccount() throws IOException {
        QifAccount account = new QifAccount();
        account.readFrom(r);
        return account;
    }

    private void applyAccountType(QifAccount account, String peek) {
        if (TextUtils.isEmpty(account.type)) {
            account.type = peek.substring(6);
        }
    }

    private void addPayeeFromTransaction(QifTransaction t) {
        if (!TextUtils.isEmpty(t.payee)) {
            payees.add(t.payee);
        }
    }

    private void addCategoryFromTransaction(QifTransaction t) {
        if (!TextUtils.isEmpty(t.category)) {
            QifCategory c = new QifCategory(t.category, false);
            categoriesFromTransactions.add(c);
        }
        if (!TextUtils.isEmpty(t.categoryClass)) {
            classes.add(t.categoryClass);
        }
    }

    private boolean shouldBreakCurrentBlock() throws IOException {
        String peek = r.peekLine();
        return peek == null || peek.startsWith("!");
    }

}
