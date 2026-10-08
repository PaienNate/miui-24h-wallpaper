package com.paiennate.miui24h.data;

import java.util.Locale;

/** Bundled video assets: {@code assets/miui-video24/video/NN.mp4}. */
public final class Video24Constant {
    private Video24Constant() {}

    public static final int COUNT = 10;
    public static final int NIGHT_VIDEO_ID = 10;

    public static final String ASSET_DIR = "miui-video24";
    public static final String VIDEO_DIR = ASSET_DIR + "/video";

    /** @param id 1..10 */
    public static String videoAsset(int id) {
        return String.format(Locale.US, "%s/%02d.mp4", VIDEO_DIR, id);
    }

    /** @param id 1..10 */
    public static String assetUri(int id) {
        return "file:///android_asset/" + videoAsset(id);
    }
}
