package com.example.mp3player.lyrics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LyricsManager {

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

    public static List<LyricLine> getSampleLyrics(String songTitle) {
        String lrc = "[00:00.00] ♪ Instrumental Intro ♪\n" +
                "[00:05.00] Yeah, we are starting up.\n" +
                "[00:12.00] Walking down the rhythm of the city lights\n" +
                "[00:20.00] Catching every heartbeat through the quiet night\n" +
                "[00:30.00] This is the current playing line.\n" +
                "[00:40.00] Swipe left or right to navigate.\n" +
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

