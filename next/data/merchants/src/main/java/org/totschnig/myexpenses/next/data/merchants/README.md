# Merchant logos and payee icons

The transaction list shows a tile for the payee of each transaction: the icon or logo the user
chose, else the logo of a well-known merchant, else the first letter of the payee on a color of
the theme, else, without payee, the icon of the category. The tile is `TransactionTile`, built
from `PayeeTile` of the design system. Other screens, e.g. of contracts, show the same tile for
their payee.

The diagram [`logos.puml`](logos.puml) shows where a logo or icon comes from. Render it with the PlantUML
plugin of Android Studio or `java -jar plantuml.jar logos.puml`.

## Recognizing merchants

`Merchants` is a fixed catalog: name, domain and the words a bank writes for the merchant, e.g.
"REWE Markt GmbH Fil. 4711". Long patterns start a word of the payee, short ones like "dm" or
"OBI" must be whole words. If several merchants are named, the first one wins: "PayPal … Netflix"
is PayPal, which takes the money. A payee that is not recognized itself takes the merchant of
the payee it belongs to (payees can be grouped under another one in the payee list).

The saving tips take their supermarkets from the same catalog, so each merchant is in one place.

## Loading logos

Logos are only loaded after the user agreed in the list settings (`MerchantLogoConsent`, off by
default), since the icon service learns the domains of the merchants the user pays. Only the
domain is sent, e.g. `rewe.de`, never amounts or transactions.

- `MerchantLogoStore` asks `icons.duckduckgo.com/ip3/<domain>.ico` once per domain and keeps the
  logo as PNG of at most 128 px in `noBackupFilesDir/merchant-logos`, not part of backups.
- An answer 404 or an icon smaller than 32 px counts as missing and is asked again after 30 days;
  the monogram looks better than a blurred favicon. Without network it is asked on the next start.
- `MerchantLogos` reads the payees, finds their merchants, loads each domain once in the
  background and offers the logo of each payee. The list shows the monogram until the logo is there.
- Withdrawing the consent removes all stored logos.

The service has no icon for some domains, e.g. lidl.de, klarna.com and dazn.com; these merchants
keep the monogram.

Without consent, merchants show an icon of Font Awesome instead (`MerchantIcons`): their brand
where the font has one, e.g. Amazon or Spotify, else what they are for, e.g. a petrol pump for
Shell. Nothing is sent for them.

## Web addresses of payees

Like KeePassXC loads the icon of an entry, the user can tap the tile in the details of a
transaction and enter the web address of its payee (`PayeeLogoDialog`). Tapping "Load" is the
consent for this one address, the switch is not needed. The logo is loaded at once, asked again
even if the domain had none recently; only if there is one, the address is kept for the payee in
`PayeeDomainStore` (data store, part of the backup), else the dialog says so.

## Icons of payees

Tapping the tile in the details opens `PayeeImageDialog`, an icon picker (`CatalogIconPicker`,
see the package `icons`) with three kinds of choice:

- an icon of the catalog, suggested from the icon of the category, the name of the payee and the
  category path; kept in `PayeeIconStore` (data store, part of the backup),
- "Logo from web address", which opens `PayeeLogoDialog` above,
- "Automatic", which forgets both.

A payee has either an icon or a web address: choosing one removes the other.

What a payee shows (`payeeLooks`) is what was chosen for it, else what was chosen for the payee it
belongs to, else for its merchant the logo with consent, the icon without. `MerchantLogos.images`
turns this into a `PayeeImage`, a logo once it is loaded or an icon. Screens that are not the
transaction list, e.g. the contracts, read it through `LocalPayeeImages`. Withdrawing the consent
keeps the logos of the addresses entered.
