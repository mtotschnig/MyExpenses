---
id: myexpenses
summary: A privacy-friendly Android app for keeping track of expenses and income across accounts in several currencies, with splits, transfers, recurring plans, budgets and bank-statement reconciliation.
category: personal-finance
tags:
- personal-finance
- expense-tracking
- budgeting
- android
authors:
- name: Michael Totschnig
  url: https://github.com/mtotschnig
license: GPL-3.0-or-later
languages: [en, ar, be, bg, bn, br, ca, cs, da, de, el, en-GB, es, eu, fa, fr, gl, hi, hr, hu, id, it, he, ja, km, kn, ko, lt, ms, nb-NO, nl, pl, pt-BR, pt-PT, ro, ru, si, sk, sl, sr, sv, ta, te, th, tk, tr, uk, ur, vi, zh-CN, zh-HK, zh-TW]
limitations:
- Data is kept on the device; there is no sign-in, and sharing between devices goes through storage the person chooses, such as WebDAV, Google Drive, Dropbox or OneDrive.
- 'Plans are events in an Android calendar: without access to the calendar, plan occurrences are neither shown nor executed.'
- Without a licence key at most five accounts and three plans can be created; split transactions, CSV import and budgeting can be tried for 60 days from first use and then need a Contrib, Extended or Professional key.
- A closed account and its transactions stay as they are until the account is reopened.
references:
- kind: doc
  role: intent
  target: README.md
  title: README
- kind: doc
  role: intent
  target: metadata/en-US/full_description.txt
  title: Store description
- kind: code
  role: implementation
  target: myExpenses/src/main/java/org/totschnig/myexpenses/model/ContribFeature.kt#ContribFeature
- kind: code
  role: context
  target: myExpenses/src/main/res/xml/locales_config.xml
---

# My Expenses

My Expenses keeps track of a person's expenses and income on their Android phone or tablet. They keep
several accounts, each in its own currency, and record transactions, transfers between accounts and split
transactions whose parts go to different categories. Templates turn frequent entries into one tap, and a
template with a plan recurs on a schedule, applied automatically or on request. Budgets compare what is
spent per category with what was allocated for each period. Transactions are marked cleared and then
reconciled when the account is balanced against a bank statement, and older transactions can be packed
into archives. Data comes in from QIF and CSV files and goes out as QIF, CSV or JSON. Some features need
a Contrib, Extended or Professional licence key, several of them after a trial.

## Intent

"My Expenses is an Android app designed to keep track of your expenses and income": "Effortless Expense
Tracking", "Flexible Account Management ... including transfers between different currencies", "Streamlined
Financial Planning" through "plans for recurring transactions", "Bank Statement Reconciliation" and
"Seamless Data Management" through QIF and CSV import and export. The store listing calls it "your
ultimate, privacy-friendly personal finance manager", with "open-source transparency".
