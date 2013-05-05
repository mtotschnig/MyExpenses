package org.totschnig.myexpenses;

/**
 * represents a tuple of success flag, and message as an R id
 * @author Michael Totschnig
 *
 */
public class Result {
  /**
   * true represents success, false failure
   */
  public boolean success;
  /**
   * a string id from {@link R} for i18n and joining with an argument
   */
  public int message;
  
  /**
   * optional argument to be passed to getString when resolving message id
   */
  public Object[] extra;
  
  public Result(boolean success) {
    this.success = success;
  }

  public Result(boolean success,int message) {
    this.success = success;
    this.message = message;
  }

  public Result(boolean success,int message,Object... extra) {
    this.success = success;
    this.message = message;
    this.extra = extra;
  }
}