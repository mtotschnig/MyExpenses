MyExpenses
==========

GPL licenced Android Expense Tracking App.

*My Expenses* is an Android app designed to keep track of your expenses and income.
Works on Android 5.0 and above.
 
<a href="https://f-droid.org/packages/org.totschnig.myexpenses" target="_blank">
<img src="https://f-droid.org/badge/get-it-on.png" alt="Get it on F-Droid" height="90"/></a>
<a href="https://play.google.com/store/apps/details?id=org.totschnig.myexpenses" target="_blank">
<img src="https://play.google.com/intl/en_us/badges/images/generic/en-play-badge.png" alt="Get it on Google Play" height="90"/></a>

Features
========

- **Effortless Expense Tracking**: Keep tabs on your expenses and income seamlessly, whether you're on your smartphone or tablet.
- **Flexible Account Management**: Effortlessly manage multiple accounts, including transfers between different currencies.
- **Streamlined Financial Planning**: Easily set up plans for recurring transactions.
- **Seamless Data Management**: Export and import data with ease using QIF and CSV formats.
- **Enhanced Security**: Protect your data with password or device lock screen security.
- **Customizable Experience**: Tailor the app to your preferences with customizable themes and font sizes.
- **Bank Statement Reconciliation**: Easily compare transaction status with your bank statements for accurate financial tracking.
- **Quick Data Entry**: Enjoy convenient access with homescreen widgets and shortcuts.
- **Powerful Data Analysis**: Filter your data based on various criteria and visualize distribution and historical trends with dynamic graphs.

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
- [OkHttp](http://square.github.io/okhttp/)
- [Timber](https://github.com/JakeWharton/timber)
- [SLF4J](https://www.slf4j.org/)
- [logback-android](http://tony19.github.io/logback-android/index.html)
- [Retrofit2](http://square.github.io/retrofit/)
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
- [sealed-enum](https://github.com/livefront/sealed-enum)
- [Reorderable](https://github.com/Calvin-LL/Reorderable)

and on the contribution of many users that helped make My Expenses available in 34 different 
<a href="http://www.myexpenses.mobi/en/#translate">languages</a>.

Code has also been contributed by:

- [khris78](https://github.com/khris78) (Configuring and applying custom colors to accounts)
- [Ayman Abdelghany](https://github.com/AymanDF) (Applying Sonar code quality checks)
- [eltos](https://github.com/eltos) (Improved Input Dialogs and Color Pickers)
- [tillgraeger](https://github.com/tillgraeger) implemented several tickets ([#640](https://github.com/mtotschnig/MyExpenses/issues/640), [#704](https://github.com/mtotschnig/MyExpenses/issues/704), [#638](https://github.com/mtotschnig/MyExpenses/issues/638)) in the context of his internship.
- [arnaldotecadm](https://github.com/arnaldotecadm) implemented JSON export

Build
=====

Requires Java 21

Build with dynamic feature modules, and OCR feature depending on external OCR helper app:

```sh
git clone https://github.com/mtotschnig/MyExpenses.git
cd MyExpenses
export ANDROID_HOME={sdk-dir}
./gradlew myExpenses:packageExternReleaseUniversalApk
```

Integrate
=========
My Expenses now has experimental support for inserting data from third party apps. See [TransactionsContract.java](https://github.com/mtotschnig/MyExpenses/blob/master/transactionscontract/src/main/java/org/totschnig/myexpenses/contract/TransactionsContract.java).
