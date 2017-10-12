package org.totschnig.myexpenses.contrib;


import java.util.ArrayList;
import java.util.List;

public final class Config {

    // SKUs for our products:
    public static final String SKU_PREMIUM = "sku_premium";
    public static final String SKU_EXTENDED = "sku_extended";
    public static final String SKU_PREMIUM2EXTENDED = "sku_premium2extended";
    public static final String SKU_PROFESSIONAL_1 = "sku_professional_monthly";
    public static final String SKU_PROFESSIONAL_12 = "sku_professional_yearly";
    public static final String SKU_EXTENDED2PROFESSIONAL_12 = "sku_extended2professional_yearly";

    public static final List<String> itemSkus = new ArrayList<>();
    public static final List<String> subsSkus = new ArrayList<>();
    public static final List<String> allSkus = new ArrayList<>();

    static {
      itemSkus.add(SKU_PREMIUM);
      itemSkus.add(SKU_EXTENDED);
      itemSkus.add(SKU_PREMIUM2EXTENDED);
      subsSkus.add(SKU_PROFESSIONAL_1);
      subsSkus.add(SKU_PROFESSIONAL_12);
      subsSkus.add(SKU_EXTENDED2PROFESSIONAL_12);
      allSkus.addAll(itemSkus);
      allSkus.addAll(subsSkus);
    }

    private Config() {
    }
}
