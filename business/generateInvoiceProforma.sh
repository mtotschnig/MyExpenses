#!/usr/bin/env bash

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
: "${PACKAGES_CSV:="${SCRIPT_DIR}/packages.csv"}"

if [ ! -f "$PACKAGES_CSV" ]; then
    echo "Packages CSV file not found: $PACKAGES_CSV" >&2
    exit 1
fi

declare -A PKG_DESC
declare -A PKG_PRICE
AVAILABLE_PKGS=()

while IFS=',' read -r id description price || [ -n "$id" ]; do
    id=$(echo "$id" | tr -d "\r\n" | xargs)
    description=$(echo "$description" | tr -d "\r\n" | xargs)
    price=$(echo "$price" | tr -d "\r\n" | xargs)

    [ -z "$id" ] || [ "$id" = "id" ] && continue

    PKG_DESC["$id"]="$description"
    PKG_PRICE["$id"]="$price"
    AVAILABLE_PKGS+=("$id")
done < "$PACKAGES_CSV"

function show_help() {
   local pkgs_str="${AVAILABLE_PKGS[*]}"
   cat >&2 << EOF
   Usage: ${0##*/} [-p PACKAGE] [-c COUNTRY] [-u USER] [-d DURATION] [-a ADR1] [-b ADR2] [-v VAT]
   PACKAGE can be specified multiple times or comma-separated.
   Available packages from $(basename "$PACKAGES_CSV"):
   ${pkgs_str}
   Generate invoice
EOF
   exit 1
}

isPro=false
DURATION=""
PACKAGES=()
INITIAL_PRICE="$PRICE"

while getopts "p:c:u:d:a:b:v:" opt; do
    case "$opt" in
        p)
           IFS=',' read -ra PKG_ARRAY <<< "$OPTARG"
           for pkg in "${PKG_ARRAY[@]}"; do
               pkg=$(echo "$pkg" | xargs)
               [ -n "$pkg" ] && PACKAGES+=("$pkg")
           done
           ;;
        c) export COUNTRY=$OPTARG
           ;;
        u) export KUNDE=$OPTARG
           ;;
        d) DURATION=$OPTARG
           ;;
        a) export KUNDE_ADDR_A=$OPTARG
           ;;
        b) export KUNDE_ADDR_B=$OPTARG
           ;;
        v) export KUNDE_VAT=$OPTARG
           ;;
        '?')
            show_help
            ;;
    esac
done

: "${CURRENCY:=\\EUR}"

KEYS=()
ITEMS=""
TOTAL_PRICE="0"

for pkg in "${PACKAGES[@]}"; do
    if [ -z "${PKG_DESC["$pkg"]+x}" ]; then
        echo "Unknown package: '$pkg'" >&2
        echo "Available packages in $(basename "$PACKAGES_CSV"): ${AVAILABLE_PKGS[*]}" >&2
        exit 1
    fi

    item_desc="${PKG_DESC["$pkg"]}"
    default_price="${PKG_PRICE["$pkg"]}"

    if [ "$pkg" = "Professional" ]; then
        isPro=true
        continue
    fi

    item_price="$default_price"
    KEYS+=("$item_desc")
    TOTAL_PRICE=$(awk "BEGIN {print $TOTAL_PRICE + $item_price}")
    if [ -n "$ITEMS" ]; then
        ITEMS+=$'\n'
    fi
    ITEMS+="${item_desc}  &  \formatNumber{${item_price}} ${CURRENCY} \\\\"
done

if [ "$isPro" = true ]; then
    if [ -z "$DURATION" ]; then
        echo "Professional key requires duration (-d DURATION)" >&2
        show_help
    fi
    if [ -z "$INITIAL_PRICE" ]; then
        echo "with Professional key provide PRICE in environment" >&2
        exit 1
    fi
    item_desc="${PKG_DESC["Professional"]:-My Expenses Professional Licence} $DURATION months"
    item_price="$INITIAL_PRICE"
    KEYS+=("$item_desc")
    TOTAL_PRICE=$(awk "BEGIN {print $TOTAL_PRICE + $item_price}")
    if [ -n "$ITEMS" ]; then
        ITEMS+=$'\n'
    fi
    ITEMS+="${item_desc}  &  \formatNumber{${item_price}} ${CURRENCY} \\\\"
fi

# Fallback if KEY and PRICE were provided manually in environment without -p
if [ ${#KEYS[@]} -eq 0 ] && [ -n "$KEY" ] && [ -n "$INITIAL_PRICE" ]; then
    KEYS+=("$KEY")
    TOTAL_PRICE="$INITIAL_PRICE"
    ITEMS="${KEY}  &  \formatNumber{${INITIAL_PRICE}} ${CURRENCY} \\\\"
fi

if [ ${#KEYS[@]} -gt 0 ]; then
    old_ifs="$IFS"
    IFS=", "
    export KEY="${KEYS[*]}"
    IFS="$old_ifs"
fi
export PRICE="$TOTAL_PRICE"
export ITEMS="$ITEMS"

if [ -z "$PRICE" ] || [ "$PRICE" = "0" ]; then
    echo "No valid packages/price provided" >&2
    show_help
fi

if [ -z "$ADDRESS" ]; then
    echo "provide ADDRESS in environment" >&2
    exit 1
fi

if [ -z "$KEY" ] || [ -z "$COUNTRY" ] || [ -z "$KUNDE" ]; then
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
