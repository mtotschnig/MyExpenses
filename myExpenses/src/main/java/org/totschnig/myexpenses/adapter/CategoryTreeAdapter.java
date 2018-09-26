package org.totschnig.myexpenses.adapter;

import android.database.Cursor;
import android.support.v4.util.LongSparseArray;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity;
import org.totschnig.myexpenses.provider.DbUtils;
import org.totschnig.myexpenses.util.CurrencyFormatter;
import org.totschnig.myexpenses.viewmodel.data.Category;

import java.util.ArrayList;
import java.util.Currency;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_COLOR;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_MAPPED_TEMPLATES;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_MAPPED_TRANSACTIONS;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PARENTID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SUM;
import static org.totschnig.myexpenses.util.ColorUtils.createBackgroundColorDrawable;
import static org.totschnig.myexpenses.util.ColorUtils.getShades;
import static org.totschnig.myexpenses.util.ColorUtils.getTints;

public class CategoryTreeAdapter extends BaseExpandableListAdapter {
  private final Currency currency;
  private List<Category> mainCategories = new ArrayList<>();
  private LongSparseArray<Integer> positionMap = new LongSparseArray<>();
  private SparseArray<List<Integer>> subColorMap = new SparseArray<>();
  private final LayoutInflater inflater;
  private final CurrencyFormatter currencyFormatter;
  private final int colorExpense;
  private final int colorIncome;
  private boolean withMainColors;
  private boolean withSubColors;

  public CategoryTreeAdapter(ProtectedFragmentActivity ctx, CurrencyFormatter currencyFormatter,
                             Currency currency, boolean withMainColors, boolean withSubColors) {
    inflater = LayoutInflater.from(ctx);
    this.currencyFormatter = currencyFormatter;
    this.currency = currency;
    this.colorExpense = ctx.getColorExpense();
    this.colorIncome = ctx.getColorIncome();
    this.withMainColors = withMainColors;
    this.withSubColors = withSubColors;
  }

  @Override
  public int getGroupCount() {
    return mainCategories.size();
  }

  public List<Category> getMainCategories() {
    return mainCategories;
  }

  public List<Category> getSubCategories(int groupPosition) {
    return getGroup(groupPosition).getChildren();
  }

  @Override
  public int getChildrenCount(int groupPosition) {
    return mainCategories.get(groupPosition).getChildCount();
  }

  @Override
  public Category getGroup(int groupPosition) {
    return mainCategories.get(groupPosition);
  }

  @Override
  public Category getChild(int groupPosition, int childPosition) {
    return getGroup(groupPosition).getChildAt(childPosition);
  }

  @Override
  public long getGroupId(int groupPosition) {
    if (groupPosition >= getGroupCount()) return 0;
    return getGroup(groupPosition).id;
  }

  @Override
  public long getChildId(int groupPosition, int childPosition) {
    if (groupPosition > getGroupCount() || childPosition >= getChildrenCount(groupPosition)) return 0;
    return getChild(groupPosition, childPosition).id;
  }

  @Override
  public boolean hasStableIds() {
    return true;
  }

  @Override
  public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
    final Category item = getGroup(groupPosition);
    return getView(item, convertView, parent, withMainColors ? item.color : 0);
  }

  @Override
  public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
    final Category parentCat = getGroup(groupPosition);
    final Category item = getChild(groupPosition, childPosition);
    int color = 0;
    if (withSubColors) {
      final List<Integer> subColors = getSubColors(parentCat.color);
      color = subColors.get(childPosition % subColors.size());
    }
    return getView(item, convertView, parent, color);
  }

  private View getView(Category item, View convertView, ViewGroup parent, int color) {
    ViewHolder holder;
    if (convertView == null) {
      convertView = inflater.inflate(R.layout.category_row, parent, false);
      holder = new ViewHolder(convertView);
      convertView.setTag(holder);
    } else {
      holder = (ViewHolder) convertView.getTag();
    }
    holder.label.setText(item.label);
    if (item.sum != null && currency != null) {
      holder.amount.setTextColor(item.sum < 0 ? colorExpense : colorIncome);
      holder.amount.setText(currencyFormatter.convAmount(item.sum, currency));
    }
    holder.color.setVisibility(color != 0 ? View.VISIBLE :
        (withMainColors ? View.INVISIBLE : View.GONE));
    if (color != 0) {
      holder.color.setBackgroundDrawable(createBackgroundColorDrawable(color));
    }

    return convertView;
  }

  @Override
  public boolean isChildSelectable(int groupPosition, int childPosition) {
    return true;
  }

  /**
   * This method expects the main categories to be sorted first
   * @param cursor
   */
  public void ingest(Cursor cursor) {
    if (cursor != null) {
      try {
        List<Category> newList = new ArrayList<>();
        int position = 0;
        final int columnIndexRowId = cursor.getColumnIndex(KEY_ROWID);
        final int columnIndexParentId = cursor.getColumnIndex(KEY_PARENTID);
        final int columnIndexSum = cursor.getColumnIndex(KEY_SUM);
        final int columnIndexMapTransactions = cursor.getColumnIndex(KEY_MAPPED_TRANSACTIONS);
        final int columnIndexMapTemplates = cursor.getColumnIndex(KEY_MAPPED_TEMPLATES);
        final int columnIndexColor = cursor.getColumnIndex(KEY_COLOR);
        while (cursor.moveToNext()) {
          final long id = cursor.getLong(columnIndexRowId);
          final Long parentId = DbUtils.getLongOrNull(cursor, columnIndexParentId);
          final Category category = new Category(
              id, parentId, cursor.getString(cursor.getColumnIndex(KEY_LABEL)),
              columnIndexSum == -1 ? null : cursor.getLong(columnIndexSum),
              columnIndexMapTemplates == -1 ? null : cursor.getInt(columnIndexMapTemplates) > 0,
              columnIndexMapTransactions == -1 ? null : cursor.getInt(columnIndexMapTransactions) > 0,
              cursor.getInt(columnIndexColor));
          if (parentId == null) {
            newList.add(category);
            positionMap.put(id, position);
            position++;
          } else {
            final Integer catPosition = positionMap.get(parentId);
            if (catPosition != null) {
              newList.get(catPosition).addChild(category);
            }
          }
        }
        mainCategories = newList;
      } finally {
        cursor.close();
      }
      notifyDataSetChanged();
    }
  }

  public List<Integer> getSubColors(int color) {
    List<Integer> result = subColorMap.get(color);
    if (result == null) {
      result = MyApplication.getThemeType().equals(MyApplication.ThemeType.dark) ?
          getTints(color) : getShades(color);
      subColorMap.put(color, result);
    }
    return result;
  }

  public void toggleColors() {
    withSubColors = !withSubColors;
    withMainColors = !withMainColors;
    notifyDataSetChanged();
  }

  class ViewHolder {
    @BindView(R.id.color1) View color;
    @BindView(R.id.label) TextView label;
    @BindView(R.id.amount) TextView amount;
    ViewHolder(View view) {
      ButterKnife.bind(this, view);
    }
  }

}
