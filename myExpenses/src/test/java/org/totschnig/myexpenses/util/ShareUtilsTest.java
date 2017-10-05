package org.totschnig.myexpenses.util;

import org.junit.Test;
import org.junit.runner.RunWith;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;

import static org.junit.Assert.assertNotNull;

@RunWith(JUnitParamsRunner.class)
public class ShareUtilsTest {

  @Test
  @Parameters({
      "ftp://login:password@my.example.org:80/my/directory",
      "mailto:john@my.example.com"
  })
  public void shouldParseUri(String target) {
    assertNotNull(ShareUtils.parseUri(target));
  }
}
