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
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import com.example.mp3player.model.Song;
import com.example.mp3player.repository.MusicRepository;
import com.example.mp3player.service.MusicPlaybackService;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 200;
    private final String[] themeOptions = {"Light Mode", "Dark Mode", "Device Default"};

    private ViewPager2 viewPager;
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

        setSupportActionBar(findViewById(R.id.toolbar));

        viewPager = findViewById(R.id.viewPager);
        TabLayout tabDots = findViewById(R.id.tabDots);

        ViewPagerAdapter adapter = new ViewPagerAdapter(this);
        viewPager.setAdapter(adapter);

        // Start on Page 1 (The Middle Player Screen)
        viewPager.setCurrentItem(1, false);

        // Connect the dots indicator to the swiping action
        new TabLayoutMediator(tabDots, viewPager, (tab, position) -> {}).attach();

        // Start and bind the music service
        Intent serviceIntent = new Intent(this, MusicPlaybackService.class);
        startService(serviceIntent);
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
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
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.top_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_theme) {
            showThemeDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showThemeDialog() {
        int currentSetting = 2; // Default to System
        int mode = AppCompatDelegate.getDefaultNightMode();
        if (mode == AppCompatDelegate.MODE_NIGHT_NO) currentSetting = 0;
        else if (mode == AppCompatDelegate.MODE_NIGHT_YES) currentSetting = 1;

        new AlertDialog.Builder(this)
                .setTitle(R.string.theme_settings)
                .setSingleChoiceItems(themeOptions, currentSetting, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                            break;
                        case 1:
                            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                            break;
                        case 2:
                            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                            break;
                    }
                    dialog.dismiss();
                })
                .setNegativeButton(R.string.close, null)
                .show();
    }
}

