package org.totschnig.myexpenses.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

import org.totschnig.myexpenses.util.LazyFontSelector.FontType;

import android.util.Log;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Font;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.FontSelector;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfWriter;

public class PdfHelper2 {
  
  private static final Pattern HAS_ANY_RTL_RE =
             Pattern.compile(".*[\\p{InArabic}\\p{InHebrew}].*");
  
  private LazyFontSelector lfs;
  private Font fNormal, fTitle, fHeader, fBold, fItalic, fUnderline, fIncome,
      fExpense;

  private boolean useSystemFonts = true;

  public PdfHelper2() {
    long start = System.currentTimeMillis();
    Log.d("MyExpenses","PdfHelper2 constructor start "+start);
    //we want the Default Font to be used first
    try {
      File dir = new File("/system/fonts");
      File[] files = dir.listFiles();
      Arrays.sort(files, new Comparator<File>() {
        public int compare(File f1, File f2) {
          if (f1.getName().equals("DroidSans.ttf")) {
            return -1;
          } else {
            return f1.compareTo(f2);
          }
        }
      });
      Log.d("MyExpenses","Files listed "+(System.currentTimeMillis()-start));
      lfs = new LazyFontSelector(files);
      Log.d("MyExpenses","Font selectors set up "+(System.currentTimeMillis()-start));
    } catch (Exception e) {
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
}
