package org.totschnig.myexpenses.util.licence;

public enum Package {
  Contrib(300), Upgrade(200), Extended(500), Professional_6(500), Professional_36(2000);

  public long getDefaultPprice() {
    return defaultPprice;
  }

  private final long defaultPprice;

  Package(long price) {
    this.defaultPprice = price;
  }
}
