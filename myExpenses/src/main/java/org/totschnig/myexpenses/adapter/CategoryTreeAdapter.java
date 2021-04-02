package org.totschnig.myexpenses.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.databinding.CategoryRowBinding;
import org.totschnig.myexpenses.model.CurrencyUnit;
import org.totschnig.myexpenses.util.CurrencyFormatter;
import org.totschnig.myexpenses.viewmodel.data.Category;

import static org.totschnig.myexpenses.util.ColorUtils.createBackgroundColorDrawable;

public class CategoryTreeAdapter extends CategoryTreeBaseAdapter<CategoryRowBinding> {
  public CategoryTreeAdapter(Context ctx, CurrencyFormatter currencyFormatter,
                             CurrencyUnit currency, boolean withMainColors, boolean withSubColors,
                             boolean withNullCategory) {
    super(ctx, currencyFormatter, currency, withMainColors, withSubColors, withNullCategory);
  }

  @Override
  protected View getView(Category item, Category parentItem, View convertView, ViewGroup parent, int color, String icon) {
    final View view = super.getView(item, parentItem, convertView, parent, color, icon);
    ViewHolder holder = (ViewHolder) view.getTag();
    if (item.getSum() != null) {
      amount(holder).setTextColor(context.getResources().getColor(item.getSum() >= 0 ?  R.color.colorIncome : R.color.colorExpense));
    }
    holder.binding.color.setVisibility(color != 0 ? View.VISIBLE :
        (withMainColors ? View.INVISIBLE : View.GONE));
    if (color != 0) {
      holder.binding.color.setBackground(createBackgroundColorDrawable(color));
    }
    return view;
  }

  @Override
  TextView label(ViewHolder viewHolder) {
    return viewHolder.binding.label;
  }

  @Override
  TextView amount(ViewHolder viewHolder) {
    return viewHolder.binding.amount;
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
  protected CategoryRowBinding getViewBinding(LayoutInflater inflater, ViewGroup parent) {
    return CategoryRowBinding.inflate(inflater, parent, false);
  }
}
