# Steps for preparing a new release
  
* Set version info in build.gradle
* Check that master is merged into distribution branch
* check if version_codes, version_names, upgrade.xml use the correction version code
* if applicable publish announcement on Google+ and Facebook and add links
* Test and assemble
  * ./gradlew lintExternRelease
  * ./gradlew testExternDebugUnitTest (-PBETA=true)
  * ./gradlew disableSystemAnimations clean connectedExternDebugAndroidTest
  * ./gradlew clean :myExpenses:packageExternReleaseUniversalApk
* test upgrade mechanism
* execute command returned by ./gradlew echoPublishTag
