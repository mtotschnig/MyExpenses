package org.totschnig.myexpenses.adapter;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Typeface;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity;
import org.totschnig.myexpenses.model.CurrencyUnit;
import org.totschnig.myexpenses.provider.DbUtils;
import org.totschnig.myexpenses.util.CurrencyFormatter;
import org.totschnig.myexpenses.util.UiUtils;
import org.totschnig.myexpenses.viewmodel.data.Category;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.collection.LongSparseArray;
import butterknife.BindView;
import butterknife.ButterKnife;

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_BUDGET;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_COLOR;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ICON;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_MAPPED_BUDGETS;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PARENTID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SUM;
import static org.totschnig.myexpenses.util.ColorUtils.getShades;
import static org.totschnig.myexpenses.util.ColorUtils.getTints;

public abstract class CategoryTreeBaseAdapter extends BaseExpandableListAdapter {
  protected final CurrencyUnit currency;
  private List<Category> mainCategories = new ArrayList<>();
  private SparseArray<List<Integer>> subColorMap = new SparseArray<>();
  final Context context;
  private final LayoutInflater inflater;
  protected final CurrencyFormatter currencyFormatter;
  protected boolean withMainColors;
  private boolean withSubColors;
  private final boolean withNullCategory;
  public static final long NULL_ITEM_ID = -1L;

  public CategoryTreeBaseAdapter(ProtectedFragmentActivity ctx, CurrencyFormatter currencyFormatter,
                                 CurrencyUnit currency, boolean withMainColors, boolean withSubColors, boolean withNullCategory) {
    this.context = ctx;
    inflater = LayoutInflater.from(ctx);
    this.currencyFormatter = currencyFormatter;
    this.currency = currency;
    this.withMainColors = withMainColors;
    this.withSubColors = withSubColors;
    this.withNullCategory = withNullCategory;
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
    if (groupPosition > getGroupCount() || childPosition >= getChildrenCount(groupPosition))
      return 0;
    return getChild(groupPosition, childPosition).id;
  }

  @Override
  public boolean hasStableIds() {
    return true;
  }

  @Override
  public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
    final Category item = getGroup(groupPosition);
    final View view = getView(item, null, convertView, parent, withMainColors ? item.color : 0, item.icon);
    ImageView indicator = ((ViewHolder) view.getTag()).groupIndicator;
    if (getChildrenCount(groupPosition) == 0) {
      indicator.setImageResource(R.drawable.expander_empty);
      indicator.setContentDescription("No children");
    } else {
      indicator.setImageResource(isExpanded ? R.drawable.expander_close_mtrl_alpha : R.drawable.expander_open_mtrl_alpha);
      indicator.setContentDescription(context.getString(isExpanded ?
          R.string.content_description_collapse : R.string.content_description_expand));
    }
    return view;
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
    final View view = getView(item, parentCat, convertView, parent, color, item.icon);
    ((ViewHolder) view.getTag()).groupIndicator.setVisibility(View.INVISIBLE);
    return view;
  }

  protected View getView(Category item, Category parentItem, View convertView, ViewGroup parent, int color, String icon) {
    ViewHolder holder;
    if (convertView == null) {
      convertView = inflater.inflate(getLayoutResourceId(), parent, false);
      holder = getHolder(convertView);
      convertView.setTag(holder);
    } else {
      holder = (ViewHolder) convertView.getTag();
    }
    holder.label.setText(item.label);
    holder.label.setTypeface(holder.label.getTypeface(), parentItem == null ? Typeface.BOLD : Typeface.NORMAL);
    if (item.sum != null && currency != null) {
      holder.amount.setText(currencyFormatter.convAmount(item.sum, currency));
    }
    holder.icon.setImageResource(icon != null ? context.getResources().getIdentifier(icon, "drawable", context.getPackageName()) : 0);
    return convertView;
  }

  protected abstract int getLayoutResourceId();

  @NonNull
  protected abstract ViewHolder getHolder(View convertView);

  @Override
  public boolean isChildSelectable(int groupPosition, int childPosition) {
    return true;
  }

  /**
   * This method expects the main categories to be sorted first
   *
   * @param cursor
   */
  public void ingest(Cursor cursor) {
    if (cursor != null) {
      try {
        List<Category> newList = new ArrayList<>();
        LongSparseArray<Integer> positionMap = new LongSparseArray<>();
        int position = 0;
        if (withNullCategory) {
          newList.add(new Category(NULL_ITEM_ID, null, context.getString(R.string.unmapped), null, null, 0, null, null));
          position = 1;
        }
        final int columnIndexRowId = cursor.getColumnIndex(KEY_ROWID);
        final int columnIndexParentId = cursor.getColumnIndex(KEY_PARENTID);
        final int columnIndexSum = cursor.getColumnIndex(KEY_SUM);
        final int columnIndexBudget = cursor.getColumnIndex(KEY_BUDGET);
        final int columnIndexMapBudgets = cursor.getColumnIndex(KEY_MAPPED_BUDGETS);
        final int columnIndexColor = cursor.getColumnIndex(KEY_COLOR);
        final int columIndexIcon = cursor.getColumnIndex(KEY_ICON);
        while (cursor.moveToNext()) {
          final long id = cursor.getLong(columnIndexRowId);
          final Long parentId = DbUtils.getLongOrNull(cursor, columnIndexParentId);
          final Category category = new Category(
              id, parentId, cursor.getString(cursor.getColumnIndex(KEY_LABEL)),
              columnIndexSum == -1 ? null : cursor.getLong(columnIndexSum),
              columnIndexMapBudgets == -1 ? null : cursor.getInt(columnIndexMapBudgets) > 0,
              cursor.getInt(columnIndexColor),
              columnIndexBudget == -1 ? null : cursor.getLong(columnIndexBudget), cursor.getString(columIndexIcon));
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
    boolean isLight =  UiUtils.themeBoolAttr(context, R.attr.isLightTheme);
    List<Integer> result = subColorMap.get(color);
    if (result == null) {
      result = isLight ? getShades(color) : getTints(color);
      subColorMap.put(color, result);
    }
    return result;
  }

  public void toggleColors() {
    withSubColors = !withSubColors;
    withMainColors = !withMainColors;
    notifyDataSetChanged();
  }

  static class ViewHolder {
    @BindView(R.id.label)
    TextView label;
    @BindView(R.id.amount)
    TextView amount;
    @BindView(R.id.explist_indicator)
    ImageView groupIndicator;
    @BindView(R.id.category_icon)
    ImageView icon;

    ViewHolder(View view) {
      ButterKnife.bind(this, view);
    }
  }

}
