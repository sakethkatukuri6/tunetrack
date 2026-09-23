package com.example.mp3player;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class MainActivity extends AppCompatActivity {

    private final String[] themeOptions = {"Light Mode", "Dark Mode", "Device Default"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        setSupportActionBar(findViewById(R.id.toolbar));

        ViewPager2 viewPager = findViewById(R.id.viewPager);
        TabLayout tabDots = findViewById(R.id.tabDots);

        // Attach the 3-page adapter
        ViewPagerAdapter adapter = new ViewPagerAdapter(this);
        viewPager.setAdapter(adapter);
        
        // Start on Page 1 (The Middle Player Screen)
        viewPager.setCurrentItem(1, false); 

        // Connect the dots indicator to the swiping action
        new TabLayoutMediator(tabDots, viewPager, (tab, position) -> {}).attach();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Assume you have a menu XML with a settings/palette icon
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
            .setTitle("Theme Settings")
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
            .setNegativeButton("Close", null)
            .show();
    }
}