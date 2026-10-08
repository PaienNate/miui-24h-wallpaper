package com.paiennate.miui24h.sun;

import com.paiennate.miui24h.common.Utils;

import android.content.Context;
import android.location.Location;

import org.json.JSONObject;

/** Free, no-key API: https://open-meteo.com — returns sunrise/sunset in local time. Needs location. */
public class OpenMeteoSunTimeProvider implements SunTimeProvider {

    @Override
    public SunTimes getSunTimes(Context context) throws Exception {
        Location loc = Utils.getLastKnownLocation(context);
        if (loc == null) throw new Exception("no last known location");
        String url = "https://api.open-meteo.com/v1/forecast?latitude=" + loc.getLatitude()
                + "&longitude=" + loc.getLongitude()
                + "&daily=sunrise,sunset&timezone=auto&forecast_days=1";
        String json = Utils.httpGet(url);
        JSONObject daily = new JSONObject(json).getJSONObject("daily");
        String sunrise = daily.getJSONArray("sunrise").getString(0); // 2026-10-07T06:16
        String sunset = daily.getJSONArray("sunset").getString(0);
        return new SunTimes(parseIsoLocal(sunrise), parseIsoLocal(sunset), name());
    }

    @Override
    public String name() {
        return "Open-Meteo";
    }

    private static int parseIsoLocal(String iso) {
        int t = iso.indexOf('T');
        String hm = iso.substring(t + 1);
        String[] p = hm.split(":");
        return Integer.parseInt(p[0]) * 60 + Integer.parseInt(p[1]);
    }
}
