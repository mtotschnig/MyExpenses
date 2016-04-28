MyExpenses
==========

GPL licenced Android Expense Tracking App.

*My Expenses* is an Android app designed to keep
  track of your expenses and incomes, and to export them as QIF files into a desktop
  finance tool, like <a href="http://www.grisbi.org">Grisbi</a> (Open Source), <a
  href="http://www.gnucash.org">Gnucash</a> (Open Source), MS Money, Quicken or Wiso Mein Geld.<br />
  Requires Android 2.1 and above.
  
  [![Bountysource](https://www.bountysource.com/badge/tracker?tracker_id=267118)](https://www.bountysource.com/trackers/267118-myexpenses?utm_source=267118&utm_medium=shield&utm_campaign=TRACKER_BADGE)

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

- <a href="https://github.com/emilsjolander/StickyListHeaders">StickyListHeaders</a>
- <a href="http://itextpdf.com/">Itext</a>
- <a href="https://github.com/PhilJay/MPAndroidChart">MPAndroidChart</a>
- <a href="https://github.com/MrBIMC/MaterialSeekBarPreference">MaterialSeekBarPreference</a>
- <a href="http://square.github.io/picasso/">Picasso</a>
- <a href="https://github.com/roomorama/Caldroid">Caldroid</a>
- <a href="https://github.com/commonsguy/cwac-wakeful">Wakeful</a>
- <a href="https://github.com/frankiesardo/icepick">Icepick</a>
- Apache Commons <a href="https://commons.apache.org/proper/commons-lang/">Lang</a> and <a href="https://commons.apache.org/proper/commons-csv/">CSV</a>
- <a href="https://github.com/google/guava">Guava</a>

and on the contribution of many users that helped make My Expenses available in 27 different languages
<a href="http://www.myexpenses.mobi/en/#translate">languages</a>:

Build
=====

```
git clone --depth 1 https://github.com/mtotschnig/MyExpenses.git
cd MyExpenses
git submodule init
git submodule update
export ANDROID_HOME={sdk-dir}
./gradlew build
```
