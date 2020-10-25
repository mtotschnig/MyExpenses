#!/usr/local/bin/bash
function show_help() {
cat >&2 << EOF
   Usage: ${0##*/} [-p PACKAGE] [-c COUNTRY] [-u USER]
   PACKAGE is Extended or Professional_6 or Professional_18 or Professional_30
   Generate invoice 
EOF
exit 1
}

while getopts "p:c:u:n:" opt; do
    case "$opt" in
        p) case "$OPTARG" in
               Extended)
                 export KEY="My Expenses Extended Licence"
                 export PRICE=5
                 ;;
               Professional_6)
                 export KEY="My Expenses Professional Licence 6 months"
                 export PRICE=5
                 ;;
               Professional_18)
                 export KEY="My Expenses Professional Licence 18 months"
                 export PRICE=9.5
                 ;;
               Professional_30)
                 export KEY="My Expenses Professional Licence 30 months"
                 export PRICE=14
                 ;;
            esac
           ;;
        c) export COUNTRY=$OPTARG
           ;;
        u) export KUNDE=$OPTARG
           ;;
        '?')
            show_help
            ;;
    esac
done

if [ -z "$KEY" ] || [ -z "$PRICE" ] || [ -z "$COUNTRY" ] || [ -z "$USER" ]
  then
     show_help
fi

YEAR=$(date +'%Y')
MONTH=$(date +'%m')
LATEST=$(<LATEST)
arrLATEST=(${LATEST//-/ })
LATEST_MONTH=${arrLATEST[0]}
LATEST_NUMBER=${arrLATEST[1]}

if [ "$MONTH" == "${LATEST_MONTH}" ]
  then
    let LATEST_NUMBER+=1
  else
    LATEST_NUMBER=1
fi
export NUMBER=${YEAR}-${MONTH}-${LATEST_NUMBER}
echo ${MONTH}-${LATEST_NUMBER} >LATEST

(
cd /Users/michaeltotschnig/Documents/MyExpenses.business/invoices
FILENAME=Invoice-${NUMBER}
TEXFILE=${FILENAME}.tex
if test -f "$TEXFILE"; then
    echo "$TEXFILE exists."
    exit 1
fi
envsubst < Invoice.tmpl > $TEXFILE
pdflatex $TEXFILE
open ${FILENAME}.pdf
)