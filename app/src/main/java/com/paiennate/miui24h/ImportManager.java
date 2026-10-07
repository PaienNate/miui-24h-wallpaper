package com.paiennate.miui24h;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Imports user-provided MIUI 24H videos into internal storage.
 * Accepts either a ZIP (containing video/NN.mp4 and optionally thumnail/NN.jpg)
 * or individual files named NN.mp4.
 */
public final class ImportManager {
    private ImportManager() {}

    private static final Pattern VIDEO_NAME = Pattern.compile("(\\d{1,2})\\.mp4$", Pattern.CASE_INSENSITIVE);
    private static final Pattern IMAGE_NAME = Pattern.compile("(\\d{1,2})\\.(jpg|jpeg|png|webp)$", Pattern.CASE_INSENSITIVE);

    /** @return number of video files imported from the zip. */
    public static int importZip(Context c, Uri uri) throws Exception {
        File videoDir = new File(Video24Constant.baseDir(c), Video24Constant.VIDEO_DIR);
        File thumbDir = new File(Video24Constant.baseDir(c), Video24Constant.THUMB_DIR);
        videoDir.mkdirs();
        thumbDir.mkdirs();

        int count = 0;
        byte[] buf = new byte[64 * 1024];
        try (InputStream is = c.getContentResolver().openInputStream(uri)) {
            if (is == null) throw new Exception("cannot open zip");
            try (ZipInputStream zis = new ZipInputStream(is)) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    if (entry.isDirectory()) continue;
                    String name = entry.getName();
                    String base = name.substring(name.lastIndexOf('/') + 1);

                    Matcher vm = VIDEO_NAME.matcher(base);
                    if (vm.find()) {
                        int id = Integer.parseInt(vm.group(1));
                        if (id >= 1 && id <= Video24Constant.COUNT) {
                            writeTo(new File(videoDir, String.format(Locale.US, "%02d.mp4", id)), zis, buf);
                            count++;
                        }
                        continue;
                    }
                    Matcher im = IMAGE_NAME.matcher(base);
                    if (im.find()) {
                        int id = Integer.parseInt(im.group(1));
                        if (id >= 1 && id <= Video24Constant.COUNT) {
                            writeTo(new File(thumbDir, String.format(Locale.US, "%02d.jpg", id)), zis, buf);
                        }
                    }
                }
            }
        }
        return count;
    }

    /** Import a single video file; the id is taken from its name (NN.mp4). @return 1 on success. */
    public static int importSingleVideo(Context c, Uri uri) throws Exception {
        String name = displayName(c, uri);
        if (name == null) name = "";
        Matcher m = VIDEO_NAME.matcher(name);
        if (!m.find()) return 0;
        int id = Integer.parseInt(m.group(1));
        if (id < 1 || id > Video24Constant.COUNT) return 0;

        File videoDir = new File(Video24Constant.baseDir(c), Video24Constant.VIDEO_DIR);
        videoDir.mkdirs();
        byte[] buf = new byte[64 * 1024];
        try (InputStream is = c.getContentResolver().openInputStream(uri)) {
            if (is == null) throw new Exception("cannot open file");
            writeTo(new File(videoDir, String.format(Locale.US, "%02d.mp4", id)), is, buf);
        }
        return 1;
    }

    private static String displayName(Context c, Uri uri) {
        try (Cursor cur = c.getContentResolver().query(uri, null, null, null, null)) {
            if (cur != null && cur.moveToFirst()) {
                int idx = cur.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (idx >= 0) return cur.getString(idx);
            }
        } catch (Throwable ignored) {
        }
        return uri.getLastPathSegment();
    }

    private static void writeTo(File out, InputStream in, byte[] buf) throws Exception {
        try (FileOutputStream fos = new FileOutputStream(out)) {
            int n;
            while ((n = in.read(buf)) > 0) {
                fos.write(buf, 0, n);
            }
        }
    }
}
