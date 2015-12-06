#!/bin/bash
#Prerequisites: run test project first on Eclipse, install Contrib app
#in MyExpenses.pages/{lang}/screenshots/orig verschieben
#for lang in ca cs de en es fr hu it pl pt tr vi zh-tw; do (echo $lang;cd ${lang}/screenshots/orig/;for image in *;do echo $image; convert $image -resize 60% ../$image;done;);done


if [ $# -lt 1 ]
then
  echo "Usage: $0 {lang} {stage} {variant}"
  exit
fi
lang=$1
stage=${2:-0}
variant=$3
script=monkey
if [ "$variant" == "tablet" ]
then
	script=${script}-${variant}
fi
echo "Using monkey script $script"
adb shell pm clear org.totschnig.myexpenses
adb shell am broadcast -a android.intent.action.BOOT_COMPLETED -n org.totschnig.myexpenses/.service.ScheduledAlarmReceiver
adb push $(dirname $0)/screenshot.jpg /sdcard/Android/data/org.totschnig.myexpenses/files/screenshot.jpg
adb shell pm grant org.totschnig.myexpenses android.permission.WRITE_CALENDAR
adb shell pm grant org.totschnig.myexpenses android.permission.READ_CALENDAR
adb shell am instrument -w -e class "org.totschnig.myexpenses.test.screenshots.TestMain#testLang_${lang}" org.totschnig.myexpenses.test/android.test.InstrumentationTestRunner
monkeyrunner $(dirname $0)/${script}.py $lang $stage
