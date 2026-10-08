package com.paiennate.miui24h.sun;

import com.paiennate.miui24h.common.Const;

import android.content.Context;

public class HardcodedSunTimeProvider implements SunTimeProvider {
    @Override
    public SunTimes getSunTimes(Context context) {
        return new SunTimes(Const.DEFAULT_SUNRISE_MIN, Const.DEFAULT_SUNSET_MIN, name());
    }

    @Override
    public String name() {
        return "固定 06:00/18:00";
    }
}
