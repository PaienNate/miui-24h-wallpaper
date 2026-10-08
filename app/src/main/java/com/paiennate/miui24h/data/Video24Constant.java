package com.paiennate.miui24h.data;

import android.content.Context;

import java.io.File;
import java.util.Locale;

/**
 * Video files are NOT bundled with the app (MIUI assets are copyrighted).
 * The user imports them at runtime; they are copied into the app's internal storage.
 */
public final class Video24Constant {
    private Video24Constant() {}

    public static final int COUNT = 10;
    public static final int NIGHT_VIDEO_ID = 10;

    public static final String DIR = "miui-video24";
    public static final String VIDEO_DIR = "video";
    public static final String THUMB_DIR = "thumnail";

    public static File baseDir(Context c) {
        return new File(c.getFilesDir(), DIR);
    }

    /** @param id 1..10 */
    public static File videoFile(Context c, int id) {
        return new File(new File(baseDir(c), VIDEO_DIR), String.format(Locale.US, "%02d.mp4", id));
    }

    /** @param id 1..10 */
    public static File thumbFile(Context c, int id) {
        return new File(new File(baseDir(c), THUMB_DIR), String.format(Locale.US, "%02d.jpg", id));
    }

    public static int readyVideoCount(Context c) {
        int n = 0;
        for (int i = 1; i <= COUNT; i++) {
            File f = videoFile(c, i);
            if (f.exists() && f.length() > 0) n++;
        }
        return n;
    }

    public static boolean hasAllVideos(Context c) {
        return readyVideoCount(c) == COUNT;
    }
}
