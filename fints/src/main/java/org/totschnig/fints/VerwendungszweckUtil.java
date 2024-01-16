/**********************************************************************
 *
 * Copyright (c) 2004 Olaf Willuhn
 * All rights reserved.
 * 
 * This software is copyrighted work licensed under the terms of the
 * Jameica License.  Please consult the file "LICENSE" for details. 
 *
 **********************************************************************/

package org.totschnig.fints;

import org.apache.commons.lang3.StringUtils;
import org.totschnig.myexpenses.util.crashreporting.CrashHandler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import timber.log.Timber;


/**
 * Hilfsklasse zum Mergen und Splitten der erweiterten Verwendungszwecke.
 */
public class VerwendungszweckUtil
{
  /**
   * Liste der bekannten Tags.
   */
  public enum Tag
  {
    /**
     * Ende-zu-Ende Referenz.
     */
    EREF,
    
    /**
     * Kundenreferenz.
     */
    KREF,
    
    /**
     * Mandatsreferenz.
     */
    MREF,
    
    /**
     * Creditor-ID.
     */
    CRED,
    
    /**
     * Debitor-ID.
     */
    DBET,
    
    /**
     * Verwendungszweck.
     */
    SVWZ,
    
    /**
     * Abweichender Auftraggeber.
     */
    ABWA,
    
    /**
     * IBAN des Gegenkontos.
     */
    IBAN,
    
    /**
     * BIC des Gegenkontos.
     */
    BIC,
    
    ;
    
    /**
     * Sucht das Tag mit dem angegebenen Namen.
     * @param s der Name des Tag.
     * @return das Tag oder NULL, wenn es nicht gefunden wurde.
     */
    public static Tag byName(String s)
    {
      if (s == null)
        return null;
      for (Tag t:Tag.values())
      {
        if (t.name().equals(s))
          return t;
      }
      return null;
    }
  }
  
  /**
   * Splittet die Verwendungszweck-Zeilen am Zeilenumbruch.
   * @param lines die Zeilen.
   * @return Ein Array mit den Zeilen.
   * Niemals NULL sondern hoechstens ein leeres Array.
   */
  public static String[] split(String lines)
  {
    if (lines == null || lines.length() == 0)
      return new String[0];
    return lines.split("\n");
  }
  
  /**
   * Zerlegt einen langen Verwendungszweck in 27 Zeichen lange Haeppchen.
   * @param line die zu parsende Zeile.
   * @return die 27 Zeichen langen Schnippsel.
   */
  public static String[] parse(String line)
  {
    if (line == null || line.length() == 0)
      return new String[0];

    // Java's Regex-Implementierung ist sowas von daemlich.
    // String.split() macht nur Rotz, wenn man mit Quantifierern
    // arbeitet. Also ersetzten wir erst mal alles gegen nen
    // eigenen String und verwenden den dann zum Splitten.
    String s = line.replaceAll("(.{27})","$1--##--##");
    return s.split("--##--##");
  }
  
  /**
   * Liefert den Wert des angegebenen Tag oder NULL, wenn er nicht gefunden wurde.
   * @param t der Auftrag.
   * @param tag das Tag.
   * @return der Wert des Tag oder NULL, wenn es nicht gefunden wurde.
   */
  public static String getTag(Transfer t, Tag tag)
  {
    if (t == null || tag == null)
      return null;

    // Sonderrolle SVWZ.
    // Bei den alten Buchungen gab es die Tags ja noch gar nicht.
    // Heisst: Wenn SVWZ angefordert wurde, der Auftrag aber gar keine
    // Tags enthaelt, wird der komplette originale Verwendungszweck zurueckgeliefert
    if (t.getTags().size() == 0)
    {
      if (tag == Tag.SVWZ)
        return toString(t);
      
      return null;
    }
      
    // Sonderrolle SVWZ.
    // Es kann sein, dass der Verwendungszweck so aussieht:
    // "EREF+1234 MREF+1234 SVWZ+"
    // Sprich: Das Tag ist zwar da, aber leer. Macht die "S-Bahn Berlin GmbH".
    // In dem Fall liefern wir ebenfalls den kompletten Text
    String value = t.getTags().get(tag);
    if (tag == Tag.SVWZ && StringUtils.trimToNull(value) == null)
      return toString(t);
    
    return value;
  }

  /**
   * Parst die SEPA-Tags aus den Verwendungszwecken des Auftrages.
   * @return Map mit den geparsten Infos. Niemals NULL sondern hoechstens eine leere Map.
   */
  public static Map<Tag,String> parse(Transfer t)
  {
    if (t == null)
      return new HashMap<>();
    
    return parse(toArray(t));
  }

  /**
   * Parst die SEPA-Tags aus den Verwendungszweck-Zeilen.
   * @param lines die Verwendungszweck-Zeilen.
   * @return Map mit den geparsten Infos. Niemals NULL sondern hoechstens eine leere Map.
   */
  public static Map<Tag,String> parse(String... lines)
  {
    // Wir parsen erstmal alles mit "+".
    Map<Tag,String> result = parse(true,'+',lines);
    if (result.size() == 0)
    {
      // Vielleicht enthaelt es ja nur Tags mit Doppelpunkt?
      return parse(true,':',lines);
    }
    
    // Jetzt schauen wir, ob wir den Verwendungszweck per ":" noch weiter zerlegen koennen
    String svwz = result.get(Tag.SVWZ);
    if (StringUtils.trimToNull(svwz) != null)
      result.putAll(parse(false,':',svwz));
    
    return result;
  }
  
  /**
   * Parst die SEPA-Tags aus den Verwendungszweck-Zeilen.
   * @param leadingSvwz true, wenn ein fuehrerender Verwendungszweck ohne dediziertes Tag beachtet werden soll.
   * @param sep das zu verwendende Trennzeichen.
   * @param lines die Verwendungszweck-Zeilen.
   * @return Map mit den geparsten Infos. Niemals NULL sondern hoechstens eine leere Map.
   */
  private static Map<Tag,String> parse(boolean leadingSvwz, char sep, String... lines)
  {
    Map<Tag,String> result = new HashMap<>();

    if (lines == null || lines.length == 0)
      return result;

    String line = merge(lines);
    int first = -1;

    try
    {

      // Jetzt iterieren wir ueber die bekannten Tags. Wenn wir eines im Text finden, extrahieren
      // wir alles bis zum naechsten Tag.
      for (Tag tag:Tag.values())
      {
        int start = line.indexOf(tag.name()+sep); // Trenner dahinter, um sicherzustellen, dass sowas wie "EREF" nicht mitten im Text steht
        if (start == -1)
          continue; // Nicht gefunden

        // Position des ersten Tag merken - brauchen wir weiter unten eventuell noch
        if (first == -1 || start < first)
          first = start;
        
        int next = 0;
        
        while (next < line.length()) // Wir suchen solange, bis wir am Ende angekommen sind.
        {
          int tagLen = tag.name().length() + 1; // Laenge des Tag + Trennzeichen
          
          // OK, wir haben das Tag. Jetzt suchen wir bis zum naechsten Tag.
          next = line.indexOf(sep,start + tagLen + next);
          if (next == -1)
          {
            // Kein weiteres Tag mehr da. Gehoert alles zum Tag.
            String substring = line.substring(start + tagLen);
            result.put(tag,StringUtils.trimToEmpty(tag == Tag.SVWZ ? substring : substring.replace("\n","")));
            break;
          }
          else
          {
            // Checken, ob vor dem "+" ein bekanntes Tag steht
            String s = line.substring(next-4,next);
            Tag found = Tag.byName(s);
            if (found == null)
            {
              // Sonderfall BIC - nur 3 Zeichen lang?
              found = Tag.byName(line.substring(next-3,next));
            }
            
            // Ist ein bekanntes Tag. Also uebernehmen wir den Text genau bis dahin
            if (found != null)
            {
              result.put(tag,StringUtils.trimToEmpty(line.substring(start + tagLen,next - found.name().length()).replace("\n","")));
              break;
            }
          }
        }
      }
      
      // Noch eine Sonderrolle bei SVWZ. Es gibt Buchungen, die so aussehen:
      // "Das ist eine Zeile ohne Tag\nKREF+Und hier kommt noch ein Tag".
      // Sprich: Der Verwendungszweck enthaelt zwar Tags, der Verwendungszweck selbst hat aber keines
      // sondern steht nur vorn dran.
      // Wenn wir Tags haben, SVWZ aber fehlt, nehmen wir als SVWZ den Text bis zum ersten Tag
      if (leadingSvwz && result.size() > 0 && !result.containsKey(Tag.SVWZ) && first > 0)
      {
        result.put(Tag.SVWZ,StringUtils.trimToEmpty(line.substring(0,first).replace("\n","")));
      }
      
      // Sonderrolle IBAN. Wir entfernen alles bis zum ersten Leerzeichen. Siehe "testParse012". Da hinter der
      // IBAN kein vernuenftiges Tag mehr kommt, wuerde sonst der ganze Rest da mit reinfallen. Aber nur, wenn
      // es erst nach 22 Zeichen kommt. Sonst steht es mitten in der IBAN drin. In dem Fall entfernen wir die
      // Leerzeichen aus der IBAN (siehe "testParse013")
      String iban = StringUtils.trimToNull(result.get(Tag.IBAN));
      if (iban != null)
      {
        int space = iban.indexOf(" ");
        if (space > 21) // Wir beginnen ja bei 0 mit dem Zaehlen
          result.put(Tag.IBAN,StringUtils.trimToEmpty(iban.substring(0,space)));
        else if (space != -1)
          result.put(Tag.IBAN,StringUtils.deleteWhitespace(iban));
      }
      
      // testParse013: Leerzeichen aus der BIC entfernen
      String bic = StringUtils.trimToNull(result.get(Tag.BIC));
      if (bic != null)
        result.put(Tag.BIC,StringUtils.deleteWhitespace(bic));
        
    }
    catch (Exception e)
    {
      Timber.w("unable to parse line: %s", line);
      CrashHandler.report(e);
    }
    return result;
  }
  
  /**
   * Verteilt die angegebenen Verwendungszweck-Zeilen auf zweck, zweck2 und zweck3.
   * @param lines die zu uebernehmenden Zeilen.
   */
  public static Transfer apply(String[] lines)
  {
    if (lines == null || lines.length == 0)
      return null;
    
    List<String> l = clean(true,lines);
    String zweck = l.size() > 0 ? l.remove(0) : null;
    String zweck2 = l.size() > 0 ? l.remove(0) : null;
    String[] weitereVerwendungszwecke = l.toArray(new String[0]);
    return new Transfer(zweck, zweck2, weitereVerwendungszwecke, parse(toArray(zweck, zweck2, weitereVerwendungszwecke)));
  }
  
  /**
   * Bricht die Verwendungszweck-Zeilen auf $limit Zeichen lange Haeppchen neu um.
   * Jedoch nur, wenn wirklich Zeilen enthalten sind, die laenger sind.
   * Andernfalls wird nichts umgebrochen.
   * @param limit das Zeichen-Limit pro Zeile.
   * @param lines die Zeilen.
   * @return die neu umgebrochenen Zeilen.
   */
  public static String[] rewrap(int limit, String... lines)
  {
    if (lines == null || lines.length == 0)
      return lines;
    
    boolean found = false;
    for (String s:lines)
    {
      if (s != null && s.length() > limit)
      {
        found = true;
        break;
      }
    }
    if (!found)
      return lines;

    List<String> l = clean(true,lines);
    
    // Zu einem String mergen
    StringBuilder sb = new StringBuilder();
    for (String line:l)
    {
      sb.append(line);
    }
    String result = sb.toString();

    // und neu zerlegen
    String s = result.replaceAll("(.{" + limit + "})","$1--##--##");
    return s.split("--##--##");
  }
  
  /**
   * Merget die Verwendungszweck-Zeilen zu einem String zusammen.
   * Die Zeilen sind mit Zeilenumbruch versehen.
   * @param lines die Zeilen.
   * @return die gemergten Zeilen. Wird NULL oder ein leeres
   * Array uebergeben, liefert die Funktion NULL.
   */
  public static String merge(String... lines)
  {
    if (lines == null || lines.length == 0)
      return null;
    
    List<String> cleaned = clean(false,lines);
    StringBuilder sb = new StringBuilder();
    for (String line:cleaned)
    {
      sb.append(line);
      sb.append("\n");
    }

    String result = sb.toString();
    return result.length() == 0 ? null : result;
  }
  
  /**
   * Liefert eine bereinigte Liste der Verwendungszweck-Zeilen des Auftrages.
   * @param t der Auftrag.
   * @return bereinigte Liste der Verwendungszweck-Zeilen des Auftrages.
   */
  public static String[] toArray(Transfer t)
  {
    return toArray(t.getZweck(), t.getZweck2(), t.getWeitereVerwendungszwecke());
  }

  /**
   * Liefert eine bereinigte Liste der Verwendungszweck-Zeilen des Auftrages.
   * @return bereinigte Liste der Verwendungszweck-Zeilen des Auftrages.
   */
  public static String[] toArray(String zweck, String zweck2, String[] weitereVerwendungsZwecke)
  {
    List<String> lines = new ArrayList<>();
    lines.add(zweck);
    lines.add(zweck2);
    Collections.addAll(lines, weitereVerwendungsZwecke);

    String[] list = lines.toArray(new String[0]);
    List<String> result = clean(false,list);
    return result.toArray(new String[0]);
  }

  /**
   * Merget die Verwendungszweck-Zeilen des Auftrages zu einer Zeile zusammen.
   * Als Trennzeichen fuer die Zeilen wird " " (ein Leerzeichen) verwendet.
   * @param t der Auftrag.
   * @return der String mit einer Zeile, die alle Verwendungszwecke enthaelt.
   */
  public static String toString(Transfer t)
  {
    return toString(t," ");
  }
  
  /**
   * Merget die Verwendungszweck-Zeilen des Auftrages zu einer Zeile zusammen.
   * @param t der Auftrag.
   * @param sep das zu verwendende Trennzeichen fuer die Zeilen. Wenn es null ist, wird " "
   * (ein Leerzeichen) verwendet.
   * @return der String mit einer Zeile, die alle Verwendungszwecke enthaelt.
   */
  public static String toString(Transfer t, String sep)
  {
    if (sep == null)
      sep = " ";
    StringBuilder sb = new StringBuilder();
    
    String[] lines = toArray(t);
    for (int i=0;i<lines.length;++i)
    {
      sb.append(lines[i]);
      
      // Trennzeichen bei der letzten Zeile weglassen
      if (i+1 < lines.length)
        sb.append(sep);
    }

    String result = sb.toString();
    
    // Wenn als Trennzeichen "\n" angegeben ist, kann es
    // bei den weiteren Verwendungszwecken drin bleiben
    if (sep.equals("\n"))
      return result;
    
    // Andernfalls ersetzen wir es gegen das angegebene Zeichen
    return result.replace("\n",sep);
  }
  
  /**
   * Bereinigt die Verwendungszweck-Zeilen.
   * Hierbei werden leere Zeilen oder NULL-Elemente entfernt.
   * Ausserdem werden alle Zeilen getrimt.
   * @param trim wenn die Zeilen-Enden getrimmt werden sollen.
   * @param lines die zu bereinigenden Zeilen.
   * @return die bereinigten Zeilen.
   */
  private static List<String> clean(boolean trim, String... lines)
  {
    return clean(trim,lines != null ? Arrays.asList(lines) : null);
  }

  /**
   * Bereinigt die Verwendungszweck-Zeilen.
   * Hierbei werden leere Zeilen oder NULL-Elemente entfernt.
   * Ausserdem werden alle Zeilen getrimt.
   * @param trim wenn die Zeilen-Enden getrimmt werden sollen.
   * @param lines die zu bereinigenden Zeilen.
   * @return die bereinigten Zeilen.
   */
  private static List<String> clean(boolean trim, List<String> lines)
  {
    List<String> result = new ArrayList<>();
    if (lines == null || lines.size() == 0)
      return result;
    
    for (String line:lines)
    {
      if (line == null)
        continue;
      
      if (trim)
        line = line.trim();
      if (line.length() > 0)
        result.add(line);
    }
    
    return result;
  }
}
