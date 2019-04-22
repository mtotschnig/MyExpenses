package org.totschnig.myexpenses.adapter;

import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity;
import org.totschnig.myexpenses.model.CurrencyUnit;
import org.totschnig.myexpenses.util.CurrencyFormatter;
import org.totschnig.myexpenses.viewmodel.data.Category;

import butterknife.BindView;

import static org.totschnig.myexpenses.util.ColorUtils.createBackgroundColorDrawable;

public class CategoryTreeAdapter extends CategoryTreeBaseAdapter {
  public CategoryTreeAdapter(ProtectedFragmentActivity ctx, CurrencyFormatter currencyFormatter,
                             CurrencyUnit currency, boolean withMainColors, boolean withSubColors,
                             boolean withNullCategory) {
    super(ctx, currencyFormatter, currency, withMainColors, withSubColors, withNullCategory);
  }

  @Override
  protected View getView(Category item, Category parentItem, View convertView, ViewGroup parent, int color, String icon) {
    final View view = super.getView(item, parentItem, convertView, parent, color, icon);
    ViewHolder holder = (ViewHolder) view.getTag();
    if (item.sum != null) {
      holder.amount.setTextColor(item.sum < 0 ? colorExpense : colorIncome);
    }
    holder.color.setVisibility(color != 0 ? View.VISIBLE :
        (withMainColors ? View.INVISIBLE : View.GONE));
    if (color != 0) {
      holder.color.setBackgroundDrawable(createBackgroundColorDrawable(color));
    }
    if (icon != null) {
      holder.icon.setImageResource(context.getResources().getIdentifier(icon, "drawable", context.getPackageName()));
    }
    return view;
  }

  @Override
  protected int getLayoutResourceId() {
    return R.layout.category_row;
  }


  @NonNull
  protected CategoryTreeBaseAdapter.ViewHolder getHolder(View convertView) {
    return new ViewHolder(convertView);
  }

  class ViewHolder extends CategoryTreeBaseAdapter.ViewHolder {
    @BindView(R.id.color1)
    View color;
    @BindView(R.id.category_icon)
    ImageView icon;

    ViewHolder(View view) {
      super(view);
    }
  }
}
