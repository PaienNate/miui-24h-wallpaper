package com.paiennate.miui24h;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ConfigurationInfo;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.service.wallpaper.WallpaperService;
import android.view.SurfaceHolder;

import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector;

import java.io.File;

public class GLWallpaperService extends WallpaperService {

    class GLWallpaperEngine extends Engine {
        private final Context context;
        private GLWallpaperSurfaceView glSurfaceView;
        private GLWallpaperRenderer renderer;
        private ExoPlayer player;
        private int currentVideoId = -1;
        private boolean visible = false;

        private final BroadcastReceiver changeReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context c, Intent intent) {
                if (!visible || renderer == null) return;
                int id = Video24Controller.currentVideoId(context);
                if (id != currentVideoId) {
                    playVideo(id);
                }
            }
        };

        /** The "genius hack": pretend the GLSurfaceView owns the wallpaper surface. */
        class GLWallpaperSurfaceView extends GLSurfaceView {
            GLWallpaperSurfaceView(Context context) {
                super(context);
            }

            @Override
            public SurfaceHolder getHolder() {
                return getSurfaceHolder();
            }

            void doDestroy() {
                super.onDetachedFromWindow();
            }
        }

        GLWallpaperEngine(Context context) {
            this.context = context;
            setTouchEventsEnabled(false);
        }

        @Override
        public void onCreate(SurfaceHolder surfaceHolder) {
            super.onCreate(surfaceHolder);
            IntentFilter filter = new IntentFilter();
            filter.addAction(Const.ACTION_VIDEO_CHANGED);
            filter.addAction(Const.ACTION_SUN_UPDATED);
            // Android 13+ (API 33) requires an explicit exported flag for
            // dynamically registered receivers that handle non-system broadcasts.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(changeReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
            } else {
                registerReceiver(changeReceiver, filter);
            }
        }

        @Override
        public void onDestroy() {
            try {
                unregisterReceiver(changeReceiver);
            } catch (Throwable ignored) {
            }
            super.onDestroy();
        }

        @Override
        public void onSurfaceCreated(SurfaceHolder surfaceHolder) {
            super.onSurfaceCreated(surfaceHolder);
            createGLSurfaceView();
            int width = surfaceHolder.getSurfaceFrame().width();
            int height = surfaceHolder.getSurfaceFrame().height();
            renderer.setScreenSize(width, height);
            visible = true;
            playVideo(Video24Controller.currentVideoId(context));
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder surfaceHolder, int format, int width, int height) {
            super.onSurfaceChanged(surfaceHolder, format, width, height);
            if (renderer != null) renderer.setScreenSize(width, height);
        }

        @Override
        public void onVisibilityChanged(boolean isVisible) {
            super.onVisibilityChanged(isVisible);
            visible = isVisible;
            if (renderer == null || glSurfaceView == null) return;
            if (isVisible) {
                glSurfaceView.onResume();
                int id = Video24Controller.currentVideoId(context);
                if (id != currentVideoId) {
                    playVideo(id);
                } else if (player != null) {
                    player.play();
                }
            } else {
                if (player != null) {
                    player.pause();
                    // Mirror MIUI: seek to start when hidden so next screen-on replays from the beginning.
                    player.seekTo(0);
                }
                glSurfaceView.onPause();
            }
        }

        @Override
        public void onSurfaceDestroyed(SurfaceHolder surfaceHolder) {
            visible = false;
            releasePlayer();
            if (glSurfaceView != null) {
                glSurfaceView.doDestroy();
                glSurfaceView = null;
            }
            renderer = null;
            super.onSurfaceDestroyed(surfaceHolder);
        }

        private void createGLSurfaceView() {
            if (glSurfaceView != null) {
                glSurfaceView.doDestroy();
                glSurfaceView = null;
            }
            glSurfaceView = new GLWallpaperSurfaceView(context);
            ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            ConfigurationInfo ci = am != null ? am.getDeviceConfigurationInfo() : null;
            int glVersion = ci != null ? ci.reqGlEsVersion : 0x20000;
            if (glVersion < 0x20000) {
                throw new RuntimeException("Needs GLESv2 or higher");
            }
            glSurfaceView.setEGLContextClientVersion(2);
            renderer = new GLES20WallpaperRenderer(context);
            glSurfaceView.setPreserveEGLContextOnPause(true);
            glSurfaceView.setRenderer(renderer);
            glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        }

        private void playVideo(int videoId) {
            if (videoId < 1 || videoId > Video24Constant.COUNT) {
                videoId = Video24Constant.NIGHT_VIDEO_ID;
            }
            releasePlayer();
            currentVideoId = videoId;

            final File file = Video24Constant.videoFile(context, videoId);
            if (!file.exists() || file.length() == 0) {
                Utils.debug("GLWallpaperEngine", "video missing: " + file);
                return;
            }

            int w = 1080, h = 1920, rot = 0;
            try {
                MediaMetadataRetriever mmr = new MediaMetadataRetriever();
                mmr.setDataSource(file.getAbsolutePath());
                String rs = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION);
                String ws = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
                String hs = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
                mmr.release();
                if (rs != null) rot = Integer.parseInt(rs);
                if (ws != null) w = Integer.parseInt(ws);
                if (hs != null) h = Integer.parseInt(hs);
            } catch (Exception e) {
                Utils.debug("GLWallpaperEngine", "metadata failed: " + e);
            }
            renderer.setVideoSizeAndRotation(w, h, rot);

            DefaultTrackSelector trackSelector = new DefaultTrackSelector(context);
            trackSelector.setParameters(
                    trackSelector.buildUponParameters().setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, true));
            player = new ExoPlayer.Builder(context).setTrackSelector(trackSelector).build();
            renderer.setSourcePlayer(player);
            player.setMediaItem(MediaItem.fromUri(Uri.fromFile(file)));
            // Match MIUI's Video24WallpaperService: setLooping(false) -> play once, hold last frame.
            player.setRepeatMode(Player.REPEAT_MODE_OFF);
            player.setVolume(0f);
            player.prepare();
            player.play();
            Utils.debug("GLWallpaperEngine", "play video " + videoId);
        }

        private void releasePlayer() {
            if (player != null) {
                try {
                    player.release();
                } catch (Throwable ignored) {
                }
                player = null;
            }
            currentVideoId = -1;
        }
    }

    @Override
    public Engine onCreateEngine() {
        return new GLWallpaperEngine(this);
    }
}
