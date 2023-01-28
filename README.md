MyExpenses
==========

GPL licenced Android Expense Tracking App.

*My Expenses* is an Android app designed to keep
  track of your expenses and incomes, and to export them as QIF files into a desktop
  finance tool, like <a href="http://www.grisbi.org">Grisbi</a> (Open Source), <a
  href="http://www.gnucash.org">Gnucash</a> (Open Source), MS Money, Quicken or Wiso Mein Geld.<br />
  Works on Android 5.0 and above.
 
<a href="https://f-droid.org/repository/browse/?fdid=org.totschnig.myexpenses" target="_blank">
<img src="https://f-droid.org/badge/get-it-on.png" alt="Get it on F-Droid" height="90"/></a>
<a href="https://play.google.com/store/apps/details?id=org.totschnig.myexpenses" target="_blank">
<img src="https://play.google.com/intl/en_us/badges/images/generic/en-play-badge.png" alt="Get it on Google Play" height="90"/></a>

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
- Aggregate financial situation over all accounts with same currency
- Two themes: light and dark

Credits
=====
*My Expenses* relies on a couple of open source libraries :

- <a href="http://itextpdf.com/">Itext</a>
- <a href="https://github.com/PhilJay/MPAndroidChart">MPAndroidChart</a>
- <a href="https://github.com/MrBIMC/MaterialSeekBarPreference">MaterialSeekBarPreference</a>
- <a href="http://square.github.io/picasso/">Picasso</a>
- <a href="https://github.com/roomorama/Caldroid">Caldroid</a>
- <a href="https://github.com/frankiesardo/icepick">Icepick</a>
- Apache Commons <a href="https://commons.apache.org/proper/commons-lang/">Lang</a> and <a href="https://commons.apache.org/proper/commons-csv/">CSV</a>
- <a href="https://github.com/google/guava">Guava</a>
- <a href="https://gitlab.com/bitfireAT/dav4android">dav4android</a>

and on the contribution of many users that helped make My Expenses available in 34 different 
<a href="http://www.myexpenses.mobi/en/#translate">languages</a>.

Various components (CalculatorInput, QifParser, HomeScreenWidgets, WhereFilter) have been inspired by [Financisto](https://launchpad.net/financisto). WebDAV setup inspired by [Car report](https://bitbucket.org/frigus02/car-report/).

Code has also been contributed by:

- [khris78](https://github.com/khris78) (Configuring and applying custom colors to accounts)
- [Ayman Abdelghany](https://github.com/AymanDF) (Applying Sonar code quality checks)
- [eltos](https://github.com/eltos) (Improved Input Dialogs and Color Pickers)

Build
=====

```sh
git clone --depth 1 https://github.com/mtotschnig/MyExpenses.git
cd MyExpenses
export ANDROID_HOME={sdk-dir}
./gradlew build
```

If gradlew gives you a "Failed to install the following Android SDK packages"
error message, the packages can be installed manually with commands such as:

```sh
$ANDROID_HOME/cmdline-tools/bin/sdkmanager --install --sdk_root=$ANDROID_HOME "platforms;android-32"
$ANDROID_HOME/cmdline-tools/bin/sdkmanager --install --sdk_root=$ANDROID_HOME "build-tools;30.0.3"
```

If gradlew errors out with "Could not dispatch a message to the daemon", just
re-run the command. This can happen when the system is low on memory. The same
is true for the "Gradle build daemon disappeared unexpectedly" error.

Integrate
=========
My Expenses now has experimental support for inserting data from third party apps. See [TransactionsContract.java](https://github.com/mtotschnig/MyExpenses/blob/master/transactionscontract/src/main/java/org/totschnig/myexpenses/contract/TransactionsContract.java).
