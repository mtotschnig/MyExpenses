#!/usr/bin/env bash
set -e
tail -n +2 $1 | xsv fmt -t ";" | while IFS=";" read -r DATE GROSS FEE TX EMAIL USER VAT COUNTRY PACKAGE; do
  VAT=${VAT/-/}
  generateInvoicePaypal.sh -g "${GROSS/,/.}" -t "$TX" -p "$PACKAGE" -c "$COUNTRY" -u "${USER/&/\\&}" -e "${EMAIL/_/\\_}" -v "${VAT/,/.}" -d "$DATE"
done
