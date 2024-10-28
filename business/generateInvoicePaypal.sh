#!/usr/bin/env bash
function show_help() {
cat >&2 << EOF
   Usage: ${0##*/} [-g GROSS] [-t TX] [-p PACKAGE] [-c COUNTRY] [-u USER] [-e EMAIL] [-v VAT] [-d DATE] [-n NUMBER]
   PACKAGE is Extended or Professional_6 or Professional_12 or Professional_24 or History or Budget or Ocr or WebUi or SplitTemplate
   Generate invoice 
EOF
exit 1
}

while getopts "p:c:u:g:t:e:v:d:n:" opt; do
    case "$opt" in
        p) case "$OPTARG" in
               History)
                 export KEY="History Chart"
                 ;;
               Budget)
                 export KEY="Budgeting"
                 ;;
               Ocr)
                 export KEY="Scan receipt"
                 ;;
               WebUi)
                 export KEY="Web User Interface"
                 ;;
               SplitTemplate)
                 export KEY="Split Template"
                 ;;
               CategoryTree)
                 export KEY="Category Tree"
                 ;;
               Contrib)
                 export KEY="My Expenses Contrib Licence"
                 ;;
               Extended)
                 export KEY="My Expenses Extended Licence"
                 ;;
               Professional_6)
                 export KEY="My Expenses Professional Licence 6 months"
                 ;;
               Professional_12)
                 export KEY="My Expenses Professional Licence 1 year"
                 ;;
               Professional_24)
                 export KEY="My Expenses Professional Licence 2 years"
                 ;;
               Upgrade)
                 export KEY="Upgrade Contrib -> Extended Licence"
                 ;;
               *)
                 export KEY=$OPTARG
           esac
           ;;
        c) export COUNTRY=$OPTARG
           ;;
        u) export KUNDE=$OPTARG
           ;;
        e) export EMAIL=$OPTARG
           ;;
        t) export TX=$OPTARG
           ;;
        g) export GROSS=$OPTARG
           ;;
        v) export VAT=$OPTARG
           ;;
        d) export DATE=$OPTARG
           ;;
        n) export NUMBER=$OPTARG
           ;;
        '?')
            show_help
            ;;
    esac
done

if [ -z "$KEY" ] || [ -z "$GROSS" ] || [ -z "$COUNTRY" ] || [ -z "$KUNDE" ] || [ -z "$EMAIL" ] || [ -z "$NUMBER" ] || [ -z "$DATE" ]
  then
     show_help
fi

if [ -z "$VAT" ] || [ "$VAT" == "0.00" ] || [ "$VAT" == "0" ]
  then
    TEMPLATE=Invoice-Paypal-noneu.tmpl
  else
    TEMPLATE=Invoice-Paypal.tmpl
fi

FILENAME=Invoice-${NUMBER}
TEXFILE=${FILENAME}.tex
if test -f "$TEXFILE"; then
    echo "$TEXFILE exists."
    exit 1
fi

if command -v xdg-user-dir &> /dev/null
then
  DOCUMENT_ROOT=$(xdg-user-dir DOCUMENTS)
else
  DOCUMENT_ROOT=$HOME/Documents
fi


envsubst < "$DOCUMENT_ROOT"/MyExpenses.business/Paypal/$TEMPLATE > $TEXFILE
pdflatex $TEXFILE
