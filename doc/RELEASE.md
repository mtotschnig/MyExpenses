# Steps for preparing a new release
  
* Set version info in build.gradle
* Check that master is merged into distribution branch
* check if version_codes, version_names, upgrade.xml use the correction version code
* if applicable publish announcement on Google+ and Facebook and add links
* Test and assemble
  * ./gradlew lintConscriptStubRelease
  * ./gradlew testConscriptStubReleaseUnitTest
  * ./gradlew clean connectedConscriptStubForTestAndroidTest
  * ./gradlew clean assembleConscriptStubRelease
* test upgrade mechanism
* Create release tag in GIT (git tag r39; git push origin r39)
* update _config.yml and push gh-pages
