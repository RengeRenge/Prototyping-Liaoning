package com.prototyping.cvcontroller;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.graphics.Color;
import android.view.View;
import java.util.WeakHashMap;

public class WarningAnimation {
    private static final WeakHashMap<View, ObjectAnimator> runningAnimations = new WeakHashMap<>();

    // 默认颜色值
    private static final int DEFAULT_FROM_COLOR = Color.WHITE;
    private static final int DEFAULT_TO_COLOR = Color.RED;
    private static final long DEFAULT_DURATION = 800; // 毫秒

    /**
     * 开始红色闪烁动画（默认参数）
     */
    public static void startBlinking(View view) {
        startBlinking(view, DEFAULT_FROM_COLOR, DEFAULT_TO_COLOR, DEFAULT_DURATION);
    }

    /**
     * 开始自定义闪烁动画
     * @param view 目标视图
     * @param fromColor 起始颜色
     * @param toColor 目标颜色
     * @param duration 单次动画持续时间(毫秒)
     */
    public static void startBlinking(View view, int fromColor, int toColor, long duration) {
        // 如果该视图已经有动画在运行，则不再创建新动画
        if (runningAnimations.containsKey(view)) {
            return;
        }

        // 创建颜色动画
        ObjectAnimator colorAnim = ObjectAnimator.ofObject(
                view,
                "backgroundColor",
                new ArgbEvaluator(),
                fromColor,
                toColor,
                fromColor);

        colorAnim.setDuration(duration);
        colorAnim.setRepeatCount(ObjectAnimator.INFINITE);
        colorAnim.setRepeatMode(ObjectAnimator.REVERSE);

        // 动画结束监听器，用于清理
        colorAnim.addListener(new android.animation.Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(android.animation.Animator animation) {}

            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                runningAnimations.remove(view);
            }

            @Override
            public void onAnimationCancel(android.animation.Animator animation) {
                runningAnimations.remove(view);
            }

            @Override
            public void onAnimationRepeat(android.animation.Animator animation) {}
        });

        runningAnimations.put(view, colorAnim);
        colorAnim.start();
    }

    /**
     * 停止指定视图的闪烁动画
     */
    public static void stopBlinking(View view) {
        ObjectAnimator animator = runningAnimations.get(view);
        if (animator != null) {
            animator.cancel();
            runningAnimations.remove(view);
        }
        // 恢复默认背景色（透明或原色）
        view.setBackgroundColor(Color.TRANSPARENT);
    }

    /**
     * 检查指定视图是否正在闪烁
     */
    public static boolean isBlinking(View view) {
        return runningAnimations.containsKey(view);
    }

    /**
     * 停止所有闪烁动画
     */
    public static void stopAll() {
        for (ObjectAnimator animator : runningAnimations.values()) {
            animator.cancel();
        }
        runningAnimations.clear();
    }
}