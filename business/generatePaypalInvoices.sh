#!/usr/bin/env bash
set -e
count=0
tail -n +2 $2 | xsv fmt -t ";" | while IFS=";" read -r DATE GROSS TX EMAIL USER VAT COUNTRY PACKAGE; do
  count=$((count+1))
  VAT=${VAT/-/}
  generateInvoicePaypal.sh -g "${GROSS/,/.}" -t "$TX" -p "$PACKAGE" -c "$COUNTRY" -u "${USER/&/\\&}" -e "${EMAIL//_/\\_}" -v "${VAT/,/.}" -d "$DATE" -n "${1}-$count"
done
