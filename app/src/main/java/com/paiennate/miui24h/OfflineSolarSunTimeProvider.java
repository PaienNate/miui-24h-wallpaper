package com.paiennate.miui24h;

import android.content.Context;
import android.location.Location;

import java.util.Calendar;
import java.util.TimeZone;

/**
 * Computes sunrise/sunset offline from a coarse location using the standard
 * sunrise equation (suncalc / "Sunrise equation"). No network, no key.
 */
public class OfflineSolarSunTimeProvider implements SunTimeProvider {

    @Override
    public SunTimes getSunTimes(Context context) throws Exception {
        Location loc = Utils.getLastKnownLocation(context);
        if (loc == null) throw new Exception("no last known location");
        Calendar now = Calendar.getInstance();
        double[] julian = sunJulian(loc.getLatitude(), loc.getLongitude(),
                now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1, now.get(Calendar.DAY_OF_MONTH));
        if (julian == null) throw new Exception("polar day/night");
        int tzOffsetSec = TimeZone.getDefault().getOffset(System.currentTimeMillis()) / 1000;
        int sunrise = julianToLocalMinutes(julian[0], tzOffsetSec);
        int sunset = julianToLocalMinutes(julian[1], tzOffsetSec);
        return new SunTimes(sunrise, sunset, name());
    }

    @Override
    public String name() {
        return "离线算法";
    }

    private static int julianToLocalMinutes(double julian, int tzOffsetSec) {
        long unix = (long) ((julian - 2440587.5) * 86400.0);
        long local = unix + tzOffsetSec;
        long mod = ((local % 86400) + 86400) % 86400;
        return (int) (mod / 60);
    }

    /** @return {julianRise, julianSet} UTC, or null for polar conditions. */
    static double[] sunJulian(double lat, double lng, int year, int month, int day) {
        double a = Math.floor((14.0 - month) / 12.0);
        double y = year + 4800 - a;
        double m = month + 12 * a - 3;
        double jdn = day + Math.floor((153 * m + 2) / 5.0) + 365 * y
                + Math.floor(y / 4.0) - Math.floor(y / 100.0) + Math.floor(y / 400.0) - 32045;
        double n = jdn - 2451545.0 + 0.0008;
        double jStar = n - lng / 360.0;
        double M = (357.5291 + 0.98560028 * jStar) % 360.0;
        double C = 1.9148 * sinDeg(M) + 0.02 * sinDeg(2 * M) + 0.0003 * sinDeg(3 * M);
        double lambda = (M + C + 180.0 + 102.9372) % 360.0;
        double jTransit = 2451545.0 + jStar + 0.0053 * sinDeg(M) - 0.0069 * sinDeg(2 * lambda);
        double sinDec = 0.39782 * sinDeg(lambda);
        double cosDec = Math.cos(Math.asin(sinDec));
        double cosH = (cosDeg(90.833) - sinDec * sinDeg(lat)) / (cosDec * cosDeg(lat));
        if (cosH > 1.0 || cosH < -1.0) return null;
        double hRise = 360.0 - Math.toDegrees(Math.acos(cosH));
        double hSet = Math.toDegrees(Math.acos(cosH));
        double jRise = jTransit - hRise / 360.0;
        double jSet = jTransit + hSet / 360.0;
        return new double[] { jRise, jSet };
    }

    private static double sinDeg(double d) {
        return Math.sin(Math.toRadians(d));
    }

    private static double cosDeg(double d) {
        return Math.cos(Math.toRadians(d));
    }
}
