#!/usr/local/bin/bash

function mkScreenshots {
    if [[ $LANGS == "all" || $LANGS = *"$1"* ]] ; then
        echo "generate screenshots for $1-$2-$3 (scenario $SCENARIO, moving to $FOLDER)"
        ./gradlew spoon -PtestScenario=$SCENARIO -PtestLang=$1 -PtestCountry=$2 -PtestCurrency=$3
        for i in group summarize budget split distribution history export sync
        do
            mv myExpenses/build/spoon-output/conscriptForTest/image/emulator-5554/org.totschnig.myexpenses.test.screenshots.TestMain/mkScreenShots/*_$i.png \
            metadata/$4/images/$FOLDER/$i.png
        done
    fi
}

set -e

case "$1" in
        phone) SCENARIO=1
        ;;
        tenInch | sevenInch)  SCENARIO=2
        ;;
        *)
        echo $"Usage: $0 {phone|tenInch|sevenInch} [all | langlist]" >&2
        exit 1
        ;;
esac

FOLDER=${1}Screenshots
LANGS="${@:2}"
./gradlew copyFileForFixture
mkScreenshots ar SA SAR ar
mkScreenshots bg BG BGN bg-BG
mkScreenshots ca ES EUR ca
mkScreenshots cs CZ CZK cs-CZ
mkScreenshots da DK DKK da-DK
mkScreenshots de DE EUR de-DE
mkScreenshots el GR EUR el-GR
mkScreenshots en US USD en-US
mkScreenshots es ES EUR es-ES
mkScreenshots eu ES EUR eu-ES
mkScreenshots fr FR EUR fr-FR
mkScreenshots hr HR HRK hr
mkScreenshots hu HU HUF hu_HU
mkScreenshots it IT EUR it-IT
mkScreenshots iw IL ILS iw-IL
mkScreenshots ja JP JPY ja-JP
mkScreenshots km KH KHR km-KH
mkScreenshots ko KO KRW ko
mkScreenshots ms MY MYR ms
mkScreenshots pl PL PLN pl-PL
mkScreenshots pt BR BRL pt-PT
mkScreenshots ro RO RON ro
mkScreenshots ru RU RUB ru-RU
mkScreenshots si LK LKR si-LK
mkScreenshots ta IN INR ta
mkScreenshots tr TR TRY tr-TR
mkScreenshots uk UA UAH uk
mkScreenshots vi VI VND vi
mkScreenshots zh TW TWD zh-TW
