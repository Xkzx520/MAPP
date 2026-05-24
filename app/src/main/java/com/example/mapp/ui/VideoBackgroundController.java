package com.example.mapp.ui;

import android.animation.ValueAnimator;
import android.net.Uri;
import android.view.Choreographer;
import android.widget.VideoView;

/**
 * 全屏背景视频循环 + requestAnimationFrame 风格淡入淡出（无 CSS transition）。
 */
public class VideoBackgroundController {

    private static final String VIDEO_URL =
            "https://d8j0ntlcm91z4.cloudfront.net/user_38xzZboKViGWJOttwIXH07lWA1P/hf_20260328_115001_bcdaa3b4-03de-47e7-ad63-ae3e392c32d4.mp4";
    private static final long FADE_MS = 500L;
    private static final long FADE_OUT_BEFORE_END_MS = 550L;
    private static final long RESET_DELAY_MS = 100L;
    /** 视频整体下移比例，顶部留黑给文字区域 */
    private static final float VIDEO_SHIFT_RATIO = 0.34f;
    private static final float VIDEO_SCALE = 1.18f;

    private final VideoView videoView;
    private final Choreographer choreographer = Choreographer.getInstance();

    private boolean fadingOut;
    private ValueAnimator runningAnimator;
    private long fadeStartTime;
    private float fadeStartAlpha;
    private float fadeTargetAlpha;
    private boolean fadeRunning;

    private final Choreographer.FrameCallback fadeFrameCallback = new Choreographer.FrameCallback() {
        @Override
        public void doFrame(long frameTimeNanos) {
            if (!fadeRunning) {
                return;
            }
            long elapsed = System.currentTimeMillis() - fadeStartTime;
            float t = Math.min(1f, elapsed / (float) FADE_MS);
            float alpha = fadeStartAlpha + (fadeTargetAlpha - fadeStartAlpha) * t;
            videoView.setAlpha(alpha);
            if (t < 1f) {
                choreographer.postFrameCallback(this);
            } else {
                fadeRunning = false;
                videoView.setAlpha(fadeTargetAlpha);
            }
        }
    };

    private final Runnable positionChecker = new Runnable() {
        @Override
        public void run() {
            if (!videoView.isShown()) {
                return;
            }
            try {
                if (videoView.isPlaying() && !fadingOut) {
                    int duration = videoView.getDuration();
                    int position = videoView.getCurrentPosition();
                    if (duration > 0 && duration - position <= FADE_OUT_BEFORE_END_MS) {
                        fadingOut = true;
                        fadeTo(0f);
                    }
                }
            } catch (Exception ignored) {
            }
            videoView.postDelayed(this, 80);
        }
    };

    public VideoBackgroundController(VideoView videoView) {
        this.videoView = videoView;
    }

    public void start() {
        // 先保持可见，避免网络视频加载失败时整屏黑屏
        videoView.setAlpha(1f);
        videoView.setVideoURI(Uri.parse(VIDEO_URL));
        videoView.setOnPreparedListener(mp -> {
            mp.setLooping(false);
            videoView.start();
            applyVerticalOffset();
            videoView.setAlpha(1f);
            videoView.removeCallbacks(positionChecker);
            videoView.post(positionChecker);
        });
        videoView.setOnErrorListener((mp, what, extra) -> {
            // 隐藏视频层，露出底层渐变兜底
            videoView.setVisibility(android.view.View.INVISIBLE);
            return true;
        });
        videoView.setOnCompletionListener(mp -> {
            fadingOut = false;
            cancelFade();
            videoView.setAlpha(0f);
            videoView.postDelayed(() -> {
                videoView.seekTo(0);
                videoView.start();
                fadeTo(1f);
            }, RESET_DELAY_MS);
        });
    }

    public void release() {
        cancelFade();
        videoView.removeCallbacks(positionChecker);
        videoView.stopPlayback();
    }

    private void applyVerticalOffset() {
        videoView.post(() -> {
            int height = videoView.getHeight();
            if (height <= 0) {
                return;
            }
            float offset = height * VIDEO_SHIFT_RATIO;
            videoView.setPivotX(videoView.getWidth() / 2f);
            videoView.setPivotY(0f);
            videoView.setScaleX(VIDEO_SCALE);
            videoView.setScaleY(VIDEO_SCALE);
            videoView.setTranslationY(offset);
        });
    }

    private void fadeTo(float targetAlpha) {
        cancelFade();
        fadeStartAlpha = videoView.getAlpha();
        fadeTargetAlpha = targetAlpha;
        fadeStartTime = System.currentTimeMillis();
        fadeRunning = true;
        choreographer.postFrameCallback(fadeFrameCallback);
    }

    private void cancelFade() {
        fadeRunning = false;
        choreographer.removeFrameCallback(fadeFrameCallback);
        if (runningAnimator != null) {
            runningAnimator.cancel();
            runningAnimator = null;
        }
    }
}
