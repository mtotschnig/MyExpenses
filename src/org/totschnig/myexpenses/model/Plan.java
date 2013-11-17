package org.totschnig.myexpenses.model;

import java.io.Serializable;

/**
 * @author Michael Totschnig
 * holds information about an event in the calendar
 */
public class Plan implements Serializable {
  public long id;
  public long dtstart;
  public long dtend;
  public String rrule;
  public String title;
  public Plan(long id, long dtstart, long dtend, String rrule, String title) {
    super();
    this.id = id;
    this.dtstart = dtstart;
    this.dtend = dtend;
    this.rrule = rrule;
    this.title = title;
  }
}
