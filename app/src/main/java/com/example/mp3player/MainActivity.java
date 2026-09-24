package com.example.mp3player;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import com.example.mp3player.model.Song;
import com.example.mp3player.repository.MusicRepository;
import com.example.mp3player.service.MusicPlaybackService;
import com.example.mp3player.util.AlbumArtHelper;

import java.util.List;

public class MainActivity extends AppCompatActivity implements MusicPlaybackService.PlaybackListener {

    private static final int PERMISSION_REQUEST_CODE = 200;

    private ViewPager2 viewPager;
    private BottomNavigationView bottomNavigation;
    private MusicPlaybackService playbackService;
    private boolean isBound = false;

    // Bottom-right floating mini player views
    private View cardMiniPlayer;
    private ImageView ivMiniAlbumArt;
    private TextView tvMiniTitle;
    private TextView tvMiniArtist;
    private ImageButton btnMiniPlayPause;
    private ImageButton btnMiniNext;
    private ProgressBar pbMiniProgress;
    private ObjectAnimator miniVinylAnim;
    private Song currentPlayingSong;

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            MusicPlaybackService.MusicBinder musicBinder = (MusicPlaybackService.MusicBinder) binder;
            playbackService = musicBinder.getService();
            isBound = true;
            checkPermissionAndLoadMusic();

            playbackService.addListener(MainActivity.this);
            Song current = playbackService.getQueueManager().getCurrentSong();
            updateMiniPlayerUI(current, playbackService.isPlaying());
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            if (playbackService != null) {
                playbackService.removeListener(MainActivity.this);
            }
            playbackService = null;
            isBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        com.example.mp3player.util.ThemeManager.applySavedTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        viewPager = findViewById(R.id.viewPager);
        bottomNavigation = findViewById(R.id.bottomNavigation);

        // Bind Mini-Player views
        cardMiniPlayer = findViewById(R.id.cardMiniPlayer);
        ivMiniAlbumArt = findViewById(R.id.ivMiniAlbumArt);
        tvMiniTitle = findViewById(R.id.tvMiniTitle);
        tvMiniArtist = findViewById(R.id.tvMiniArtist);
        btnMiniPlayPause = findViewById(R.id.btnMiniPlayPause);
        btnMiniNext = findViewById(R.id.btnMiniNext);
        pbMiniProgress = findViewById(R.id.pbMiniProgress);

        ViewPagerAdapter adapter = new ViewPagerAdapter(this);
        viewPager.setAdapter(adapter);
        // Disable outer swiping so in-player horizontal gestures work cleanly without tab conflict
        viewPager.setUserInputEnabled(false);

        // Start on Page 1 (The Center Player Screen, matching Image 2)
        viewPager.setCurrentItem(1, false);
        if (bottomNavigation != null) {
            bottomNavigation.setSelectedItemId(R.id.nav_player);
        }

        setupBottomNavigation();
        setupMiniPlayer();

        // Start and bind the music service
        Intent serviceIntent = new Intent(this, MusicPlaybackService.class);
        startService(serviceIntent);
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private void setupBottomNavigation() {
        if (bottomNavigation == null || viewPager == null) return;

        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                viewPager.setCurrentItem(0, true);
                return true;
            } else if (itemId == R.id.nav_player) {
                viewPager.setCurrentItem(1, true);
                return true;
            } else if (itemId == R.id.nav_settings) {
                viewPager.setCurrentItem(2, true);
                return true;
            }
            return false;
        });

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                int navId = R.id.nav_player;
                if (position == 0) navId = R.id.nav_home;
                else if (position == 2) navId = R.id.nav_settings;

                if (bottomNavigation.getSelectedItemId() != navId) {
                    bottomNavigation.setSelectedItemId(navId);
                }

                updateMiniPlayerVisibility();
            }
        });
    }

    private void setupMiniPlayer() {
        if (cardMiniPlayer == null) return;

        // Tapping mini player tab smoothly opens full player page
        cardMiniPlayer.setOnClickListener(v -> switchToPlayerPage());

        btnMiniPlayPause.setOnClickListener(v -> {
            if (playbackService != null) {
                playbackService.togglePlayPause();
            }
        });

        btnMiniNext.setOnClickListener(v -> {
            if (playbackService != null) {
                playbackService.next();
            }
        });

        if (ivMiniAlbumArt != null) {
            miniVinylAnim = ObjectAnimator.ofFloat(ivMiniAlbumArt, "rotation", 0f, 360f);
            miniVinylAnim.setDuration(16000);
            miniVinylAnim.setInterpolator(new LinearInterpolator());
            miniVinylAnim.setRepeatCount(ObjectAnimator.INFINITE);
        }
    }

    private void updateMiniPlayerUI(@Nullable Song song, boolean isPlaying) {
        currentPlayingSong = song;

        if (song != null) {
            if (tvMiniTitle != null) tvMiniTitle.setText(song.getTitle());
            if (tvMiniArtist != null) tvMiniArtist.setText(song.getArtist());
            if (ivMiniAlbumArt != null) {
                Bitmap art = AlbumArtHelper.getAlbumArt(this, song);
                ivMiniAlbumArt.setImageBitmap(art);
            }
            updateMiniPlayerPlaybackState(isPlaying);
        }

        updateMiniPlayerVisibility();
    }

    private void updateMiniPlayerPlaybackState(boolean isPlaying) {
        if (btnMiniPlayPause != null) {
            btnMiniPlayPause.setImageResource(isPlaying ? R.drawable.ic_pause_modern : R.drawable.ic_play_modern);
        }

        if (miniVinylAnim != null) {
            if (isPlaying) {
                if (miniVinylAnim.isPaused()) {
                    miniVinylAnim.resume();
                } else if (!miniVinylAnim.isStarted()) {
                    miniVinylAnim.start();
                }
            } else {
                if (miniVinylAnim.isRunning()) {
                    miniVinylAnim.pause();
                }
            }
        }
    }

    private void updateMiniPlayerVisibility() {
        if (cardMiniPlayer == null) return;

        int currentTab = (viewPager != null) ? viewPager.getCurrentItem() : 1;
        // Show mini player in bottom right corner ONLY when NOT on the Player tab (tab 1) and music is active
        boolean shouldShow = (currentPlayingSong != null) && (currentTab != 1);

        cardMiniPlayer.setVisibility(shouldShow ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onSongChanged(@Nullable Song song) {
        runOnUiThread(() -> updateMiniPlayerUI(song, playbackService != null && playbackService.isPlaying()));
    }

    @Override
    public void onPlaybackStateChanged(boolean isPlaying) {
        runOnUiThread(() -> {
            updateMiniPlayerPlaybackState(isPlaying);
            updateMiniPlayerVisibility();
        });
    }

    @Override
    public void onProgressUpdate(int currentPositionMs, int durationMs) {
        if (pbMiniProgress != null && durationMs > 0) {
            int progress = (int) ((currentPositionMs * 1000L) / durationMs);
            pbMiniProgress.setProgress(progress);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (playbackService != null) {
            playbackService.removeListener(this);
        }
        if (isBound) {
            unbindService(serviceConnection);
            isBound = false;
        }
    }

    public MusicPlaybackService getPlaybackService() {
        return playbackService;
    }

    public void playSongAtIndex(int index) {
        if (playbackService != null) {
            playbackService.playSong(index);
        }
    }

    public void switchToPlayerPage() {
        if (viewPager != null) {
            viewPager.setCurrentItem(1, true);
        }
        if (bottomNavigation != null) {
            bottomNavigation.setSelectedItemId(R.id.nav_player);
        }
    }

    public void switchToHomePage() {
        if (viewPager != null) {
            viewPager.setCurrentItem(0, true);
        }
        if (bottomNavigation != null) {
            bottomNavigation.setSelectedItemId(R.id.nav_home);
        }
    }

    public void switchToQueuePage() {
        if (viewPager != null) {
            viewPager.setCurrentItem(0, true);
        }
        if (bottomNavigation != null) {
            bottomNavigation.setSelectedItemId(R.id.nav_home);
        }
    }

    public void switchToSettingsPage() {
        if (viewPager != null) {
            viewPager.setCurrentItem(2, true);
        }
        if (bottomNavigation != null) {
            bottomNavigation.setSelectedItemId(R.id.nav_settings);
        }
    }

    private void checkPermissionAndLoadMusic() {
        String permission;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permission = Manifest.permission.READ_MEDIA_AUDIO;
        } else {
            permission = Manifest.permission.READ_EXTERNAL_STORAGE;
        }

        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{permission}, PERMISSION_REQUEST_CODE);
        } else {
            loadMusicIntoService();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            loadMusicIntoService();
        }
    }

    private void loadMusicIntoService() {
        if (playbackService != null) {
            List<Song> songs = MusicRepository.loadSongs(this);
            playbackService.getQueueManager().setQueue(songs, 0);
            Toast.makeText(this, "Loaded " + songs.size() + " songs", Toast.LENGTH_SHORT).show();
            if (!songs.isEmpty()) {
                updateMiniPlayerUI(songs.get(0), false);
            }
        }
    }
}
