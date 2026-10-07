package com.paiennate.miui24h;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;

import java.util.Calendar;
import java.util.List;

public final class Video24Controller {
    private Video24Controller() {}

    public static boolean isNightMode(Context c) {
        int mask = c.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return mask == Configuration.UI_MODE_NIGHT_YES;
    }

    public static int currentVideoId(Context c) {
        if (isNightMode(c)) return Video24Constant.NIGHT_VIDEO_ID;
        SunTimes st = SunTimeManager.getTodayCachedOrFallback(c);
        List<Video24Schedule.Entry> table = Video24Schedule.buildTable(st.sunriseMin, st.sunsetMin);
        Calendar cal = Calendar.getInstance();
        int now = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE);
        return Video24Schedule.selectVideoId(table, now);
    }

    public static void broadcastIfChanged(Context c) {
        int id = currentVideoId(c);
        SharedPreferences sp = c.getSharedPreferences(Const.STATE_PREF, Context.MODE_PRIVATE);
        int prev = sp.getInt(Const.KEY_CURRENT_VIDEO, -1);
        if (id != prev) {
            sp.edit().putInt(Const.KEY_CURRENT_VIDEO, id).apply();
            Intent i = new Intent(Const.ACTION_VIDEO_CHANGED);
            i.putExtra(Const.EXTRA_VIDEO_ID, id);
            c.sendBroadcast(i);
        }
    }

    /** P4-B: exact alarm at the next boundary (best-effort; falls back to inexact). */
    public static void scheduleNextAlarm(Context c) {
        SunTimes st = SunTimeManager.getTodayCachedOrFallback(c);
        List<Video24Schedule.Entry> table = Video24Schedule.buildTable(st.sunriseMin, st.sunsetMin);
        Calendar now = Calendar.getInstance();
        int nowMin = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);
        int nextMin = Video24Schedule.nextBoundaryMin(table, nowMin);
        if (nextMin < 0) return;

        Calendar at = Calendar.getInstance();
        at.set(Calendar.HOUR_OF_DAY, nextMin / 60);
        at.set(Calendar.MINUTE, nextMin % 60);
        at.set(Calendar.SECOND, 0);
        at.set(Calendar.MILLISECOND, 0);
        if (at.getTimeInMillis() <= System.currentTimeMillis()) {
            at.add(Calendar.DAY_OF_YEAR, 1);
        }

        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        Intent i = new Intent(c, SchedulerReceiver.class).setAction(Const.ACTION_ALARM);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pi = PendingIntent.getBroadcast(c, 1001, i, flags);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at.getTimeInMillis(), pi);
            } else {
                am.setExact(AlarmManager.RTC_WAKEUP, at.getTimeInMillis(), pi);
            }
        } catch (SecurityException se) {
            am.set(AlarmManager.RTC_WAKEUP, at.getTimeInMillis(), pi);
        }
    }
}
