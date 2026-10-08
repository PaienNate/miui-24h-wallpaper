package com.paiennate.miui24h.common;

public final class Const {
    private Const() {}

    public static final String OPTIONS_PREF = "options";
    public static final String KEY_SUN_SOURCE = "sun_source";
    public static final String KEY_HIDE_FROM_RECENTS = "hide_from_recents";

    public static final String CACHE_PREF = "sun_cache";
    public static final String KEY_CACHE_DAY = "cache_day";
    public static final String KEY_SUNRISE_MIN = "sunrise_min";
    public static final String KEY_SUNSET_MIN = "sunset_min";
    public static final String KEY_LAST_SUN_SOURCE_NAME = "last_source_name";

    public static final String STATE_PREF = "state";
    public static final String KEY_CURRENT_VIDEO = "current_video";

    public static final int SRC_OFFLINE = 0;
    public static final int SRC_TENCENT = 1;
    public static final int SRC_OPENMETEO = 2;
    public static final int SRC_SUNRISESUNSET = 3;
    public static final int SRC_HARDCODED = 4;

    public static final int DEFAULT_SUNRISE_MIN = 6 * 60;
    public static final int DEFAULT_SUNSET_MIN = 18 * 60;

    public static final String ACTION_VIDEO_CHANGED = "com.paiennate.miui24h.VIDEO_CHANGED";
    public static final String ACTION_SUN_UPDATED = "com.paiennate.miui24h.SUN_UPDATED";
    public static final String ACTION_ALARM = "com.paiennate.miui24h.ALARM";
    public static final String EXTRA_VIDEO_ID = "video_id";
}
