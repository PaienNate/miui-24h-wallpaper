package com.paiennate.miui24h.sun;

import com.paiennate.miui24h.common.Utils;

import android.content.Context;

import org.json.JSONObject;

import java.net.URLEncoder;
import java.util.Calendar;
import java.util.Iterator;
import java.util.Locale;

/**
 * Uses the (undocumented, unofficial) Tencent weather endpoints:
 *   1. https://r.inews.qq.com/api/ip2city?type=1   -> province / city from IP
 *   2. https://wis.qq.com/weather/common?...weather_type=rise -> sunrise/sunset
 * No API key. Fragile by nature — always guarded by the fallback chain.
 */
public class TencentSunTimeProvider implements SunTimeProvider {

    @Override
    public SunTimes getSunTimes(Context context) throws Exception {
        String ipJson = Utils.httpGet("https://r.inews.qq.com/api/ip2city?type=1");
        JSONObject ip = new JSONObject(ipJson);
        String province = ip.optString("province", "");
        String city = ip.optString("city", "");
        if (city.isEmpty()) city = province;
        if (province.isEmpty()) throw new Exception("ip2city returned no province");

        String url = "https://wis.qq.com/weather/common?source=pc&weather_type=rise"
                + "&province=" + URLEncoder.encode(province, "UTF-8")
                + "&city=" + URLEncoder.encode(city, "UTF-8");
        String wJson = Utils.httpGet(url);
        JSONObject rise = new JSONObject(wJson).getJSONObject("data").getJSONObject("rise");
        JSONObject today = pickToday(rise);
        if (today == null) throw new Exception("no rise entry");
        int sunrise = parseHm(today.getString("sunrise"));
        int sunset = parseHm(today.getString("sunset"));
        return new SunTimes(sunrise, sunset, name());
    }

    @Override
    public String name() {
        return "腾讯天气";
    }

    private static JSONObject pickToday(JSONObject rise) {
        Calendar c = Calendar.getInstance();
        String today = String.format(Locale.US, "%04d%02d%02d",
                c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH));
        Iterator<String> it = rise.keys();
        while (it.hasNext()) {
            String key = it.next();
            JSONObject o = rise.optJSONObject(key);
            if (o != null && today.equals(o.optString("time", ""))) return o;
        }
        return rise.optJSONObject("0");
    }

    private static int parseHm(String hm) {
        String[] p = hm.split(":");
        return Integer.parseInt(p[0]) * 60 + Integer.parseInt(p[1]);
    }
}
