package com.paiennate.miui24h;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class SunTimeManager {
    private SunTimeManager() {}

    public static int getSunSource(Context c) {
        return c.getSharedPreferences(Const.OPTIONS_PREF, Context.MODE_PRIVATE)
                .getInt(Const.KEY_SUN_SOURCE, Const.SRC_OFFLINE);
    }

    public static void setSunSource(Context c, int value) {
        c.getSharedPreferences(Const.OPTIONS_PREF, Context.MODE_PRIVATE)
                .edit().putInt(Const.KEY_SUN_SOURCE, value).apply();
    }

    private static String today() {
        return new SimpleDateFormat("yyyyMMdd", Locale.US).format(new Date());
    }

    /** Main-thread safe: today's cached values if present, else the hardcoded fallback. */
    public static SunTimes getTodayCachedOrFallback(Context c) {
        SharedPreferences p = c.getSharedPreferences(Const.CACHE_PREF, Context.MODE_PRIVATE);
        if (today().equals(p.getString(Const.KEY_CACHE_DAY, ""))) {
            return new SunTimes(
                    p.getInt(Const.KEY_SUNRISE_MIN, Const.DEFAULT_SUNRISE_MIN),
                    p.getInt(Const.KEY_SUNSET_MIN, Const.DEFAULT_SUNSET_MIN),
                    p.getString(Const.KEY_LAST_SUN_SOURCE_NAME, "cache"));
        }
        return new SunTimes(Const.DEFAULT_SUNRISE_MIN, Const.DEFAULT_SUNSET_MIN, "固定 06:00/18:00");
    }

    public static boolean hasTodayCache(Context c) {
        SharedPreferences p = c.getSharedPreferences(Const.CACHE_PREF, Context.MODE_PRIVATE);
        return today().equals(p.getString(Const.KEY_CACHE_DAY, ""));
    }

    private static SunTimeProvider providerFor(int src) {
        switch (src) {
            case Const.SRC_TENCENT: return new TencentSunTimeProvider();
            case Const.SRC_OPENMETEO: return new OpenMeteoSunTimeProvider();
            case Const.SRC_SUNRISESUNSET: return new SunriseSunsetOrgSunTimeProvider();
            case Const.SRC_HARDCODED: return new HardcodedSunTimeProvider();
            case Const.SRC_OFFLINE:
            default: return new OfflineSolarSunTimeProvider();
        }
    }

    /** Blocking (network/location). Call from a worker thread. */
    public static SunTimes compute(Context c) {
        List<SunTimeProvider> chain = new ArrayList<>();
        chain.add(providerFor(getSunSource(c)));
        chain.add(new OfflineSolarSunTimeProvider());
        chain.add(new HardcodedSunTimeProvider());
        for (SunTimeProvider p : chain) {
            try {
                return p.getSunTimes(c);
            } catch (Throwable ignored) {
                // try next in the fallback chain
            }
        }
        return new SunTimes(Const.DEFAULT_SUNRISE_MIN, Const.DEFAULT_SUNSET_MIN, "固定 06:00/18:00");
    }

    public static SunTimes refresh(Context c) {
        SunTimes st = compute(c);
        cache(c, st);
        return st;
    }

    public static void cache(Context c, SunTimes st) {
        c.getSharedPreferences(Const.CACHE_PREF, Context.MODE_PRIVATE).edit()
                .putString(Const.KEY_CACHE_DAY, today())
                .putInt(Const.KEY_SUNRISE_MIN, st.sunriseMin)
                .putInt(Const.KEY_SUNSET_MIN, st.sunsetMin)
                .putString(Const.KEY_LAST_SUN_SOURCE_NAME, st.sourceName)
                .apply();
    }

    public static void refreshAsync(final Context c) {
        final Context app = c.getApplicationContext();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    refresh(app);
                } catch (Throwable ignored) {
                }
                app.sendBroadcast(new Intent(Const.ACTION_SUN_UPDATED));
                Video24Controller.broadcastIfChanged(app);
                Video24Controller.scheduleNextAlarm(app);
            }
        }).start();
    }
}
