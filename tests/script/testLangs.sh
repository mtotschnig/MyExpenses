#!/bin/bash
#Prerequisites: run test project first on Eclipse, install Contrib app
#TODO: create dirs if needed, for the moment need to be created by hand:
#for i in en fr de it es tr vi; do mkdir $i; done
if [ $# -ne 1 ]
then
  echo "Usage: $0 {lang}"
  exit
fi
lang=$1
adb shell pm clear org.totschnig.myexpenses
adb shell am instrument -w -e class "org.totschnig.myexpenses.test.screenshots.TestMain#testLang_${lang}" org.totschnig.myexpenses.test/android.test.InstrumentationTestRunner
case $lang in
 en) country=US ;;
 ar) country=SA ;;
 *) country=${lang^^};;
esac
monkeyrunner monkey.py $lang $country
