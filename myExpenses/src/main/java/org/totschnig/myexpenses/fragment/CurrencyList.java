package org.totschnig.myexpenses.fragment;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity;
import org.totschnig.myexpenses.adapter.CurrencyAdapter;
import org.totschnig.myexpenses.dialog.EditCurrencyDialog;
import org.totschnig.myexpenses.model.CurrencyContext;
import org.totschnig.myexpenses.model.CurrencyUnit;
import org.totschnig.myexpenses.viewmodel.EditCurrencyViewModel;
import org.totschnig.myexpenses.viewmodel.data.Currency;

import java.util.Locale;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.ListFragment;
import androidx.lifecycle.ViewModelProvider;

import static org.totschnig.myexpenses.activity.ConstantsKt.EDIT_REQUEST;
import static org.totschnig.myexpenses.dialog.EditCurrencyDialog.KEY_RESULT;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY;
import static org.totschnig.myexpenses.util.Utils.isFrameworkCurrency;

public class CurrencyList extends ListFragment {
  private EditCurrencyViewModel currencyViewModel;
  private CurrencyAdapter currencyAdapter;

  @Inject
  CurrencyContext currencyContext;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    ((MyApplication) requireActivity().getApplication()).getAppComponent().inject(this);
    setAdapter();
    currencyViewModel = new ViewModelProvider(this).get(EditCurrencyViewModel.class);
    currencyViewModel.getCurrencies().observe(this, currencies -> {
      currencyAdapter.clear();
      currencyAdapter.addAll(currencies);
    });
    currencyViewModel.getDeleteComplete().observe(this, success -> {
      if (success != null && !success) {
        ((ProtectedFragmentActivity) getActivity()).showSnackbar(R.string.currency_still_used);
      }
    });
  }

  @Override
  public void onActivityCreated(@Nullable Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    registerForContextMenu(getListView());
  }

  @Override
  public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
    Currency currency = currencyAdapter.getItem(((AdapterView.AdapterContextMenuInfo) menuInfo).position);
    if (!isFrameworkCurrency(currency.getCode())) {
      menu.add(0, R.id.DELETE_COMMAND, 0, R.string.menu_delete);
    }
  }

  @Override
  public boolean onContextItemSelected(MenuItem item) {
    if (item.getItemId() == R.id.DELETE_COMMAND) {
      currencyViewModel.deleteCurrency(
          currencyAdapter.getItem(((AdapterView.AdapterContextMenuInfo) item.getMenuInfo()).position).getCode());
      return true;
    }
    return false;
  }

  private void setAdapter() {
    currencyAdapter = new CurrencyAdapter(getActivity(), android.R.layout.simple_list_item_1) {
      @NonNull
      @Override
      public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        TextView v = (TextView) super.getView(position, convertView, parent);
        Currency item = currencyAdapter.getItem(position);
        final CurrencyUnit currencyUnit = currencyContext.get(item.getCode());
        v.setText(String.format(Locale.getDefault(), "%s (%s, %d)", v.getText(),
            currencyUnit.getSymbol(),
            currencyUnit.getFractionDigits()));
        return v;
      }
    };
    setListAdapter(currencyAdapter);
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == EDIT_REQUEST && resultCode == Activity.RESULT_OK) {
      currencyAdapter.notifyDataSetChanged();
      if (data != null) {
        int result = data.getIntExtra(KEY_RESULT, 0);
        ((ProtectedFragmentActivity) getActivity()).showSnackbar(
            getString(R.string.change_fraction_digits_result, result, data.getStringExtra(KEY_CURRENCY)));
      }
    }
  }

  @Override
  public void onListItemClick(ListView l, View v, int position, long id) {
    Currency item = currencyAdapter.getItem(position);
    final EditCurrencyDialog editCurrencyDialog = EditCurrencyDialog.newInstance(item);
    editCurrencyDialog.setTargetFragment(this, EDIT_REQUEST);
    editCurrencyDialog.show(getParentFragmentManager(), "SET_FRACTION_DIGITS");
  }
}
