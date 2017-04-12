package org.totschnig.myexpenses.export.pdf;


import android.database.Cursor;
import android.net.Uri;
import android.support.v4.provider.DocumentFile;

import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.draw.LineSeparator;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.fragment.TransactionList;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.Category;
import org.totschnig.myexpenses.model.Grouping;
import org.totschnig.myexpenses.model.Model;
import org.totschnig.myexpenses.model.Money;
import org.totschnig.myexpenses.model.Transaction;
import org.totschnig.myexpenses.model.Transfer;
import org.totschnig.myexpenses.provider.DatabaseConstants;
import org.totschnig.myexpenses.provider.DbUtils;
import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.provider.filter.WhereFilter;
import org.totschnig.myexpenses.util.AppDirHelper;
import org.totschnig.myexpenses.util.CurrencyFormatter;
import org.totschnig.myexpenses.util.FileUtils;
import org.totschnig.myexpenses.util.LazyFontSelector;
import org.totschnig.myexpenses.util.PdfHelper;
import org.totschnig.myexpenses.util.Result;
import org.totschnig.myexpenses.util.Utils;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.inject.Inject;

import timber.log.Timber;

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNT_LABEL;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_AMOUNT;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CATID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_COMMENT;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DATE;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DAY;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_INTERIM_BALANCE;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_IS_SAME_CURRENCY;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL_MAIN;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL_SUB;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_MONTH;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PARENTID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PAYEE_NAME;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_REFERENCE_NUMBER;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SECOND_GROUP;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SUM_EXPENSES;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SUM_INCOME;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SUM_TRANSFERS;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSFER_PEER;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_WEEK;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_YEAR;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_YEAR_OF_WEEK_START;
import static org.totschnig.myexpenses.provider.DatabaseConstants.SPLIT_CATID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.TABLE_ACCOUNTS;

public class PdfPrinter {
  private Account account;
  private DocumentFile destDir;
  private WhereFilter filter;

  @Inject
  CurrencyFormatter currencyFormatter;

  public PdfPrinter(Account account, DocumentFile destDir, WhereFilter filter) {
    this.account = account;
    this.destDir = destDir;
    this.filter = filter;
  }

  public Result print() throws IOException, DocumentException {
    MyApplication.getInstance().getAppComponent().inject(this);
    long start = System.currentTimeMillis();
    Timber.d("Print start %d", start);
    PdfHelper helper = new PdfHelper();
    Timber.d("Helper created %d", (System.currentTimeMillis() - start));
    String selection;
    String[] selectionArgs;
    if (account.getId() < 0) {
      selection = KEY_ACCOUNTID + " IN " +
          "(SELECT " + KEY_ROWID + " from " + TABLE_ACCOUNTS + " WHERE " + KEY_CURRENCY + " = ?)";
      selectionArgs = new String[]{account.currency.getCurrencyCode()};
    } else {
      selection = KEY_ACCOUNTID + " = ?";
      selectionArgs = new String[]{String.valueOf(account.getId())};
    }
    if (filter != null && !filter.isEmpty()) {
      selection += " AND " + filter.getSelectionForParents(DatabaseConstants.VIEW_EXTENDED);
      selectionArgs = Utils.joinArrays(selectionArgs, filter.getSelectionArgs(false));
    }
    Cursor transactionCursor;
    String fileName = account.label.replaceAll("\\W", "");
    DocumentFile outputFile = AppDirHelper.timeStampedFile(
        destDir,
        fileName,
        "application/pdf", false);
    Document document = new Document();
    transactionCursor = Model.cr().query(
        account.getExtendedUriForTransactionList(), account.getExtendedProjectionForTransactionList(),
        selection + " AND " + KEY_PARENTID + " is null", selectionArgs, KEY_DATE + " ASC");
    //first we check if there are any exportable transactions
    //String selection = KEY_ACCOUNTID + " = " + getId() + " AND " + KEY_PARENTID + " is null";
    if (transactionCursor.getCount() == 0) {
      transactionCursor.close();
      return new Result(false, R.string.no_exportable_expenses);
    }
    //then we check if the filename we construct already exists
    if (outputFile == null) {
      transactionCursor.close();
      return new Result(
          false,
          R.string.io_error_unable_to_create_file,
          fileName,
          FileUtils.getPath(MyApplication.getInstance(), destDir.getUri()));
    }
    PdfWriter.getInstance(document, Model.cr().openOutputStream(outputFile.getUri()));
    Timber.d("All setup %d", (System.currentTimeMillis() - start));
    document.open();
    Timber.d("Document open %d", (System.currentTimeMillis() - start));
    addMetaData(document);
    Timber.d("Metadata %d", (System.currentTimeMillis() - start));
    addHeader(document, helper);
    Timber.d("Header %d", (System.currentTimeMillis() - start));
    addTransactionList(document, transactionCursor, helper, filter);
    Timber.d("List %d", (System.currentTimeMillis() - start));
    transactionCursor.close();
    document.close();
    return new Result(true, R.string.export_sdcard_success, outputFile.getUri());
  }
  private void addMetaData(Document document) {
    document.addTitle(account.label);
    document.addSubject("Generated by MyExpenses.mobi");
  }

  private void addHeader(Document document, PdfHelper helper)
      throws DocumentException, IOException {
    String selection, column;
    String[] selectionArgs;
    if (account.getId() < 0) {
      column = "sum(" + Account.CURRENT_BALANCE_EXPR + ")";
      selection = KEY_ROWID + " IN " +
          "(SELECT " + KEY_ROWID + " from " + TABLE_ACCOUNTS + " WHERE " + KEY_CURRENCY + " = ?)";
      selectionArgs = new String[]{account.currency.getCurrencyCode()};
    } else {
      column = Account.CURRENT_BALANCE_EXPR;
      selection = KEY_ROWID + " = ?";
      selectionArgs = new String[]{String.valueOf(account.getId())};
    }
    Cursor accountCursor = Model.cr().query(
        Account.CONTENT_URI,
        new String[]{column},
        selection,
        selectionArgs,
        null);
    accountCursor.moveToFirst();
    long currentBalance = accountCursor.getLong(0);
    accountCursor.close();
    PdfPTable preface = new PdfPTable(1);

    preface.addCell(helper.printToCell(account.label, LazyFontSelector.FontType.TITLE));

    preface.addCell(helper.printToCell(
        java.text.DateFormat.getDateInstance(java.text.DateFormat.FULL).format(new Date()), LazyFontSelector.FontType.BOLD));
    preface.addCell(helper.printToCell(
        MyApplication.getInstance().getString(R.string.current_balance) + " : " +
            currencyFormatter.formatCurrency(new Money(account.currency, currentBalance)), LazyFontSelector.FontType.BOLD));

    document.add(preface);
    Paragraph empty = new Paragraph();
    addEmptyLine(empty, 1);
    document.add(empty);
  }

  private void addTransactionList(Document document, Cursor transactionCursor, PdfHelper helper, WhereFilter filter)
      throws DocumentException, IOException {
    String selection;
    String[] selectionArgs;
    if (!filter.isEmpty()) {
      selection = filter.getSelectionForParts(DatabaseConstants.VIEW_EXTENDED);//GROUP query uses extended view
      selectionArgs = filter.getSelectionArgs(true);
    } else {
      selection = null;
      selectionArgs = null;
    }
    Uri.Builder builder = Transaction.CONTENT_URI.buildUpon();
    builder.appendPath(TransactionProvider.URI_SEGMENT_GROUPS)
        .appendPath(account.grouping.name());
    if (account.getId() < 0) {
      builder.appendQueryParameter(KEY_CURRENCY, account.currency.getCurrencyCode());
    } else {
      builder.appendQueryParameter(KEY_ACCOUNTID, String.valueOf(account.getId()));
    }
    Cursor groupCursor = Model.cr().query(builder.build(), null, selection, selectionArgs,
        KEY_YEAR + " ASC," + KEY_SECOND_GROUP + " ASC");

    MyApplication ctx = MyApplication.getInstance();

    int columnIndexGroupSumIncome = groupCursor.getColumnIndex(KEY_SUM_INCOME);
    int columnIndexGroupSumExpense = groupCursor.getColumnIndex(KEY_SUM_EXPENSES);
    int columnIndexGroupSumTransfer = groupCursor.getColumnIndex(KEY_SUM_TRANSFERS);
    int columIndexGroupSumInterim = groupCursor.getColumnIndex(KEY_INTERIM_BALANCE);
    int columnIndexRowId = transactionCursor.getColumnIndex(KEY_ROWID);
    int columnIndexYear = transactionCursor.getColumnIndex(KEY_YEAR);
    int columnIndexYearOfWeekStart = transactionCursor.getColumnIndex(KEY_YEAR_OF_WEEK_START);
    int columnIndexMonth = transactionCursor.getColumnIndex(KEY_MONTH);
    int columnIndexWeek = transactionCursor.getColumnIndex(KEY_WEEK);
    int columnIndexDay = transactionCursor.getColumnIndex(KEY_DAY);
    int columnIndexAmount = transactionCursor.getColumnIndex(KEY_AMOUNT);
    int columnIndexLabelSub = transactionCursor.getColumnIndex(KEY_LABEL_SUB);
    int columnIndexLabelMain = transactionCursor.getColumnIndex(KEY_LABEL_MAIN);
    int columnIndexComment = transactionCursor.getColumnIndex(KEY_COMMENT);
    int columnIndexReferenceNumber = transactionCursor.getColumnIndex(KEY_REFERENCE_NUMBER);
    int columnIndexPayee = transactionCursor.getColumnIndex(KEY_PAYEE_NAME);
    int columnIndexTransferPeer = transactionCursor.getColumnIndex(KEY_TRANSFER_PEER);
    int columnIndexDate = transactionCursor.getColumnIndex(KEY_DATE);
    DateFormat itemDateFormat;
    switch (account.grouping) {
      case DAY:
        itemDateFormat = android.text.format.DateFormat.getTimeFormat(ctx);
        break;
      case MONTH:
        //noinspection SimpleDateFormat
        itemDateFormat = new SimpleDateFormat("dd");
        break;
      case WEEK:
        //noinspection SimpleDateFormat
        itemDateFormat = new SimpleDateFormat("EEE");
        break;
      default:
        itemDateFormat = Utils.localizedYearlessDateFormat();
    }
    PdfPTable table = null;

    int prevHeaderId = 0, currentHeaderId;

    transactionCursor.moveToFirst();
    groupCursor.moveToFirst();

    while (transactionCursor.getPosition() < transactionCursor.getCount()) {
      int year = transactionCursor.getInt(account.grouping.equals(Grouping.WEEK) ? columnIndexYearOfWeekStart : columnIndexYear);
      int month = transactionCursor.getInt(columnIndexMonth);
      int week = transactionCursor.getInt(columnIndexWeek);
      int day = transactionCursor.getInt(columnIndexDay);
      int second = -1;

      switch (account.grouping) {
        case DAY:
          currentHeaderId = year * 1000 + day;
          break;
        case WEEK:
          currentHeaderId = year * 1000 + week;
          break;
        case MONTH:
          currentHeaderId = year * 1000 + month;
          break;
        case YEAR:
          currentHeaderId = year * 1000;
          break;
        default:
          currentHeaderId = 1;
      }
      if (currentHeaderId != prevHeaderId) {
        if (table != null) {
          document.add(table);
        }
        switch (account.grouping) {
          case DAY:
            second = transactionCursor.getInt(columnIndexDay);
            break;
          case MONTH:
            second = transactionCursor.getInt(columnIndexMonth);
            break;
          case WEEK:
            second = transactionCursor.getInt(columnIndexWeek);
            break;
        }
        table = helper.newTable(2);
        table.setWidthPercentage(100f);
        PdfPCell cell = helper.printToCell(account.grouping.getDisplayTitle(ctx, year, second, transactionCursor), LazyFontSelector.FontType.HEADER);
        table.addCell(cell);
        Long sumExpense = DbUtils.getLongOr0L(groupCursor, columnIndexGroupSumExpense);
        Long sumIncome = DbUtils.getLongOr0L(groupCursor, columnIndexGroupSumIncome);
        Long sumTransfer = DbUtils.getLongOr0L(groupCursor, columnIndexGroupSumTransfer);
        Long delta = sumIncome - sumExpense + sumTransfer;
        Long interimBalance = DbUtils.getLongOr0L(groupCursor, columIndexGroupSumInterim);
        Long previousBalance = interimBalance - delta;
        cell = helper.printToCell(String.format("%s %s %s = %s",
            currencyFormatter.convAmount(previousBalance, account.currency),
            Long.signum(delta) > -1 ? "+" : "-",
            currencyFormatter.convAmount(Math.abs(delta), account.currency),
            currencyFormatter.convAmount(interimBalance, account.currency)), LazyFontSelector.FontType.HEADER);
        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(cell);
        document.add(table);
        table = helper.newTable(3);
        table.setWidthPercentage(100f);
        cell = helper.printToCell("+ " + currencyFormatter.convAmount(sumIncome,
            account.currency), LazyFontSelector.FontType.NORMAL);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
        cell = helper.printToCell("- " + currencyFormatter.convAmount(sumExpense,
            account.currency), LazyFontSelector.FontType.NORMAL);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
        cell = helper.printToCell(Transfer.BI_ARROW + " " + currencyFormatter.convAmount(
            DbUtils.getLongOr0L(groupCursor, columnIndexGroupSumTransfer),
            account.currency), LazyFontSelector.FontType.NORMAL);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
        table.setSpacingAfter(2f);
        document.add(table);
        LineSeparator sep = new LineSeparator();
        document.add(sep);
        table = helper.newTable(4);
        table.setWidths(table.getRunDirection() == PdfWriter.RUN_DIRECTION_RTL ?
            new int[]{2, 3, 5, 1} : new int[]{1, 5, 3, 2});
        table.setSpacingBefore(2f);
        table.setSpacingAfter(2f);
        table.setWidthPercentage(100f);
        prevHeaderId = currentHeaderId;
        groupCursor.moveToNext();
      }
      long amount = transactionCursor.getLong(columnIndexAmount);

      PdfPCell cell = helper.printToCell(Utils.convDateTime(transactionCursor.getString(columnIndexDate), itemDateFormat), LazyFontSelector.FontType.NORMAL);
      table.addCell(cell);

      String catText = transactionCursor.getString(columnIndexLabelMain);
      if (DbUtils.getLongOrNull(transactionCursor, columnIndexTransferPeer) != null) {
        catText = Transfer.getIndicatorPrefixForLabel(amount) + catText;
      } else {
        Long catId = DbUtils.getLongOrNull(transactionCursor, KEY_CATID);
        if (SPLIT_CATID.equals(catId)) {
          Cursor splits = Model.cr().query(Transaction.CONTENT_URI, null,
              KEY_PARENTID + " = " + transactionCursor.getLong(columnIndexRowId), null, null);
          splits.moveToFirst();
          catText = "";
          while (splits.getPosition() < splits.getCount()) {
            String splitText = DbUtils.getString(splits, KEY_LABEL_MAIN);
            if (splitText.length() > 0) {
              if (DbUtils.getLongOrNull(splits, KEY_TRANSFER_PEER) != null) {
                splitText = "[" + splitText + "]";
              } else {
                String label_sub = DbUtils.getString(splits, KEY_LABEL_SUB);
                if (label_sub.length() > 0)
                  splitText += TransactionList.CATEGORY_SEPARATOR + label_sub;
              }
            } else {
              splitText = Category.NO_CATEGORY_ASSIGNED_LABEL;
            }
            splitText += " " + currencyFormatter.convAmount(splits.getLong(
                splits.getColumnIndexOrThrow(KEY_AMOUNT)), account.currency);
            String splitComment = DbUtils.getString(splits, KEY_COMMENT);
            if (splitComment != null && splitComment.length() > 0) {
              splitText += " (" + splitComment + ")";
            }
            catText += splitText;
            if (splits.getPosition() != splits.getCount() - 1) {
              catText += "; ";
            }
            splits.moveToNext();
          }
          splits.close();
        } else if (catId == null) {
          catText = Category.NO_CATEGORY_ASSIGNED_LABEL;
        } else {
          String label_sub = transactionCursor.getString(columnIndexLabelSub);
          if (label_sub != null && label_sub.length() > 0) {
            catText = catText + TransactionList.CATEGORY_SEPARATOR + label_sub;
          }
        }
      }
      if (account.getId() < 0) {
        //for aggregate accounts we need to indicate the account name
        catText = transactionCursor.getString(transactionCursor.getColumnIndex(KEY_ACCOUNT_LABEL))
            + " " + catText;
      }
      String referenceNumber = transactionCursor.getString(columnIndexReferenceNumber);
      if (referenceNumber != null && referenceNumber.length() > 0)
        catText = "(" + referenceNumber + ") " + catText;
      cell = helper.printToCell(catText, LazyFontSelector.FontType.NORMAL);
      String payee = transactionCursor.getString(columnIndexPayee);
      if (payee == null || payee.length() == 0) {
        cell.setColspan(2);
      }
      table.addCell(cell);
      if (payee != null && payee.length() > 0) {
        table.addCell(helper.printToCell(payee, LazyFontSelector.FontType.UNDERLINE));
      }
      LazyFontSelector.FontType t;
      if (account.getId() < 0 &&
          transactionCursor.getInt(transactionCursor.getColumnIndex(KEY_IS_SAME_CURRENCY)) == 1) {
        t = LazyFontSelector.FontType.NORMAL;
      } else {
        t = amount < 0 ? LazyFontSelector.FontType.EXPENSE : LazyFontSelector.FontType.INCOME;
      }
      cell = helper.printToCell(currencyFormatter.convAmount(amount, account.currency), t);
      cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
      table.addCell(cell);
      String comment = transactionCursor.getString(columnIndexComment);
      if (comment != null && comment.length() > 0) {
        cell = helper.printToCell(comment, LazyFontSelector.FontType.ITALIC);
        cell.setColspan(2);
        table.addCell(helper.emptyCell());
        table.addCell(cell);
        table.addCell(helper.emptyCell());
      }
      transactionCursor.moveToNext();
    }
    // now add all this to the document
    document.add(table);
    groupCursor.close();
  }

  private void addEmptyLine(Paragraph paragraph, int number) {
    for (int i = 0; i < number; i++) {
      paragraph.add(new Paragraph(" "));
    }
  }
}
