package com.paiennate.miui24h.sun;

import com.paiennate.miui24h.common.Utils;

import android.content.Context;
import android.location.Location;

import org.json.JSONObject;

import java.util.TimeZone;

/** Free, no-key API: https://sunrise-sunset.org — returns UTC, converted to local. Needs location. */
public class SunriseSunsetOrgSunTimeProvider implements SunTimeProvider {

    @Override
    public SunTimes getSunTimes(Context context) throws Exception {
        Location loc = Utils.getLastKnownLocation(context);
        if (loc == null) throw new Exception("no last known location");
        String url = "https://api.sunrise-sunset.org/json?lat=" + loc.getLatitude()
                + "&lng=" + loc.getLongitude() + "&formatted=0";
        String json = Utils.httpGet(url);
        JSONObject results = new JSONObject(json).getJSONObject("results");
        String sunrise = results.getString("sunrise"); // 2026-10-07T22:14:58+00:00 (UTC)
        String sunset = results.getString("sunset");
        int tzOffsetSec = TimeZone.getDefault().getOffset(System.currentTimeMillis()) / 1000;
        return new SunTimes(
                parseUtcToLocalMinutes(sunrise, tzOffsetSec),
                parseUtcToLocalMinutes(sunset, tzOffsetSec),
                name());
    }

    @Override
    public String name() {
        return "sunrise-sunset.org";
    }

    private static int parseUtcToLocalMinutes(String iso, int tzOffsetSec) {
        int t = iso.indexOf('T');
        String hms = iso.substring(t + 1, t + 9); // HH:mm:ss
        String[] p = hms.split(":");
        int seconds = Integer.parseInt(p[0]) * 3600 + Integer.parseInt(p[1]) * 60 + Integer.parseInt(p[2]);
        long local = seconds + tzOffsetSec;
        long mod = ((local % 86400) + 86400) % 86400;
        return (int) (mod / 60);
    }
}
