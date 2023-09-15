MyExpenses
==========

GPL licenced Android Expense Tracking App.

*My Expenses* is an Android app designed to keep
  track of your expenses and incomes, and to export them as QIF files into a desktop
  finance tool, like <a href="http://www.grisbi.org">Grisbi</a> (Open Source), <a
  href="http://www.gnucash.org">Gnucash</a> (Open Source), MS Money, Quicken or Wiso Mein Geld.<br />
  Works on Android 5.0 and above.
 
<a href="https://f-droid.org/packages/org.totschnig.myexpenses" target="_blank">
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
*My Expenses* relies on many Open Source libraries and has borrowed code from other Open Source projects :

- [Financisto](https://launchpad.net/financisto) (CalculatorInput, QifParser, WhereFilter)
- [iText](http://itextpdf.com)
- [MPAndroidChart](https://github.com/PhilJay/MPAndroidChart)
- [SimpleDialogFragments](https://github.com/eltos/SimpleDialogFragments)
- [Grisbi](http://www.grisbi.org) (category definitions)
- [Android Open Source Project](https://source.android.com/) (SetupWizardLib, EventRecurrenceFormatter, Licencing Verification)
- [MaterialSeekBarPreference](https://github.com/MrBIMC/MaterialSeekBarPreference)
- [Picasso](http://square.github.io/picasso/)
- [Caldroid](https://github.com/roomorama/Caldroid)
- [Android-State](https://github.com/evernote/android-state/)
- [dav4android](https://gitlab.com/bitfireAT/dav4android)
- [Apache commons](https://commons.apache.org/) (Lang, CSV and Collections)
- [Guava](https://github.com/google/guava) (Int, IntMath, Files)
- [CarReport](https://bitbucket.org/frigus02/car-report/) (WebDAV setup)
- [Acra](http://acra.ch/)
- [Phrase](https://github.com/square/phrase)
- [Dagger](https://google.github.io/dagger/)
- [Gson](https://github.com/google/gson)
- [AutoValue](https://github.com/google/auto/tree/master/value)
- [AutoValue: Gson Extension](https://github.com/rharter/auto-value-gson)
- [AutoValue: Cursor Extension](https://github.com/gabrielittner/auto-value-cursor)
- [AutoValue: Parcel Extension](https://github.com/rharter/auto-value-parcel)
- [OkHttp](http://square.github.io/okhttp/)
- [Timber](https://github.com/JakeWharton/timber)
- [SLF4J](https://www.slf4j.org/)
- [logback-android](http://tony19.github.io/logback-android/index.html)
- [Retrofit2](http://square.github.io/retrofit/)
- [CircleProgress](https://github.com/lzyzsd/CircleProgress/)
- [Donut](https://github.com/futuredapp/donut)
- [Copper](https://github.com/cashapp/copper)
- [andOTP](https://github.com/andOTP/andOTP) (EncryptionHelper)
- [Font-Awesome](https://github.com/FortAwesome/Font-Awesome) (Category Icons)
- [TapTargetView](https://github.com/KeepSafe/TapTargetView)
- [PageIndicatorView](https://github.com/romandanylyk/PageIndicatorView)
- [FlexboxLayout](https://github.com/google/flexbox-layout)
- [Android Image Cropper](https://github.com/ArthurHub/Android-Image-Cropper)
- [Ktor](https://ktor.io/)
- [FontDrawable](https://github.com/k4zy/FontDrawable/)
- [Accompanist](https://github.com/google/accompanist) (drawablepainter, pager, flowlayout with patch from Sven Obser (https://github.com/brudaswen/accompanist/commit/582feb405276ce406260ad50634fd6b76ba19904))
- [Email Intent Builder](https://github.com/cketti/EmailIntentBuilder)
- [HBCI4Java](https://github.com/hbci4j/hbci4java)
- [Hibiscus](https://github.com/willuhn/hibiscus)

and on the contribution of many users that helped make My Expenses available in 34 different 
<a href="http://www.myexpenses.mobi/en/#translate">languages</a>.

Code has also been contributed by:

- [khris78](https://github.com/khris78) (Configuring and applying custom colors to accounts)
- [Ayman Abdelghany](https://github.com/AymanDF) (Applying Sonar code quality checks)
- [eltos](https://github.com/eltos) (Improved Input Dialogs and Color Pickers)
- [tillgraeger](https://github.com/tillgraeger) implemented several tickets ([#640](https://github.com/mtotschnig/MyExpenses/issues/640), [#704](https://github.com/mtotschnig/MyExpenses/issues/704), [#638](https://github.com/mtotschnig/MyExpenses/issues/638)) in the context of his internship.

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
