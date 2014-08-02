package org.totschnig.myexpenses.provider.filter;

public class TextCriteria extends Criteria {
  private String searchString;
  public TextCriteria(String title, String columnName, String searchString) {
    super(columnName, WhereFilter.Operation.LIKE, "%"+searchString+"%");
    this.searchString = searchString;
    this.title = title;
  }
  @Override
  public String prettyPrint() {
    return prettyPrintInternal(searchString);
  }
}
