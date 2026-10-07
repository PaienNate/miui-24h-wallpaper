package com.paiennate.miui24h;

import android.Manifest;
import android.app.Activity;
import android.app.WallpaperManager;
import android.content.ClipData;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int REQ_LOCATION = 100;
    private static final int REQ_ZIP = 101;
    private static final int REQ_VIDEOS = 102;

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

        TextView videoHint = new TextView(this);
        videoHint.setText("① 先导入视频（不随 App 分发，需自行提供 MIUI 24H 的 10 段视频）");
        root.addView(videoHint);

        Button importZip = new Button(this);
        importZip.setText("导入 ZIP 包（含 video/01..10.mp4）");
        importZip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickZip();
            }
        });
        root.addView(importZip);

        Button importVideos = new Button(this);
        importVideos.setText("选择视频文件（命名为 01.mp4 … 10.mp4）");
        importVideos.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickVideos();
            }
        });
        root.addView(importVideos);

        TextView sunHint = new TextView(this);
        sunHint.setText("② 选择日出日落来源");
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
        apply.setText("③ 应用壁纸");
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

        status = new TextView(this);
        root.addView(status);

        setContentView(root);
        initialized = true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        SunTimeManager.refreshAsync(this);
        maybeRequestLocation(SunTimeManager.getSunSource(this));
        updateStatus();
    }

    private void pickZip() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[] { "application/zip", "application/octet-stream", "*/*" });
        startActivityForResult(intent, REQ_ZIP);
    }

    private void pickVideos() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("video/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(intent, REQ_VIDEOS);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null) return;

        if (requestCode == REQ_ZIP) {
            final Uri uri = data.getData();
            if (uri == null) return;
            final Context ctx = this;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    int count;
                    try {
                        count = ImportManager.importZip(ctx, uri);
                    } catch (Throwable t) {
                        count = -1;
                    }
                    final int result = count;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(ctx, result >= 0 ? ("已导入 " + result + " 个视频") : "导入失败", Toast.LENGTH_LONG).show();
                            updateStatus();
                        }
                    });
                }
            }).start();
        } else if (requestCode == REQ_VIDEOS) {
            final List<Uri> uris = new ArrayList<>();
            if (data.getClipData() != null) {
                ClipData clip = data.getClipData();
                for (int i = 0; i < clip.getItemCount(); i++) {
                    uris.add(clip.getItemAt(i).getUri());
                }
            } else if (data.getData() != null) {
                uris.add(data.getData());
            }
            if (uris.isEmpty()) return;
            final Context ctx = this;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    int count = 0;
                    for (Uri u : uris) {
                        try {
                            count += ImportManager.importSingleVideo(ctx, u);
                        } catch (Throwable ignored) {
                        }
                    }
                    final int result = count;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(ctx, "已导入 " + result + "/" + uris.size() + " 个视频", Toast.LENGTH_LONG).show();
                            updateStatus();
                        }
                    });
                }
            }).start();
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
        int ready = Video24Constant.readyVideoCount(this);
        SunTimes st = SunTimeManager.getTodayCachedOrFallback(this);
        int videoId = Video24Controller.currentVideoId(this);
        boolean night = Video24Controller.isNightMode(this);
        String text = String.format(Locale.US,
                "视频：%d/%d %s\n来源：%s\n日出：%02d:%02d　日落：%02d:%02d\n当前视频：第 %d 段%s",
                ready, Video24Constant.COUNT, ready == Video24Constant.COUNT ? "（就绪）" : "（未就绪，请先导入）",
                st.sourceName,
                st.sunriseMin / 60, st.sunriseMin % 60,
                st.sunsetMin / 60, st.sunsetMin % 60,
                videoId, night ? "（深色模式）" : "");
        status.setText(text);
    }

    private void applyWallpaper() {
        if (!Video24Constant.hasAllVideos(this)) {
            Toast.makeText(this, "请先导入 10 段视频", Toast.LENGTH_LONG).show();
            return;
        }
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
