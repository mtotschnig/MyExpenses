package org.totschnig.myexpenses.export.qif;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.text.SimpleDateFormat;
import java.util.Date;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;

import static org.junit.Assert.assertEquals;

/**
 * format "6/21' 1" -> 6/21/2001 format "6/21'01" -> 6/21/2001 format "9/18'2001 -> 9/18/2001 format "06/21/2001"
 * format "06/21/01" format "3.26.03" -> German version of quicken format "03-26-2005" -> MSMoney format format
 * "1.1.2005" -> kmymoney2 20.1.94 European dd/mm/yyyy has been confirmed
 * <p/>
 * 21/2/07 -> 02/21/2007 UK, Quicken 2007 D15/2/07
 */
@RunWith(JUnitParamsRunner.class)
public class QifUtilParseDateTest {
  private SimpleDateFormat VERIFICATION_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
  private SimpleDateFormat VERIFICATION_FORMAT_WITH_TIME = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
  private String VERIFICATION = "2001-06-21";
  private String VERIFICATION_WITH_TIME = "2001-06-21 14:58:00";

  @Test
  @Parameters({
      "6/21' 1",
      "6/21'01",
      "6/21'2001",
      "6/21/2001",
      "06/21/2001",
      "06/21/01",
      "06.21.01",
      "06.21.2001",
  })
  public void shouldParseUs(String dateString) {
    Date date = QifUtils.parseDate(dateString, QifDateFormat.US);
    assertEquals(VERIFICATION, VERIFICATION_FORMAT.format(date));
  }

  @Test
  @Parameters({
      "21/6' 1",
      "21/6'01",
      "21/6'2001",
      "21/6/2001",
      "21/06/2001",
      "21/06/01",
      "21.06.01",
      "21.06.2001",
  })
  public void shouldParseEu(String dateString) {
    Date date = QifUtils.parseDate(dateString, QifDateFormat.EU);
    assertEquals(VERIFICATION, VERIFICATION_FORMAT.format(date));
  }

  @Test
  @Parameters({
      "2001/6/21",
      "2001/06/21",
      "01/06/21"
  })
  public void shouldParseYMD(String dateString) {
    Date date = QifUtils.parseDate(dateString, QifDateFormat.YMD);
    assertEquals(VERIFICATION, VERIFICATION_FORMAT.format(date));
  }

  @Test
  @Parameters({
      "21.06.2001 14:58",
  })
  public void shouldParseWithTime(String dateTimeString) {
    Date date = QifUtils.parseDate(dateTimeString, QifDateFormat.EU);
    assertEquals(VERIFICATION_WITH_TIME, VERIFICATION_FORMAT_WITH_TIME.format(date));
  }
}
