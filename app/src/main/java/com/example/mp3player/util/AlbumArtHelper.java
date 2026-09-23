package com.example.mp3player.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.util.LruCache;

import com.example.mp3player.model.Song;

public class AlbumArtHelper {

    private static final int CACHE_SIZE = 20;
    private static final LruCache<String, Bitmap> memoryCache = new LruCache<>(CACHE_SIZE);

    public static Bitmap getAlbumArt(Context context, Song song) {
        if (song == null) return getPlaceholderArt("default");

        String cacheKey = String.valueOf(song.getId()) + "_" + song.getTitle();
        Bitmap cached = memoryCache.get(cacheKey);
        if (cached != null) return cached;

        Bitmap art = loadEmbeddedArt(context, song);
        if (art == null) {
            art = generateStylizedArt(song);
        }

        if (art != null) {
            memoryCache.put(cacheKey, art);
        }
        return art;
    }

    private static Bitmap loadEmbeddedArt(Context context, Song song) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            if (song.getDataPath() != null) {
                retriever.setDataSource(song.getDataPath());
            } else if (song.getUri() != null && song.getUriString().startsWith("content://")) {
                retriever.setDataSource(context, song.getUri());
            } else {
                return null;
            }

            byte[] embedded = retriever.getEmbeddedPicture();
            if (embedded != null && embedded.length > 0) {
                return BitmapFactory.decodeByteArray(embedded, 0, embedded.length);
            }
        } catch (Exception ignored) {
        } finally {
            try {
                retriever.release();
            } catch (Exception ignored) {}
        }
        return null;
    }

    /**
     * Generates a modern gradient album cover with a stylized vinyl record graphic,
     * directly matching the design reference in Image 2!
     */
    public static Bitmap generateStylizedArt(Song song) {
        int size = 512;
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        String title = song != null ? song.getTitle() : "Music";
        int primaryColor;
        int secondaryColor;

        if (title.contains("Neon") || title.contains("Prototype") || title.contains("Astral")) {
            // Neon Purple / Violet theme matching Image 2
            primaryColor = Color.parseColor("#8A2BE2");
            secondaryColor = Color.parseColor("#4A0E8F");
        } else if (title.contains("Sunrise") || title.contains("Acoustic")) {
            // Sunrise Gold / Coral Amber theme
            primaryColor = Color.parseColor("#FF6B35");
            secondaryColor = Color.parseColor("#781D00");
        } else if (title.contains("Midnight") || title.contains("Drive")) {
            // Cyber Cyan / Electric Teal theme
            primaryColor = Color.parseColor("#00E5FF");
            secondaryColor = Color.parseColor("#002B49");
        } else {
            // Adaptive gradient based on hash
            int hash = Math.abs(title.hashCode());
            float hue1 = (hash % 360);
            float hue2 = ((hue1 + 45) % 360);
            primaryColor = Color.HSVToColor(new float[]{hue1, 0.85f, 0.85f});
            secondaryColor = Color.HSVToColor(new float[]{hue2, 0.90f, 0.35f});
        }

        // Draw Rounded Background with Linear Gradient
        Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setShader(new LinearGradient(
                0, 0, size, size,
                primaryColor, secondaryColor,
                Shader.TileMode.CLAMP
        ));
        RectF rect = new RectF(0, 0, size, size);
        canvas.drawRoundRect(rect, 48, 48, bgPaint);

        // Draw Outer Subtle Glow Ring
        Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        ringPaint.setStyle(Paint.Style.STROKE);
        ringPaint.setStrokeWidth(6);
        ringPaint.setColor(Color.argb(70, 255, 255, 255));
        canvas.drawRoundRect(new RectF(16, 16, size - 16, size - 16), 40, 40, ringPaint);

        // Center vinyl record graphic matching Image 2
        float cx = size / 2f;
        float cy = size / 2f;

        // Outer Vinyl Disc Circle
        Paint discPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        discPaint.setStyle(Paint.Style.STROKE);
        discPaint.setStrokeWidth(24);
        discPaint.setColor(Color.argb(220, 255, 255, 255));
        canvas.drawCircle(cx, cy, 140, discPaint);

        // Inner Disc Groove Line 1
        Paint groove1 = new Paint(Paint.ANTI_ALIAS_FLAG);
        groove1.setStyle(Paint.Style.STROKE);
        groove1.setStrokeWidth(12);
        groove1.setColor(Color.argb(160, 255, 255, 255));
        canvas.drawCircle(cx, cy, 80, groove1);

        // Center Spindle Solid Circle
        Paint centerDot = new Paint(Paint.ANTI_ALIAS_FLAG);
        centerDot.setStyle(Paint.Style.FILL);
        centerDot.setColor(Color.argb(240, 255, 255, 255));
        canvas.drawCircle(cx, cy, 26, centerDot);

        // Center Spindle Hole
        Paint hole = new Paint(Paint.ANTI_ALIAS_FLAG);
        hole.setStyle(Paint.Style.FILL);
        hole.setColor(secondaryColor);
        canvas.drawCircle(cx, cy, 12, hole);

        return bitmap;
    }

    public static Bitmap getPlaceholderArt(String seed) {
        return generateStylizedArt(new Song(0, seed, "Artist", "Album", 0, null, null));
    }
}

