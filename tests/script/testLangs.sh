#!/bin/sh
adb shell pm clear org.totschnig.myexpenses
adb shell am instrument -w -e class "org.totschnig.myexpenses.test.activity.TestMain#testLangEn" org.totschnig.myexpenses.test/android.test.InstrumentationTestRunner
adb shell pm clear org.totschnig.myexpenses
adb shell am instrument -w -e class "org.totschnig.myexpenses.test.activity.TestMain#testLangFr" org.totschnig.myexpenses.test/android.test.InstrumentationTestRunner
adb shell pm clear org.totschnig.myexpenses
adb shell am instrument -w -e class "org.totschnig.myexpenses.test.activity.TestMain#testLangDe" org.totschnig.myexpenses.test/android.test.InstrumentationTestRunner
adb shell pm clear org.totschnig.myexpenses
adb shell am instrument -w -e class "org.totschnig.myexpenses.test.activity.TestMain#testLangIt" org.totschnig.myexpenses.test/android.test.InstrumentationTestRunner
adb shell pm clear org.totschnig.myexpenses
adb shell am instrument -w -e class "org.totschnig.myexpenses.test.activity.TestMain#testLangEs" org.totschnig.myexpenses.test/android.test.InstrumentationTestRunner
adb shell pm clear org.totschnig.myexpenses
adb shell am instrument -w -e class "org.totschnig.myexpenses.test.activity.TestMain#testLangTr" org.totschnig.myexpenses.test/android.test.InstrumentationTestRunner
adb shell pm clear org.totschnig.myexpenses
adb shell am instrument -w -e class "org.totschnig.myexpenses.test.activity.TestMain#testLangVi" org.totschnig.myexpenses.test/android.test.InstrumentationTestRunner

