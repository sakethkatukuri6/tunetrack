package com.example.mp3player;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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
    private TextView tvLyricsSongTitle;
    private TextView tvLyricsSongArtist;

    // LRC Location Bar views
    private View llLrcLocationBar;
    private ImageView ivLrcStatusIcon;
    private View llLrcTextContainer;
    private TextView tvLrcLocation;
    private Button btnSelectLrc;
    private ImageButton btnClearLrc;

    // Scroll seek controls
    private View llScrollControls;
    private TextView tvScrollPreview;
    private Button btnReSyncLyrics;

    private final List<LyricsManager.LyricLine> lyricLines = new ArrayList<>();
    private final List<TextView> lineViews = new ArrayList<>();
    private int currentActiveIndex = -1;
    private long currentSongId = -1;
    private Song currentSong;

    private boolean isUserScrolling = false;
    private int previewLineIndex = -1;
    private final Handler scrollHandler = new Handler(Looper.getMainLooper());
    private final Runnable autoScrollResumeRunnable = () -> {
        isUserScrolling = false;
        if (llScrollControls != null) {
            llScrollControls.setVisibility(View.GONE);
        }
        if (currentActiveIndex >= 0 && currentActiveIndex < lineViews.size()) {
            scrollToLine(currentActiveIndex);
        }
    };

    private final ActivityResultLauncher<String[]> lrcPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri != null && currentSong != null && getContext() != null) {
                    try {
                        getContext().getContentResolver().takePersistableUriPermission(
                                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                        );
                    } catch (Exception ignored) {}

                    String displayName = getFileNameFromUri(uri);
                    LyricsManager.saveCustomLrc(requireContext(), currentSong.getId(), uri.toString(), displayName);
                    Toast.makeText(getContext(), "Linked LRC: " + displayName, Toast.LENGTH_SHORT).show();
                    loadLyrics(currentSong);
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_lyrics, container, false);

        svLyrics = view.findViewById(R.id.svLyrics);
        llLyricsContainer = view.findViewById(R.id.llLyricsContainer);
        tvEmptyLyrics = view.findViewById(R.id.tvEmptyLyrics);
        tvLyricsSongTitle = view.findViewById(R.id.tvLyricsSongTitle);
        tvLyricsSongArtist = view.findViewById(R.id.tvLyricsSongArtist);

        llLrcLocationBar = view.findViewById(R.id.llLrcLocationBar);
        ivLrcStatusIcon = view.findViewById(R.id.ivLrcStatusIcon);
        llLrcTextContainer = view.findViewById(R.id.llLrcTextContainer);
        tvLrcLocation = view.findViewById(R.id.tvLrcLocation);
        btnSelectLrc = view.findViewById(R.id.btnSelectLrc);
        btnClearLrc = view.findViewById(R.id.btnClearLrc);

        llScrollControls = view.findViewById(R.id.llScrollControls);
        tvScrollPreview = view.findViewById(R.id.tvScrollPreview);
        btnReSyncLyrics = view.findViewById(R.id.btnReSyncLyrics);

        setupLrcPicker();
        setupManualScrollSeeking();

        return view;
    }

    private void setupLrcPicker() {
        View.OnClickListener openPicker = v -> {
            if (currentSong == null) {
                Toast.makeText(getContext(), "Play a song first to link an LRC file", Toast.LENGTH_SHORT).show();
                return;
            }
            lrcPickerLauncher.launch(new String[]{"*/*"});
        };

        btnSelectLrc.setOnClickListener(openPicker);

        llLrcTextContainer.setOnClickListener(v -> {
            if (currentSong == null) return;
            showLocationDetailsDialog();
        });

        btnClearLrc.setOnClickListener(v -> {
            if (currentSong != null && getContext() != null) {
                LyricsManager.clearCustomLrc(requireContext(), currentSong.getId());
                Toast.makeText(getContext(), "Reset to auto-detection", Toast.LENGTH_SHORT).show();
                loadLyrics(currentSong);
            }
        });
    }

    private void showLocationDetailsDialog() {
        if (getContext() == null || currentSong == null) return;

        String location = tvLrcLocation.getText().toString();
        String msg = "Song: " + currentSong.getTitle() + "\n" +
                "Artist: " + currentSong.getArtist() + "\n\n" +
                "Active Lyrics Source:\n" + location + "\n\n" +
                "Total Lyric Lines: " + lyricLines.size();

        new AlertDialog.Builder(requireContext())
                .setTitle("LRC File Details")
                .setMessage(msg)
                .setPositiveButton("Choose Different LRC", (dialog, which) -> lrcPickerLauncher.launch(new String[]{"*/*"}))
                .setNeutralButton("Reset", (dialog, which) -> {
                    LyricsManager.clearCustomLrc(requireContext(), currentSong.getId());
                    loadLyrics(currentSong);
                })
                .setNegativeButton("Close", null)
                .show();
    }

    private void setupManualScrollSeeking() {
        svLyrics.setOnTouchListener((v, event) -> {
            int action = event.getAction();
            if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE) {
                isUserScrolling = true;
                scrollHandler.removeCallbacks(autoScrollResumeRunnable);
                updateScrollCenterPreview();
            } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                scrollHandler.postDelayed(autoScrollResumeRunnable, 4500);
            }
            return false;
        });

        svLyrics.getViewTreeObserver().addOnScrollChangedListener(() -> {
            if (isUserScrolling) {
                updateScrollCenterPreview();
            }
        });

        tvScrollPreview.setOnClickListener(v -> {
            if (previewLineIndex >= 0 && previewLineIndex < lyricLines.size()) {
                LyricsManager.LyricLine line = lyricLines.get(previewLineIndex);
                seekToPosition((int) line.getTimestampMs(), previewLineIndex);
            }
        });

        btnReSyncLyrics.setOnClickListener(v -> {
            isUserScrolling = false;
            if (llScrollControls != null) llScrollControls.setVisibility(View.GONE);
            if (currentActiveIndex >= 0 && currentActiveIndex < lineViews.size()) {
                scrollToLine(currentActiveIndex);
            }
        });
    }

    private void updateScrollCenterPreview() {
        if (lyricLines.isEmpty() || lineViews.isEmpty() || svLyrics == null) return;

        int scrollCenter = svLyrics.getScrollY() + (svLyrics.getHeight() / 2);
        int closest = findClosestLineIndex(scrollCenter);

        if (closest >= 0 && closest < lyricLines.size()) {
            previewLineIndex = closest;
            LyricsManager.LyricLine line = lyricLines.get(closest);
            String snippet = line.getText();
            if (snippet.length() > 22) snippet = snippet.substring(0, 22) + "…";

            tvScrollPreview.setText(String.format("▶ Seek: %s  \"%s\"",
                    Song.formatTime(line.getTimestampMs()), snippet));

            if (llScrollControls != null && llScrollControls.getVisibility() != View.VISIBLE) {
                llScrollControls.setVisibility(View.VISIBLE);
            }
        }
    }

    private int findClosestLineIndex(int targetY) {
        int bestIndex = -1;
        int minDiff = Integer.MAX_VALUE;

        for (int i = 0; i < lineViews.size(); i++) {
            TextView tv = lineViews.get(i);
            int itemCenter = tv.getTop() + (tv.getHeight() / 2);
            int diff = Math.abs(itemCenter - targetY);
            if (diff < minDiff) {
                minDiff = diff;
                bestIndex = i;
            }
        }
        return bestIndex;
    }

    private void seekToPosition(int positionMs, int lineIndex) {
        if (getActivity() instanceof MainActivity) {
            MusicPlaybackService service = ((MainActivity) getActivity()).getPlaybackService();
            if (service != null) {
                service.seekTo(positionMs);
                highlightActiveLine(lineIndex);
                scrollToLine(lineIndex);
                isUserScrolling = false;
                if (llScrollControls != null) llScrollControls.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Jumped to " + Song.formatTime(positionMs), Toast.LENGTH_SHORT).show();
            }
        }
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
        scrollHandler.removeCallbacks(autoScrollResumeRunnable);
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

        currentSong = song;

        if (song == null) {
            showEmpty(true);
            if (tvLyricsSongTitle != null) tvLyricsSongTitle.setText("No track playing");
            if (tvLyricsSongArtist != null) tvLyricsSongArtist.setText("");
            if (tvLrcLocation != null) tvLrcLocation.setText("No active track");
            if (btnClearLrc != null) btnClearLrc.setVisibility(View.GONE);
            return;
        }

        if (tvLyricsSongTitle != null) tvLyricsSongTitle.setText(song.getTitle());
        if (tvLyricsSongArtist != null) tvLyricsSongArtist.setText(song.getArtist());

        if (song.getId() == currentSongId && !lyricLines.isEmpty()) {
            return;
        }

        currentSongId = song.getId();
        loadLyrics(song);
    }

    private void loadLyrics(@NonNull Song song) {
        if (!isAdded() || getContext() == null) return;

        lyricLines.clear();
        lineViews.clear();
        llLyricsContainer.removeAllViews();
        currentActiveIndex = -1;

        LyricsManager.LyricsResult result = LyricsManager.loadLyricsForSong(requireContext(), song);

        if (tvLrcLocation != null) {
            tvLrcLocation.setText(result.locationDescription);
        }

        if (btnClearLrc != null) {
            btnClearLrc.setVisibility(result.isCustom ? View.VISIBLE : View.GONE);
        }

        if (ivLrcStatusIcon != null) {
            int iconColor = (result.isCustom || result.isAutoDetected)
                    ? ContextCompat.getColor(requireContext(), R.color.primary)
                    : ContextCompat.getColor(requireContext(), R.color.text_secondary);
            ivLrcStatusIcon.setImageTintList(android.content.res.ColorStateList.valueOf(iconColor));
        }

        if (result.lines.isEmpty()) {
            showEmpty(true);
        } else {
            showEmpty(false);
            lyricLines.addAll(result.lines);
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

        int padVertical = (int) (14 * getResources().getDisplayMetrics().density);
        TypedValue outValue = new TypedValue();
        requireContext().getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);

        for (int i = 0; i < lyricLines.size(); i++) {
            final int lineIndex = i;
            final LyricsManager.LyricLine line = lyricLines.get(i);

            TextView tv = new TextView(getContext());
            tv.setText(line.getText());
            tv.setTextSize(16);
            tv.setGravity(Gravity.CENTER);
            tv.setPadding(24, padVertical, 24, padVertical);
            tv.setTextColor(ContextCompat.getColor(getContext(), R.color.text_secondary));

            // Clickable ripple & Seek on Click!
            tv.setBackgroundResource(outValue.resourceId);
            tv.setClickable(true);
            tv.setFocusable(true);
            tv.setOnClickListener(v -> seekToPosition((int) line.getTimestampMs(), lineIndex));

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
            highlightActiveLine(activeIndex);

            // Auto-scroll to active line ONLY if user is not currently scrolling manually
            if (!isUserScrolling) {
                scrollToLine(activeIndex);
            }
        }
    }

    private void highlightActiveLine(int index) {
        if (getContext() == null) return;

        // Reset previous line
        if (currentActiveIndex >= 0 && currentActiveIndex < lineViews.size()) {
            TextView prev = lineViews.get(currentActiveIndex);
            prev.setTextColor(ContextCompat.getColor(getContext(), R.color.text_secondary));
            prev.setTextSize(16);
            prev.setTypeface(null, Typeface.NORMAL);
        }

        currentActiveIndex = index;

        // Highlight new line
        if (currentActiveIndex >= 0 && currentActiveIndex < lineViews.size()) {
            TextView curr = lineViews.get(currentActiveIndex);
            curr.setTextColor(ContextCompat.getColor(getContext(), R.color.primary));
            curr.setTextSize(20);
            curr.setTypeface(null, Typeface.BOLD);
        }
    }

    private void scrollToLine(int index) {
        if (index >= 0 && index < lineViews.size() && svLyrics != null) {
            TextView curr = lineViews.get(index);
            svLyrics.post(() -> {
                int scrollTo = curr.getTop() - (svLyrics.getHeight() / 2) + (curr.getHeight() / 2);
                svLyrics.smoothScrollTo(0, Math.max(0, scrollTo));
            });
        }
    }

    private String getFileNameFromUri(Uri uri) {
        String name = null;
        if (getContext() != null && "content".equals(uri.getScheme())) {
            try (Cursor cursor = getContext().getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (idx >= 0) name = cursor.getString(idx);
                }
            } catch (Exception ignored) {}
        }
        if (name == null) {
            name = uri.getLastPathSegment();
        }
        return name != null ? name : "selected.lrc";
    }
}
