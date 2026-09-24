package com.example.mp3player.lyrics;

import android.content.Context;
import android.net.Uri;

import com.example.mp3player.model.Song;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LyricsManager {

    private static final String PREF_NAME = "tunetrack_lyrics_prefs";
    private static final String KEY_PREFIX_URI = "lrc_uri_";
    private static final String KEY_PREFIX_NAME = "lrc_name_";

    public static class LyricLine implements Comparable<LyricLine> {
        private final long timestampMs;
        private final String text;

        public LyricLine(long timestampMs, String text) {
            this.timestampMs = timestampMs;
            this.text = text != null ? text.trim() : "";
        }

        public long getTimestampMs() {
            return timestampMs;
        }

        public String getText() {
            return text;
        }

        @Override
        public int compareTo(LyricLine o) {
            return Long.compare(this.timestampMs, o.timestampMs);
        }
    }

    public static class LyricsResult {
        public final List<LyricLine> lines;
        public final String locationDescription;
        public final boolean isCustom;
        public final boolean isAutoDetected;

        public LyricsResult(List<LyricLine> lines, String locationDescription, boolean isCustom, boolean isAutoDetected) {
            this.lines = lines != null ? lines : Collections.emptyList();
            this.locationDescription = locationDescription != null ? locationDescription : "";
            this.isCustom = isCustom;
            this.isAutoDetected = isAutoDetected;
        }
    }

    private static final Pattern LRC_PATTERN = Pattern.compile("\\[(\\d{2}):(\\d{2})(?:\\.(\\d{2,3}))?\\](.*)");

    public static List<LyricLine> parseLrc(String lrcContent) {
        List<LyricLine> lines = new ArrayList<>();
        if (lrcContent == null) return lines;

        String[] split = lrcContent.split("\n");
        for (String line : split) {
            Matcher matcher = LRC_PATTERN.matcher(line.trim());
            if (matcher.matches()) {
                long minutes = Long.parseLong(matcher.group(1));
                long seconds = Long.parseLong(matcher.group(2));
                long ms = 0;
                String msStr = matcher.group(3);
                if (msStr != null) {
                    if (msStr.length() == 2) ms = Long.parseLong(msStr) * 10;
                    else ms = Long.parseLong(msStr);
                }
                long timestamp = (minutes * 60 + seconds) * 1000 + ms;
                String text = matcher.group(4);
                lines.add(new LyricLine(timestamp, text));
            }
        }
        Collections.sort(lines);
        return lines;
    }

    public static int getActiveLineIndex(long currentPositionMs, List<LyricLine> lines) {
        if (lines == null || lines.isEmpty()) return -1;
        int active = -1;
        for (int i = 0; i < lines.size(); i++) {
            if (currentPositionMs >= lines.get(i).getTimestampMs()) {
                active = i;
            } else {
                break;
            }
        }
        return active;
    }

    public static String readStreamToString(InputStream is) {
        if (is == null) return null;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static List<LyricLine> loadFromStream(InputStream is) {
        String content = readStreamToString(is);
        return parseLrc(content);
    }

    public static List<LyricLine> loadFromFile(File file) {
        if (file == null || !file.exists() || !file.canRead()) return Collections.emptyList();
        try (FileInputStream fis = new FileInputStream(file)) {
            return loadFromStream(fis);
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    public static void saveCustomLrc(Context context, long songId, String uriString, String displayName) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_PREFIX_URI + songId, uriString)
                .putString(KEY_PREFIX_NAME + songId, displayName)
                .apply();
    }

    public static void clearCustomLrc(Context context, long songId) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit()
                .remove(KEY_PREFIX_URI + songId)
                .remove(KEY_PREFIX_NAME + songId)
                .apply();
    }

    public static String getCustomLrcUri(Context context, long songId) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .getString(KEY_PREFIX_URI + songId, null);
    }

    public static String getCustomLrcName(Context context, long songId) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .getString(KEY_PREFIX_NAME + songId, null);
    }

    public static LyricsResult loadLyricsForSong(Context context, Song song) {
        if (song == null) {
            return new LyricsResult(Collections.emptyList(), "No song playing", false, false);
        }

        // 1. Check custom user-selected LRC file from SharedPreferences
        String customUriStr = getCustomLrcUri(context, song.getId());
        if (customUriStr != null) {
            try {
                Uri uri = Uri.parse(customUriStr);
                InputStream is = context.getContentResolver().openInputStream(uri);
                List<LyricLine> lines = loadFromStream(is);
                if (!lines.isEmpty()) {
                    String customName = getCustomLrcName(context, song.getId());
                    if (customName == null) customName = uri.getLastPathSegment();
                    return new LyricsResult(lines, "Custom: " + customName, true, false);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // 2. Check for auto-detected .lrc file next to the audio file
        if (song.getDataPath() != null) {
            try {
                File audioFile = new File(song.getDataPath());
                if (audioFile.exists()) {
                    String baseName = audioFile.getName();
                    int dot = baseName.lastIndexOf('.');
                    String lrcName = (dot > 0 ? baseName.substring(0, dot) : baseName) + ".lrc";
                    File lrcFile = new File(audioFile.getParentFile(), lrcName);
                    if (lrcFile.exists() && lrcFile.canRead()) {
                        List<LyricLine> lines = loadFromFile(lrcFile);
                        if (!lines.isEmpty()) {
                            return new LyricsResult(lines, "Auto-detected: " + lrcFile.getAbsolutePath(), false, true);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // 3. Fallback to sample demo lyrics
        List<LyricLine> sample = getSampleLyrics(song.getTitle());
        return new LyricsResult(sample, "Sample preview (No .lrc file found)", false, false);
    }

    public static List<LyricLine> getSampleLyrics(String songTitle) {
        String lrc = "[00:00.00] ♪ Instrumental Intro ♪\n" +
                "[00:05.00] Yeah, we are starting up.\n" +
                "[00:12.00] Walking down the rhythm of the city lights\n" +
                "[00:20.00] Catching every heartbeat through the quiet night\n" +
                "[00:30.00] This is the current playing line.\n" +
                "[00:40.00] Tap any line to jump to that timestamp.\n" +
                "[00:52.00] Feel the bass line guiding every step we take\n" +
                "[01:05.00] Endless harmony in every breath we make\n" +
                "[01:18.00] Now the chorus echoes high into the sky\n" +
                "[01:30.00] We let the music carry us as time goes by\n" +
                "[01:45.00] ♪ Synth Solo & Melodic Interlude ♪\n" +
                "[02:10.00] Fading back into the calm and gentle breeze\n" +
                "[02:30.00] Memories playing softly through the trees\n" +
                "[03:00.00] ♪ Outro Fade ♪";
        return parseLrc(lrc);
    }
}
