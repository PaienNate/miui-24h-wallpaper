package com.paiennate.miui24h.ui;

import com.paiennate.miui24h.LWApplication;
import com.paiennate.miui24h.common.Const;
import com.paiennate.miui24h.schedule.Video24Controller;
import com.paiennate.miui24h.sun.SunTimeManager;
import com.paiennate.miui24h.sun.SunTimes;
import com.paiennate.miui24h.wallpaper.GLWallpaperService;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.WallpaperManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
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

    private Spinner spinner;
    private TextView status;
    private boolean initialized = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        float density = getResources().getDisplayMetrics().density;
        int pad = (int) (16 * density);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad, pad, pad);

        TextView title = new TextView(this);
        title.setText("MIUI 24H 壁纸");
        title.setTextSize(18);
        root.addView(title);

        TextView sunHint = new TextView(this);
        sunHint.setText("① 选择日出日落来源");
        root.addView(sunHint);

        spinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_dropdown_item, labels);
        spinner.setAdapter(adapter);
        spinner.setSelection(indexOf(SunTimeManager.getSunSource(this)));
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                SunTimeManager.setSunSource(MainActivity.this, values[position]);
                if (!initialized) return;
                maybeRequestLocation(values[position]);
                SunTimeManager.refreshAsync(MainActivity.this);
                status.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        updateStatus();
                    }
                }, 800);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        root.addView(spinner);

        Button apply = new Button(this);
        apply.setText("② 应用壁纸");
        apply.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applyWallpaper();
            }
        });
        root.addView(apply);

        Button refresh = new Button(this);
        refresh.setText("立即刷新日出日落");
        refresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SunTimeManager.refreshAsync(MainActivity.this);
                status.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        updateStatus();
                    }
                }, 800);
            }
        });
        root.addView(refresh);

        Button crashBtn = new Button(this);
        crashBtn.setText("查看 / 复制崩溃日志");
        crashBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showCrashLog();
            }
        });
        root.addView(crashBtn);

        CheckBox hideRecents = new CheckBox(this);
        hideRecents.setText("在最近任务中隐藏（防误杀）");
        hideRecents.setChecked(isHideFromRecents());
        hideRecents.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                setHideFromRecents(isChecked);
                applyExcludeFromRecents(isChecked);
            }
        });
        root.addView(hideRecents);

        Button about = new Button(this);
        about.setText("关于");
        about.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, AboutActivity.class));
            }
        });
        root.addView(about);

        status = new TextView(this);
        root.addView(status);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(root);
        setContentView(scroll);
        initialized = true;

        // Apply on launch (mirrors SmsForwarder): hide this task from recents if enabled.
        applyExcludeFromRecents(isHideFromRecents());
    }

    @Override
    protected void onResume() {
        super.onResume();
        SunTimeManager.refreshAsync(this);
        maybeRequestLocation(SunTimeManager.getSunSource(this));
        updateStatus();
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
     * Same approach as SmsForwarder / sealdice: ActivityManager.AppTask.setExcludeFromRecents()
     * modifies the Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS flag of the task's root intent.
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

    private int indexOf(int value) {
        for (int i = 0; i < values.length; i++) {
            if (values[i] == value) return i;
        }
        return 0;
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
        float d = getResources().getDisplayMetrics().density;
        tv.setPadding((int) (12 * d), (int) (12 * d), (int) (12 * d), (int) (12 * d));
        new AlertDialog.Builder(this)
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
