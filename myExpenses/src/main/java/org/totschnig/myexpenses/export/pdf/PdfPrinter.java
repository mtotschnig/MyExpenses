package org.totschnig.myexpenses.export.pdf;


import static com.itextpdf.text.Chunk.GENERICTAG;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNT_LABEL;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_AMOUNT;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CATID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_COMMENT;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CR_STATUS;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DATE;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_DAY;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_IS_SAME_CURRENCY;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_MONTH;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PARENTID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PAYEE_NAME;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_REFERENCE_NUMBER;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SECOND_GROUP;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SUM_EXPENSES;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SUM_INCOME;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SUM_TRANSFERS;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TAGLIST;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSFER_PEER;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_WEEK;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_YEAR;
import static org.totschnig.myexpenses.provider.DatabaseConstants.KEY_YEAR_OF_WEEK_START;
import static org.totschnig.myexpenses.provider.DatabaseConstants.SPLIT_CATID;
import static org.totschnig.myexpenses.util.ArrayUtilsKt.joinArrays;
import static org.totschnig.myexpenses.util.CurrencyFormatterKt.convAmount;
import static org.totschnig.myexpenses.util.CurrencyFormatterKt.formatMoney;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import androidx.documentfile.provider.DocumentFile;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPRow;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfPTableEvent;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.draw.LineSeparator;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.db2.Repository;
import org.totschnig.myexpenses.db2.RepositoryAccountKt;
import org.totschnig.myexpenses.model.CrStatus;
import org.totschnig.myexpenses.model.CurrencyContext;
import org.totschnig.myexpenses.model.CurrencyUnit;
import org.totschnig.myexpenses.model.Grouping;
import org.totschnig.myexpenses.model.Model;
import org.totschnig.myexpenses.model.Money;
import org.totschnig.myexpenses.model.Transaction;
import org.totschnig.myexpenses.model.Transfer;
import org.totschnig.myexpenses.model2.Account;
import org.totschnig.myexpenses.provider.DatabaseConstants;
import org.totschnig.myexpenses.provider.DbUtils;
import org.totschnig.myexpenses.provider.MoreDbUtilsKt;
import org.totschnig.myexpenses.provider.filter.WhereFilter;
import org.totschnig.myexpenses.util.AppDirHelper;
import org.totschnig.myexpenses.util.CurrencyFormatter;
import org.totschnig.myexpenses.util.LazyFontSelector.FontType;
import org.totschnig.myexpenses.util.PdfHelper;
import org.totschnig.myexpenses.util.Result;
import org.totschnig.myexpenses.util.Utils;
import org.totschnig.myexpenses.util.io.DocumentFileExtensionKt;
import org.totschnig.myexpenses.viewmodel.data.Category;
import org.totschnig.myexpenses.viewmodel.data.DateInfo;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.inject.Inject;

import kotlin.Pair;
import timber.log.Timber;

public class PdfPrinter {
  private static final String VOID_MARKER = "void";
  private final Account account;
  private final DocumentFile destDir;
  private final WhereFilter filter;
  private final long currentBalance;

  @Inject
  CurrencyFormatter currencyFormatter;

  @Inject
  CurrencyContext currencyContext;

  @Inject
  Repository repository;

  private CurrencyUnit currencyUnit() {
    return currencyContext.get(account.getCurrency());
  }

  public PdfPrinter(long accountId, DocumentFile destDir, WhereFilter filter, long currentBalance) {
    this.destDir = destDir;
    this.filter = filter;
    this.currentBalance = currentBalance;
    MyApplication.getInstance().getAppComponent().inject(this);
    //TODO aggregate
    this.account = accountId > 0 ? RepositoryAccountKt.loadAccount(repository, accountId) :
      RepositoryAccountKt.loadAggregateAccount(repository, accountId);
  }

  public Result<Uri> print(Context context) throws IOException, DocumentException {
    long start = System.currentTimeMillis();
    Timber.d("Print start %d", start);
    PdfHelper helper = new PdfHelper();
    Timber.d("Helper created %d", (System.currentTimeMillis() - start));
    Pair<String, String[]> selectionInfo = account.getSelectionInfo();
    String selection = selectionInfo.getFirst();
    String[] selectionArgs = selectionInfo.getSecond();
    if (filter != null && !filter.isEmpty()) {
      selection += " AND " + filter.getSelectionForParents(DatabaseConstants.VIEW_EXTENDED);
      selectionArgs = joinArrays(selectionArgs, filter.getSelectionArgs(false));
    }
    Cursor transactionCursor;
    String fileName = account.getLabel().replaceAll("\\W", "");
    DocumentFile outputFile = AppDirHelper.timeStampedFile(
        destDir,
        fileName,
        "application/pdf", "pdf");
    Document document = new Document();
    transactionCursor = context.getContentResolver().query(
        account.extendedUriForTransactionList(false, false),
        account.extendedProjectionForTransactionList(),
        selection + " AND " + KEY_PARENTID + " is null", selectionArgs, KEY_DATE + " ASC");
    //first we check if there are any exportable transactions
    //String selection = KEY_ACCOUNTID + " = " + getId() + " AND " + KEY_PARENTID + " is null";
    if (transactionCursor.getCount() == 0) {
      transactionCursor.close();
      return Result.ofFailure(R.string.no_exportable_expenses);
    }
    //then we check if the filename we construct already exists
    if (outputFile == null) {
      transactionCursor.close();
      return Result.ofFailure(
          R.string.io_error_unable_to_create_file,
          fileName,
          DocumentFileExtensionKt.getDisplayName(destDir)
      );
    }
    PdfWriter.getInstance(document, context.getContentResolver().openOutputStream(outputFile.getUri()));
    Timber.d("All setup %d", (System.currentTimeMillis() - start));
    document.open();
    Timber.d("Document open %d", (System.currentTimeMillis() - start));
    addMetaData(document);
    Timber.d("Metadata %d", (System.currentTimeMillis() - start));
    addHeader(document, helper, context);
    Timber.d("Header %d", (System.currentTimeMillis() - start));
    addTransactionList(document, transactionCursor, helper, filter, context);
    Timber.d("List %d", (System.currentTimeMillis() - start));
    transactionCursor.close();
    document.close();
    return Result.ofSuccess(R.string.export_sdcard_success, outputFile.getUri(),
        DocumentFileExtensionKt.getDisplayName(outputFile)
    );
  }

  private void addMetaData(Document document) {
    document.addTitle(account.getLabel());
    document.addSubject("Generated by MyExpenses.mobi");
  }

  private void addHeader(Document document, PdfHelper helper, Context context)
      throws DocumentException, IOException {
    PdfPTable preface = new PdfPTable(1);

    preface.addCell(helper.printToCell(account.getLabel(), FontType.TITLE));

    preface.addCell(helper.printToCell(
        java.text.DateFormat.getDateInstance(java.text.DateFormat.FULL).format(new Date()), FontType.BOLD));
    preface.addCell(helper.printToCell(
        context.getString(R.string.current_balance) + " : " +
            formatMoney(currencyFormatter, new Money(currencyUnit(), currentBalance)), FontType.BOLD));

    document.add(preface);
    Paragraph empty = new Paragraph();
    addEmptyLine(empty, 1);
    document.add(empty);
  }

  private void addTransactionList(Document document, Cursor transactionCursor, PdfHelper helper, WhereFilter filter, Context context)
      throws DocumentException, IOException {
    String selection;
    String[] selectionArgs;
    if (!filter.isEmpty()) {
      selection = filter.getSelectionForParts(DatabaseConstants.VIEW_WITH_ACCOUNT);//GROUP query uses view with account
      selectionArgs = filter.getSelectionArgs(true);
    } else {
      selection = null;
      selectionArgs = null;
    }
    Cursor groupCursor = context.getContentResolver().query(account.getGroupingUri(), null, selection, selectionArgs,
        KEY_YEAR + " ASC," + KEY_SECOND_GROUP + " ASC");

    int columnIndexGroupSumIncome = groupCursor.getColumnIndex(KEY_SUM_INCOME);
    int columnIndexGroupSumExpense = groupCursor.getColumnIndex(KEY_SUM_EXPENSES);
    int columnIndexGroupSumTransfer = groupCursor.getColumnIndex(KEY_SUM_TRANSFERS);
    int columnIndexRowId = transactionCursor.getColumnIndex(KEY_ROWID);
    int columnIndexYear = transactionCursor.getColumnIndex(KEY_YEAR);
    int columnIndexYearOfWeekStart = transactionCursor.getColumnIndex(KEY_YEAR_OF_WEEK_START);
    int columnIndexMonth = transactionCursor.getColumnIndex(KEY_MONTH);
    int columnIndexWeek = transactionCursor.getColumnIndex(KEY_WEEK);
    int columnIndexDay = transactionCursor.getColumnIndex(KEY_DAY);
    int columnIndexAmount = transactionCursor.getColumnIndex(KEY_AMOUNT);
    int columnIndexLabel = transactionCursor.getColumnIndex(KEY_LABEL);
    int columnIndexComment = transactionCursor.getColumnIndex(KEY_COMMENT);
    int columnIndexReferenceNumber = transactionCursor.getColumnIndex(KEY_REFERENCE_NUMBER);
    int columnIndexPayee = transactionCursor.getColumnIndex(KEY_PAYEE_NAME);
    int columnIndexTransferPeer = transactionCursor.getColumnIndex(KEY_TRANSFER_PEER);
    int columnIndexDate = transactionCursor.getColumnIndex(KEY_DATE);
    int columnIndexCrStatus = transactionCursor.getColumnIndex(KEY_CR_STATUS);

    DateFormat itemDateFormat;
    switch (account.getGrouping()) {
      case DAY:
        itemDateFormat = android.text.format.DateFormat.getTimeFormat(context);
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
        itemDateFormat = Utils.localizedYearLessDateFormat(context);
    }
    PdfPTable table = null;

    int prevHeaderId = 0, currentHeaderId;

    transactionCursor.moveToFirst();
    groupCursor.moveToFirst();
    long previousBalance = account.getOpeningBalance();

    while (transactionCursor.getPosition() < transactionCursor.getCount()) {
      int year = transactionCursor.getInt(account.getGrouping().equals(Grouping.WEEK) ? columnIndexYearOfWeekStart : columnIndexYear);
      int month = transactionCursor.getInt(columnIndexMonth);
      int week = transactionCursor.getInt(columnIndexWeek);
      int day = transactionCursor.getInt(columnIndexDay);
      int second = -1;

      switch (account.getGrouping()) {
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
        switch (account.getGrouping()) {
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
        PdfPCell cell = helper.printToCell(account.getGrouping().getDisplayTitle(context, year, second, DateInfo.fromCursor(transactionCursor)), FontType.HEADER);
        table.addCell(cell);
        if (groupCursor.isAfterLast()) {
          Timber.w("Grouping: %s, currentHeaderId; %d, prevHeaderId: %d, filter: %s",
                  account.getGrouping(), currentHeaderId, prevHeaderId, filter);
          throw new IllegalStateException();
        }
        long sumExpense = groupCursor.getLong(columnIndexGroupSumExpense);
        long sumIncome = groupCursor.getLong(columnIndexGroupSumIncome);
        long sumTransfer = groupCursor.getLong(columnIndexGroupSumTransfer);
        long delta = sumIncome + sumExpense + sumTransfer;
        long interimBalance = previousBalance + delta;
        String formattedDelta = String.format("%s %s", Long.signum(delta) > -1 ? "+" : "-",
            convAmount(currencyFormatter, Math.abs(delta), currencyUnit()));
        cell = helper.printToCell(
            filter.isEmpty() ? String.format("%s %s = %s",
                convAmount(currencyFormatter, previousBalance, currencyUnit()), formattedDelta,
                convAmount(currencyFormatter, interimBalance, currencyUnit())) :
                formattedDelta, FontType.HEADER);
        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(cell);
        document.add(table);
        table = helper.newTable(3);
        table.setWidthPercentage(100f);
        cell = helper.printToCell("+ " + convAmount(currencyFormatter, sumIncome,
            currencyUnit()), FontType.NORMAL);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
        cell = helper.printToCell("- " + convAmount(currencyFormatter, -sumExpense,
            currencyUnit()), FontType.NORMAL);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
        cell = helper.printToCell(Transfer.BI_ARROW + " " + convAmount(currencyFormatter, sumTransfer,
            currencyUnit()), FontType.NORMAL);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
        table.setSpacingAfter(2f);
        document.add(table);
        LineSeparator sep = new LineSeparator();
        document.add(sep);
        table = helper.newTable(4);
        table.setTableEvent(new PdfPTableEvent() {

          private Object findFirstChunkGenericTag(PdfPRow row) {
            for (PdfPCell cell : row.getCells()) {
              if (cell != null) {
                Phrase phrase = cell.getPhrase();
                if (phrase != null) {
                  final List<Chunk> chunks = phrase.getChunks();
                  if (chunks.size() > 0) {
                    final HashMap<String, Object> attributes = chunks.get(0).getAttributes();
                    if (attributes != null) {
                      return attributes.get(GENERICTAG);
                    }
                  }
                }
              }
            }
            return null;
          }

          @Override
          public void tableLayout(PdfPTable table1, float[][] widths, float[] heights, int headerRows, int rowStart, PdfContentByte[] canvases) {
            for (int row = rowStart; row < widths.length; row++) {
              if (VOID_MARKER.equals(findFirstChunkGenericTag(table1.getRow(row)))) {
                final PdfContentByte canvas = canvases[PdfPTable.BASECANVAS];
                canvas.saveState();
                canvas.setColorStroke(BaseColor.RED);
                final float left = widths[row][0];
                final float right = widths[row][widths[row].length - 1];
                final float bottom = heights[row];
                final float top = heights[row + 1];
                final float center = (bottom + top) / 2;
                canvas.moveTo(left, center);
                canvas.lineTo(right, center);
                canvas.stroke();
                canvas.restoreState();
              }
            }
          }
        });
        table.setWidths(table.getRunDirection() == PdfWriter.RUN_DIRECTION_RTL ?
            new int[]{2, 3, 5, 1} : new int[]{1, 5, 3, 2});
        table.setSpacingBefore(2f);
        table.setSpacingAfter(2f);
        table.setWidthPercentage(100f);
        prevHeaderId = currentHeaderId;
        groupCursor.moveToNext();
        previousBalance = interimBalance;
      }
      long amount = transactionCursor.getLong(columnIndexAmount);
      boolean isVoid = false;
      try {
        isVoid = CrStatus.valueOf(transactionCursor.getString(columnIndexCrStatus)) == CrStatus.VOID;
      } catch (IllegalArgumentException ignored) {
      }

      PdfPCell cell = helper.printToCell(Utils.convDateTime(transactionCursor.getLong(columnIndexDate), itemDateFormat),
          FontType.NORMAL);
      table.addCell(cell);
      if (isVoid) {
        cell.getPhrase().getChunks().get(0).setGenericTag(VOID_MARKER);
      }

      String catText = transactionCursor.getString(columnIndexLabel);
      if (DbUtils.getLongOrNull(transactionCursor, columnIndexTransferPeer) != null) {
        catText = Transfer.getIndicatorPrefixForLabel(amount) + catText;
      } else {
        Long catId = DbUtils.getLongOrNull(transactionCursor, KEY_CATID);
        if (SPLIT_CATID.equals(catId)) {
          Cursor splits = context.getContentResolver().query(Transaction.CONTENT_URI, null,
              KEY_PARENTID + " = " + transactionCursor.getLong(columnIndexRowId), null, null);
          splits.moveToFirst();
          StringBuilder catTextBuilder = new StringBuilder();
          while (splits.getPosition() < splits.getCount()) {
            String splitText = DbUtils.getString(splits, KEY_LABEL);
            if (splitText.length() > 0) {
              if (DbUtils.getLongOrNull(splits, KEY_TRANSFER_PEER) != null) {
                splitText = "[" + splitText + "]";
              }
            } else {
              splitText = Category.NO_CATEGORY_ASSIGNED_LABEL;
            }
            splitText += " " + convAmount(currencyFormatter, splits.getLong(
                splits.getColumnIndexOrThrow(KEY_AMOUNT)), currencyUnit());
            String splitComment = DbUtils.getString(splits, KEY_COMMENT);
            if (splitComment != null && splitComment.length() > 0) {
              splitText += " (" + splitComment + ")";
            }
            catTextBuilder.append(splitText);
            if (splits.getPosition() != splits.getCount() - 1) {
              catTextBuilder.append("; ");
            }
            splits.moveToNext();
          }
          catText = catTextBuilder.toString();
          splits.close();
        } else if (catId == null) {
          catText = Category.NO_CATEGORY_ASSIGNED_LABEL;
        }
      }
      if (account.getId() < 0) {
        //for aggregate accounts we need to indicate the account name
        catText = transactionCursor.getString(transactionCursor.getColumnIndexOrThrow(KEY_ACCOUNT_LABEL))
            + " " + catText;
      }
      String referenceNumber = transactionCursor.getString(columnIndexReferenceNumber);
      if (referenceNumber != null && referenceNumber.length() > 0)
        catText = "(" + referenceNumber + ") " + catText;
      cell = helper.printToCell(catText, FontType.NORMAL);
      String payee = transactionCursor.getString(columnIndexPayee);
      if (payee == null || payee.length() == 0) {
        cell.setColspan(2);
      }
      table.addCell(cell);
      if (payee != null && payee.length() > 0) {
        table.addCell(helper.printToCell(payee, FontType.UNDERLINE));
      }
      FontType t;
      if (account.getId() < 0 &&
          transactionCursor.getInt(transactionCursor.getColumnIndexOrThrow(KEY_IS_SAME_CURRENCY)) == 1) {
        t = FontType.NORMAL;
      } else {
        t = amount < 0 ? FontType.EXPENSE : FontType.INCOME;
      }
      cell = helper.printToCell(convAmount(currencyFormatter, amount, currencyUnit()), t);
      cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
      table.addCell(cell);
      String comment = transactionCursor.getString(columnIndexComment);
      List<String> tagList = MoreDbUtilsKt.splitStringList(transactionCursor, KEY_TAGLIST);
      final boolean hasComment = comment != null && comment.length() > 0;
      final boolean hasTags = tagList.size() > 0;
      if (hasComment || hasTags) {
        table.addCell(helper.emptyCell());
        if (hasComment) {
          cell = helper.printToCell(comment, FontType.ITALIC);
          if (isVoid) {
            cell.getPhrase().getChunks().get(0).setGenericTag(VOID_MARKER);
          }
          if (!hasTags) {
            cell.setColspan(2);
          }
          table.addCell(cell);
        }
        if (hasTags) {
          //for the moment we stick with the result of java.util.AbstractCollection.toString
          //when we convert this to Kotlin, we will add more accurate rendering
          cell = helper.printToCell(tagList.toString(), FontType.BOLD);
          if (isVoid) {
            cell.getPhrase().getChunks().get(0).setGenericTag(VOID_MARKER);
          }
          if (!hasComment) {
            cell.setColspan(2);
          }
          table.addCell(cell);
        }
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
