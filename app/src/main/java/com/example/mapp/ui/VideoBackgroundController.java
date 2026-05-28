package com.example.mapp.ui;

import android.animation.ValueAnimator;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.LinearInterpolator;
import android.widget.VideoView;

/**
 * 全屏背景视频：17% 下移裁剪 + 500ms 淡入淡出无缝循环。
 */
public class VideoBackgroundController {

    private static final String VIDEO_URL =
            "https://d8j0ntlcm91z4.cloudfront.net/user_38xzZboKViGWJOttwIXH07lWA1P/hf_20260328_115001_bcdaa3b4-03de-47e7-ad63-ae3e392c32d4.mp4";
    private static final float VIDEO_SHIFT_RATIO = 0.17f;
    private static final float VIDEO_SCALE = 1.22f;
    private static final long FADE_MS = 500L;
    private static final float FADE_OUT_BEFORE_END_SEC = 0.55f;
    private static final long LOOP_RESET_DELAY_MS = 100L;
    private static final long PROGRESS_TICK_MS = 80L;

    private final VideoView videoView;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean released;
    private boolean prepared;
    private boolean fadingOut;
    private float currentAlpha;
    private ValueAnimator fadeAnimator;
    private Runnable progressRunnable;
    private Runnable loopResetRunnable;
    private Runnable onErrorFallback;

    public VideoBackgroundController(VideoView videoView) {
        this.videoView = videoView;
        currentAlpha = 0f;
        videoView.setAlpha(0f);
    }

    public void setOnErrorFallback(Runnable onErrorFallback) {
        this.onErrorFallback = onErrorFallback;
    }

    public void start() {
        if (released) {
            return;
        }
        videoView.setVideoURI(Uri.parse(VIDEO_URL));
        videoView.setOnPreparedListener(mp -> {
            if (released) {
                return;
            }
            prepared = true;
            mp.setLooping(false);
            mp.setVolume(0f, 0f);
            applyVerticalOffset();
            fadingOut = false;
            fadeTo(1f, () -> {
                if (released) {
                    return;
                }
                safeStart();
                startProgressWatch();
            });
        });
        videoView.setOnCompletionListener(mp -> onVideoEnded());
        videoView.setOnErrorListener((mp, what, extra) -> {
            if (!released) {
                cancelPendingWork();
                try {
                    videoView.setVisibility(android.view.View.INVISIBLE);
                } catch (Exception ignored) {
                }
                if (onErrorFallback != null) {
                    onErrorFallback.run();
                }
            }
            return true;
        });
    }

    private void onVideoEnded() {
        if (released) {
            return;
        }
        cancelFade();
        currentAlpha = 0f;
        safeSetAlpha(0f);
        fadingOut = false;
        cancelLoopReset();
        loopResetRunnable = () -> {
            loopResetRunnable = null;
            if (released || !prepared) {
                return;
            }
            try {
                videoView.seekTo(0);
                safeStart();
                fadeTo(1f, null);
            } catch (Exception ignored) {
            }
        };
        handler.postDelayed(loopResetRunnable, LOOP_RESET_DELAY_MS);
    }

    private void startProgressWatch() {
        stopProgressWatch();
        progressRunnable = new Runnable() {
            @Override
            public void run() {
                if (released || !prepared) {
                    return;
                }
                try {
                    int duration = videoView.getDuration();
                    int position = videoView.getCurrentPosition();
                    if (duration > 0) {
                        float remainingSec = (duration - position) / 1000f;
                        if (remainingSec <= FADE_OUT_BEFORE_END_SEC && remainingSec > 0 && !fadingOut) {
                            fadingOut = true;
                            fadeTo(0f, null);
                        }
                    }
                } catch (Exception ignored) {
                }
                if (!released) {
                    handler.postDelayed(this, PROGRESS_TICK_MS);
                }
            }
        };
        handler.post(progressRunnable);
    }

    private void stopProgressWatch() {
        if (progressRunnable != null) {
            handler.removeCallbacks(progressRunnable);
            progressRunnable = null;
        }
    }

    private void cancelLoopReset() {
        if (loopResetRunnable != null) {
            handler.removeCallbacks(loopResetRunnable);
            loopResetRunnable = null;
        }
    }

    private void cancelPendingWork() {
        stopProgressWatch();
        cancelLoopReset();
        handler.removeCallbacksAndMessages(null);
    }

    private void cancelFade() {
        if (fadeAnimator != null) {
            fadeAnimator.cancel();
            fadeAnimator.removeAllUpdateListeners();
            fadeAnimator.removeAllListeners();
            fadeAnimator = null;
        }
    }

    private void fadeTo(float target, Runnable onEnd) {
        if (released) {
            return;
        }
        cancelFade();
        float from = currentAlpha;
        fadeAnimator = ValueAnimator.ofFloat(from, target);
        fadeAnimator.setDuration(FADE_MS);
        fadeAnimator.setInterpolator(new LinearInterpolator());
        fadeAnimator.addUpdateListener(animation -> {
            if (released) {
                return;
            }
            currentAlpha = (float) animation.getAnimatedValue();
            safeSetAlpha(currentAlpha);
        });
        fadeAnimator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                if (!released && onEnd != null) {
                    onEnd.run();
                }
            }
        });
        fadeAnimator.start();
    }

    private void safeSetAlpha(float alpha) {
        try {
            videoView.setAlpha(alpha);
        } catch (Exception ignored) {
        }
    }

    private void safeStart() {
        try {
            if (!videoView.isPlaying()) {
                videoView.start();
            }
        } catch (Exception ignored) {
        }
    }

    public void pause() {
        cancelPendingWork();
        cancelFade();
        if (released || !prepared) {
            return;
        }
        try {
            if (videoView.isPlaying()) {
                videoView.pause();
            }
        } catch (Exception ignored) {
        }
    }

    public void resume() {
        if (released || !prepared) {
            return;
        }
        safeStart();
        startProgressWatch();
    }

    public void release() {
        if (released) {
            return;
        }
        released = true;
        prepared = false;
        cancelPendingWork();
        cancelFade();
        videoView.setOnPreparedListener(null);
        videoView.setOnCompletionListener(null);
        videoView.setOnErrorListener(null);
        try {
            videoView.stopPlayback();
        } catch (Exception ignored) {
        }
    }

    private void applyVerticalOffset() {
        if (released) {
            return;
        }
        videoView.post(() -> {
            if (released) {
                return;
            }
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
}
