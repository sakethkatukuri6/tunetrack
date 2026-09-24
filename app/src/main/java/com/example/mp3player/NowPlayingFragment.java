package com.example.mp3player;

import android.animation.ObjectAnimator;
import android.app.AlertDialog;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.mp3player.lan.LanSyncManager;
import com.example.mp3player.model.Song;
import com.example.mp3player.service.MusicPlaybackService;
import com.example.mp3player.service.QueueManager;
import com.example.mp3player.util.AlbumArtHelper;
import com.example.mp3player.util.ColorExtractor;
import com.example.mp3player.util.ThemeManager;

public class NowPlayingFragment extends Fragment implements MusicPlaybackService.PlaybackListener {

    private ImageView ivAlbumArt;
    private TextView tvSongTitle;
    private TextView tvSongArtist;
    private TextView tvCurrentTime;
    private TextView tvTotalTime;
    private SeekBar seekBarProgress;
    private FrameLayout flPlayPause;
    private ImageView ivPlayPauseIcon;
    private ImageButton btnShuffle;
    private ImageButton btnPrevious;
    private ImageButton btnNext;
    private ImageButton btnRepeat;
    private ImageButton btnFavorite;
    private ImageButton btnLanShare;
    private View btnGoToQueue;
    private View btnGoToLyrics;
    private ImageView ivQueueShortcutIcon;
    private TextView tvQueueShortcutLabel;
    private ImageView ivLyricsShortcutIcon;
    private TextView tvLyricsShortcutLabel;
    private TextView tvDotSeparator;

    private ObjectAnimator vinylRotationAnim;
    private boolean isUserSeeking = false;
    private long currentSongId = -1;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_now_playing, container, false);

        ivAlbumArt = view.findViewById(R.id.ivAlbumArt);
        tvSongTitle = view.findViewById(R.id.tvSongTitle);
        tvSongArtist = view.findViewById(R.id.tvSongArtist);
        tvCurrentTime = view.findViewById(R.id.tvCurrentTime);
        tvTotalTime = view.findViewById(R.id.tvTotalTime);
        seekBarProgress = view.findViewById(R.id.seekBarProgress);
        flPlayPause = view.findViewById(R.id.flPlayPause);
        ivPlayPauseIcon = view.findViewById(R.id.ivPlayPauseIcon);
        btnShuffle = view.findViewById(R.id.btnShuffle);
        btnPrevious = view.findViewById(R.id.btnPrevious);
        btnNext = view.findViewById(R.id.btnNext);
        btnRepeat = view.findViewById(R.id.btnRepeat);
        btnFavorite = view.findViewById(R.id.btnFavorite);
        btnLanShare = view.findViewById(R.id.btnLanShare);
        tvDotSeparator = view.findViewById(R.id.tvDotSeparator);

        btnGoToQueue = view.findViewById(R.id.btnGoToQueue);
        btnGoToLyrics = view.findViewById(R.id.btnGoToLyrics);
        ivQueueShortcutIcon = view.findViewById(R.id.ivQueueShortcutIcon);
        tvQueueShortcutLabel = view.findViewById(R.id.tvQueueShortcutLabel);
        ivLyricsShortcutIcon = view.findViewById(R.id.ivLyricsShortcutIcon);
        tvLyricsShortcutLabel = view.findViewById(R.id.tvLyricsShortcutLabel);

        setupControls();
        setupShortcuts();
        setupVinylAnimation();

        return view;
    }

    private void setupVinylAnimation() {
        if (ivAlbumArt != null) {
            vinylRotationAnim = ObjectAnimator.ofFloat(ivAlbumArt, "rotation", 0f, 360f);
            vinylRotationAnim.setDuration(18000);
            vinylRotationAnim.setInterpolator(new LinearInterpolator());
            vinylRotationAnim.setRepeatCount(ObjectAnimator.INFINITE);
        }
    }

    private void setupControls() {
        flPlayPause.setOnClickListener(v -> {
            MusicPlaybackService service = getService();
            if (service != null) service.togglePlayPause();
        });

        btnPrevious.setOnClickListener(v -> {
            MusicPlaybackService service = getService();
            if (service != null) service.previous();
        });

        btnNext.setOnClickListener(v -> {
            MusicPlaybackService service = getService();
            if (service != null) service.next();
        });

        btnShuffle.setOnClickListener(v -> {
            MusicPlaybackService service = getService();
            if (service != null) {
                boolean shuffle = service.getQueueManager().toggleShuffle();
                btnShuffle.setAlpha(shuffle ? 1.0f : 0.4f);
            }
        });

        btnRepeat.setOnClickListener(v -> {
            MusicPlaybackService service = getService();
            if (service != null) {
                QueueManager.RepeatMode mode = service.getQueueManager().cycleRepeatMode();
                updateRepeatButton(mode);
            }
        });

        btnFavorite.setOnClickListener(v -> {
            MusicPlaybackService service = getService();
            if (service != null) {
                Song song = service.getQueueManager().getCurrentSong();
                if (song != null) {
                    song.setFavorite(!song.isFavorite());
                    updateFavoriteIcon(song.isFavorite());
                    Toast.makeText(getContext(), song.isFavorite() ? "Added to Favorites" : "Removed from Favorites", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnLanShare.setOnClickListener(v -> showQrModal());

        seekBarProgress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    tvCurrentTime.setText(Song.formatTime(progress));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                isUserSeeking = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                isUserSeeking = false;
                MusicPlaybackService service = getService();
                if (service != null) {
                    service.seekTo(seekBar.getProgress());
                }
            }
        });
    }

    private void setupShortcuts() {
        if (btnGoToQueue != null) {
            btnGoToQueue.setOnClickListener(v -> {
                Fragment parent = getParentFragment();
                if (parent instanceof PlayerFragment) {
                    ((PlayerFragment) parent).navigateToPage(0);
                }
            });
        }

        if (btnGoToLyrics != null) {
            btnGoToLyrics.setOnClickListener(v -> {
                Fragment parent = getParentFragment();
                if (parent instanceof PlayerFragment) {
                    ((PlayerFragment) parent).navigateToPage(2);
                }
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        MusicPlaybackService service = getService();
        if (service != null) {
            service.addListener(this);
            Song current = service.getQueueManager().getCurrentSong();
            onSongChanged(current);
            onPlaybackStateChanged(service.isPlaying());
            btnShuffle.setAlpha(service.getQueueManager().isShuffle() ? 1.0f : 0.4f);
            updateRepeatButton(service.getQueueManager().getRepeatMode());
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        MusicPlaybackService service = getService();
        if (service != null) {
            service.removeListener(this);
        }
    }

    @Nullable
    private MusicPlaybackService getService() {
        if (getActivity() instanceof MainActivity) {
            return ((MainActivity) getActivity()).getPlaybackService();
        }
        return null;
    }

    @Override
    public void onSongChanged(@Nullable Song song) {
        if (!isAdded() || getContext() == null) return;

        if (song != null) {
            tvSongTitle.setText(song.getTitle());
            tvSongArtist.setText(song.getArtist());
            tvTotalTime.setText(song.getFormattedDuration());
            seekBarProgress.setMax((int) song.getDurationMs());
            updateFavoriteIcon(song.isFavorite());

            if (song.getId() != currentSongId) {
                currentSongId = song.getId();
                Bitmap art = AlbumArtHelper.getAlbumArt(getContext(), song);
                ivAlbumArt.setImageBitmap(art);

                // Dynamic palette colors
                boolean isDark = ThemeManager.isCurrentlyDark(getContext());
                ColorExtractor.PaletteColors palette = ColorExtractor.extractColors(art, song.getTitle(), isDark);
                applyDynamicPalette(palette, isDark);

                // Notify parent PlayerFragment so container background gradient also animates
                Fragment parent = getParentFragment();
                if (parent instanceof PlayerFragment) {
                    ((PlayerFragment) parent).updatePalette(palette, isDark);
                }
            }
        } else {
            tvSongTitle.setText("No track playing");
            tvSongArtist.setText("");
            tvCurrentTime.setText("0:00");
            tvTotalTime.setText("0:00");
            seekBarProgress.setProgress(0);
        }
    }

    private void applyDynamicPalette(ColorExtractor.PaletteColors palette, boolean isDark) {
        if (getContext() == null) return;

        // Radiant Play/Pause Button Gradient
        GradientDrawable playGradient = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{palette.accentColor, palette.accentGradientEnd}
        );
        playGradient.setShape(GradientDrawable.OVAL);
        flPlayPause.setBackground(playGradient);

        // Seekbar Accent
        seekBarProgress.setProgressTintList(ColorStateList.valueOf(palette.accentColor));
        seekBarProgress.setThumbTintList(ColorStateList.valueOf(palette.accentColor));

        // Text & Icons
        int primaryText = isDark ? Color.WHITE : Color.parseColor("#0F172A");
        int secondaryText = isDark ? Color.parseColor("#B3FFFFFF") : Color.parseColor("#475569");
        int iconTint = isDark ? Color.WHITE : Color.parseColor("#1E293B");

        tvSongTitle.setTextColor(primaryText);
        tvSongArtist.setTextColor(secondaryText);
        tvCurrentTime.setTextColor(secondaryText);
        tvTotalTime.setTextColor(secondaryText);

        btnShuffle.setImageTintList(ColorStateList.valueOf(iconTint));
        btnPrevious.setImageTintList(ColorStateList.valueOf(iconTint));
        btnNext.setImageTintList(ColorStateList.valueOf(iconTint));
        btnRepeat.setImageTintList(ColorStateList.valueOf(iconTint));
        btnLanShare.setImageTintList(ColorStateList.valueOf(iconTint));

        if (ivQueueShortcutIcon != null) ivQueueShortcutIcon.setImageTintList(ColorStateList.valueOf(palette.accentColor));
        if (tvQueueShortcutLabel != null) tvQueueShortcutLabel.setTextColor(palette.accentColor);
        if (ivLyricsShortcutIcon != null) ivLyricsShortcutIcon.setImageTintList(ColorStateList.valueOf(palette.accentColor));
        if (tvLyricsShortcutLabel != null) tvLyricsShortcutLabel.setTextColor(palette.accentColor);
        if (tvDotSeparator != null) tvDotSeparator.setTextColor(palette.accentColor);
    }

    private void updateFavoriteIcon(boolean isFavorite) {
        if (btnFavorite != null) {
            btnFavorite.setImageResource(isFavorite ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline);
            btnFavorite.setImageTintList(isFavorite ? ColorStateList.valueOf(0xFFFF3B69) : ColorStateList.valueOf(0x80FFFFFF));
        }
    }

    private void updateRepeatButton(QueueManager.RepeatMode mode) {
        if (btnRepeat != null) {
            btnRepeat.setAlpha(mode == QueueManager.RepeatMode.OFF ? 0.4f : 1.0f);
        }
    }

    private void showQrModal() {
        if (getContext() == null) return;
        String serverUrl = LanSyncManager.getServerUrl(requireContext());
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.lan_audio_cast)
                .setMessage(getString(R.string.lan_cast_instructions) + "\n\nServer: " + serverUrl + "\n\nAny browser on Wi-Fi can connect to stream and sync live audio.")
                .setPositiveButton(R.string.done, null)
                .show();
    }

    @Override
    public void onPlaybackStateChanged(boolean isPlaying) {
        if (!isAdded()) return;

        ivPlayPauseIcon.setImageResource(isPlaying ? R.drawable.ic_pause_modern : R.drawable.ic_play_modern);

        if (vinylRotationAnim != null) {
            if (isPlaying) {
                if (vinylRotationAnim.isPaused()) {
                    vinylRotationAnim.resume();
                } else if (!vinylRotationAnim.isStarted()) {
                    vinylRotationAnim.start();
                }
            } else {
                if (vinylRotationAnim.isRunning()) {
                    vinylRotationAnim.pause();
                }
            }
        }
    }

    @Override
    public void onProgressUpdate(int currentPositionMs, int durationMs) {
        if (!isAdded() || isUserSeeking) return;

        seekBarProgress.setProgress(currentPositionMs);
        tvCurrentTime.setText(Song.formatTime(currentPositionMs));
        if (durationMs > 0 && seekBarProgress.getMax() != durationMs) {
            seekBarProgress.setMax(durationMs);
            tvTotalTime.setText(Song.formatTime(durationMs));
        }
    }
}

