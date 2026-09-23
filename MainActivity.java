package com.example.mp3player;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import com.example.mp3player.model.Song;
import com.example.mp3player.repository.MusicRepository;
import com.example.mp3player.service.MusicPlaybackService;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 200;

    private ViewPager2 viewPager;
    private BottomNavigationView bottomNavigation;
    private MusicPlaybackService playbackService;
    private boolean isBound = false;

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            MusicPlaybackService.MusicBinder musicBinder = (MusicPlaybackService.MusicBinder) binder;
            playbackService = musicBinder.getService();
            isBound = true;
            checkPermissionAndLoadMusic();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            playbackService = null;
            isBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        viewPager = findViewById(R.id.viewPager);
        bottomNavigation = findViewById(R.id.bottomNavigation);

        ViewPagerAdapter adapter = new ViewPagerAdapter(this);
        viewPager.setAdapter(adapter);

        // Start on Page 1 (The Center Player Screen, matching Image 2)
        viewPager.setCurrentItem(1, false);
        if (bottomNavigation != null) {
            bottomNavigation.setSelectedItemId(R.id.nav_player);
        }

        setupBottomNavigation();

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
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
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
    }

    public void switchToQueuePage() {
        if (viewPager != null) {
            viewPager.setCurrentItem(0, true);
        }
    }

    public void switchToSettingsPage() {
        if (viewPager != null) {
            viewPager.setCurrentItem(2, true);
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
            playbackService.getQueueManager().setQueue(songs, 1); // Start on Song 2 (Neon Horizon) matching Image 2!
            Toast.makeText(this, "Loaded " + songs.size() + " songs", Toast.LENGTH_SHORT).show();
        }
    }
}
