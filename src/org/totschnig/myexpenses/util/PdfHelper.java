package org.totschnig.myexpenses.util;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;
import java.util.regex.Pattern;

import org.totschnig.myexpenses.MyApplication;
import org.totschnig.myexpenses.util.LazyFontSelector.FontType;

import android.annotation.SuppressLint;
import android.os.Build;
import android.text.TextUtils;
import android.view.View;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Font;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

public class PdfHelper {
  
  private static final Pattern HAS_ANY_RTL_RE =
             Pattern.compile(".*[\\p{InArabic}\\p{InHebrew}].*");
  
  private LazyFontSelector lfs;
  private Font fNormal, fTitle, fHeader, fBold, fItalic, fUnderline, fIncome,
      fExpense;

  private boolean useSystemFonts = true;

  private boolean layoutDirectionFromLocaleIsRTL;

  @SuppressLint("NewApi")
  public PdfHelper() {
    final Locale l = Locale.getDefault();
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
    layoutDirectionFromLocaleIsRTL = TextUtils.getLayoutDirectionFromLocale(l)
        == View.LAYOUT_DIRECTION_RTL;
    } else {
      final int directionality = Character.getDirectionality(l.getDisplayName().charAt(0));
      layoutDirectionFromLocaleIsRTL =  directionality == Character.DIRECTIONALITY_RIGHT_TO_LEFT ||
             directionality == Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC;
    }
    if (MyApplication.getInstance().getMemoryClass()>=32) {
      //we want the Default Font to be used first
      try {
        File dir = new File("/system/fonts");
        File[] files = dir.listFiles();
        Arrays.sort(files, new Comparator<File>() {
          public int compare(File f1, File f2) {
            String n1 = f1.getName();
            String n2 = f2.getName();
            if (n1.equals("DroidSans.ttf")) {
              return -1;
            } else if (n2.equals("DroidSans.ttf")) {
              return 1;
            }
            if (n1.startsWith("Droid")) {
              if (n2.startsWith("Droid")) {
                return n1.compareTo(n2);
              } else {
                return -1;
              }
            } else if (n2.startsWith("Droid")) {
              return 1;
            }
            return n1.compareTo(n2);
          }
        });
        lfs = new LazyFontSelector(files);
        return;
      } catch (Exception e) {}
    }
    useSystemFonts = false;
    fNormal = new Font(Font.FontFamily.TIMES_ROMAN, 12, Font.NORMAL);
    fTitle = new Font(Font.FontFamily.TIMES_ROMAN, 18, Font.BOLD);
    fHeader = new Font(Font.FontFamily.TIMES_ROMAN, 12, Font.BOLD,
        BaseColor.BLUE);
    fBold = new Font(Font.FontFamily.TIMES_ROMAN, 12, Font.BOLD);
    fItalic = new Font(Font.FontFamily.TIMES_ROMAN, 12, Font.ITALIC);
    fUnderline = new Font(Font.FontFamily.TIMES_ROMAN, 12, Font.UNDERLINE);
    fIncome = new Font(Font.FontFamily.TIMES_ROMAN, 12, Font.NORMAL,
        BaseColor.GREEN);
    fExpense = new Font(Font.FontFamily.TIMES_ROMAN, 12, Font.NORMAL,
        BaseColor.RED);
  }

  public PdfPCell printToCell(String text, FontType font) throws DocumentException, IOException {
    PdfPCell cell = new PdfPCell(print(text,font));
    if (hasAnyRtl(text)) {
      cell.setRunDirection(PdfWriter.RUN_DIRECTION_RTL);
    }
    cell.setBorder(Rectangle.NO_BORDER);
    return cell;
  }
  
  public Phrase print(String text, FontType font) throws DocumentException, IOException {
    if (useSystemFonts) {
      return lfs.process(text,font);
    } else {
      switch (font) {
      case BOLD:
        return new Phrase(text, fBold);
      case EXPENSE:
        return new Phrase(text, fExpense);
      case HEADER:
        return new Phrase(text, fHeader);
      case INCOME:
        return new Phrase(text, fIncome);
      case ITALIC:
        return new Phrase(text, fItalic);
      case NORMAL:
        return new Phrase(text, fNormal);
      case TITLE:
        return new Phrase(text, fTitle);
      case UNDERLINE:
        return new Phrase(text, fUnderline);
      }
    }
    return null;
  }
  public static boolean hasAnyRtl(String str) {
        return HAS_ANY_RTL_RE.matcher(str).matches();
      }

  public PdfPCell emptyCell() {
    PdfPCell cell = new PdfPCell();
    cell.setBorder(Rectangle.NO_BORDER);
    return cell;
  }
  public PdfPTable newTable(int numColumns) {
    PdfPTable t = new PdfPTable(numColumns);
    if (layoutDirectionFromLocaleIsRTL) {
      t.setRunDirection(PdfWriter.RUN_DIRECTION_RTL);
    }
    return t;
  }
}
