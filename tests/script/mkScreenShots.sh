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
mkScreenshots en US USD en-US
mkScreenshots de DE EUR de-DE
mkScreenshots fr FR EUR fr-FR
mkScreenshots it IT EUR it-IT
mkScreenshots es ES EUR es-ES
mkScreenshots tr TR TRY tr-TR
mkScreenshots bg BG BGN bg-BG
mkScreenshots vi VI VND vi
mkScreenshots ar SA SAR ar
mkScreenshots hu HU HUF hu_HU
mkScreenshots ca ES EUR ca
mkScreenshots km KH KHR km-KH
mkScreenshots zh TW TWD zh-TW
mkScreenshots pt BR BRL pt-PT
mkScreenshots pl PL PLN pl-PL
mkScreenshots cs CZ CZK cs-CZ
mkScreenshots ru RU RUB ru-RU
mkScreenshots hr HR HRK hr
mkScreenshots ja JP JPY ja-JP
mkScreenshots ms MY MYR ms
mkScreenshots ro RO RON ro
mkScreenshots si LK LKR si-LK
mkScreenshots eu ES EUR eu-ES
mkScreenshots da DK DKK da-DK
mkScreenshots iw IL ILS iw-IL
mkScreenshots uk UA UAH uk
mkScreenshots ko KO KRW ko
mkScreenshots ta IN INR ta
mkScreenshots el GR EUR el-GR