package com.paiennate.miui24h.ui;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.paiennate.miui24h.BuildConfig;

public class AboutActivity extends Activity {

    private static final String GITHUB_ALYNX = "https://github.com/AlynxZhou/alynx-live-wallpaper";
    private static final String GITHUB_PROJECT = "https://github.com/PaienNate/miui-24h-wallpaper";
    private static final String FIRMWARE = "miui_CEPHEUS_V11.0.9.0.QFACNXM_aa7683f027_10.0.zip";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        float d = getResources().getDisplayMetrics().density;
        int pad = (int) (16 * d);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad, pad, pad);

        root.addView(title("MIUI 24H 壁纸"));
        root.addView(body("版本：V" + BuildConfig.VERSION_NAME + "（build " + BuildConfig.VERSION_CODE + "）"));
        root.addView(body("构建时间：" + BuildConfig.BUILD_TIME));

        root.addView(section("开发者"));
        root.addView(body("PaienNate"));
        root.addView(body("酷安：派恩纳特的猫"));
        root.addView(body("B 站：Pinenutn"));

        root.addView(section("视频来源"));
        root.addView(body("本应用的 10 段动态壁纸视频来自 MIUI 固件：" + FIRMWARE));
        root.addView(body("视频版权归小米科技（MIUI）所有。"));

        root.addView(section("鸣谢"));
        root.addView(link("AlynxZhou / alynx-live-wallpaper", GITHUB_ALYNX));
        root.addView(body("本项目的动态壁纸渲染方案参考并改编自该项目（Apache-2.0）。"));

        root.addView(section("项目"));
        root.addView(link(GITHUB_PROJECT, GITHUB_PROJECT));
        root.addView(body("开源协议：Apache-2.0"));

        root.addView(section("省电优化建议"));
        root.addView(body("部分系统（如 MIUI / HyperOS）会清理后台应用，可能导致壁纸不能按时切换。"
                + "建议把本应用加入电池优化白名单（设为“无限制”/“不优化”），并在“最近任务”里给本应用上锁。"));
        Button battery = new Button(this);
        battery.setText("前往电池优化设置");
        battery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openBatterySettings();
            }
        });
        root.addView(battery);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(root);
        setContentView(scroll);
    }

    private TextView title(String s) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(20);
        t.setPadding(0, 0, 0, (int) (8 * getResources().getDisplayMetrics().density));
        return t;
    }

    private TextView section(String s) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(15);
        int top = (int) (16 * getResources().getDisplayMetrics().density);
        t.setPadding(0, top, 0, (int) (4 * getResources().getDisplayMetrics().density));
        return t;
    }

    private TextView body(String s) {
        TextView t = new TextView(this);
        t.setText(s);
        return t;
    }

    private TextView link(String label, final String url) {
        TextView t = new TextView(this);
        t.setText(label);
        t.setTextColor(Color.parseColor("#0A6EBD"));
        t.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                } catch (Throwable ignored) {
                }
            }
        });
        return t;
    }

    private void openBatterySettings() {
        try {
            Intent i = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            i.setData(Uri.parse("package:" + getPackageName()));
            startActivity(i);
            return;
        } catch (Throwable ignored) {
        }
        try {
            startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
            return;
        } catch (Throwable ignored) {
        }
        try {
            startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:" + getPackageName())));
        } catch (Throwable ignored) {
        }
    }
}
