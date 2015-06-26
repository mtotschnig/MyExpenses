package org.totschnig.myexpenses.contrib;


import org.totschnig.myexpenses.BuildConfig;
import org.onepf.oms.OpenIabHelper;

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
/*
    public static final String YANDEX_PUBLIC_KEY
        = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAyJFvxKYy9yjv92JMLDOip3H+EmizsnRtEE3XrTOgWn6tXVlz6urkRmRUwaEWTVvTI4hydxiHtYwFIKHcgm+tXfemm/kPoSnxORG/i/1o44tt2JyRvsgUx5IvMxKU3zg3dsytReRXKd8Pe6Op/2dIZ6sGbYY5sFYpLu7QGcFklAEjk8ipRAQm30W/N+b8VUindSQU/Q/kzgxMyndi0P2cP+z46el3Ww3Y2qVfpZ55QbFVsGl15DOJL9IyIyLLsqq2356otVGV4hLIqIpWSNvIwWiXxc65cwz4FOV4UqwVfjNS/G86d0V2vPLLvwa6DfYoZ3XzWeKtEKKyBPs+UM53rQIDAQAB";
*/

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
/*
    public static final String SLIDEME_PUBLIC_KEY =
        "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA0WqcShQRg/FrvO+Tz8Vgdk+CcjhKyjMeWcJIL4i0gYtO/SA5L13OpSwBUaX46omKMFckDdNCRUW6uWRs6dFF/oDfpPYjgg6KuqG0v7/oR0G5UJxtEBgMke8eRXaDth88CH9aRAp4EmNoJK5BfYLL8nu/9hN05hoqPwpxZCT6DnWgfJpSsAnPyrCkayhhZ3lkhPrkeGN7jBdzyXtOb/B27DA2R8ikRjk7DBO4WBNwSkIFlyHUZA/M2elwGhojooFTlFk6HYFV1dzSzERi28cgi8sSHrN/mFQ7Q2/4vOmxAh+S0DIK9w8+ie2aFguTZEfJzVyIECyhBJ3zJ3MOyx5GiwIDAQAB";
*/

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
      switch (BuildConfig.FLAVOR_distribution) {
        case "play":
          STORE_KEYS_MAP.put(OpenIabHelper.NAME_GOOGLE, Config.GOOGLE_PLAY_KEY);
          break;
/*        case "onepf":
          STORE_KEYS_MAP.put(OpenIabHelper.NAME_YANDEX, Config.YANDEX_PUBLIC_KEY);
          STORE_KEYS_MAP.put(OpenIabHelper.NAME_SLIDEME, Config.SLIDEME_PUBLIC_KEY);
          break;*/
      }

//        STORE_KEYS_MAP.put(OpenIabHelper.NAME_APPLAND, Config.APPLAND_PUBLIC_KEY);
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
