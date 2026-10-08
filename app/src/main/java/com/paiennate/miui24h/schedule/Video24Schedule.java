package com.paiennate.miui24h.schedule;

import com.paiennate.miui24h.data.Video24Constant;

import java.util.ArrayList;
import java.util.List;

/**
 * Faithful port of MIUI Video24LocalService's sunrise/sunset -> video-id scheduling.
 * See MIUI SystemUI: com.android.systemui.wallpaper.Video24LocalService#calculateSunSet / #scheduleWallpaper.
 */
public final class Video24Schedule {
    private Video24Schedule() {}

    public static final class Entry {
        public final int time;    // minutes since midnight
        public final int videoId; // 1..10
        Entry(int time, int videoId) {
            this.time = time;
            this.videoId = videoId;
        }
    }

    /** Ported from calculateSunSet(): build the 10-point daily table. */
    public static List<Entry> buildTable(int sunriseMin, int sunsetMin) {
        List<Entry> list = new ArrayList<>(12);
        list.add(new Entry(sunriseMin - 20, 1));
        list.add(new Entry(sunriseMin, 2));
        list.add(new Entry(sunriseMin + 20, 3));
        int evening = sunsetMin - 20;
        int seg = ((evening - sunriseMin) - 20) / 4;
        list.add(new Entry(sunriseMin + 20 + seg, 4));
        list.add(new Entry(sunriseMin + 20 + seg * 2, 5));
        list.add(new Entry(sunriseMin + 20 + seg * 3, 6));
        list.add(new Entry(sunsetMin - 20, 7));
        list.add(new Entry(sunsetMin, 8));
        list.add(new Entry(sunsetMin + 20, 9));
        int nightMid = sunsetMin + 20 + ((((1440 - sunsetMin) - 20) + sunriseMin - 20) / 2);
        if (nightMid < 1440) {
            list.add(new Entry(nightMid, 10));
        } else {
            list.add(0, new Entry(nightMid - 1440, 10));
        }
        return list;
    }

    /** Ported from scheduleWallpaper() selection loop. Returns index into table, or -1. */
    public static int selectIndex(List<Entry> table, int nowMin) {
        if (table == null || table.isEmpty()) return -1;
        int size = 0;
        int video = 0;
        for (int k = 0; k < table.size(); k++) {
            int t = table.get(k).time;
            if (nowMin == t) {
                size = k;
                video = table.get(k).videoId;
                break;
            } else if (nowMin < t) {
                if (k == 0) {
                    size = table.size() - 1;
                    video = table.get(size).videoId;
                } else {
                    size = k - 1;
                    video = table.get(size).videoId;
                }
                break;
            }
        }
        if (video == 0) {
            size = table.size() - 1;
            video = table.get(size).videoId;
        }
        return size;
    }

    public static int selectVideoId(List<Entry> table, int nowMin) {
        int idx = selectIndex(table, nowMin);
        if (idx < 0) return Video24Constant.NIGHT_VIDEO_ID;
        return table.get(idx).videoId;
    }

    /** Next boundary time-of-day (minutes) strictly after nowMin, wrapping to tomorrow. -1 if empty. */
    public static int nextBoundaryMin(List<Entry> table, int nowMin) {
        if (table == null || table.isEmpty()) return -1;
        int best = -1;
        for (Entry e : table) {
            if (e.time > nowMin && (best < 0 || e.time < best)) best = e.time;
        }
        if (best >= 0) return best;
        int earliest = -1;
        for (Entry e : table) {
            if (earliest < 0 || e.time < earliest) earliest = e.time;
        }
        return earliest;
    }
}
