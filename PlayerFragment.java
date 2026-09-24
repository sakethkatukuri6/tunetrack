package com.example.mp3player;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.example.mp3player.util.ColorExtractor;

public class PlayerFragment extends Fragment {

    private View playerRoot;
    private ImageButton btnCollapse;
    private ImageButton btnEqualizer;
    private TextView tvPlayerHeaderTitle;

    private View dotQueue;
    private View dotPlayer;
    private View dotLyrics;
    private FrameLayout flDotQueue;
    private FrameLayout flDotPlayer;
    private FrameLayout flDotLyrics;

    private ViewPager2 vpPlayerContent;
    private int currentAccentColor = Color.parseColor("#7C3AED");
    private final int[] currentBgColors = new int[]{0, 0};

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_player, container, false);

        playerRoot = view.findViewById(R.id.playerRoot);
        btnCollapse = view.findViewById(R.id.btnCollapse);
        btnEqualizer = view.findViewById(R.id.btnEqualizer);
        tvPlayerHeaderTitle = view.findViewById(R.id.tvPlayerHeaderTitle);

        dotQueue = view.findViewById(R.id.dotQueue);
        dotPlayer = view.findViewById(R.id.dotPlayer);
        dotLyrics = view.findViewById(R.id.dotLyrics);
        flDotQueue = view.findViewById(R.id.flDotQueue);
        flDotPlayer = view.findViewById(R.id.flDotPlayer);
        flDotLyrics = view.findViewById(R.id.flDotLyrics);

        vpPlayerContent = view.findViewById(R.id.vpPlayerContent);

        setupViewPager();
        setupHeaderActions();

        return view;
    }

    private void setupViewPager() {
        vpPlayerContent.setAdapter(new FragmentStateAdapter(this) {
            @NonNull
            @Override
            public Fragment createFragment(int position) {
                switch (position) {
                    case 0:
                        return new CurrentQueueFragment();
                    case 2:
                        return new LyricsFragment();
                    case 1:
                    default:
                        return new NowPlayingFragment();
                }
            }

            @Override
            public int getItemCount() {
                return 3;
            }
        });

        // Default to Page 1 (Now Playing)
        vpPlayerContent.setCurrentItem(1, false);
        updateHeaderDots(1);

        vpPlayerContent.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                updateHeaderDots(position);
            }
        });
    }

    private void setupHeaderActions() {
        btnCollapse.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).switchToHomePage();
            }
        });

        btnEqualizer.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).switchToSettingsPage();
            }
        });

        if (flDotQueue != null) {
            flDotQueue.setOnClickListener(v -> navigateToPage(0));
        }
        if (flDotPlayer != null) {
            flDotPlayer.setOnClickListener(v -> navigateToPage(1));
        }
        if (flDotLyrics != null) {
            flDotLyrics.setOnClickListener(v -> navigateToPage(2));
        }
    }

    public void navigateToPage(int page) {
        if (vpPlayerContent != null) {
            vpPlayerContent.setCurrentItem(page, true);
        }
    }

    private void updateHeaderDots(int position) {
        if (getContext() == null) return;

        boolean isDark = (getResources().getConfiguration().uiMode &
                android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES;

        int inactiveColor = isDark ? Color.parseColor("#44FFFFFF") : Color.parseColor("#CBD5E1");

        // Update Title
        if (position == 0) {
            tvPlayerHeaderTitle.setText("CURRENT QUEUE");
        } else if (position == 2) {
            tvPlayerHeaderTitle.setText("LIVE LYRICS");
        } else {
            tvPlayerHeaderTitle.setText("NOW PLAYING");
        }

        // Update Dots size and color
        setDotState(dotQueue, position == 0, currentAccentColor, inactiveColor);
        setDotState(dotPlayer, position == 1, currentAccentColor, inactiveColor);
        setDotState(dotLyrics, position == 2, currentAccentColor, inactiveColor);
    }

    private void setDotState(View dot, boolean isActive, int activeColor, int inactiveColor) {
        if (dot == null) return;
        ViewGroup.LayoutParams params = dot.getLayoutParams();
        int sizeDp = isActive ? 8 : 6;
        int sizePx = (int) (sizeDp * getResources().getDisplayMetrics().density);
        params.width = sizePx;
        params.height = sizePx;
        dot.setLayoutParams(params);
        dot.setBackgroundTintList(ColorStateList.valueOf(isActive ? activeColor : inactiveColor));
    }

    public void updatePalette(@Nullable ColorExtractor.PaletteColors palette, boolean isDark) {
        if (!isAdded() || getContext() == null || playerRoot == null || palette == null) return;

        currentAccentColor = palette.accentColor;

        ColorExtractor.applyAnimatedGradient(playerRoot, palette, currentBgColors);

        // Update Header Icon Tints
        int iconTint = isDark ? Color.WHITE : Color.parseColor("#1E293B");
        btnCollapse.setImageTintList(ColorStateList.valueOf(iconTint));
        btnEqualizer.setImageTintList(ColorStateList.valueOf(iconTint));
        tvPlayerHeaderTitle.setTextColor(isDark ? Color.WHITE : Color.parseColor("#0F172A"));

        // Refresh dots with new accent color
        if (vpPlayerContent != null) {
            updateHeaderDots(vpPlayerContent.getCurrentItem());
        }
    }
}
