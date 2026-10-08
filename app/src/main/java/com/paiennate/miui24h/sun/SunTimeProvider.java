package com.paiennate.miui24h.sun;

import android.content.Context;

public interface SunTimeProvider {
    SunTimes getSunTimes(Context context) throws Exception;
    String name();
}
