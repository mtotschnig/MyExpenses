package org.totschnig.myexpenses.activity;

import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.support.annotation.NonNull;

import com.annimon.stream.Stream;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.fragment.BudgetFragment;
import org.totschnig.myexpenses.model.AggregateAccount;
import org.totschnig.myexpenses.model.BudgetType;
import org.totschnig.myexpenses.model.Money;
import org.totschnig.myexpenses.preference.PrefKey;
import org.totschnig.myexpenses.viewmodel.BudgetViewModel;
import org.totschnig.myexpenses.viewmodel.data.Budget;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;

import eltos.simpledialogfragment.form.AmountEdit;
import eltos.simpledialogfragment.form.SimpleFormDialogWithoutDefaultFocus;
import eltos.simpledialogfragment.form.Spinner;
import eltos.simpledialogfragment.input.SimpleInputDialog;

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_AMOUNT;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TYPE;

public class BudgetActivity extends CategoryActivity<BudgetFragment> implements
    SimpleInputDialog.OnDialogResultListener {

  public static final String ACTION_BUDGET = "ACTION_BUDGET";
  private static final String NEW_BUDGET_DIALOG = "NEW_BUDGET";
  private static final String PREFKEY_PREFIX = "current_budgetType_";
  private BudgetViewModel budgetViewModel;
  private long accountId;
  @NonNull
  private String currency;
  private List<Budget> budgetList;
  private BudgetType currentType;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    setTheme(MyApplication.getThemeId());
    super.onCreate(savedInstanceState);
    budgetViewModel = ViewModelProviders.of(this).get(BudgetViewModel.class);
    if (savedInstanceState == null) {
      accountId = getIntent().getLongExtra(KEY_ACCOUNTID, 0);
      currency = getIntent().getStringExtra(KEY_CURRENCY);
      if (currency == null) {
        throw new NullPointerException();
      }
    }
    currentType = getCurrentTypeFromPreference();
    budgetViewModel.getData().observe(this, result -> {
      if (result.isEmpty()) {
        new SimpleFormDialogWithoutDefaultFocus()
            .title(R.string.menu_new_budget)
            .fields(
                Spinner.plain(KEY_TYPE).label(R.string.type)
                    .items(Stream.of(BudgetType.values())
                        .map(type -> type.getLabel(this)).toArray(String[]::new))
                    .required().preset(0),
                AmountEdit.plain(KEY_AMOUNT).label("Allocated amount").fractionDigits(2).required()
            )
            .show(this, NEW_BUDGET_DIALOG);
      } else {
        budgetList = result;
        mListFragment.setBudget(Stream.of(budgetList).filter(
            budget -> budget.getType().equals(getCurrentTypeFromPreference())).findFirst().orElse(budgetList.get(0)));
      }
    });
    budgetViewModel.loadBudgets(accountId, currency,
        cursor -> {
          final boolean isHomeAggregate = isHomeAggregate();
          Currency currency = getCurrency();
          return new Budget(accountId, currency, BudgetType.valueOf(cursor.getString(1)), new Money(currency, cursor.getLong(2)), isHomeAggregate);
        });
  }

  private BudgetType getCurrentTypeFromPreference() {
    final String typeFromPreference = prefHandler.getString(getPrefKey(), null);
    if (typeFromPreference != null) {
      try {
        return BudgetType.valueOf(typeFromPreference);
      } catch (IllegalArgumentException ignored) { }
    }
    return BudgetType.MONTHLY;
  }

  @NonNull
  private String getPrefKey() {
    return PREFKEY_PREFIX + (accountId != 0 ? String.valueOf(accountId) : currency);
  }

  @NonNull
  @Override
  public String getAction() {
    return ACTION_BUDGET;
  }

  @Override
  protected int getContentView() {
    return R.layout.activity_budget;
  }

  @Override
  public boolean onResult(@NonNull String dialogTag, int which, @NonNull Bundle extras) {
    if (dialogTag.equals(NEW_BUDGET_DIALOG)) {
      final boolean isHomeAggregate = isHomeAggregate();
      Currency currency = getCurrency();

      currentType = BudgetType.values()[extras.getInt(KEY_TYPE)];
      prefHandler.putString(getPrefKey(), currentType.name());
      Budget budget = new Budget(accountId, currency, currentType,
          new Money(currency, (BigDecimal) extras.getSerializable(KEY_AMOUNT)), isHomeAggregate);
      budgetViewModel.createBudget(budget);
      return true;
    }
    return false;
  }

  protected Currency getCurrency() {
    return Currency.getInstance(isHomeAggregate() ?
            prefHandler.getString(PrefKey.HOME_CURRENCY, "EUR") : this.currency);
  }

  protected boolean isHomeAggregate() {
    return AggregateAccount.AGGREGATE_HOME_CURRENCY_CODE.equals(this.currency);
  }
}
