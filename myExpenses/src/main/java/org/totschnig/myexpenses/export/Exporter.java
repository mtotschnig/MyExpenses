package org.totschnig.myexpenses.export;

import android.database.Cursor;
import android.net.Uri;

import org.apache.commons.lang3.StringUtils;
import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.Category;
import org.totschnig.myexpenses.model.ExportFormat;
import org.totschnig.myexpenses.model.Model;
import org.totschnig.myexpenses.model.Money;
import org.totschnig.myexpenses.model.PaymentMethod;
import org.totschnig.myexpenses.model.SplitTransaction;
import org.totschnig.myexpenses.model.Transaction;
import org.totschnig.myexpenses.provider.DatabaseConstants;
import org.totschnig.myexpenses.provider.DbUtils;
import org.totschnig.myexpenses.provider.filter.WhereFilter;
import org.totschnig.myexpenses.util.AppDirHelper;
import org.totschnig.myexpenses.util.Result;
import org.totschnig.myexpenses.util.StringBuilderWrapper;
import org.totschnig.myexpenses.util.TextUtils;
import org.totschnig.myexpenses.util.Utils;
import org.totschnig.myexpenses.util.io.FileUtils;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import androidx.documentfile.provider.DocumentFile;
import timber.log.Timber;

import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_AMOUNT;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CATID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_COMMENT;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CR_STATUS;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DATE;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL_MAIN;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL_SUB;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_METHODID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PARENTID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PAYEE_NAME;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PICTURE_URI;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_REFERENCE_NUMBER;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_STATUS;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSFER_PEER;
import static org.totschnig.myexpenses.provider.DatabaseConstants.SPLIT_CATID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.STATUS_NONE;

public class Exporter {
  private final char delimiter;
  private final Account account;
  private final WhereFilter filter;
  private final DocumentFile destDir;
  private final String fileName;
  private final ExportFormat format;
  private final boolean notYetExportedP;
  private final String dateFormat;
  private final char decimalSeparator;
  private final String encoding;
  private final boolean append;
  private final boolean withAccountColumn;

  /**
   * @param account          Account to print
   * @param filter           only transactions matched by filter will be considered
   * @param destDir          destination directory
   * @param fileName         Filename for exported file
   * @param format           QIF or CSV
   * @param notYetExportedP  if true only transactions not marked as exported will be handled
   * @param dateFormat       format parseable by SimpleDateFormat class
   * @param decimalSeparator , or .
   * @param encoding         the string describing the desired character encoding.
   * @param delimiter   , or ; or \t
   * @param append          append to file
   * @param withAccountColumn put account in column
   */
  public Exporter(Account account, WhereFilter filter, DocumentFile destDir, String fileName,
                  ExportFormat format, boolean notYetExportedP, String dateFormat,
                  char decimalSeparator, String encoding, char delimiter, boolean append,
                  boolean withAccountColumn) {
    this.account = account;
    this.destDir = destDir;
    this.filter = filter;
    this.fileName = fileName;
    this.format = format;
    this.notYetExportedP = notYetExportedP;
    this.dateFormat = dateFormat;
    this.decimalSeparator = decimalSeparator;
    this.encoding = encoding;
    this.delimiter = delimiter;
    this.append = append;
    this.withAccountColumn = withAccountColumn;
  }

  public Result<Uri> export() throws IOException {
    MyApplication ctx = MyApplication.getInstance();
    DecimalFormat nfFormat = Utils.getDecimalFormat(account.getCurrencyUnit(), decimalSeparator);
    Timber.i("now starting export");
    //first we check if there are any exportable transactions
    String selection = KEY_ACCOUNTID + " = ? AND " + KEY_PARENTID + " is null";
    String[] selectionArgs = new String[]{String.valueOf(account.getId())};
    if (notYetExportedP)
      selection += " AND " + KEY_STATUS + " = " + STATUS_NONE;
    if (filter != null && !filter.isEmpty()) {
      selection += " AND " + filter.getSelectionForParents(DatabaseConstants.VIEW_EXTENDED);
      selectionArgs = Utils.joinArrays(selectionArgs, filter.getSelectionArgs(false));
    }
    Cursor c = Model.cr().query(
        Transaction.EXTENDED_URI,
        null, selection, selectionArgs, KEY_DATE);
    if (c.getCount() == 0) {
      c.close();
      return Result.ofFailure(R.string.no_exportable_expenses);
    }
    //then we check if the destDir is writable
    DocumentFile outputFile = AppDirHelper.buildFile(
        destDir,
        fileName,
        format.getMimeType(),
        format.getMimeType().split("/")[1],
        append);
    if (outputFile == null) {
      c.close();
      return Result.ofFailure(
          R.string.io_error_unable_to_create_file,
          fileName, FileUtils.getPath(MyApplication.getInstance(), destDir.getUri()));
    }
    c.moveToFirst();
    StringBuilderWrapper sb = new StringBuilderWrapper();
    SimpleDateFormat formatter = new SimpleDateFormat(dateFormat, Locale.US);
    OutputStreamWriter out = new OutputStreamWriter(
        Model.cr().openOutputStream(outputFile.getUri(), append ? "wa" : "w"),
        encoding);
    switch (format) {
      case CSV:
        if (!append) {
          int[] columns = {R.string.split_transaction, R.string.date, R.string.payer_or_payee, R.string.income, R.string.expense,
              R.string.category, R.string.subcategory, R.string.comment, R.string.method, R.string.status, R.string.reference_number, R.string.picture};
          if (withAccountColumn) {
            sb.appendQ(ctx.getString(R.string.account)).append(delimiter);
          }
          for (int column : columns) {
            sb.appendQ(ctx.getString(column)).append(delimiter);
          }
          sb.append("\n");
        }
        break;
      //QIF
      default:
        sb.append("!Account\nN")
            .append(account.getLabel())
            .append("\nT")
            .append(account.getType().toQifName())
            .append("\n^\n!Type:")
            .append(account.getType().toQifName())
            .append("\n");
    }
    //Write header
    out.write(sb.toString());
    while (c.getPosition() < c.getCount()) {
      String comment = DbUtils.getString(c, KEY_COMMENT);
      String full_label = "", label_sub = "", label_main;
      Transaction.CrStatus status;
      Long catId = DbUtils.getLongOrNull(c, KEY_CATID);
      Cursor splits = null, readCat;
      if (SPLIT_CATID.equals(catId)) {
        //split transactions take their full_label from the first split part
        splits = Model.cr().query(Transaction.CONTENT_URI, null,
            KEY_PARENTID + " = " + c.getLong(c.getColumnIndex(KEY_ROWID)), null, null);
        if (splits != null && splits.moveToFirst()) {
          readCat = splits;
        } else {
          readCat = c;
        }
      } else {
        readCat = c;
      }
      Long transfer_peer = DbUtils.getLongOrNull(readCat, KEY_TRANSFER_PEER);
      label_main = DbUtils.getString(readCat, KEY_LABEL_MAIN);
      if (label_main.length() > 0) {
        if (transfer_peer != null) {
          full_label = "[" + label_main + "]";
          label_main = ctx.getString(R.string.transfer);
          label_sub = full_label;
        } else {
          label_sub = DbUtils.getString(readCat, KEY_LABEL_SUB);
          full_label = TextUtils.formatQifCategory(label_main, label_sub);
        }
      }
      String payee = DbUtils.getString(c, KEY_PAYEE_NAME);
      String dateStr = formatter.format(new Date(c.getLong(
          c.getColumnIndexOrThrow(KEY_DATE)) * 1000));
      long amount = c.getLong(
          c.getColumnIndexOrThrow(KEY_AMOUNT));
      BigDecimal bdAmount = new Money(account.getCurrencyUnit(), amount).getAmountMajor();
      String amountQIF = nfFormat.format(bdAmount);
      String amountAbsCSV = nfFormat.format(bdAmount.abs());
      try {
        status = Transaction.CrStatus.valueOf(c.getString(c.getColumnIndexOrThrow(KEY_CR_STATUS)));
      } catch (IllegalArgumentException ex) {
        status = Transaction.CrStatus.UNRECONCILED;
      }
      String referenceNumber = DbUtils.getString(c, KEY_REFERENCE_NUMBER);
      String splitIndicator = SPLIT_CATID.equals(catId) ? SplitTransaction.CSV_INDICATOR : "";
      sb.clear();
      switch (format) {
        case CSV:
          Long methodId = DbUtils.getLongOrNull(c, KEY_METHODID);
          PaymentMethod method = methodId == null ? null : PaymentMethod.getInstanceFromDb(methodId);
          if (withAccountColumn) {
            sb.appendQ(account.getLabel()).append(delimiter);
          }
          sb.appendQ(splitIndicator)
              .append(delimiter)
              .appendQ(dateStr)
              .append(delimiter)
              .appendQ(payee)
              .append(delimiter)
              .appendQ(amount > 0 ? amountAbsCSV : "0")
              .append(delimiter)
              .appendQ(amount < 0 ? amountAbsCSV : "0")
              .append(delimiter)
              .appendQ(label_main)
              .append(delimiter)
              .appendQ(label_sub)
              .append(delimiter)
              .appendQ(comment)
              .append(delimiter)
              .appendQ(method == null ? "" : method.getLabel())
              .append(delimiter)
              .appendQ(status.symbol)
              .append(delimiter)
              .appendQ(referenceNumber)
              .append(delimiter)
              .appendQ(StringUtils.substringAfterLast(DbUtils.getString(c, KEY_PICTURE_URI), "/"));
          break;
        default:
          sb.append("D")
              .append(dateStr)
              .append("\nT")
              .append(amountQIF);
          if (comment.length() > 0) {
            sb.append("\nM")
                .append(comment);
          }
          if (full_label.length() > 0) {
            sb.append("\nL")
                .append(full_label);
          }
          if (payee.length() > 0) {
            sb.append("\nP")
                .append(payee);
          }
          if (!"".equals(status.symbol))
            sb.append("\nC")
                .append(status.symbol);
          if (referenceNumber.length() > 0) {
            sb.append("\nN")
                .append(referenceNumber);
          }
      }
      sb.append("\n");
      out.write(sb.toString());
      if (SPLIT_CATID.equals(catId) && splits != null) {
        while (splits.getPosition() < splits.getCount()) {
          transfer_peer = DbUtils.getLongOrNull(splits, KEY_TRANSFER_PEER);
          comment = DbUtils.getString(splits, KEY_COMMENT);
          label_main = DbUtils.getString(splits, KEY_LABEL_MAIN);
          if (label_main.length() > 0) {
            if (transfer_peer != null) {
              full_label = "[" + label_main + "]";
              label_main = ctx.getString(R.string.transfer);
              label_sub = full_label;
            } else {
              label_sub = DbUtils.getString(splits, KEY_LABEL_SUB);
              full_label = TextUtils.formatQifCategory(label_main, label_sub);
            }
          } else {
            label_main = full_label = Category.NO_CATEGORY_ASSIGNED_LABEL;
            label_sub = "";
          }
          amount = splits.getLong(
              splits.getColumnIndexOrThrow(KEY_AMOUNT));
          bdAmount = new Money(account.getCurrencyUnit(), amount).getAmountMajor();
          amountQIF = nfFormat.format(bdAmount);
          amountAbsCSV = nfFormat.format(bdAmount.abs());
          sb.clear();
          switch (format) {
            case CSV:
              Long methodId = DbUtils.getLongOrNull(c, KEY_METHODID);
              PaymentMethod method = methodId == null ? null : PaymentMethod.getInstanceFromDb(methodId);
              sb.appendQ(SplitTransaction.CSV_PART_INDICATOR)
                  .append(delimiter)
                  .appendQ(dateStr)
                  .append(delimiter)
                  .appendQ(payee)
                  .append(delimiter)
                  .appendQ(amount > 0 ? amountAbsCSV : "0")
                  .append(delimiter)
                  .appendQ(amount < 0 ? amountAbsCSV : "0")
                  .append(delimiter)
                  .appendQ(label_main)
                  .append(delimiter)
                  .appendQ(label_sub)
                  .append(delimiter)
                  .appendQ(comment)
                  .append(delimiter)
                  .appendQ(method == null ? "" : method.getLabel())
                  .append(delimiter)
                  .appendQ("")
                  .append(delimiter)
                  .appendQ("")
                  .append(delimiter)
                  .appendQ(StringUtils.substringAfterLast(DbUtils.getString(splits, KEY_PICTURE_URI), "/"));
              break;
            //QIF
            default:
              sb.append("S")
                  .append(full_label);
              if ((comment.length() > 0)) {
                sb.append("\nE")
                    .append(comment);
              }
              sb.append("\n$")
                  .append(amountQIF);
          }
          sb.append("\n");
          out.write(sb.toString());
          splits.moveToNext();
        }
        splits.close();
      }
      if (format.equals(ExportFormat.QIF)) {
        out.write("^\n");
      }
      c.moveToNext();
    }
    out.close();
    c.close();
    return Result.ofSuccess(R.string.export_sdcard_success, outputFile.getUri(), FileUtils.getPath(ctx, outputFile.getUri()));
  }
}
