# Steps for preparing a new release
  
* Set version info in build.gradle
* Check that master is merged into distribution branch
* check if version_codes, version_names, upgrade.xml use the correction version code
* if applicable publish announcement on Google+ and Facebook and add links
* Test and assemble
  * ./gradlew lintExternRelease
  * ./gradlew testExternDebugUnitTest (-PBETA=true)
  * adb uninstall org.totschnig.myexpenses.debug
  * adb shell settings put global hidden_api_policy 1 # needed for LocaleUtil
  * ./gradlew :myExpenses:connectedExternDebugAndroidTest
  * ./gradlew clean :myExpenses:packageExternReleaseUniversalApk
* test upgrade mechanism
* execute command returned by ./gradlew echoPublishTag

#How to build ChangeLog tool

* uncomment module ChangeLog in settings.gradle
* ./gradlew ChangeLog:install
* mv ChangeLog/build/install/ChangeLog/ ChangeLog.install
