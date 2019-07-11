/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.setupwizardlib.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.Animatable;
import android.media.MediaPlayer;
import android.os.Build.VERSION_CODES;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.annotation.RawRes;
import androidx.annotation.VisibleForTesting;

import com.android.setupwizardlib.R;

/**
 * A view for displaying videos in a continuous loop (without audio). This is typically used for
 * animated illustrations.
 *
 * <p>The video can be specified using {@code app:suwVideo}, specifying the raw resource to the mp4
 * video. Optionally, {@code app:suwLoopStartMs} can be used to specify which part of the video it
 * should loop back to
 *
 * <p>For optimal file size, use avconv or other video compression tool to remove the unused audio
 * track and reduce the size of your video asset:
 *     avconv -i [input file] -vcodec h264 -crf 20 -an [output_file]
 */
@TargetApi(VERSION_CODES.ICE_CREAM_SANDWICH)
public class IllustrationVideoView extends TextureView implements Animatable,
        TextureView.SurfaceTextureListener,
        MediaPlayer.OnPreparedListener,
        MediaPlayer.OnSeekCompleteListener,
        MediaPlayer.OnInfoListener {

    private static final String TAG = "IllustrationVideoView";

    protected float mAspectRatio = 1.0f; // initial guess until we know

    @Nullable // Can be null when media player fails to initialize
    protected MediaPlayer mMediaPlayer;

    private @RawRes int mVideoResId = 0;

    @VisibleForTesting Surface mSurface;

    protected int mWindowVisibility;

    public IllustrationVideoView(Context context, AttributeSet attrs) {
        super(context, attrs);
        final TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.SuwIllustrationVideoView);
        mVideoResId = a.getResourceId(R.styleable.SuwIllustrationVideoView_suwVideo, 0);
        a.recycle();

        // By default the video scales without interpolation, resulting in jagged edges in the
        // video. This works around it by making the view go through scaling, which will apply
        // anti-aliasing effects.
        setScaleX(0.9999999f);
        setScaleX(0.9999999f);

        setSurfaceTextureListener(this);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);

        if (height < width * mAspectRatio) {
            // Height constraint is tighter. Need to scale down the width to fit aspect ratio.
            width = (int) (height / mAspectRatio);
        } else {
            // Width constraint is tighter. Need to scale down the height to fit aspect ratio.
            height = (int) (width * mAspectRatio);
        }

        super.onMeasure(
                MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
    }

    /**
     * Set the video to be played by this view.
     *
     * @param resId Resource ID of the video, typically an MP4 under res/raw.
     */
    public void setVideoResource(@RawRes int resId) {
        if (resId != mVideoResId) {
            mVideoResId = resId;
            createMediaPlayer();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
        if (hasWindowFocus) {
            start();
        } else {
            stop();
        }
    }

    /**
     * Creates a media player for the current URI. The media player will be started immediately if
     * the view's window is visible. If there is an existing media player, it will be released.
     */
    protected void createMediaPlayer() {
        if (mMediaPlayer != null) {
            mMediaPlayer.release();
        }
        if (mSurface == null || mVideoResId == 0) {
            return;
        }

        mMediaPlayer = MediaPlayer.create(getContext(), mVideoResId);

        if (mMediaPlayer != null) {
            mMediaPlayer.setSurface(mSurface);
            mMediaPlayer.setOnPreparedListener(this);
            mMediaPlayer.setOnSeekCompleteListener(this);
            mMediaPlayer.setOnInfoListener(this);

            float aspectRatio =
                    (float) mMediaPlayer.getVideoHeight() / mMediaPlayer.getVideoWidth();
            if (mAspectRatio != aspectRatio) {
                mAspectRatio = aspectRatio;
                requestLayout();
            }
        } else {
            Log.wtf(TAG, "Unable to initialize media player for video view");
        }
        if (mWindowVisibility == View.VISIBLE) {
            start();
        }
    }

    protected void createSurface() {
        if (mSurface != null) {
            mSurface.release();
            mSurface = null;
        }
        // Reattach only if it has been previously released
        SurfaceTexture surfaceTexture = getSurfaceTexture();
        if (surfaceTexture != null) {
            setVisibility(View.INVISIBLE);
            mSurface = new Surface(surfaceTexture);
        }
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        mWindowVisibility = visibility;
        if (visibility == View.VISIBLE) {
            reattach();
        } else {
            release();
        }
    }

    /**
     * Whether the media player should play the video in a continuous loop. The default value is
     * true.
     */
    protected boolean shouldLoop() {
        return true;
    }

    /**
     * Release any resources used by this view. This is automatically called in
     * onSurfaceTextureDestroyed so in most cases you don't have to call this.
     */
    public void release() {
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
        if (mSurface != null) {
            mSurface.release();
            mSurface = null;
        }
    }

    private void reattach() {
        if (mSurface == null) {
            initVideo();
        }
    }

    private void initVideo() {
        if (mWindowVisibility != View.VISIBLE) {
            return;
        }
        createSurface();
        if (mSurface != null) {
            createMediaPlayer();
        } else {
            Log.w("IllustrationVideoView", "Surface creation failed");
        }
    }

    protected void onRenderingStart() {
    }

    /* SurfaceTextureListener methods */

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
        // Keep the view hidden until video starts
        setVisibility(View.INVISIBLE);
        initVideo();
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height) {
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
        release();
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
    }

    /* Animatable methods */

    @Override
    public void start() {
        if (mMediaPlayer != null && !mMediaPlayer.isPlaying()) {
            mMediaPlayer.start();
        }
    }

    @Override
    public void stop() {
        if (mMediaPlayer != null) {
            mMediaPlayer.pause();
        }
    }

    @Override
    public boolean isRunning() {
        return mMediaPlayer != null && mMediaPlayer.isPlaying();
    }

    /* MediaPlayer callbacks */

    @Override
    public boolean onInfo(MediaPlayer mp, int what, int extra) {
        if (what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) {
            // Video available, show view now
            setVisibility(View.VISIBLE);
            onRenderingStart();
        }
        return false;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        mp.setLooping(shouldLoop());
    }

    @Override
    public void onSeekComplete(MediaPlayer mp) {
        mp.start();
    }

    public int getCurrentPosition() {
        return mMediaPlayer == null ? 0 : mMediaPlayer.getCurrentPosition();
    }
}
