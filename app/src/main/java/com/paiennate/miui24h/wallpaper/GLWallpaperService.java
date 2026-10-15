package com.paiennate.miui24h.wallpaper;

import com.paiennate.miui24h.common.Const;
import com.paiennate.miui24h.common.Utils;
import com.paiennate.miui24h.data.Video24Constant;
import com.paiennate.miui24h.schedule.Video24Controller;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ConfigurationInfo;
import android.content.res.AssetFileDescriptor;
import android.database.ContentObserver;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.service.wallpaper.WallpaperService;
import android.view.SurfaceHolder;

import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
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
        /** MIUI's mNextVideoId: a requested video that is applied once the current one finishes. */
        private int pendingVideoId = -1;
        private boolean visible = false;

        /** MIUI switches only on completion/error, and only if a next video was requested. */
        private final Player.Listener playerListener = new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int state) {
                if (state == Player.STATE_ENDED) {
                    applyPending();
                }
            }

            @Override
            public void onPlayerError(PlaybackException error) {
                Utils.debug("GLWallpaperEngine", "player error: " + error.getErrorCodeName());
                applyPending();
            }
        };

        private final BroadcastReceiver changeReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context c, Intent intent) {
                recomputeAndMaybeSwitch();
            }
        };

        /** MIUI keys off real screen on/off (not visibility): replay on screen-on, rewind on screen-off. */
        private final BroadcastReceiver screenReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context c, Intent intent) {
                final String action = intent.getAction();
                if (Intent.ACTION_SCREEN_ON.equals(action)) {
                    if (player != null) {
                        player.seekTo(0);
                        player.play();
                    }
                } else if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                    if (player != null) {
                        player.pause();
                        player.seekTo(0);
                    }
                }
            }
        };

        /** MIUI observes ui_night_mode and reschedules immediately. */
        private final ContentObserver uiModeObserver = new ContentObserver(new Handler(Looper.getMainLooper())) {
            @Override
            public void onChange(boolean self) {
                recomputeAndMaybeSwitch();
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
            try {
                context.getContentResolver().registerContentObserver(
                        Settings.Secure.getUriFor("ui_night_mode"), false, uiModeObserver);
            } catch (Throwable ignored) {
            }
            try {
                // System broadcasts -> the exported flag is not required.
                IntentFilter screenFilter = new IntentFilter();
                screenFilter.addAction(Intent.ACTION_SCREEN_ON);
                screenFilter.addAction(Intent.ACTION_SCREEN_OFF);
                registerReceiver(screenReceiver, screenFilter);
            } catch (Throwable ignored) {
            }
        }

        @Override
        public void onDestroy() {
            try {
                unregisterReceiver(changeReceiver);
            } catch (Throwable ignored) {
            }
            try {
                unregisterReceiver(screenReceiver);
            } catch (Throwable ignored) {
            }
            try {
                context.getContentResolver().unregisterContentObserver(uiModeObserver);
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
                recomputeAndMaybeSwitch();
                // Resume (do not replay) unless playback already ended.
                if (player != null && player.getPlaybackState() != Player.STATE_ENDED) {
                    player.play();
                }
            } else {
                // Plain pause on hide; the real screen-off receiver does the rewind/replay.
                if (player != null) player.pause();
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

        private void applyPending() {
            if (pendingVideoId > 0) {
                final int id = pendingVideoId;
                pendingVideoId = -1;
                playVideo(id);
            }
        }

        /** Called on any external trigger (broadcast / dark-mode change). */
        private void recomputeAndMaybeSwitch() {
            if (!visible || renderer == null) return;
            final int id = Video24Controller.currentVideoId(context);
            if (id == currentVideoId) return;
            if (player != null && player.isPlaying()) {
                // MIUI: while playing, remember the next video and switch on completion.
                pendingVideoId = id;
            } else {
                playVideo(id);
            }
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
            pendingVideoId = -1;

            final String asset = Video24Constant.videoAsset(videoId);

            int w = 1080, h = 1920, rot = 0;
            try {
                MediaMetadataRetriever mmr = new MediaMetadataRetriever();
                AssetFileDescriptor afd = getAssets().openFd(asset);
                mmr.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getDeclaredLength());
                afd.close();
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
            player.addListener(playerListener);
            renderer.setSourcePlayer(player);
            player.setMediaItem(MediaItem.fromUri(Video24Constant.assetUri(videoId)));
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
                    player.removeListener(playerListener);
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
