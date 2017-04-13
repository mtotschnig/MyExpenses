package org.totschnig.myexpenses.util;

import org.junit.Before;

//TODO postponed until Money is refactored to not depende on Android context, or migrated to JSR 354
public class CurrencyFormatterTest {
  private CurrencyFormatter currencyFormatter;

  @Before
  public void setUp() {
    currencyFormatter = CurrencyFormatter.instance();
  }
}
