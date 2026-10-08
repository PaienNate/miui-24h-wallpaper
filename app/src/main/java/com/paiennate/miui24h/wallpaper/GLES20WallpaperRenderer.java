package com.paiennate.miui24h.wallpaper;

import com.paiennate.miui24h.R;
import com.paiennate.miui24h.common.Utils;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.os.Handler;
import android.os.Looper;
import android.view.Surface;

import androidx.media3.exoplayer.ExoPlayer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

class GLES20WallpaperRenderer extends GLWallpaperRenderer {
    private static final String TAG = "GLES20WallpaperRenderer";
    private static final int BYTES_PER_FLOAT = 4;
    private static final int BYTES_PER_INT = 4;

    private final FloatBuffer vertices;
    private final FloatBuffer texCoords;
    private final IntBuffer indices;
    private final int[] textures = new int[1];
    private final int[] buffers = new int[3];
    private final float[] mvp = new float[16];

    private int program = 0;
    private int mvpLocation = 0;
    private int positionLocation = 0;
    private int texCoordLocation = 0;

    private SurfaceTexture surfaceTexture = null;
    private Surface outSurface = null;

    private volatile int screenWidth = 0;
    private volatile int screenHeight = 0;
    private volatile int videoWidth = 0;
    private volatile int videoHeight = 0;
    private volatile int videoRotation = 0;

    private float xOffset = 0;
    private float yOffset = 0;
    private float maxXOffset = 0;
    private float maxYOffset = 0;

    private long updatedFrame = 0;
    private long renderedFrame = 0;

    /** Player requested from the main thread; bound on the GL thread once textures exist. */
    private volatile ExoPlayer pendingPlayer = null;

    GLES20WallpaperRenderer(Context context) {
        super(context);

        final float[] vertexArray = { -1f, -1f, -1f, 1f, 1f, -1f, 1f, 1f };
        vertices = ByteBuffer.allocateDirect(vertexArray.length * BYTES_PER_FLOAT)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        vertices.put(vertexArray).position(0);

        final float[] texCoordArray = { 0f, 1f, 0f, 0f, 1f, 1f, 1f, 0f };
        texCoords = ByteBuffer.allocateDirect(texCoordArray.length * BYTES_PER_FLOAT)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        texCoords.put(texCoordArray).position(0);

        final int[] indexArray = { 0, 1, 2, 3, 2, 1 };
        indices = ByteBuffer.allocateDirect(indexArray.length * BYTES_PER_INT)
                .order(ByteOrder.nativeOrder()).asIntBuffer();
        indices.put(indexArray).position(0);

        mvp[0] = mvp[5] = mvp[10] = mvp[15] = 1f;
    }

    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        GLES20.glDepthMask(false);
        GLES20.glDisable(GLES20.GL_CULL_FACE);
        GLES20.glDisable(GLES20.GL_BLEND);

        GLES20.glGenTextures(textures.length, textures, 0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textures[0]);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

        program = Utils.linkProgramGLES20(
                Utils.compileShaderResourceGLES20(context, GLES20.GL_VERTEX_SHADER, R.raw.vertex_20),
                Utils.compileShaderResourceGLES20(context, GLES20.GL_FRAGMENT_SHADER, R.raw.fragment_20));
        mvpLocation = GLES20.glGetUniformLocation(program, "mvp");
        positionLocation = GLES20.glGetAttribLocation(program, "in_position");
        texCoordLocation = GLES20.glGetAttribLocation(program, "in_tex_coord");

        GLES20.glGenBuffers(buffers.length, buffers, 0);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, buffers[0]);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, vertices.capacity() * BYTES_PER_FLOAT, vertices, GLES20.GL_STATIC_DRAW);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, buffers[1]);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, texCoords.capacity() * BYTES_PER_FLOAT, texCoords, GLES20.GL_STATIC_DRAW);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, buffers[2]);
        GLES20.glBufferData(GLES20.GL_ELEMENT_ARRAY_BUFFER, indices.capacity() * BYTES_PER_INT, indices, GLES20.GL_STATIC_DRAW);
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);

        GLES20.glClearColor(0f, 0f, 0f, 1f);
    }

    @Override
    public void onSurfaceChanged(GL10 gl10, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl10) {
        // Bind a pending player once the GL texture exists (GL thread has the context).
        ExoPlayer pp = pendingPlayer;
        if (pp != null && textures[0] != 0) {
            pendingPlayer = null;
            bindSurface(pp);
        }

        if (surfaceTexture == null) {
            return;
        }
        if (renderedFrame < updatedFrame) {
            surfaceTexture.updateTexImage();
            ++renderedFrame;
        }

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glUseProgram(program);
        GLES20.glUniformMatrix4fv(mvpLocation, 1, false, mvp, 0);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, buffers[0]);
        GLES20.glEnableVertexAttribArray(positionLocation);
        GLES20.glVertexAttribPointer(positionLocation, 2, GLES20.GL_FLOAT, false, 2 * BYTES_PER_FLOAT, 0);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, buffers[1]);
        GLES20.glEnableVertexAttribArray(texCoordLocation);
        GLES20.glVertexAttribPointer(texCoordLocation, 2, GLES20.GL_FLOAT, false, 2 * BYTES_PER_FLOAT, 0);
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, buffers[2]);
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, 6, GLES20.GL_UNSIGNED_INT, 0);
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);
        GLES20.glDisableVertexAttribArray(texCoordLocation);
        GLES20.glDisableVertexAttribArray(positionLocation);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
        GLES20.glUseProgram(0);
    }

    @Override
    void setSourcePlayer(ExoPlayer player) {
        pendingPlayer = player;
    }

    private void bindSurface(final ExoPlayer player) {
        if (surfaceTexture != null) {
            surfaceTexture.release();
            surfaceTexture = null;
        }
        if (outSurface != null) {
            outSurface.release();
            outSurface = null;
        }
        updatedFrame = 0;
        renderedFrame = 0;
        surfaceTexture = new SurfaceTexture(textures[0]);
        surfaceTexture.setDefaultBufferSize(Math.max(1, videoWidth), Math.max(1, videoHeight));
        surfaceTexture.setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
            @Override
            public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                ++updatedFrame;
            }
        });
        outSurface = new Surface(surfaceTexture);
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                player.setVideoSurface(outSurface);
            }
        });
    }

    @Override
    void setScreenSize(int width, int height) {
        if (screenWidth != width || screenHeight != height) {
            screenWidth = width;
            screenHeight = height;
            recomputeOffsets();
            updateMatrix();
        }
    }

    @Override
    void setVideoSizeAndRotation(int width, int height, int rotation) {
        if (rotation % 180 != 0) {
            final int swap = width;
            //noinspection SuspiciousNameCombination
            width = height;
            height = swap;
        }
        if (videoWidth != width || videoHeight != height || videoRotation != rotation) {
            videoWidth = width;
            videoHeight = height;
            videoRotation = rotation;
            if (surfaceTexture != null) {
                surfaceTexture.setDefaultBufferSize(Math.max(1, videoWidth), Math.max(1, videoHeight));
            }
            recomputeOffsets();
            updateMatrix();
        }
    }

    @Override
    void setOffset(float xOffset, float yOffset) {
        if (xOffset > maxXOffset) xOffset = maxXOffset;
        if (xOffset < -maxXOffset) xOffset = -maxXOffset;
        if (yOffset > maxYOffset) yOffset = maxYOffset;
        if (yOffset < -maxYOffset) yOffset = -maxYOffset;
        if (this.xOffset != xOffset || this.yOffset != yOffset) {
            this.xOffset = xOffset;
            this.yOffset = yOffset;
            updateMatrix();
        }
    }

    private void recomputeOffsets() {
        if (videoWidth <= 0 || videoHeight <= 0 || screenWidth <= 0 || screenHeight <= 0) return;
        maxXOffset = (1f - ((float) screenWidth / screenHeight) / ((float) videoWidth / videoHeight)) / 2f;
        maxYOffset = (1f - ((float) screenHeight / screenWidth) / ((float) videoHeight / videoWidth)) / 2f;
    }

    private void updateMatrix() {
        for (int i = 0; i < 16; ++i) mvp[i] = 0f;
        mvp[0] = mvp[5] = mvp[10] = mvp[15] = 1f;
        if (videoWidth <= 0 || videoHeight <= 0 || screenWidth <= 0 || screenHeight <= 0) return;
        final float videoRatio = (float) videoWidth / videoHeight;
        final float screenRatio = (float) screenWidth / screenHeight;
        if (videoRatio >= screenRatio) {
            Matrix.scaleM(mvp, 0, ((float) videoWidth / videoHeight) / ((float) screenWidth / screenHeight), 1, 1);
            if (videoRotation % 360 != 0) Matrix.rotateM(mvp, 0, -videoRotation, 0, 0, 1);
            Matrix.translateM(mvp, 0, xOffset, 0, 0);
        } else {
            Matrix.scaleM(mvp, 0, 1, ((float) videoHeight / videoWidth) / ((float) screenHeight / screenWidth), 1);
            if (videoRotation % 360 != 0) Matrix.rotateM(mvp, 0, -videoRotation, 0, 0, 1);
            Matrix.translateM(mvp, 0, 0, yOffset, 0);
        }
    }
}
