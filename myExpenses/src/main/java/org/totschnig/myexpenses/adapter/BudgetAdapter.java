package org.totschnig.myexpenses.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.databinding.BudgetRowBinding;
import org.totschnig.myexpenses.model.CurrencyUnit;
import org.totschnig.myexpenses.util.CurrencyFormatter;
import org.totschnig.myexpenses.util.UiUtils;
import org.totschnig.myexpenses.viewmodel.data.Category;

import static org.totschnig.myexpenses.activity.BudgetActivity.getBackgroundForAvailable;
import static org.totschnig.myexpenses.util.ColorUtils.getComplementColor;

public class BudgetAdapter extends CategoryTreeBaseAdapter<BudgetRowBinding> {
  OnBudgetClickListener listener;
  public interface OnBudgetClickListener {
    void onBudgetClick(Category category, Category parentItem);
  }

  public BudgetAdapter(Context ctx, CurrencyFormatter currencyFormatter, CurrencyUnit currency, OnBudgetClickListener listener) {
    super(ctx, currencyFormatter, currency, true, true, false);
    this.listener = listener;
  }

  @Override
  protected View getView(Category item, Category parentItem, View convertView, ViewGroup parent, int color, String icon) {
    final View view = super.getView(item, parentItem, convertView, parent, color, icon);
    ViewHolder holder = (ViewHolder) view.getTag();
    holder.binding.budgetContainer.budget.setText(currencyFormatter.convAmount(item.getBudget(), currency));
    holder.binding.budgetContainer.budget.setOnClickListener(view1 -> listener.onBudgetClick(item, parentItem));
    final long available = item.getBudget() + item.getSum();
    final boolean onBudget = available >= 0;
    holder.binding.budgetContainer.available.setText(currencyFormatter.convAmount(available, currency));
    holder.binding.budgetContainer.available.setBackgroundResource(getBackgroundForAvailable(onBudget));
    holder.binding.budgetContainer.available.setTextColor(context.getResources().getColor(onBudget ?  R.color.colorIncome : R.color.colorExpense));
    int progress = item.getBudget() == 0 ? 100 : Math.round(-item.getSum() * 100F / item.getBudget());
    UiUtils.configureProgress(holder.binding.budgetProgress, progress);
    holder.binding.budgetProgress.setFinishedStrokeColor(color);
    holder.binding.budgetProgress.setUnfinishedStrokeColor(getComplementColor(color));
    return view;
  }

  @Override
  TextView label(ViewHolder viewHolder) {
    return viewHolder.binding.label;
  }

  @Override
  TextView amount(ViewHolder viewHolder) {
    return viewHolder.binding.budgetContainer.amount;
  }

  @Override
  ImageView groupIndicator(ViewHolder viewHolder) {
    return viewHolder.binding.explistIndicator;
  }

  @Override
  ImageView icon(ViewHolder viewHolder) {
    return viewHolder.binding.categoryIcon;
  }

  @Override
  protected BudgetRowBinding getViewBinding(LayoutInflater inflater, ViewGroup parent) {
    return BudgetRowBinding.inflate(inflater, parent, false);
  }
}
