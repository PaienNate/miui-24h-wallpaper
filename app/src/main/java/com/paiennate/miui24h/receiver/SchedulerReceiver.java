package com.paiennate.miui24h.receiver;

import com.paiennate.miui24h.schedule.Video24Controller;
import com.paiennate.miui24h.sun.SunTimeManager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Handles boot / time-change / exact-alarm events (P4-B):
 * refresh sun times, notify the running wallpaper, and schedule the next alarm.
 */
public class SchedulerReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        final PendingResult pending = goAsync();
        final Context app = context.getApplicationContext();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    SunTimeManager.refresh(app);
                    Video24Controller.broadcastIfChanged(app);
                } catch (Throwable ignored) {
                } finally {
                    Video24Controller.scheduleNextAlarm(app);
                    pending.finish();
                }
            }
        }).start();
    }
}
