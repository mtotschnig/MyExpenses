#!/bin/bash
#see doc/RELEASE on in what sequence this script is executed
#ATTENTION: script needs to be run on test device, it will erase data from MyExpenses
adb shell pm clear org.totschnig.myexpenses
adb shell am instrument -w -e package "org.totschnig.myexpenses.test.activity.myexpenses" org.totschnig.myexpenses.test/android.test.InstrumentationTestRunner
adb shell pm clear org.totschnig.myexpenses
adb shell am instrument -w -e package "org.totschnig.myexpenses.test.activity.expenseedit" org.totschnig.myexpenses.test/android.test.InstrumentationTestRunner
adb shell pm clear org.totschnig.myexpenses
adb shell am instrument -w -e package "org.totschnig.myexpenses.test.activity.managecurrencies" org.totschnig.myexpenses.test/android.test.InstrumentationTestRunner
adb shell pm clear org.totschnig.myexpenses
adb shell am instrument -w -e package "org.totschnig.myexpenses.test.activity.managecategories" org.totschnig.myexpenses.test/android.test.InstrumentationTestRunner
adb shell am instrument -w -e package "org.totschnig.myexpenses.test.misc" org.totschnig.myexpenses.test/android.test.InstrumentationTestRunner
adb shell am instrument -w -e package "org.totschnig.myexpenses.test.model" org.totschnig.myexpenses.test/android.test.InstrumentationTestRunner
adb shell am instrument -w -e package "org.totschnig.myexpenses.test.provider" org.totschnig.myexpenses.test/android.test.InstrumentationTestRunner

