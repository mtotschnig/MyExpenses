/*   This file is part of My Expenses.
 *   My Expenses is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   My Expenses is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with My Expenses.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.totschnig.myexpenses.model;

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CODE;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_COLOR;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CRITERION;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DESCRIPTION;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_EXCLUDE_FROM_TOTALS;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_OPENING_BALANCE;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SYNC_ACCOUNT_NAME;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TYPE;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_UUID;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.viewmodel.data.DistributionAccountInfo;

import java.math.BigDecimal;

/**
 * Account represents an account stored in the database.
 * Accounts have label, opening balance, description and currency
 *
 * @author Michael Totschnig
 */
@Deprecated
public class Account extends Model implements DistributionAccountInfo {

  public static final int EXPORT_HANDLE_DELETED_DO_NOTHING = -1;
  public static final int EXPORT_HANDLE_DELETED_UPDATE_BALANCE = 0;
  public static final int EXPORT_HANDLE_DELETED_CREATE_HELPER = 1;

  private String label;

  public Money openingBalance;

  private CurrencyUnit currencyUnit;

  public String description;

  public int color;

  public boolean excludeFromTotals = false;

  private String syncAccountName;

  private Money criterion;

  /**
   * exchange rate comparing major units
   */
  private double exchangeRate = 1D;

  private boolean sealed;

  @Nullable
  public String getSyncAccountName() {
    return syncAccountName;
  }

  public void setSyncAccountName(String syncAccountName) {
    this.syncAccountName = syncAccountName;
  }

  public double getExchangeRate() {
    return exchangeRate;
  }

  public void setExchangeRate(double exchangeRate) {
    this.exchangeRate = exchangeRate;
  }

  public CurrencyUnit getCurrencyUnit() {
    return currencyUnit;
  }

  public static final Uri CONTENT_URI = TransactionProvider.ACCOUNTS_URI;

  private AccountType type;

  @NonNull
  private Grouping grouping = Grouping.NONE;

  public static final int DEFAULT_COLOR = 0xff009688;

  /**
   * returns an empty Account instance
   */
  public Account(CurrencyUnit currencyUnit) {
    this("", currencyUnit, (long) 0, "");
  }

  public Account(String label, CurrencyUnit currencyUnit, long openingBalance, AccountType accountType) {
    this(label, currencyUnit, openingBalance, "", accountType, DEFAULT_COLOR);
  }

  public Account(String label, CurrencyUnit currencyUnit, long openingBalance, String description) {
    this(label, currencyUnit, openingBalance, description, AccountType.CASH, DEFAULT_COLOR);
  }

  public Account(String label, CurrencyUnit currency, long openingBalance, String description,
                 AccountType type, int color) {
    this(label, new Money(currency, openingBalance), description, type, color);
  }

  public Account(String label, CurrencyUnit currency, long openingBalance, String description, AccountType accountType) {
    this(label, currency, openingBalance, description, accountType, DEFAULT_COLOR);
  }

  public Account(String label, Money openingBalance, String description,
                 AccountType type) {
    this(label, openingBalance, description, type, DEFAULT_COLOR);
  }

  public Account(String label, Money openingBalance, String description,
                 AccountType type, int color) {
    this.setLabel(label);
    this.currencyUnit = openingBalance.getCurrencyUnit();
    this.openingBalance = openingBalance;
    this.description = description;
    this.setType(type);
    this.color = color;
  }

  public void setCurrency(CurrencyUnit currencyUnit) throws IllegalArgumentException {
    this.currencyUnit = currencyUnit;
    openingBalance = new Money(this.currencyUnit, openingBalance.getAmountMajor());
  }

  @Nullable
  @Override
  public Uri save() {
    Uri uri;
    ensureCurrency(currencyUnit);
    ContentValues initialValues = new ContentValues();
    initialValues.put(KEY_LABEL, getLabel());
    initialValues.put(KEY_OPENING_BALANCE, openingBalance.getAmountMinor());
    initialValues.put(KEY_DESCRIPTION, description);
    initialValues.put(KEY_CURRENCY, currencyUnit.getCode());
    initialValues.put(KEY_TYPE, getType().name());
    initialValues.put(KEY_COLOR, color);
    initialValues.put(KEY_SYNC_ACCOUNT_NAME, syncAccountName);
    initialValues.put(KEY_UUID, requireUuid());
    initialValues.put(KEY_EXCLUDE_FROM_TOTALS, excludeFromTotals);
    if (criterion != null) {
      initialValues.put(KEY_CRITERION, criterion.getAmountMinor());
    } else {
      initialValues.putNull(KEY_CRITERION);
    }

    if (getId() == 0) {
      uri = cr().insert(CONTENT_URI, initialValues);
      if (uri == null) {
        return null;
      }
      setId(ContentUris.parseId(uri));
    } else {
      uri = ContentUris.withAppendedId(CONTENT_URI, getId());
      if (cr().update(uri, initialValues, null, null) == 0) return null;
    }
    return uri;
  }

  private void ensureCurrency(CurrencyUnit currencyUnit) {
    Cursor cursor = cr().query(TransactionProvider.CURRENCIES_URI, new String[]{"count(*)"},
        KEY_CODE + " = ?", new String[]{currencyUnit.getCode()}, null);
    if (cursor == null) {
      throw new IllegalStateException("Unable to ensure currency (" + currencyUnit + "). Cursor is null");
    }
    cursor.moveToFirst();
    int result = cursor.getInt(0);
    cursor.close();
    switch (result) {
      case 0: {
        ContentValues contentValues = new ContentValues(2);
        contentValues.put(KEY_LABEL, currencyUnit.getCode());
        contentValues.put(KEY_CODE, currencyUnit.getCode());
        if (cr().insert(TransactionProvider.CURRENCIES_URI, contentValues) == null) {
          throw new IllegalStateException("Unable to ensure currency (" + currencyUnit + "). Insert failed");
        }
      }
      case 1: return;
      default: throw new IllegalStateException("Unable to ensure currency (" + currencyUnit + "). Inconsistent query result");
    }
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    Account other = (Account) obj;
    if (color != other.color)
      return false;
    if (currencyUnit == null) {
      if (other.currencyUnit != null)
        return false;
    } else if (!currencyUnit.equals(other.currencyUnit))
      return false;
    if (description == null) {
      if (other.description != null)
        return false;
    } else if (!description.equals(other.description))
      return false;
    if (getId() != other.getId())
      return false;
    if (getLabel() == null) {
      if (other.getLabel() != null)
        return false;
    } else if (!getLabel().equals(other.getLabel()))
      return false;
    if (openingBalance == null) {
      if (other.openingBalance != null)
        return false;
    } else if (!openingBalance.equals(other.openingBalance))
      return false;
    if (criterion == null) {
      if (other.criterion != null)
        return false;
    } else if (!criterion.equals(other.criterion))
      return false;
    if (getType() != other.getType())
      return false;
    return true;
  }

  @Override
  public int hashCode() {
    int result = this.getLabel() != null ? this.getLabel().hashCode() : 0;
    result = 31 * result + (this.openingBalance != null ? this.openingBalance.hashCode() : 0);
    result = 31 * result + (this.currencyUnit != null ? this.currencyUnit.hashCode() : 0);
    result = 31 * result + (this.description != null ? this.description.hashCode() : 0);
    result = 31 * result + this.color;
    result = 31 * result + (this.excludeFromTotals ? 1 : 0);
    result = 31 * result + (this.getType() != null ? this.getType().hashCode() : 0);
    result = 31 * result + (this.getGrouping() != null ? this.getGrouping().hashCode() : 0);
    return result;
  }

  public boolean isAggregate() {
    return isAggregate(getId());
  }

  public static boolean isAggregate(long id) {
    return id < 0;
  }

  public AccountType getType() {
    return type;
  }

  public void setType(AccountType type) {
    this.type = type;
  }

  @NonNull
  public Grouping getGrouping() {
    return grouping;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = StringUtils.strip(label);
  }

  public void setCriterion(BigDecimal criterion) {
    if (criterion.compareTo(BigDecimal.ZERO) != 0) {
      this.criterion = new Money(currencyUnit, criterion);
    } else {
      this.criterion = null;
    }
  }

  public void setCriterion(Money criterion) {
    this.criterion = criterion;
  }

  @Nullable
  public Money getCriterion() {
    return criterion;
  }

  public boolean isSealed() {
    return sealed;
  }

  @NonNull
  @Override
  public CurrencyUnit getCurrency() {
    return currencyUnit;
  }

  @Override
  public int getColor() {
    return -1;
  }

  @NonNull
  @Override
  public String label(@NonNull Context context) {
    return label;
  }

  @Override
  public long getAccountId() {
    return getId();
  }
}
