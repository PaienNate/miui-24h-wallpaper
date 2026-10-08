# MIUI 24H Wallpaper

一个「24 小时动态视频壁纸」应用：把 10 段视频**按当天日出日落自动切换**，运行在一套独立的 Live Wallpaper 引擎上（无任何系统/厂商依赖）。

## 视频

10 段视频**内置**在 `app/src/main/assets/miui-video24/video/01..10.mp4`，用 x264 重编码（约 20 MB），**无需用户导入**，装上即用。

## 行为说明

### 如何根据日出日落计算播放哪一段

1. 取当日 **日出**、**日落** 时刻（本地时间，单位：分钟）。
2. 以它们为基准，把一天切成 **10 个时间点**，每个时间点对应一段视频：

   | 时间点 | 视频 |
   |---|---|
   | 日出 − 20 分 | 1 |
   | 日出 | 2 |
   | 日出 + 20 分 | 3 |
   | 白天均分三段 | 4 / 5 / 6 |
   | 日落 − 20 分 | 7 |
   | 日落 | 8 |
   | 日落 + 20 分 | 9 |
   | 夜间中点 | 10 |

3. **当前时刻**落在哪个区间，就播放对应编号的视频。
4. **深色模式**下直接播放第 10 段。
5. 到达下一个时间点时，用精确闹钟触发切换；切换只在**当前视频播放结束/出错**后发生，不会硬切。
6. 播放策略：视频**播完定格**（不循环）；**灭屏回绕到开头**，**亮屏从头重播**。

### 日出日落来源（应用内可选，带降级链）

| 来源 | 需要 | 说明 |
|---|---|---|
| 离线算法（默认） | 定位 | 本地太阳位置算法，离线、无 key |
| 腾讯天气 | 网络 | IP 定位取城市 + 天气接口取日出日落 |
| Open-Meteo | 网络 | 免费无 key |
| sunrise-sunset.org | 网络 | 免费无 key（返回 UTC，已转本地） |
| 固定 06:00 / 18:00 | — | 兜底 |

降级链：所选来源失败 → 离线算法 → 固定 06:00/18:00。每天缓存一次。

### 其它

- **锁屏**：不单独设置锁屏壁纸，锁屏是否显示由系统决定（多数系统会回落显示系统壁纸）。
- **静音**：播放时关闭音频轨。
- **最近任务隐藏**：可在设置中开启，开启后本应用不出现在最近任务列表（`ActivityManager.AppTask.setExcludeFromRecents`）。
- **崩溃日志**：未处理异常会写入 `filesDir/last_crash.txt`，可在应用内查看/复制。

## 代码结构

```
app/src/main/java/com/paiennate/miui24h/
├── LWApplication.java              应用入口；捕获未处理异常写入 last_crash.txt
├── ui/
│   └── MainActivity.java           设置界面（选择来源、应用壁纸、隐藏最近任务、崩溃日志）
├── common/
│   ├── Const.java                  全局常量（prefs key、来源编号、广播动作）
│   └── Utils.java                  工具：着色器编译、HTTP GET、最后已知位置、日志
├── data/
│   └── Video24Constant.java        内置视频资源路径（assets/miui-video24/video/NN.mp4）
├── sun/
│   ├── SunTimes.java               日出/日落（分钟）+ 来源名
│   ├── SunTimeProvider.java        日出日落来源接口
│   ├── SunTimeManager.java         来源选择、降级链、当天缓存、异步刷新
│   ├── OfflineSolarSunTimeProvider.java     离线太阳位置算法
│   ├── OpenMeteoSunTimeProvider.java        联网 Open-Meteo
│   ├── SunriseSunsetOrgSunTimeProvider.java 联网 sunrise-sunset.org
│   ├── TencentSunTimeProvider.java          IP 定位 + 腾讯天气
│   └── HardcodedSunTimeProvider.java        固定 06:00/18:00
├── schedule/
│   ├── Video24Schedule.java        由日出日落构建「时间 → 视频」表并选出当前视频
│   └── Video24Controller.java      当前视频判定、变化广播、下一次切换的精确闹钟
├── wallpaper/
│   ├── GLWallpaperService.java     动态壁纸服务（ExoPlayer 解码 + 亮灭屏处理）
│   ├── GLWallpaperRenderer.java    渲染器抽象
│   └── GLES20WallpaperRenderer.java GLES2 将视频帧贴到壁纸 Surface（居中裁剪）
└── receiver/
    └── SchedulerReceiver.java      开机 / 改时间 / 定时 触发刷新与切换
```

**模块依赖方向**（自上而下依赖，无环）：`ui` → `schedule`/`sun`/`data`/`wallpaper` → `common`。

## 构建

GitHub Actions 自动编译（`.github/workflows/build.yml`），产物为 `miui-24h-wallpaper-debug` artifact（固定 keystore 签名，可直接覆盖安装）。

- 工具链：AGP 8.5.2 / Java 17 / compileSdk 34
- 依赖仓库优先走阿里云镜像（见 `settings.gradle`）

## 版权

代码 Apache-2.0。项目的 Live Wallpaper 渲染方案参考并改编自 [AlynxZhou/alynx-live-wallpaper](https://github.com/AlynxZhou/alynx-live-wallpaper)（Apache-2.0）。

内置视频素材版权归其各自所有者，仅供个人使用，请勿分发。
