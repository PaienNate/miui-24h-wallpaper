package com.paiennate.miui24h;

import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.opengl.GLES20;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

public final class Utils {
    private Utils() {}

    private static final String TAG = "Miui24H";

    public static void debug(String tag, String msg) {
        Log.d(TAG, tag + ": " + msg);
    }

    public static int compileShaderResourceGLES20(Context ctx, int type, int resId) {
        String src = readRaw(ctx, resId);
        int shader = GLES20.glCreateShader(type);
        if (shader == 0) throw new RuntimeException("Failed to create shader");
        GLES20.glShaderSource(shader, src);
        GLES20.glCompileShader(shader);
        int[] status = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, status, 0);
        if (status[0] == 0) {
            String log = GLES20.glGetShaderInfoLog(shader);
            GLES20.glDeleteShader(shader);
            throw new RuntimeException(log);
        }
        return shader;
    }

    public static int linkProgramGLES20(int vertexShader, int fragmentShader) {
        int program = GLES20.glCreateProgram();
        if (program == 0) throw new RuntimeException("Failed to create program");
        GLES20.glAttachShader(program, vertexShader);
        GLES20.glAttachShader(program, fragmentShader);
        GLES20.glLinkProgram(program);
        int[] status = new int[1];
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, status, 0);
        if (status[0] == 0) {
            String log = GLES20.glGetProgramInfoLog(program);
            GLES20.glDeleteProgram(program);
            throw new RuntimeException(log);
        }
        return program;
    }

    private static String readRaw(Context ctx, int resId) {
        StringBuilder sb = new StringBuilder();
        try (InputStream is = ctx.getResources().openRawResource(resId);
             BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append('\n');
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return sb.toString();
    }

    public static String httpGet(String urlStr) throws Exception {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Android) miui24h");
            conn.setInstanceFollowRedirects(true);
            int code = conn.getResponseCode();
            InputStream is = (code >= 200 && code < 400) ? conn.getInputStream() : conn.getErrorStream();
            if (is == null) throw new Exception("HTTP " + code);
            BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            br.close();
            return sb.toString();
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    @SuppressWarnings("MissingPermission")
    public static Location getLastKnownLocation(Context ctx) {
        try {
            LocationManager lm = (LocationManager) ctx.getSystemService(Context.LOCATION_SERVICE);
            if (lm == null) return null;
            Location best = null;
            List<String> providers = lm.getProviders(true);
            for (String provider : providers) {
                Location l;
                try {
                    l = lm.getLastKnownLocation(provider);
                } catch (Throwable t) {
                    continue;
                }
                if (l == null) continue;
                if (best == null || l.getTime() > best.getTime()) best = l;
            }
            return best;
        } catch (Throwable t) {
            return null;
        }
    }
}
