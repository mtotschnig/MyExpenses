package org.totschnig.myexpenses.adapter;

import android.database.Cursor;
import android.support.v4.util.LongSparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity;
import org.totschnig.myexpenses.provider.DbUtils;
import org.totschnig.myexpenses.util.CurrencyFormatter;

import java.util.ArrayList;
import java.util.Currency;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_MAPPED_TEMPLATES;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_MAPPED_TRANSACTIONS;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PARENTID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SUM;

public class CategoryTreeAdapter extends BaseExpandableListAdapter {
  private final Currency currency;
  private List<Category> mainCategories = new ArrayList<>();
  private LongSparseArray<Integer> positionMap = new LongSparseArray<>();
  private final LayoutInflater inflater;
  private final CurrencyFormatter currencyFormatter;
  private final int colorExpense;
  private final int colorIncome;

  public CategoryTreeAdapter(ProtectedFragmentActivity ctx, CurrencyFormatter currencyFormatter, Currency currency) {
    inflater = LayoutInflater.from(ctx);
    this.currencyFormatter = currencyFormatter;
    this.currency = currency;
    this.colorExpense = ctx.getColorExpense();
    this.colorIncome = ctx.getColorIncome();
  }

  @Override
  public int getGroupCount() {
    return mainCategories.size();
  }

  public List<Category> getMainCategories() {
    return mainCategories;
  }

  public List<Category> getSubCategories(int groupPosition) {
    return getGroup(groupPosition).children;
  }

  @Override
  public int getChildrenCount(int groupPosition) {
    return mainCategories.get(groupPosition).children.size();
  }

  @Override
  public Category getGroup(int groupPosition) {
    return mainCategories.get(groupPosition);
  }

  @Override
  public Category getChild(int groupPosition, int childPosition) {
    return getGroup(groupPosition).children.get(childPosition);
  }

  @Override
  public long getGroupId(int groupPosition) {
    return getGroup(groupPosition).id;
  }

  @Override
  public long getChildId(int groupPosition, int childPosition) {
    return getChild(groupPosition, childPosition).id;
  }

  @Override
  public boolean hasStableIds() {
    return true;
  }

  @Override
  public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
    return getView(getGroup(groupPosition), convertView, parent);
  }

  @Override
  public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
    return getView(getChild(groupPosition, childPosition), convertView, parent);
  }

  private View getView(Category item, View convertView, ViewGroup parent) {
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
        while (cursor.moveToNext()) {
          final long id = cursor.getLong(columnIndexRowId);
          final Long parentId = DbUtils.getLongOrNull(cursor, columnIndexParentId);
          final Category category = new Category(
              id, parentId, cursor.getString(cursor.getColumnIndex(KEY_LABEL)),
              columnIndexSum == -1 ? null : cursor.getLong(columnIndexSum),
              columnIndexMapTemplates == -1 ? null : cursor.getInt(columnIndexMapTemplates) > 0,
              columnIndexMapTransactions == -1 ? null : cursor.getInt(columnIndexMapTransactions) > 0);
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

  class ViewHolder {
    @BindView(R.id.color1) View color;
    @BindView(R.id.label) TextView label;
    @BindView(R.id.amount) TextView amount;
    ViewHolder(View view) {
      ButterKnife.bind(this, view);
    }
  }

  public class Category {
    public final long id;
    public final Long parentId;
    public final String label;
    public final Long sum;
    public final Boolean hasMappedTemplates;
    public final Boolean hasMappedTransactions;
    List<Category> children = new ArrayList<>();

    public Category(long id, Long parentId, String label, Long sum, Boolean hasMappedTemplates,
                    Boolean hasMappedTransactions) {
      this.id = id;
      this.parentId = parentId;
      this.label = label;
      this.sum = sum;
      this.hasMappedTemplates = hasMappedTemplates;
      this.hasMappedTransactions = hasMappedTransactions;
    }

    void addChild(Category child) {
      if (child.parentId != id) {
        throw new IllegalStateException("Cannot accept child with wrong parent");
      }
      children.add(child);
    }

    public boolean hasChildren() {
      return !children.isEmpty();
    }
  }
}
