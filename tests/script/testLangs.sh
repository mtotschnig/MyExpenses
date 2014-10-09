#!/bin/bash
#Prerequisites: run test project first on Eclipse, install Contrib app
#in MyExpenses.pages/{lang}/screenshots/orig verschieben
#for lang in ca cs de en es fr hu it pl pt tr vi zh-tv; do (echo $lang;cd ${lang}/screenshots/orig/;for image in *;do echo $image; convert $image -resize 60% ../$image;done;);done


if [ $# -lt 1 ]
then
  echo "Usage: $0 {lang} {stage}"
  exit
fi
lang=$1
adb shell pm clear org.totschnig.myexpenses
adb shell am instrument -w -e class "org.totschnig.myexpenses.test.screenshots.TestMain#testLang_${lang}" org.totschnig.myexpenses.test/android.test.InstrumentationTestRunner
monkeyrunner $(dirname $0)/monkey.py $lang ${2:-0}
