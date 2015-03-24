package org.totschnig.myexpenses.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import android.util.Log;
import android.util.SparseArray;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Font;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Utilities;
import com.itextpdf.text.error_messages.MessageLocalization;
import com.itextpdf.text.pdf.BaseFont;

public class LazyFontSelector {
  public enum FontType {
    NORMAL(12, Font.NORMAL, null), TITLE(18, Font.BOLD, null), HEADER(12,
        Font.BOLD, BaseColor.BLUE), BOLD(12, Font.BOLD, null), ITALIC(12,
        Font.ITALIC, null), UNDERLINE(12, Font.UNDERLINE, null), INCOME(12,
        Font.NORMAL, BaseColor.GREEN), EXPENSE(12, Font.NORMAL, BaseColor.RED);

    SparseArray<Font> fonts = new SparseArray<Font>();
    float size;
    int style;
    BaseColor color;

    FontType(float size, int style, BaseColor color) {
      this.size = size;
      this.style = style;
      this.color = color;
    }

    public Font addFont(int index, BaseFont base) {
      Font f = new Font(base, size, style, color);
      fonts.put(index, f);
      return f;
    }

    public Font getFont(int index) {
      return fonts.get(index);
    }
  }

  protected File[] files;
  protected ArrayList<BaseFont> baseFonts = new ArrayList<BaseFont>();
  protected Font currentFont = null;
  protected FontType type;

  public LazyFontSelector(File[] files) throws DocumentException, IOException {
    this.files = files;
  }

  /**
   * Process the text so that it will render with a combination of fonts if
   * needed.
   * 
   * @param text
   *          the text
   * @param font
   * @return a <CODE>Phrase</CODE> with one or more chunks
   * @throws IOException 
   * @throws DocumentException 
   */
  public Phrase process(String text, FontType type) throws DocumentException, IOException {
    if (files.length == 0)
      throw new IndexOutOfBoundsException(
          MessageLocalization.getComposedMessage("no.font.is.defined"));
    char cc[] = text.toCharArray();
    int len = cc.length;
    StringBuffer sb = new StringBuffer();
    Phrase ret = new Phrase();
    currentFont = null;
    this.type = type;
    for (int k = 0; k < len; ++k) {
      Chunk newChunk = processChar(cc, k, sb);
      if (newChunk != null) {
        ret.add(newChunk);
      }
    }
    if (sb.length() > 0) {
      Chunk ck = new Chunk(sb.toString(), currentFont != null ? currentFont
          : getFont(0));
      ret.add(ck);
    }
    return ret;
  }

  protected Chunk processChar(char[] cc, int k, StringBuffer sb) throws DocumentException, IOException {
    Chunk newChunk = null;
    char c = cc[k];
    if (c == '\n' || c == '\r') {
      sb.append(c);
    } else {
      Font font = null;
      if (Utilities.isSurrogatePair(cc, k)) {
        int u = Utilities.convertToUtf32(cc, k);
        for (int f = 0; f < files.length; ++f) {
          font = getFont(f);
          if (font.getBaseFont().charExists(u)
              || Character.getType(u) == Character.FORMAT) {
            if (currentFont != font) {
              if (sb.length() > 0 && currentFont != null) {
                newChunk = new Chunk(sb.toString(), currentFont);
                sb.setLength(0);
              }
              currentFont = font;
            }
            sb.append(c);
            sb.append(cc[++k]);
            break;
          }
        }
      } else {
        for (int f = 0; f < files.length; ++f) {
          font = getFont(f);
          if (font.getBaseFont().charExists(c)
              || Character.getType(c) == Character.FORMAT) {
            if (currentFont != font) {
              if (sb.length() > 0 && currentFont != null) {
                newChunk = new Chunk(sb.toString(), currentFont);
                sb.setLength(0);
              }
              currentFont = font;
            }
            sb.append(c);
            break;
          }
        }
      }
    }
    return newChunk;
  }

  private BaseFont getBaseFont(int index) throws DocumentException, IOException {
    if (baseFonts.size()<index+1) {
      String file = files[index].getAbsolutePath();
      Log.i("MyExpenses","now loading font file "+file);
      BaseFont bf = BaseFont.createFont(file, BaseFont.IDENTITY_H,
          BaseFont.EMBEDDED);
      baseFonts.add(bf);
      return bf;
    } else {
      return baseFonts.get(index);
    }
  }

  private Font getFont(int index) throws DocumentException, IOException {
    Font f = type.getFont(index);
    if (f == null) {
      f = type.addFont(index, getBaseFont(index));
    }
    return f;
  }
}
