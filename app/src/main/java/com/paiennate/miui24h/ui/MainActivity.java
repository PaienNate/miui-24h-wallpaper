package com.paiennate.miui24h.ui;

import com.paiennate.miui24h.LWApplication;
import com.paiennate.miui24h.common.Const;
import com.paiennate.miui24h.schedule.Video24Controller;
import com.paiennate.miui24h.sun.SunTimeManager;
import com.paiennate.miui24h.sun.SunTimes;
import com.paiennate.miui24h.wallpaper.GLWallpaperService;

import android.Manifest;
import android.app.ActivityManager;
import android.app.WallpaperManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputLayout;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private static final int REQ_LOCATION = 100;

    private final String[] labels = {
            "离线算法（需定位）",
            "腾讯天气（IP 定位）",
            "Open-Meteo（联网）",
            "sunrise-sunset.org（联网）",
            "固定 06:00 / 18:00"
    };
    private final int[] values = {
            Const.SRC_OFFLINE,
            Const.SRC_TENCENT,
            Const.SRC_OPENMETEO,
            Const.SRC_SUNRISESUNSET,
            Const.SRC_HARDCODED
    };

    private MaterialAutoCompleteTextView sourceView;
    private TextView status;
    private boolean initialized = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);

        MaterialToolbar toolbar = new MaterialToolbar(this);
        toolbar.setTitle("MIUI 24H 壁纸");
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

        // 日出日落来源
        LinearLayout sourceCard = newCard(root);
        sourceCard.addView(label("日出日落来源"));
        TextInputLayout til = new TextInputLayout(this);
        til.setHint("来源");
        til.setEndIconMode(TextInputLayout.END_ICON_DROPDOWN_MENU);
        sourceView = new MaterialAutoCompleteTextView(this);
        sourceView.setInputType(InputType.TYPE_NULL);
        sourceView.setKeyListener(null);
        sourceView.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, labels));
        sourceView.setText(labels[indexOf(SunTimeManager.getSunSource(this))], false);
        sourceView.setOnItemClickListener((parent, view, position, id) -> {
            SunTimeManager.setSunSource(MainActivity.this, values[position]);
            if (!initialized) return;
            maybeRequestLocation(values[position]);
            SunTimeManager.refreshAsync(MainActivity.this);
            status.postDelayed(MainActivity.this::updateStatus, 800);
        });
        til.addView(sourceView);
        sourceCard.addView(til);

        // 操作
        LinearLayout actionCard = newCard(root);
        MaterialButton apply = new MaterialButton(this);
        apply.setText("应用壁纸");
        apply.setOnClickListener(v -> applyWallpaper());
        actionCard.addView(apply, matchWrap());

        MaterialButton refresh = new MaterialButton(this);
        refresh.setText("立即刷新日出日落");
        refresh.setOnClickListener(v -> {
            SunTimeManager.refreshAsync(this);
            status.postDelayed(this::updateStatus, 800);
        });
        LinearLayout.LayoutParams refreshLp = matchWrap();
        refreshLp.topMargin = dp(8);
        actionCard.addView(refresh, refreshLp);

        // 设置
        LinearLayout settingsCard = newCard(root);
        MaterialSwitch hide = new MaterialSwitch(this);
        hide.setText("在最近任务中隐藏（防误杀）");
        hide.setChecked(isHideFromRecents());
        hide.setOnCheckedChangeListener((buttonView, isChecked) -> {
            setHideFromRecents(isChecked);
            applyExcludeFromRecents(isChecked);
        });
        settingsCard.addView(hide);

        MaterialButton about = new MaterialButton(this);
        about.setText("关于");
        about.setOnClickListener(v -> startActivity(new Intent(this, AboutActivity.class)));
        LinearLayout.LayoutParams aboutLp = matchWrap();
        aboutLp.topMargin = dp(8);
        settingsCard.addView(about, aboutLp);

        // 状态
        LinearLayout statusCard = newCard(root);
        status = new TextView(this);
        statusCard.addView(status);

        MaterialButton crash = new MaterialButton(this);
        crash.setText("查看 / 复制崩溃日志");
        crash.setOnClickListener(v -> showCrashLog());
        LinearLayout.LayoutParams crashLp = matchWrap();
        crashLp.topMargin = dp(8);
        statusCard.addView(crash, crashLp);

        setContentView(container);
        initialized = true;
        applyExcludeFromRecents(isHideFromRecents());
    }

    @Override
    protected void onResume() {
        super.onResume();
        SunTimeManager.refreshAsync(this);
        maybeRequestLocation(SunTimeManager.getSunSource(this));
        updateStatus();
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

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private TextView label(String s) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setPadding(0, 0, 0, dp(8));
        return t;
    }

    private int indexOf(int value) {
        for (int i = 0; i < values.length; i++) {
            if (values[i] == value) return i;
        }
        return 0;
    }

    private boolean isHideFromRecents() {
        return getSharedPreferences(Const.OPTIONS_PREF, MODE_PRIVATE)
                .getBoolean(Const.KEY_HIDE_FROM_RECENTS, false);
    }

    private void setHideFromRecents(boolean value) {
        getSharedPreferences(Const.OPTIONS_PREF, MODE_PRIVATE)
                .edit().putBoolean(Const.KEY_HIDE_FROM_RECENTS, value).apply();
    }

    /**
     * Hide/show this task in the recent-tasks list.
     * ActivityManager.AppTask.setExcludeFromRecents() modifies the root intent's
     * FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS flag.
     */
    private void applyExcludeFromRecents(boolean exclude) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return;
        try {
            ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            if (am == null) return;
            List<ActivityManager.AppTask> tasks = am.getAppTasks();
            if (tasks != null && !tasks.isEmpty()) {
                tasks.get(0).setExcludeFromRecents(exclude);
            }
        } catch (Throwable ignored) {
        }
    }

    private void maybeRequestLocation(int src) {
        boolean needsLocation = src == Const.SRC_OFFLINE
                || src == Const.SRC_OPENMETEO
                || src == Const.SRC_SUNRISESUNSET;
        if (!needsLocation) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[] { Manifest.permission.ACCESS_COARSE_LOCATION }, REQ_LOCATION);
        }
    }

    private void updateStatus() {
        SunTimes st = SunTimeManager.getTodayCachedOrFallback(this);
        int videoId = Video24Controller.currentVideoId(this);
        boolean night = Video24Controller.isNightMode(this);
        String text = String.format(Locale.US,
                "来源：%s\n日出：%02d:%02d　日落：%02d:%02d\n当前视频：第 %d 段%s",
                st.sourceName,
                st.sunriseMin / 60, st.sunriseMin % 60,
                st.sunsetMin / 60, st.sunsetMin % 60,
                videoId, night ? "（深色模式）" : "");
        status.setText(text);
    }

    private void showCrashLog() {
        File f = new File(getFilesDir(), LWApplication.CRASH_FILE);
        String text;
        if (f.exists()) {
            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new FileReader(f))) {
                String line;
                while ((line = br.readLine()) != null) sb.append(line).append('\n');
            } catch (Exception e) {
                sb.append("read failed: ").append(e);
            }
            text = sb.toString();
        } else {
            text = "暂无崩溃日志（文件不存在）";
        }
        try {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null) cm.setPrimaryClip(ClipData.newPlainText("crash", text));
        } catch (Throwable ignored) {
        }
        final TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextIsSelectable(true);
        tv.setPadding(dp(20), dp(8), dp(20), dp(8));
        new MaterialAlertDialogBuilder(this)
                .setTitle("崩溃日志（已复制到剪贴板）")
                .setView(tv)
                .setPositiveButton("关闭", null)
                .show();
    }

    private void applyWallpaper() {
        Intent intent = new Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER);
        intent.putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                new ComponentName(this, GLWallpaperService.class));
        try {
            startActivity(intent);
            Toast.makeText(this, "已应用，请在预览页确认", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "无法打开壁纸选择器：" + e, Toast.LENGTH_LONG).show();
        }
    }
}
