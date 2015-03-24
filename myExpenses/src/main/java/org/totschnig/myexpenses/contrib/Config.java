package org.totschnig.myexpenses.contrib;

import org.onepf.oms.OpenIabHelper;
import org.onepf.oms.SkuManager;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for purchase.
 * <p/>
 * Created by krozov on 01.09.14.
 */
public final class Config {

    // SKUs for our products: the premium upgrade (non-consumable) and gas (consumable)
    public static final String SKU_PREMIUM = "sku_premium";

    /**
     * Google play public key.
     */
    public static final String GOOGLE_PLAY_KEY
        = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAlyDg92FlUJWJFGnjJalxr54jgqz4FavtyBhB0buUKhhJ9paHM+ygKYbCrKat6l9haatuwmWjnAY/TA1USi+Cbsnd4pRAQdHSca0UUDDUGvW5QfWdGsogxxOE9YNwk1oDf2dkgVeow/bnGKd/hMZA54TimXK2vc4qemfA/TL7QHIOEnJSc/VVGCTBOm5NMswqRohQHz+2qM9KbLr1u/S71TTM8svIInkJPwCqlUwAl+eAuUra4DNvZvCusQ73oyCTNxUFW2Xhe/cZWdRAEu69yEwD3esyjY6zGav0rd2n7T2qWyiQ4H+B8LrfDXhqjmowTXhdfCpmo60LUocRDPdRZQIDAQAB";

    /**
     * Yandex.Store public key.
     */
//    public static final String YANDEX_PUBLIC_KEY
//            = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAsGEswqmagEHk5nY+xStc8cBxdfEuKgbt4j" +
//            "KSpYc7T1nwUulX6E+7zY4+vk/l6hmcZiHYT8cuXUEV1+Kq2rJLKlZnf4HATEMQgtzHxbBBmcHccYKOb6t" +
//            "pVi/Tj/ws9l+KBiX8o3JlF3zpzdx0y1dPuVlOyUA7dmB2X4+7DXQDumLvjTkTxMOpZmb/ajKBNF73OeO" +
//            "q/Fsi0MNhzzBv+GHeKDE2rBHNCuAVL2hsHhYYldjogZNd4j5a54xH8h0Wn5UKIvZPZ2r5kOxU/dpUi0Fp" +
//            "+iokOxuMV9yX5rOYw+5t+Asok5+dtrCpLBZx2fzAoANLnmRK/3mNWr9KY7YXJPiQ1QIDAQAB";

    /**
     * Appland store public key.
     */
//    public static final String APPLAND_PUBLIC_KEY =
//            "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQC5idC9c24V7a7qCJu7kdIyOZsk\n" +
//                    "W0Rc7/q+K+ujEXsUaAdb5nwmlOJqpoJeCh5Fmq5A1NdF3BwkI8+GwTkH757NBZAS\n" +
//                    "SdEuN0pLZmA6LopOiMIy0LoIWknM5eWMa3e41CxCEFoMv48gFIVxDNJ/KAQAX7+K\n" +
//                    "ysYzIdlA3W3fBXXyGQIDAQAB";

    /**
     * SlideMe store public key.
     */
//    public static final String SLIDEME_PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBC" +
//            "gKCAQEAq6rFm2wb9sm" +
//            "bcowrfZHYw71ISHYxF/tG9Jn9c+nRzFCVDSXjvedBxKllw16/GEx9DQ32Ut8azVAznB2wBDNUsS" +
//            "M8nzNhHeCSDvEX2/Ozq1dEq3V3DF4jBEKDAkIOMzIBRWN8fpA5MU/9m8QD9xkJDfP7Mw/6zEMidk" +
//            "2CEE8EZRTlpQ8ULVgBlFISd8Mt9w8ZFyeTyJTZhF2Z9+RZN8woU+cSXiVRmiA0+v2R8Pf+YNJb9fd" +
//            "V5yvM8r9K1MEdRaXisJyMOnjL7H2mZWigWLm7uGoUGuIg9HHi09COBMm3dzAe9yLZoPSG75SvYDs" +
//            "AZ6ms8IYxF6FAniNqfMOuMFV8zwIDAQAB";

//    public static final String SKU_GAS_NOKIA_STORE = "1290250";
//    public static final String SKU_INFINITE_GAS_NOKIA_STORE = "1290302";
//    public static final String SKU_PREMIUM_NOKIA_STORE = "1290315";
//
//    public static final String SKU_GAS_AMAZON = "amazon.sku_gas";
//    public static final String SKU_INFINITE_GAS_AMAZON = "amazon.sku_infinite_gas";
//    public static final String SKU_PREMIUM_AMAZON = "amazon.sku_premium";
//
//    public static final String SKU_GAS_YANDEX = "yandex.sku_gas";
//    public static final String SKU_INFINITE_GAS_YANDEX = "yandex.sku_infinite_gas";
//    public static final String SKU_PREMIUM_YANDEX = "yandex.sku_premium";
//
//    public static final String SKU_GAS_APPLAND = "appland.sku_gas";
//    public static final String SKU_INFINITE_GAS_APPLAND = "appland.sku_infinite_gas";
//    public static final String SKU_PREMIUM_APPLAND = "appland.sku_premium";
//
//    public static final String SKU_GAS_SLIDEME = "slideme.sku_gas";
//    public static final String SKU_INFINITE_GAS_SLIDEME = "slideme.sku_infinite_gas";
//    public static final String SKU_PREMIUM_SLIDEME = "slideme.sku_premium";

    public static final Map<String, String> STORE_KEYS_MAP;

    static {
        STORE_KEYS_MAP = new HashMap<String,String>();
        STORE_KEYS_MAP.put(OpenIabHelper.NAME_GOOGLE, Config.GOOGLE_PLAY_KEY);
//        STORE_KEYS_MAP.put(OpenIabHelper.NAME_YANDEX, Config.YANDEX_PUBLIC_KEY);
//        STORE_KEYS_MAP.put(OpenIabHelper.NAME_APPLAND, Config.APPLAND_PUBLIC_KEY);
//        STORE_KEYS_MAP.put(OpenIabHelper.NAME_SLIDEME, Config.SLIDEME_PUBLIC_KEY);
//        STORE_KEYS_MAP.put(OpenIabHelper.NAME_AMAZON,
//                "Unavailable. Amazon doesn't support RSA verification. So this mapping is not needed");
//        STORE_KEYS_MAP.put(OpenIabHelper.NAME_SAMSUNG,
//                "Unavailable. SamsungApps doesn't support RSA verification. So this mapping is not needed");

        //Only map SKUs for stores where SKU that using in app different from described in store console.
        // In this sample only Google Play store use the same skus.
//        SkuManager.getInstance()
//                .mapSku(SKU_INFINITE_GAS, OpenIabHelper.NAME_NOKIA, SKU_INFINITE_GAS_NOKIA_STORE)
//                .mapSku(SKU_INFINITE_GAS, OpenIabHelper.NAME_YANDEX, SKU_INFINITE_GAS_YANDEX)
//                .mapSku(SKU_INFINITE_GAS, OpenIabHelper.NAME_AMAZON, SKU_INFINITE_GAS_AMAZON)
//                .mapSku(SKU_INFINITE_GAS, OpenIabHelper.NAME_APPLAND, SKU_INFINITE_GAS_APPLAND)
//                .mapSku(SKU_INFINITE_GAS, OpenIabHelper.NAME_SLIDEME, SKU_INFINITE_GAS_SLIDEME)
//
//                .mapSku(SKU_GAS, OpenIabHelper.NAME_NOKIA, SKU_GAS_NOKIA_STORE)
//                .mapSku(SKU_GAS, OpenIabHelper.NAME_YANDEX, SKU_GAS_YANDEX)
//                .mapSku(SKU_GAS, OpenIabHelper.NAME_AMAZON, SKU_GAS_AMAZON)
//                .mapSku(SKU_GAS, OpenIabHelper.NAME_APPLAND, SKU_GAS_APPLAND)
//                .mapSku(SKU_GAS, OpenIabHelper.NAME_SLIDEME, SKU_GAS_SLIDEME)
//
//                .mapSku(SKU_PREMIUM, OpenIabHelper.NAME_NOKIA, SKU_PREMIUM_NOKIA_STORE)
//                .mapSku(SKU_PREMIUM, OpenIabHelper.NAME_YANDEX, SKU_PREMIUM_YANDEX)
//                .mapSku(SKU_PREMIUM, OpenIabHelper.NAME_AMAZON, SKU_PREMIUM_AMAZON)
//                .mapSku(SKU_PREMIUM, OpenIabHelper.NAME_APPLAND, SKU_PREMIUM_APPLAND)
//                .mapSku(SKU_PREMIUM, OpenIabHelper.NAME_SLIDEME, SKU_PREMIUM_SLIDEME);
    }

    private Config() {
    }
}
