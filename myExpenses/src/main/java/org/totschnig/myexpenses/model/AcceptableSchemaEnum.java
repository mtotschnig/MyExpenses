package org.totschnig.myexpenses.model;

public enum AcceptableSchemaEnum {

    FTP("ftp"),
    MAILTO("mailto"),
    HTTP("http"),
    HTTPS("https");

    private final String label;

    AcceptableSchemaEnum(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static AcceptableSchemaEnum getEnumFromDescription(String description) {
        for (AcceptableSchemaEnum el : AcceptableSchemaEnum.values()) {
            if (el.getLabel().equalsIgnoreCase(description)) {
                return el;
            }
        }
        return null;
    }
}