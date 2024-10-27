#!/usr/bin/env bash
function show_help() {
cat >&2 << EOF
   Usage: ${0##*/} [-p PACKAGE] [-c COUNTRY] [-u USER] [-d DURATION]
   PACKAGE is Contrib or Extended or Professional or History or Budget or Ocr
   Generate invoice 
EOF
exit 1
}

isPro=false

while getopts "p:c:u:d:" opt; do
    case "$opt" in
        p) case "$OPTARG" in
               Contrib)
                 export KEY="My Expenses Contrib Licence"
                 export PRICE=${PRICE:=12.4}
                 ;;
               History)
                 export KEY="History"
                 export PRICE=${PRICE:=4.5}
                 ;;
               Budget)
                 export KEY="Budgeting"
                 export PRICE=${PRICE:=4.5}
                 ;;
               Ocr)
                 export KEY="Scan receipt"
                 export PRICE=${PRICE:=4.5}
                 ;;
               Banking)
                 export KEY="Banking"
                 export PRICE=${PRICE:=4.5}
                 ;;
               CategoryTree)
                 export KEY="Category Tree"
                 export PRICE=${PRICE:=4.5}
                 ;;
               Extended)
                 export KEY="My Expenses Extended Licence"
                 export PRICE=${PRICE:=15.5}
                 ;;
               Professional)
                 isPro=true
                 ;;
            esac
           ;;
        c) export COUNTRY=$OPTARG
           ;;
        u) export KUNDE=$OPTARG
           ;;
        d)
          if [ "$isPro" = true ]
          then
            export KEY="My Expenses Professional Licence $OPTARG months"
          else
            show_help
          fi
          ;;
        '?')
            show_help
            ;;
    esac
done

if [ -z "$PRICE" ]
  then
     echo "with Professional key provide PRICE in environment"
     exit 1
fi

if [ -z "$ADDRESS" ]
  then
     echo "provide ADDRESS in environment"
     exit 1
fi

if [ -z "$KEY" ] || [ -z "$COUNTRY" ] || [ -z "$KUNDE" ]
  then
     show_help
fi

: "${TEMPLATE:=Invoice-ProForma.tmpl}"

if command -v xdg-user-dir &> /dev/null
then
  DOCUMENT_ROOT=$(xdg-user-dir DOCUMENTS)
else
  DOCUMENT_ROOT=$HOME/Documents
fi

(
cd "$DOCUMENT_ROOT"/MyExpenses.business/invoices || exit
YEAR=$(date +'%Y')
if test -f LATEST-PROFORMA
  then
    LATEST=$(<LATEST-PROFORMA)
    # shellcheck disable=SC2206
    arrLATEST=(${LATEST//-/ })
    LATEST_YEAR=${arrLATEST[0]}
    LATEST_NUMBER=${arrLATEST[1]}

    if [ "$YEAR" == "${LATEST_YEAR}" ]
      then
        # shellcheck disable=SC2219
        let LATEST_NUMBER+=1
      else
        LATEST_NUMBER=1
    fi
  else
    LATEST_NUMBER=1
fi

export NUMBER=${YEAR}-${LATEST_NUMBER}

FILENAME=ProForma-${NUMBER}
TEX_FILE=${FILENAME}.tex
if test -f "$TEX_FILE"; then
    echo "$TEX_FILE exists."
    exit 1
fi
envsubst < $TEMPLATE > "$TEX_FILE"
pdflatex "$TEX_FILE"
echo "${YEAR}"-${LATEST_NUMBER} >LATEST-PROFORMA
if command -v xdg-open &> /dev/null
then
  xdg-open "${FILENAME}".pdf
else
  open "${FILENAME}".pdf
fi
)
