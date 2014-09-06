package org.totschnig.myexpenses.util;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

import android.util.Log;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Font;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.FontSelector;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfWriter;

public class PdfHelper {
  
  private static final Pattern HAS_ANY_RTL_RE =
             Pattern.compile(".*[\\p{InArabic}\\p{InHebrew}].*");
  
  private FontSelector fsNormal, fsTitle, fsHeader, fsBold, fsItalic,
      fsUnderline, fsIncome, fsExpense;
  private Font fNormal, fTitle, fHeader, fBold, fItalic, fUnderline, fIncome,
      fExpense;

  public enum FontType {
    NORMAL, TITLE, HEADER, BOLD, ITALIC, UNDERLINE, INCOME, EXPENSE;
  }

  private boolean useSystemFonts = true;

  public PdfHelper() {
    List<BaseFont> bfList = new ArrayList<BaseFont>();
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
      for (File file : files) {
        String filePath = file.getAbsolutePath();
        Log.i("DEBUG", filePath);
        if (filePath.endsWith("ttf")) {
          bfList.add(BaseFont.createFont(file.getAbsolutePath(),
              BaseFont.IDENTITY_H, BaseFont.EMBEDDED));
        }
      }
      // prepare fsNormal
      fsNormal = new FontSelector();
      fsTitle = new FontSelector();
      fsHeader = new FontSelector();
      fsBold = new FontSelector();
      fsItalic = new FontSelector();
      fsUnderline = new FontSelector();
      fsIncome = new FontSelector();
      fsExpense = new FontSelector();
      for (BaseFont baseFont : bfList) {
        fsNormal.addFont(new Font(baseFont, 12, Font.NORMAL));
        fsTitle.addFont(new Font(baseFont, 18, Font.BOLD));
        fsHeader.addFont(new Font(baseFont, 12, Font.BOLD, BaseColor.BLUE));
        fsBold.addFont(new Font(baseFont, 12, Font.BOLD));
        fsItalic.addFont(new Font(baseFont, 12, Font.ITALIC));
        fsUnderline.addFont(new Font(baseFont, 12, Font.UNDERLINE));
        fsIncome.addFont(new Font(baseFont, 12, Font.NORMAL, BaseColor.GREEN));
        fsExpense.addFont(new Font(baseFont, 12, Font.NORMAL, BaseColor.RED));
      }
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

  public PdfPCell printToCell(String text, FontType font) {
    PdfPCell cell = new PdfPCell(print(text,font));
    if (hasAnyRtl(text)) {
      cell.setRunDirection(PdfWriter.RUN_DIRECTION_RTL);
    }
    cell.setBorder(Rectangle.NO_BORDER);
    return cell;
  }
  
  public Phrase print(String text, FontType font) {
    if (useSystemFonts) {
      switch (font) {
      case BOLD:
        return fsBold.process(text);
      case EXPENSE:
        return fsExpense.process(text);
      case HEADER:
        return fsHeader.process(text);
      case INCOME:
        return fsIncome.process(text);
      case ITALIC:
        return fsItalic.process(text);
      case NORMAL:
        return fsNormal.process(text);
      case TITLE:
        return fsTitle.process(text);
      case UNDERLINE:
        return fsUnderline.process(text);
      }
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
