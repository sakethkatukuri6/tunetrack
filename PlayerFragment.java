package com.example.mp3player;

import android.app.AlertDialog;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.mp3player.lan.LanSyncManager;
import com.example.mp3player.model.Song;
import com.example.mp3player.service.MusicPlaybackService;
import com.example.mp3player.service.QueueManager;
import com.example.mp3player.util.AlbumArtHelper;
import com.example.mp3player.util.ColorExtractor;

public class PlayerFragment extends Fragment implements MusicPlaybackService.PlaybackListener {

    private View playerRoot;
    private View dotActive;
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
    private ImageButton btnEqualizer;
    private ImageButton btnCollapse;

    private boolean isUserSeeking = false;
    private final int[] currentBgColors = new int[]{0, 0};
    private long currentSongId = -1;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_player, container, false);

        playerRoot = view.findViewById(R.id.playerRoot);
        dotActive = view.findViewById(R.id.dotActive);
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
        btnEqualizer = view.findViewById(R.id.btnEqualizer);
        btnCollapse = view.findViewById(R.id.btnCollapse);

        setupListeners();
        return view;
    }

    private void setupListeners() {
        flPlayPause.setOnClickListener(v -> {
            MusicPlaybackService service = getService();
            if (service != null) {
                service.togglePlayPause();
            }
        });

        btnPrevious.setOnClickListener(v -> {
            MusicPlaybackService service = getService();
            if (service != null) {
                service.previous();
            }
        });

        btnNext.setOnClickListener(v -> {
            MusicPlaybackService service = getService();
            if (service != null) {
                service.next();
            }
        });

        btnShuffle.setOnClickListener(v -> {
            MusicPlaybackService service = getService();
            if (service != null) {
                boolean shuffle = service.getQueueManager().toggleShuffle();
                btnShuffle.setAlpha(shuffle ? 1.0f : 0.4f);
                Toast.makeText(getContext(), shuffle ? "Shuffle ON" : "Shuffle OFF", Toast.LENGTH_SHORT).show();
            }
        });

        btnRepeat.setOnClickListener(v -> {
            MusicPlaybackService service = getService();
            if (service != null) {
                QueueManager.RepeatMode mode = service.getQueueManager().cycleRepeatMode();
                updateRepeatButton(mode);
                Toast.makeText(getContext(), "Repeat: " + mode.name(), Toast.LENGTH_SHORT).show();
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

        btnEqualizer.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).switchToSettingsPage();
            }
        });

        btnCollapse.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                // Switch to Queue / Playlist page (page 0)
                ((MainActivity) getActivity()).switchToQueuePage();
            }
        });

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
        String serverUrl = LanSyncManager.getServerUrl(requireContext());
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.lan_audio_cast)
                .setMessage(getString(R.string.lan_cast_instructions) + "\n\nServer: " + serverUrl + "\n\nAny browser on Wi-Fi can connect to stream and sync live audio.")
                .setPositiveButton(R.string.done, null)
                .show();
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

    @Override
    public void onSongChanged(@Nullable Song song) {
        if (!isAdded() || getContext() == null) return;

        if (song != null) {
            tvSongTitle.setText(song.getTitle());
            tvSongArtist.setText(song.getArtist());
            tvTotalTime.setText(song.getFormattedDuration());
            seekBarProgress.setMax((int) song.getDurationMs());
            updateFavoriteIcon(song.isFavorite());

            // 1. Load or Generate Album Art
            Bitmap art = AlbumArtHelper.getAlbumArt(getContext(), song);
            ivAlbumArt.setImageBitmap(art);

            // 2. Extract Dynamic Palette Colors from Album Art
            ColorExtractor.PaletteColors palette = ColorExtractor.extractColors(art, song.getTitle());

            // 3. Smoothly Animate the Background Gradient to match Cover Colors!
            ColorExtractor.applyAnimatedGradient(playerRoot, palette, currentBgColors);

            // 4. Update Radiant Play/Pause Button Gradient to match Cover Palette
            GradientDrawable playGradient = new GradientDrawable(
                    GradientDrawable.Orientation.TL_BR,
                    new int[]{palette.accentColor, palette.accentGradientEnd}
            );
            playGradient.setShape(GradientDrawable.OVAL);
            flPlayPause.setBackground(playGradient);

            // 5. Update Active Page Dot Accent
            if (dotActive != null) {
                dotActive.setBackgroundTintList(ColorStateList.valueOf(palette.accentColor));
            }

            // 6. Update Seekbar Progress Color to match Cover Palette
            seekBarProgress.setProgressTintList(ColorStateList.valueOf(palette.accentColor));
            seekBarProgress.setThumbTintList(ColorStateList.valueOf(palette.accentColor));

            currentSongId = song.getId();
        } else {
            tvSongTitle.setText("Neon Horizon");
            tvSongArtist.setText("Astral Drift");
            tvCurrentTime.setText("0:00");
            tvTotalTime.setText("0:00");
            seekBarProgress.setProgress(0);
        }
    }

    @Override
    public void onPlaybackStateChanged(boolean isPlaying) {
        if (ivPlayPauseIcon != null) {
            ivPlayPauseIcon.setImageResource(isPlaying ? R.drawable.ic_pause_modern : R.drawable.ic_play_modern);
        }
    }

    @Override
    public void onProgressUpdate(int currentPositionMs, int durationMs) {
        if (!isUserSeeking) {
            seekBarProgress.setProgress(currentPositionMs);
            tvCurrentTime.setText(Song.formatTime(currentPositionMs));
            if (durationMs > 0 && seekBarProgress.getMax() != durationMs) {
                seekBarProgress.setMax(durationMs);
                tvTotalTime.setText(Song.formatTime(durationMs));
            }
        }
    }

    private MusicPlaybackService getService() {
        if (getActivity() instanceof MainActivity) {
            return ((MainActivity) getActivity()).getPlaybackService();
        }
        return null;
    }
}
