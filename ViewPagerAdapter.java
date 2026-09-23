package com.example.mp3player;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class ViewPagerAdapter extends FragmentStateAdapter {

    public ViewPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new PlaylistFragment(); // Page 0: Home / Library / Queue
            case 2:
                return new SettingsFragment(); // Page 2: Settings (Theme, LAN, Equalizer, About)
            case 1:
            default:
                return new PlayerFragment();   // Page 1: Player (Now Playing)
        }
    }

    @Override
    public int getItemCount() {
        return 3;
    }
}
