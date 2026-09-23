package com.example.mp3player;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import com.example.mp3player.lan.LanSyncManager;
import com.example.mp3player.model.Song;
import com.example.mp3player.service.MusicPlaybackService;
import com.example.mp3player.service.QueueManager;

public class PlayerFragment extends Fragment implements MusicPlaybackService.PlaybackListener {

    private TextView tvSongTitle;
    private TextView tvSongArtist;
    private TextView tvCurrentTime;
    private TextView tvTotalTime;
    private SeekBar seekBarProgress;
    private FloatingActionButton fabPlayPause;
    private ImageButton btnShuffle;
    private ImageButton btnPrevious;
    private ImageButton btnNext;
    private ImageButton btnRepeat;
    private ImageButton btnFavorite;
    private ImageButton btnLanShare;
    private ImageButton btnEqualizer;

    private boolean isUserSeeking = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_player, container, false);

        tvSongTitle = view.findViewById(R.id.tvSongTitle);
        tvSongArtist = view.findViewById(R.id.tvSongArtist);
        tvCurrentTime = view.findViewById(R.id.tvCurrentTime);
        tvTotalTime = view.findViewById(R.id.tvTotalTime);
        seekBarProgress = view.findViewById(R.id.seekBarProgress);
        fabPlayPause = view.findViewById(R.id.fabPlayPause);
        btnShuffle = view.findViewById(R.id.btnShuffle);
        btnPrevious = view.findViewById(R.id.btnPrevious);
        btnNext = view.findViewById(R.id.btnNext);
        btnRepeat = view.findViewById(R.id.btnRepeat);
        btnFavorite = view.findViewById(R.id.btnFavorite);
        btnLanShare = view.findViewById(R.id.btnLanShare);
        btnEqualizer = view.findViewById(R.id.btnEqualizer);

        setupListeners();
        return view;
    }

    private void setupListeners() {
        fabPlayPause.setOnClickListener(v -> {
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
                    btnFavorite.setSelected(song.isFavorite());
                    Toast.makeText(getContext(), song.isFavorite() ? "Added to Favorites" : "Removed from Favorites", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnLanShare.setOnClickListener(v -> showQrModal());

        btnEqualizer.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Equalizer: Balanced (Flat)", Toast.LENGTH_SHORT).show();
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

    private void updateRepeatButton(QueueManager.RepeatMode mode) {
        if (mode == QueueManager.RepeatMode.OFF) {
            btnRepeat.setAlpha(0.4f);
        } else {
            btnRepeat.setAlpha(1.0f);
        }
    }

    private void showQrModal() {
        String serverUrl = LanSyncManager.getServerUrl(requireContext());
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.lan_audio_cast)
                .setMessage(getString(R.string.lan_cast_instructions) + "\n\nLAN Cast Server:\n" + serverUrl + "\n\nAny browser on the same Wi-Fi can connect to listen and control playback.")
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
        if (song != null) {
            tvSongTitle.setText(song.getTitle());
            tvSongArtist.setText(song.getArtist());
            tvTotalTime.setText(song.getFormattedDuration());
            seekBarProgress.setMax((int) song.getDurationMs());
            btnFavorite.setSelected(song.isFavorite());
        } else {
            tvSongTitle.setText("No Track Selected");
            tvSongArtist.setText("Tap Queue to select");
            tvCurrentTime.setText("0:00");
            tvTotalTime.setText("0:00");
            seekBarProgress.setProgress(0);
        }
    }

    @Override
    public void onPlaybackStateChanged(boolean isPlaying) {
        if (isPlaying) {
            fabPlayPause.setImageResource(android.R.drawable.ic_media_pause);
        } else {
            fabPlayPause.setImageResource(android.R.drawable.ic_media_play);
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

