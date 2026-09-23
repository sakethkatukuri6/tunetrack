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
                return new PlaylistFragment(); // Left Page: Queue
            case 2:
                return new LyricsFragment();   // Right Page: Live Lyrics
            case 1:
            default:
                return new PlayerFragment();   // Center Page: Main Controls
        }
    }

    @Override
    public int getItemCount() {
        return 3;
    }
}

