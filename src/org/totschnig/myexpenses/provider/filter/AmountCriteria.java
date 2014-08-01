package org.totschnig.myexpenses.provider.filter;

import java.math.BigDecimal;
import java.util.Currency;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.AmountActivity;
import org.totschnig.myexpenses.model.Money;
import org.totschnig.myexpenses.provider.DatabaseConstants;
import org.totschnig.myexpenses.provider.filter.WhereFilter.Operation;
import org.totschnig.myexpenses.util.Utils;

public class AmountCriteria extends Criteria {
  private boolean type;
  private Currency currency;
  private Operation origOperation;
  private BigDecimal origValue1,origValue2;

  public AmountCriteria(Operation operation, Currency currency, boolean type, BigDecimal... values) {
    super(transformCriteria(operation,currency,type,values));
    this.type = type;
    this.currency = currency;
    this.origOperation=operation;
    this.origValue1=values[0];
    this.origValue2=values[1];
  }
  @Override
  public String prettyPrint() {
    String result = MyApplication.getInstance().getString(
        type == AmountActivity.EXPENSE ? R.string.expense : R.string.income) + ", ";
    String amount1 = Utils.formatCurrency(new Money(currency,origValue1.abs()));
    switch (origOperation) {
    case EQ:
      result += "= " + amount1;
      break;
    case GTE:
      result += "≥ " + amount1;
      break;
    case LTE:
      result += "≤ " + amount1;
      break;
    case BTW:
      String amount2 = Utils.formatCurrency(new Money(currency,origValue2.abs()));
      result += MyApplication.getInstance().getString(R.string.between_and,amount1,amount2);
    }
    return result;
  }
  private static Criteria transformCriteria(Operation operation, Currency currency, boolean type,BigDecimal... values) {
    switch(operation) {
    case BTW:
    case EQ:
    case GTE:
    case LTE:
      break;
    default:
      throw new UnsupportedOperationException("Operator not supported: "+operation.name());
    }
    Long longAmount1,longAmount2;
    if (type == AmountActivity.EXPENSE) {
      values[0] = values[0].negate();
    }
    longAmount1 = new Money(
        currency,
        values[0])
      .getAmountMinor();
    if (operation==Operation.BTW) {
      if (values[1]==null) {
        throw new UnsupportedOperationException("Operator BTW needs two values");
      }
      if (type == AmountActivity.EXPENSE) {
        values[1] = values[1].negate();
      }
      longAmount2 = new Money(
          currency,
          values[1])
        .getAmountMinor();
      boolean needSwap = longAmount2<longAmount1;
      return new Criteria(
          DatabaseConstants.KEY_AMOUNT,
          WhereFilter.Operation.BTW,
          String.valueOf(needSwap?longAmount2:longAmount1),
          String.valueOf(needSwap?longAmount1:longAmount2));
    }
    if (type == AmountActivity.EXPENSE) {
      if (operation==Operation.GTE) {
        operation=Operation.LTE;
      } else if (operation==Operation.LTE) {
        return new Criteria(
            DatabaseConstants.KEY_AMOUNT,
            WhereFilter.Operation.BTW,
            String.valueOf(longAmount1),
            "0");
      }
    } else {
      if (operation==Operation.LTE) {
        return new Criteria(
            DatabaseConstants.KEY_AMOUNT,
            WhereFilter.Operation.BTW,
            "0",
            String.valueOf(longAmount1));
      }
    }
    return new Criteria(
        DatabaseConstants.KEY_AMOUNT,
        operation,
        String.valueOf(longAmount1));
  }
}
