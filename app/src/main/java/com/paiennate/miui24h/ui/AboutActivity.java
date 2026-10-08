package com.paiennate.miui24h.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.paiennate.miui24h.BuildConfig;

public class AboutActivity extends AppCompatActivity {

    private static final String GITHUB_ALYNX = "https://github.com/AlynxZhou/alynx-live-wallpaper";
    private static final String GITHUB_PROJECT = "https://github.com/PaienNate/miui-24h-wallpaper";
    private static final String FIRMWARE = "miui_CEPHEUS_V11.0.9.0.QFACNXM_aa7683f027_10.0.zip";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);

        MaterialToolbar toolbar = new MaterialToolbar(this);
        toolbar.setTitle("关于");
        container.addView(toolbar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(16);
        root.setPadding(pad, dp(8), pad, dp(16));
        scroll.addView(root);
        container.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        LinearLayout info = newCard(root);
        info.addView(title("MIUI 24H 壁纸"));
        info.addView(body("版本：V" + BuildConfig.VERSION_NAME + "（build " + BuildConfig.VERSION_CODE + "）"));
        info.addView(body("构建时间：" + BuildConfig.BUILD_TIME));

        LinearLayout dev = newCard(root);
        dev.addView(label("开发者"));
        dev.addView(body("PaienNate"));
        dev.addView(body("酷安：派恩纳特的猫"));
        dev.addView(body("B 站：Pinenutn"));

        LinearLayout video = newCard(root);
        video.addView(label("视频来源"));
        video.addView(body("本应用的 10 段动态壁纸视频来自 MIUI 固件：" + FIRMWARE));
        video.addView(body("视频版权归小米科技（MIUI）所有。"));

        LinearLayout credits = newCard(root);
        credits.addView(label("鸣谢"));
        credits.addView(link("AlynxZhou / alynx-live-wallpaper", GITHUB_ALYNX));
        credits.addView(body("本项目的动态壁纸渲染方案参考并改编自该项目（Apache-2.0）。"));
        credits.addView(body("代码移植 By DeepSeek V4"));
        credits.addView(body("图标首图生成 By 豆包网页版"));

        LinearLayout project = newCard(root);
        project.addView(label("项目"));
        project.addView(link(GITHUB_PROJECT, GITHUB_PROJECT));
        project.addView(body("开源协议：Apache-2.0"));

        LinearLayout battery = newCard(root);
        battery.addView(label("省电优化建议"));
        battery.addView(body("部分系统（如 MIUI / HyperOS）会清理后台应用，可能导致壁纸不能按时切换。"
                + "建议把本应用加入电池优化白名单（设为“无限制”/“不优化”），并在“最近任务”里给本应用上锁。"));
        MaterialButton btn = new MaterialButton(this);
        btn.setText("前往电池优化设置");
        btn.setOnClickListener(v -> openBatterySettings());
        LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        btnLp.topMargin = dp(8);
        battery.addView(btn, btnLp);

        setContentView(container);
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }

    private LinearLayout newCard(LinearLayout parent) {
        MaterialCardView card = new MaterialCardView(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(12);
        card.setLayoutParams(lp);
        card.setRadius(dp(12));
        card.setCardElevation(dp(1));

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        int cpad = dp(16);
        col.setPadding(cpad, cpad, cpad, cpad);
        card.addView(col, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        parent.addView(card);
        return col;
    }

    private TextView title(String s) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(20);
        t.setPadding(0, 0, 0, dp(8));
        return t;
    }

    private TextView label(String s) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setPadding(0, 0, 0, dp(4));
        return t;
    }

    private TextView body(String s) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setPadding(0, dp(2), 0, dp(2));
        return t;
    }

    private TextView link(String text, final String url) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextColor(0xFF0A6EBD);
        t.setPadding(0, dp(2), 0, dp(2));
        t.setOnClickListener(v -> {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
            } catch (Throwable ignored) {
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
