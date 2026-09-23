package com.example.mp3player.util;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.View;

public class ColorExtractor {

    public static class PaletteColors {
        public final int topGradient;
        public final int bottomGradient;
        public final int accentColor;
        public final int accentGradientEnd;

        public PaletteColors(int topGradient, int bottomGradient, int accentColor, int accentGradientEnd) {
            this.topGradient = topGradient;
            this.bottomGradient = bottomGradient;
            this.accentColor = accentColor;
            this.accentGradientEnd = accentGradientEnd;
        }
    }

    public static PaletteColors extractColors(Bitmap bitmap, String fallbackSeed) {
        int dominant = 0;
        if (bitmap != null) {
            dominant = sampleDominantColor(bitmap);
        }

        if (dominant == 0 && fallbackSeed != null) {
            dominant = getSeedColor(fallbackSeed);
        }

        if (dominant == 0) {
            dominant = Color.parseColor("#7C3AED"); // Default vibrant purple
        }

        float[] hsv = new float[3];
        Color.colorToHSV(dominant, hsv);

        // Top Gradient: Dark, rich, saturated tone matching cover hue
        float[] topHsv = new float[]{hsv[0], Math.min(1.0f, Math.max(0.6f, hsv[1])), 0.22f};
        int topGradient = Color.HSVToColor(topHsv);

        // Bottom Gradient: Deep midnight slate with slight cover hue tint
        float[] bottomHsv = new float[]{hsv[0], 0.25f, 0.07f};
        int bottomGradient = Color.HSVToColor(bottomHsv);

        // Accent Color: Bright vibrant tone for play button & indicators
        float[] accentHsv = new float[]{hsv[0], Math.max(0.7f, hsv[1]), 0.95f};
        int accentColor = Color.HSVToColor(accentHsv);

        // Secondary Accent for Play Button gradient (slightly shifted hue)
        float[] accentEndHsv = new float[]{(hsv[0] + 25f) % 360f, 0.9f, 0.85f};
        int accentGradientEnd = Color.HSVToColor(accentEndHsv);

        return new PaletteColors(topGradient, bottomGradient, accentColor, accentGradientEnd);
    }

    private static int sampleDominantColor(Bitmap bitmap) {
        try {
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            int stepX = Math.max(1, width / 20);
            int stepY = Math.max(1, height / 20);

            long totalR = 0, totalG = 0, totalB = 0;
            int count = 0;
            float maxSaturation = -1f;
            int mostVibrant = 0;

            float[] hsv = new float[3];

            for (int x = 0; x < width; x += stepX) {
                for (int y = 0; y < height; y += stepY) {
                    int pixel = bitmap.getPixel(x, y);
                    int r = Color.red(pixel);
                    int g = Color.green(pixel);
                    int b = Color.blue(pixel);

                    // Skip near-black or near-white
                    Color.colorToHSV(pixel, hsv);
                    if (hsv[1] > 0.3f && hsv[2] > 0.2f && hsv[2] < 0.95f) {
                        if (hsv[1] > maxSaturation) {
                            maxSaturation = hsv[1];
                            mostVibrant = pixel;
                        }
                        totalR += r;
                        totalG += g;
                        totalB += b;
                        count++;
                    }
                }
            }

            if (mostVibrant != 0) {
                return mostVibrant;
            }

            if (count > 0) {
                return Color.rgb((int)(totalR / count), (int)(totalG / count), (int)(totalB / count));
            }
        } catch (Exception ignored) {}
        return 0;
    }

    private static int getSeedColor(String seed) {
        int hash = Math.abs(seed.hashCode());
        float hue = (hash % 360);
        return Color.HSVToColor(new float[]{hue, 0.85f, 0.85f});
    }

    public static void applyAnimatedGradient(View targetView, PaletteColors newColors, int[] currentColors) {
        if (targetView == null || newColors == null) return;

        int fromTop = (currentColors != null && currentColors.length >= 2) ? currentColors[0] : newColors.topGradient;
        int fromBottom = (currentColors != null && currentColors.length >= 2) ? currentColors[1] : newColors.bottomGradient;

        int toTop = newColors.topGradient;
        int toBottom = newColors.bottomGradient;

        ArgbEvaluator evaluator = new ArgbEvaluator();
        ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(600);
        animator.addUpdateListener(animation -> {
            float fraction = animation.getAnimatedFraction();
            int top = (int) evaluator.evaluate(fraction, fromTop, toTop);
            int bottom = (int) evaluator.evaluate(fraction, fromBottom, toBottom);

            GradientDrawable gradient = new GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM,
                    new int[]{top, bottom}
            );
            targetView.setBackground(gradient);
        });
        animator.start();

        if (currentColors != null && currentColors.length >= 2) {
            currentColors[0] = toTop;
            currentColors[1] = toBottom;
        }
    }
}
