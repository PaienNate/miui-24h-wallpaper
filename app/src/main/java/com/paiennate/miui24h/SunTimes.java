package com.paiennate.miui24h;

public final class SunTimes {
    public final int sunriseMin;
    public final int sunsetMin;
    public final String sourceName;

    public SunTimes(int sunriseMin, int sunsetMin, String sourceName) {
        this.sunriseMin = sunriseMin;
        this.sunsetMin = sunsetMin;
        this.sourceName = sourceName;
    }
}
