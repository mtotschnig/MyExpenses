package org.totschnig.myexpenses.provider.filter;

public class TextCriteria extends Criteria {
  private String searchString;
  public TextCriteria(String columnName, String searchString) {
    super(columnName, WhereFilter.Operation.LIKE, "%"+searchString+"%");
    this.searchString = searchString;
  }
  @Override
  public String prettyPrint() {
    return searchString;
  }
}
