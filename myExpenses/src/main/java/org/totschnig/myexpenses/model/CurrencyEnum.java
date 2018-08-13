package org.totschnig.myexpenses.model;

import android.annotation.TargetApi;
import android.os.Build;

import org.totschnig.myexpenses.util.Utils;

import java.text.Collator;
import java.util.Arrays;
import java.util.Currency;

/**
 * @see <a href="http://www.currency-iso.org/dl_iso_table_a1.xml">http://www.currency-iso.org/dl_iso_table_a1.xml</a>
 */
public enum CurrencyEnum {
  AFN("Afghani"),
  ALL("Albania Lek"),
  DZD("Algerian Dinar"),
  AOA("Angola Kwanza"),
  ARS("Argentine Peso"),
  AMD("Armenian Dram"),
  AWG("Aruban Florin"),
  AUD("Australian Dollar"),
  AZN("Azerbaijanian Manat"),
  BSD("Bahamian Dollar"),
  BHD("Bahraini Dinar"),
  BDT("Bangladesh Taka"),
  BBD("Barbados Dollar"),
  BYR("Belarussian Ruble"),
  BYN("New Belarusian ruble"),
  BZD("Belize Dollar"),
  BMD("Bermudian Dollar"),
  BTN("Bhutan Ngultrum"),
  BOB("Bolivia Boliviano"),
  BAM("Bosnia and Herzegovina Convertible Mark"),
  BWP("Botswana Pula"),
  BRL("Brazilian Real"),
  BND("Brunei Dollar"),
  BGN("Bulgarian Lev"),
  BIF("Burundi Franc"),
  KHR("Cambodia Riel"),
  CAD("Canadian Dollar"),
  CVE("Cape Verde Escudo"),
  KYD("Cayman Islands Dollar"),
  XOF("CFA Franc BCEAO"),
  XAF("CFA Franc BEAC"),
  XPF("CFP Franc"),
  CLP("Chilean Peso"),
  CNY("China Yuan Renminbi"),
  COP("Colombian Peso"),
  KMF("Comoro Franc"),
  CDF("Congolese Franc"),
  CRC("Costa Rican Colon"),
  HRK("Croatian Kuna"),
  CUP("Cuban Peso"),
  CUC("Cuba Peso Convertible"),
  CZK("Czech Koruna"),
  DKK("Danish Krone"),
  DJF("Djibouti Franc"),
  DOP("Dominican Peso"),
  XCD("East Caribbean Dollar"),
  EGP("Egyptian Pound"),
  SVC("El Salvador Colon"),
  ERN("Eritrea Nakfa"),
  ETB("Ethiopian Birr"),
  EUR("Euro"),
  FKP("Falkland Islands Pound"),
  FJD("Fiji Dollar"),
  GMD("Gambia Dalasi"),
  GEL("Georgia Lari"),
  GHS("Ghana Cedi"),
  GIP("Gibraltar Pound"),
  GTQ("Guatemala Quetzal"),
  GNF("Guinea Franc"),
  GYD("Guyana Dollar"),
  HTG("Haiti Gourde"),
  HNL("Honduras Lempira"),
  HKD("Hong Kong Dollar"),
  HUF("Hungary Forint"),
  ISK("Iceland Krona"),
  INR("Indian Rupee"),
  IDR("Indonesia Rupiah"),
  IRR("Iranian Rial"),
  IQD("Iraqi Dinar"),
  ILS("Israeli Sheqel"),
  JMD("Jamaican Dollar"),
  JPY("Japan Yen"),
  JOD("Jordanian Dinar"),
  KZT("Kazakhstan Tenge"),
  KES("Kenyan Shilling"),
  KRW("Korea Won"),
  KWD("Kuwaiti Dinar"),
  KGS("Kyrgyzstan Som"),
  LAK("Lao Kip"),
  LVL("Latvian Lats"),
  LBP("Lebanese Pound"),
  LSL("Lesotho Loti"),
  LRD("Liberian Dollar"),
  LYD("Libyan Dinar"),
  LTL("Lithuanian Litas"),
  MOP("Macao Pataca"),
  MKD("Macedonia Denar"),
  MGA("Malagasy Ariary"),
  MWK("Malawi Kwacha"),
  MYR("Malaysian Ringgit"),
  MVR("Maldives Rufiyaa"),
  MRO("Mauritania Ouguiya"),
  MUR("Mauritius Rupee"),
  MXN("Mexican Peso"),
  MDL("Moldovan Leu"),
  MNT("Mongolia Tugrik"),
  MAD("Moroccan Dirham"),
  MZN("Mozambique Metical"),
  MMK("Myanmar Kyat"),
  NAD("Namibia Dollar"),
  NPR("Nepalese Rupee"),
  ANG("Netherlands Antillean Guilder"),
  NZD("New Zealand Dollar"),
  NIO("Nicaragua Cordoba Oro"),
  NGN("Nigeria Naira"),
  KPW("North Korean Won"),
  NOK("Norwegian Krone"),
  OMR("Omani Rial"),
  PKR("Pakistan Rupee"),
  PAB("Panama Balboa"),
  PGK("Papua New Guinea Kina"),
  PYG("Paraguay Guarani"),
  PEN("Peru Nuevo Sol"),
  PHP("Philippine Peso"),
  PLN("Poland Zloty"),
  QAR("Qatari Rial"),
  RON("Romanian Leu"),
  RUB("Russian Ruble"),
  RWF("Rwanda Franc"),
  SHP("Saint Helena Pound"),
  WST("Samoa Tala"),
  STD("Sao Tome and Principe Dobra"),
  SAR("Saudi Riyal"),
  RSD("Serbian Dinar"),
  SCR("Seychelles Rupee"),
  SLL("Sierra Leone Leone"),
  SGD("Singapore Dollar"),
  SBD("Solomon Islands Dollar"),
  SOS("Somali Shilling"),
  ZAR("South Africa Rand"),
  SSP("South Sudanese Pound"),
  LKR("Sri Lanka Rupee"),
  SDG("Sudanese Pound"),
  SRD("Surinam Dollar"),
  SZL("Swaziland Lilangeni"),
  SEK("Swedish Krona"),
  CHF("Swiss Franc"),
  SYP("Syrian Pound"),
  TWD("Taiwan Dollar"),
  TJS("Tajikistan Somoni"),
  TZS("Tanzanian Shilling"),
  THB("Thai Baht"),
  TOP("Tonga Pa’anga"),
  TTD("Trinidad and Tobago Dollar"),
  TND("Tunisian Dinar"),
  TRY("Turkish Lira"),
  TMT("Turkmenistan New Manat"),
  AED("UAE Dirham"),
  UGX("Uganda Shilling"),
  UAH("Ukraine Hryvnia"),
  GBP("United Kingdom Pound Sterling"),
  UYU("Uruguayo Peso"),
  USD("US Dollar"),
  UZS("Uzbekistan Sum"),
  VUV("Vanuatu Vatu"),
  VEF("Venezuela Bolivar Fuerte"),
  VND("Vietnam Dong"),
  YER("Yemeni Rial"),
  ZMW("Zambian Kwacha"),
  ZWL("Zimbabwe Dollar"),
  XXX("No currency "),
  XAU("Gold"),
  XPD("Palladium"),
  XPT("Platinum"),
  XAG("Silver");
  // ISO 4217 code of some commonly used cryptocurrencies, for those who don't have their respective code
  // (such as Litecoin), common ticker symbol will be used.
  XBT("Bitcoin");
  XBC("Bitcoin Cash");
  LTC("Litecoin");
  ETH("Ether");
  XMR("Monero");
  XRP("Ripple");
  DASH("Dash");
  ZEC("Zcash");
  
  private String description;

  CurrencyEnum(String description) {
    this.description = description;
  }

  @TargetApi(Build.VERSION_CODES.KITKAT)
  public String toString() {
    if (Utils.hasApiLevel(Build.VERSION_CODES.KITKAT)) {
      try {
        return Currency.getInstance(name()).getDisplayName();
      } catch (IllegalArgumentException e) {
      }
    }
    return description;
  }

  public static CurrencyEnum[] sortedValues() {
    CurrencyEnum[] result = values();
    if (Utils.hasApiLevel(Build.VERSION_CODES.KITKAT)) {
      final Collator collator = Collator.getInstance();
      Arrays.sort(result, (lhs, rhs) -> {
        int classCompare = Utils.compare(lhs.sortClass(), rhs.sortClass());
        return classCompare == 0 ?
            collator.compare(lhs.toString(), rhs.toString()) : classCompare;
      });
    }
    return result;
  }

  private int sortClass() {
    switch (this) {
      case XXX:
        return 3;
      case XAU:
      case XPD:
      case XPT:
      case XAG:
        return 2;
      default:
        return 1;
    }
  }
}
