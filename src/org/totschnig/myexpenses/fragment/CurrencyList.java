
package org.totschnig.myexpenses.fragment;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Currency;
import java.util.List;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.FolderBrowser;
import org.totschnig.myexpenses.dialog.EditTextDialog;
import org.totschnig.myexpenses.model.Account;

public class CurrencyList extends ListFragment {

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
  }
  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    setAdapter();
  }


  private void setAdapter() {
    ArrayAdapter<Account.CurrencyEnum> curAdapter = new ArrayAdapter<Account.CurrencyEnum>(
        getActivity(), android.R.layout.simple_list_item_1,Account.CurrencyEnum.values());

    setListAdapter(curAdapter);
  }

  @Override
  public void onListItemClick(ListView l, View v, int position, long id) {
    super.onListItemClick(l, v, position, id);
    Account.CurrencyEnum selected = Account.CurrencyEnum.values()[position];
    Toast.makeText(getActivity(), selected.name(), Toast.LENGTH_LONG).show();
    
  }
}
