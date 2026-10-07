package com.paiennate.miui24h;

import android.content.Context;

public interface SunTimeProvider {
    SunTimes getSunTimes(Context context) throws Exception;
    String name();
}
