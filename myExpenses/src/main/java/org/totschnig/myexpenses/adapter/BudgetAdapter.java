package org.totschnig.myexpenses.adapter;

import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.github.lzyzsd.circleprogress.DonutProgress;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity;
import org.totschnig.myexpenses.model.CurrencyUnit;
import org.totschnig.myexpenses.util.CurrencyFormatter;
import org.totschnig.myexpenses.util.UiUtils;
import org.totschnig.myexpenses.viewmodel.data.Category;

import androidx.annotation.NonNull;
import butterknife.BindView;

import static org.totschnig.myexpenses.activity.BudgetActivity.getBackgroundForAvailable;
import static org.totschnig.myexpenses.util.ColorUtils.getContrastColor;

public class BudgetAdapter extends CategoryTreeBaseAdapter {
  OnBudgetClickListener listener;
  public interface OnBudgetClickListener {
    void onBudgetClick(Category category, Category parentItem);
  }

  public BudgetAdapter(ProtectedFragmentActivity ctx, CurrencyFormatter currencyFormatter, CurrencyUnit currency, OnBudgetClickListener listener) {
    super(ctx, currencyFormatter, currency, true, true, false);
    this.listener = listener;
  }

  @NonNull
  @Override
  protected CategoryTreeBaseAdapter.ViewHolder getHolder(View convertView) {
    return new ViewHolder(convertView);
  }

  @Override
  protected View getView(Category item, Category parentItem, View convertView, ViewGroup parent, int color, String icon) {
    final View view = super.getView(item, parentItem, convertView, parent, color, icon);
    ViewHolder holder = (ViewHolder) view.getTag();
    holder.budget.setText(currencyFormatter.convAmount(item.budget, currency));
    holder.budget.setOnClickListener(view1 -> listener.onBudgetClick(item, parentItem));
    final long available = item.budget + item.sum;
    final boolean onBudget = available >= 0;
    holder.available.setText(currencyFormatter.convAmount(available, currency));
    holder.available.setBackgroundResource(getBackgroundForAvailable(onBudget, context));
    holder.available.setTextColor(onBudget ? colorIncome : colorExpense);
    int progress = item.budget == 0 ? 100 : Math.round(-item.sum * 100F / item.budget);
    UiUtils.configureProgress(holder.budgetProgress, progress);
    holder.budgetProgress.setFinishedStrokeColor(color);
    holder.budgetProgress.setUnfinishedStrokeColor(getContrastColor(color));
    return view;
  }

  @Override
  protected int getLayoutResourceId() {
    return R.layout.budget_row;
  }

  class ViewHolder extends CategoryTreeBaseAdapter.ViewHolder {
    @BindView(R.id.budget) TextView budget;
    @BindView(R.id.available) TextView available;
    @BindView(R.id.budgetProgress) DonutProgress budgetProgress;
    ViewHolder(View view) {
      super(view);
    }
  }
}
