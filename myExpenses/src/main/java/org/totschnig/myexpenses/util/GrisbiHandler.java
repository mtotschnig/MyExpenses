package org.totschnig.myexpenses.util;

import org.totschnig.myexpenses.R;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.ArrayList;

import androidx.core.util.Pair;

public class GrisbiHandler extends DefaultHandler {
  CategoryTree catTree;
  ArrayList<String> partiesList;
  private StringBuilder builder;
  String grisbiFileVersion;
  String mainElementName6 = "Category",
      subElementName6 = "Sub_category",
      partiesElementName6 = "Party",
      nameAttributeName6 = "Na",
      mainElementName5 = "Categorie",
      subElementName5 = "Sous-categorie",
      partiesElementName5 = "Tiers",
      nameAttributeName5 = "Nom";
  Integer currentMainCategorieId;

  public class FileVersionNotSupportedException extends SAXException {

    public FileVersionNotSupportedException(String grisbiFileVersion) {
      super(grisbiFileVersion);
    }

  }

  private boolean checkFileVersion(String version) {
    return version.equals("0.6.0") || version.equals("0.5.0");
  }


  public Result<Pair<CategoryTree, ArrayList<String>>> getResult() {
    if (catTree.getTotal() > 0 || !partiesList.isEmpty()) {
      return Result.ofSuccess(0, Pair.create(catTree, partiesList));
    } else {
      return Result.ofFailure(R.string.no_data);
    }
  }

  @Override
  public void characters(char[] ch, int start, int length)
      throws SAXException {
    super.characters(ch, start, length);
    builder.append(ch, start, length);
  }

  @Override
  public void endElement(String uri, String localName, String name)
      throws SAXException {
    super.endElement(uri, localName, name);
    if (localName.equals("Version_fichier") || localName.equals("Version_fichier_categ")) {
      String grisbiFileVersion = builder.toString();
      if (!checkFileVersion(grisbiFileVersion)) {
        throw new FileVersionNotSupportedException(grisbiFileVersion);
      }
    }
  }

  @Override
  public void startDocument() throws SAXException {
    super.startDocument();
    catTree = new CategoryTree("root");
    partiesList = new ArrayList<>();
    builder = new StringBuilder();
  }

  @Override
  public void startElement(String uri, String localName, String name,
                           Attributes attributes) throws SAXException {
    String label, id, parent_id;
    super.startElement(uri, localName, name, attributes);
    if (localName.equalsIgnoreCase("General")) {
      String grisbiFileVersion = attributes.getValue("File_version");
      if (!checkFileVersion(grisbiFileVersion)) {
        throw new FileVersionNotSupportedException(grisbiFileVersion);
      }
    } else if (localName.equals(mainElementName6)) {
      label = attributes.getValue(nameAttributeName6);
      id = attributes.getValue("Nb");
      if (label != null && id != null) {
        catTree.add(label, Integer.parseInt(id), 0);
      }
    } else if (localName.equals(subElementName6)) {
      label = attributes.getValue(nameAttributeName6);
      id = attributes.getValue("Nb");
      parent_id = attributes.getValue("Nbc");
      if (label != null && id != null && parent_id != null) {
        catTree.add(label, Integer.parseInt(id), Integer.parseInt(parent_id));
      }
    } else if (localName.equals(mainElementName5)) {
      label = attributes.getValue(nameAttributeName5);
      id = attributes.getValue("No");
      if (label != null && id != null) {
        currentMainCategorieId = Integer.parseInt(id);
        catTree.add(label, currentMainCategorieId, 0);
      }
    } else if (localName.equals(subElementName5)) {
      label = attributes.getValue(nameAttributeName5);
      id = attributes.getValue("No");
      if (label != null && id != null) {
        catTree.add(label, Integer.parseInt(id), currentMainCategorieId);
      }
    } else if (localName.equals(partiesElementName6)) {
      label = attributes.getValue(nameAttributeName6);
      if (label != null) {
        partiesList.add(label);
      }
    } else if (localName.equals(partiesElementName5)) {
      label = attributes.getValue(nameAttributeName5);
      if (label != null) {
        partiesList.add(label);
      }
    }
    builder.setLength(0);
  }
}