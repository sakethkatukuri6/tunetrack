package com.example.mp3player;

import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.mp3player.lyrics.LyricsManager;
import com.example.mp3player.model.Song;
import com.example.mp3player.service.MusicPlaybackService;

import java.util.ArrayList;
import java.util.List;

public class LyricsFragment extends Fragment implements MusicPlaybackService.PlaybackListener {

    private ScrollView svLyrics;
    private LinearLayout llLyricsContainer;
    private TextView tvEmptyLyrics;

    private final List<LyricsManager.LyricLine> lyricLines = new ArrayList<>();
    private final List<TextView> lineViews = new ArrayList<>();
    private int currentActiveIndex = -1;
    private long currentSongId = -1;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_lyrics, container, false);
        svLyrics = view.findViewById(R.id.svLyrics);
        llLyricsContainer = view.findViewById(R.id.llLyricsContainer);
        tvEmptyLyrics = view.findViewById(R.id.tvEmptyLyrics);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof MainActivity) {
            MainActivity activity = (MainActivity) getActivity();
            MusicPlaybackService service = activity.getPlaybackService();
            if (service != null) {
                service.addListener(this);
                onSongChanged(service.getQueueManager().getCurrentSong());
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (getActivity() instanceof MainActivity) {
            MusicPlaybackService service = ((MainActivity) getActivity()).getPlaybackService();
            if (service != null) {
                service.removeListener(this);
            }
        }
    }

    @Override
    public void onSongChanged(@Nullable Song song) {
        if (!isAdded() || getContext() == null) return;

        if (song == null) {
            showEmpty(true);
            return;
        }

        if (song.getId() == currentSongId && !lyricLines.isEmpty()) {
            return;
        }

        currentSongId = song.getId();
        lyricLines.clear();
        lineViews.clear();
        llLyricsContainer.removeAllViews();

        List<LyricsManager.LyricLine> parsed = LyricsManager.getSampleLyrics(song.getTitle());
        if (parsed.isEmpty()) {
            showEmpty(true);
        } else {
            showEmpty(false);
            lyricLines.addAll(parsed);
            populateViews();
        }
    }

    private void showEmpty(boolean empty) {
        if (tvEmptyLyrics != null) {
            tvEmptyLyrics.setVisibility(empty ? View.VISIBLE : View.GONE);
        }
    }

    private void populateViews() {
        if (getContext() == null) return;

        int padVertical = (int) (12 * getResources().getDisplayMetrics().density);
        for (int i = 0; i < lyricLines.size(); i++) {
            LyricsManager.LyricLine line = lyricLines.get(i);
            TextView tv = new TextView(getContext());
            tv.setText(line.getText());
            tv.setTextSize(16);
            tv.setGravity(Gravity.CENTER);
            tv.setPadding(16, padVertical, 16, padVertical);
            tv.setTextColor(ContextCompat.getColor(getContext(), R.color.text_secondary_light));
            lineViews.add(tv);
            llLyricsContainer.addView(tv);
        }
    }

    @Override
    public void onPlaybackStateChanged(boolean isPlaying) {}

    @Override
    public void onProgressUpdate(int currentPositionMs, int durationMs) {
        if (lyricLines.isEmpty() || lineViews.isEmpty() || getContext() == null) return;

        int activeIndex = LyricsManager.getActiveLineIndex(currentPositionMs, lyricLines);
        if (activeIndex != currentActiveIndex) {
            if (currentActiveIndex >= 0 && currentActiveIndex < lineViews.size()) {
                TextView prev = lineViews.get(currentActiveIndex);
                prev.setTextColor(ContextCompat.getColor(getContext(), R.color.text_secondary_light));
                prev.setTextSize(16);
                prev.setTypeface(null, Typeface.NORMAL);
            }

            currentActiveIndex = activeIndex;
            if (currentActiveIndex >= 0 && currentActiveIndex < lineViews.size()) {
                TextView curr = lineViews.get(currentActiveIndex);
                curr.setTextColor(ContextCompat.getColor(getContext(), R.color.primary));
                curr.setTextSize(20);
                curr.setTypeface(null, Typeface.BOLD);

                // Auto-scroll to center line
                svLyrics.post(() -> {
                    int scrollTo = curr.getTop() - (svLyrics.getHeight() / 2) + (curr.getHeight() / 2);
                    svLyrics.smoothScrollTo(0, Math.max(0, scrollTo));
                });
            }
        }
    }
}

