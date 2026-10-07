# MIUI 24H Wallpaper

一个 MIUI 11 「24 小时动态视频壁纸」的**替代品**：把 10 段视频按日出日落自动切换，跑在**自己的、无系统依赖**的 Live Wallpaper 引擎上。

基于 [AlynxZhou/alynx-live-wallpaper](https://github.com/AlynxZhou/alynx-live-wallpaper)（Apache-2.0）魔改。

## 重要：视频需自行导入

本仓库**不包含** MIUI 的视频（版权原因）。首次使用请在 App 内导入，二选一：

1. **导入 ZIP 包**：ZIP 内含 `video/01.mp4 … 10.mp4`（可选 `thumnail/01.jpg … 10.jpg`）。
2. **选择视频文件**：多选 10 个文件，命名为 `01.mp4 … 10.mp4`。

导入后会复制到 App 私有目录，壁纸服务从那里读取。

## 它做什么

- 按 MIUI `Video24LocalService` 的时间表自动切换视频：
  - 日出前 20 分→视频1，日出→2，日出后20分→3，白天均分三段→4/5/6，日落前20分→7，日落→8，日落后20分→9，夜间中点→10。
  - **深色模式 → 强制视频 10**（与 MIUI 原逻辑一致）。
- 锁屏：**不碰 `FLAG_LOCK`**，靠系统默认（锁屏回落到系统壁纸即可见），密码照常可用。
- 静音：ExoPlayer 关闭音频轨。

## 日出日落来源（设置页下拉，带降级链）

| 来源 | 需要 | 说明 |
|---|---|---|
| 离线算法（默认） | 定位 | 本地太阳位置算法，离线、无 key |
| 腾讯天气 | 网络 | `r.inews.qq.com/api/ip2city` + `wis.qq.com/...weather_type=rise`（非官方接口）|
| Open-Meteo | 网络 | 免费无 key |
| sunrise-sunset.org | 网络 | 免费无 key（返回 UTC，已转本地）|
| 固定 06:00 / 18:00 | — | 兜底 |

降级链：所选来源失败 → 离线算法 → 固定 06:00/18:00。每天缓存一次。

## 构建

GitHub Actions 自动编译（`.github/workflows/build.yml`），产物为 `miui-24h-wallpaper-debug` artifact（debug 签名，可直接安装）。本地构建依赖走阿里云镜像（见 `settings.gradle`）。

## 版权

代码 Apache-2.0。如自行导入 MIUI 视频，仅供个人使用，请勿分发。
