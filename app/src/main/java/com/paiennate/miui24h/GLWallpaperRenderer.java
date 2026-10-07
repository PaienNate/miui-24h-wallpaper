package com.paiennate.miui24h;

import android.content.Context;
import android.opengl.GLSurfaceView;

import androidx.media3.exoplayer.ExoPlayer;

abstract class GLWallpaperRenderer implements GLSurfaceView.Renderer {
    final Context context;

    GLWallpaperRenderer(Context context) {
        this.context = context;
    }

    abstract void setSourcePlayer(ExoPlayer player);

    abstract void setScreenSize(int width, int height);

    abstract void setVideoSizeAndRotation(int width, int height, int rotation);

    abstract void setOffset(float xOffset, float yOffset);
}
