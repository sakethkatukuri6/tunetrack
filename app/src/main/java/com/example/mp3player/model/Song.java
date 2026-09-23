package com.example.mp3player.model;

import android.net.Uri;
import java.io.Serializable;
import java.util.Locale;

public class Song implements Serializable {
    private final long id;
    private final String title;
    private final String artist;
    private final String album;
    private final long durationMs;
    private final String dataPath;
    private final String uriString;
    private boolean isFavorite;

    public Song(long id, String title, String artist, String album, long durationMs, String dataPath, String uriString) {
        this.id = id;
        this.title = (title != null && !title.trim().isEmpty()) ? title : "Unknown Title";
        this.artist = (artist != null && !artist.trim().isEmpty() && !artist.equals("<unknown>")) ? artist : "Unknown Artist";
        this.album = (album != null && !album.trim().isEmpty()) ? album : "Unknown Album";
        this.durationMs = durationMs;
        this.dataPath = dataPath;
        this.uriString = uriString;
        this.isFavorite = false;
    }

    public long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    public String getAlbum() {
        return album;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public String getDataPath() {
        return dataPath;
    }

    public String getUriString() {
        return uriString;
    }

    public Uri getUri() {
        return uriString != null ? Uri.parse(uriString) : null;
    }

    public boolean isFavorite() {
        return isFavorite;
    }

    public void setFavorite(boolean favorite) {
        isFavorite = favorite;
    }

    public String getFormattedDuration() {
        return formatTime(durationMs);
    }

    public static String formatTime(long ms) {
        if (ms < 0) ms = 0;
        long totalSeconds = ms / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds);
    }
}

