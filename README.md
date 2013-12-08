MyExpenses
==========

GPL licenced Android Expense Tracking App.

*My Expenses* is an Android app designed to keep
  track of your expenses and incomes, and to export them as QIF files into a desktop
  finance tool, like <a href="http://www.grisbi.org">Grisbi</a> (Open Source), <a
  href="http://www.gnucash.org">Gnucash</a> (Open Source), MS Money, Quicken or Wiso Mein Geld.<br />
  Requires Android 2.1 and above.

Features
========
- Up to five accounts with transfers (unlimited in Contrib version)
- Define plans (3) for future and recurrent transactions  (unlimited in Contrib version)
- Group transactions per day, week, month, year and display sums per group
- Two levels of categories (import from Grisbi XML), display distribution of transactions in Contrib version
- Split transactions (Contrib version)
- Calculator
- Export to QIF and CSV (MS Excel), can be automatically shared (via email, FTP, Dropbox, ...) and done in batch in Contrib version
- Password protection, recoverable with security question in Contrib version
- Integrated Help
- Data backup and restore
- Aggregate financial situation (Contrib version)
- Two themes: light and dark

Build
=====
*My Expenses* depends on two libraries:
- Jake Wharton's <a href="http://actionbarsherlock.com">ActionBarSherlock</a>
- Emil Sjölander's StickyListHeaders in the following <a href="https://github.com/mtotschnig/StickyListHeaders">fork</a>

```
git clone git@github.com:mtotschnig/MyExpenses.git
git clone git@github.com:mtotschnig/StickyListHeaders.git
```

Gradle
------
```
cd MyExpenses
#need to figure out how to migrate this from Ant to Task
cp template/app.properties res/raw/
gradle build
```

Ant
---
```
cp {sdk-dir}/extras/android/support/v4/android-support-v4.jar MyExpenses/libs
cd StickyListHeaders/library
android update lib-project --path .
cd ../..
git clone https://github.com/JakeWharton/ActionBarSherlock.git
cd ActionBarSherlock/actionbarsherlock
android update lib-project --path .
cp {sdk-dir}/extras/android/support/v4/android-support-v4.jar libs
cd ../..
cd MyExpenses
ant release
