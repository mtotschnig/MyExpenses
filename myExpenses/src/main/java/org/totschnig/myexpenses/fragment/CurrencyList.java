package org.totschnig.myexpenses.fragment;

import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ListFragment;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.adapter.CurrencyAdapter;
import org.totschnig.myexpenses.dialog.EditCurrencyDialog;
import org.totschnig.myexpenses.model.CurrencyContext;
import org.totschnig.myexpenses.viewmodel.CurrencyViewModel;
import org.totschnig.myexpenses.viewmodel.data.Currency;

import java.util.Locale;

import javax.inject.Inject;

public class CurrencyList extends ListFragment {
  private CurrencyViewModel currencyViewModel;
  private CurrencyAdapter currencyAdapter;

  @Inject
  CurrencyContext currencyContext;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    MyApplication.getInstance().getAppComponent().inject(this);
    setAdapter();
    currencyViewModel = ViewModelProviders.of(this).get(CurrencyViewModel.class);
    currencyViewModel.getCurrencies().observe(this, currencies -> currencyAdapter.addAll(currencies));
    currencyViewModel.loadCurrencies();
  }

  private void setAdapter() {
    currencyAdapter = new CurrencyAdapter(getActivity(), android.R.layout.simple_list_item_1) {
      @NonNull
      @Override
      public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        TextView v = (TextView) super.getView(position, convertView, parent);
        Currency item = currencyAdapter.getItem(position);
        v.setText(String.format(Locale.getDefault(), "%s (%s)", v.getText(), currencyContext.get(item.code()).fractionDigits()));
        return v;
      }
    };
    setListAdapter(currencyAdapter);
  }

  @Override
  public void onListItemClick(ListView l, View v, int position, long id) {
    Currency item = currencyAdapter.getItem(position);
    EditCurrencyDialog.newInstance(item).show(getFragmentManager(), "SET_FRACTION_DIGITS");
  }
}
