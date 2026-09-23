package com.example.mp3player;

import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mp3player.model.Playlist;
import com.example.mp3player.model.Song;
import com.example.mp3player.repository.PlaylistManager;
import com.example.mp3player.service.MusicPlaybackService;

import java.util.ArrayList;
import java.util.List;

public class PlaylistFragment extends Fragment implements MusicPlaybackService.PlaybackListener {

    private RecyclerView rvQueue;
    private SongAdapter adapter;
    private LinearLayout llPlaylistChips;
    private TextView tvEmptyPlaylist;
    private Button btnCreatePlaylist;

    private final List<Song> allSongs = new ArrayList<>();
    private final List<Song> displayedSongs = new ArrayList<>();
    private int currentPlayingIndex = -1;
    private String selectedPlaylistId = "all_songs"; // "all_songs" or custom playlist ID

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_playlist, container, false);
        rvQueue = view.findViewById(R.id.rvQueue);
        llPlaylistChips = view.findViewById(R.id.llPlaylistChips);
        tvEmptyPlaylist = view.findViewById(R.id.tvEmptyPlaylist);
        btnCreatePlaylist = view.findViewById(R.id.btnCreatePlaylist);

        rvQueue.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new SongAdapter();
        rvQueue.setAdapter(adapter);

        btnCreatePlaylist.setOnClickListener(v -> showCreatePlaylistDialog());

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof MainActivity) {
            MainActivity activity = (MainActivity) getActivity();
            MusicPlaybackService service = activity.getPlaybackService();
            if (service != null) {
                service.addListener(this);
                allSongs.clear();
                allSongs.addAll(service.getQueueManager().getQueue());
                currentPlayingIndex = service.getQueueManager().getCurrentIndex();
                refreshChipsAndList();
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (getActivity() instanceof MainActivity) {
            MusicPlaybackService service = ((MainActivity) getActivity()).getPlaybackService();
            if (service != null) {
                service.removeListener(this);
            }
        }
    }

    private void showCreatePlaylistDialog() {
        if (getContext() == null) return;

        EditText input = new EditText(getContext());
        input.setHint("Playlist Name (e.g. Chill Beats)");
        int pad = (int) (18 * getResources().getDisplayMetrics().density);
        input.setPadding(pad, pad, pad, pad);
        input.setTextColor(Color.WHITE);
        input.setHintTextColor(Color.GRAY);

        new AlertDialog.Builder(getContext())
                .setTitle("Create New Playlist")
                .setView(input)
                .setPositiveButton("Create", (dialog, which) -> {
                    String name = input.getText().toString().trim();
                    if (!name.isEmpty()) {
                        Playlist created = PlaylistManager.createPlaylist(requireContext(), name);
                        selectedPlaylistId = created.getId();
                        refreshChipsAndList();
                        Toast.makeText(getContext(), "Created playlist: " + name, Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void refreshChipsAndList() {
        if (getContext() == null || llPlaylistChips == null) return;

        llPlaylistChips.removeAllViews();
        List<Playlist> playlists = PlaylistManager.getPlaylists(getContext());

        // 1. "All Songs" Chip
        addChip("All Songs (" + allSongs.size() + ")", "all_songs");

        // 2. Custom Playlist Chips
        for (Playlist p : playlists) {
            int count = 0;
            for (Song s : allSongs) {
                if (p.containsSongId(s.getId())) count++;
            }
            addChip(p.getName() + " (" + count + ")", p.getId());
        }

        // Filter displayed songs based on selected playlist
        displayedSongs.clear();
        if ("all_songs".equals(selectedPlaylistId)) {
            displayedSongs.addAll(allSongs);
        } else {
            Playlist selectedP = null;
            for (Playlist p : playlists) {
                if (p.getId().equals(selectedPlaylistId)) {
                    selectedP = p;
                    break;
                }
            }
            if (selectedP != null) {
                for (Song s : allSongs) {
                    if (selectedP.containsSongId(s.getId())) {
                        displayedSongs.add(s);
                    }
                }
            }
        }

        tvEmptyPlaylist.setVisibility(displayedSongs.isEmpty() ? View.VISIBLE : View.GONE);
        adapter.notifyDataSetChanged();
    }

    private void addChip(String label, String id) {
        if (getContext() == null) return;

        TextView chip = new TextView(getContext());
        chip.setText(label);
        chip.setTextSize(13);
        chip.setPadding(32, 16, 32, 16);

        boolean isSelected = id.equals(selectedPlaylistId);
        chip.setBackgroundResource(isSelected ? R.drawable.bg_chip_selected : R.drawable.bg_chip_unselected);
        chip.setTextColor(isSelected ? Color.BLACK : Color.WHITE);
        chip.setTypeface(null, isSelected ? Typeface.BOLD : Typeface.NORMAL);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMarginEnd((int) (10 * getResources().getDisplayMetrics().density));
        chip.setLayoutParams(params);

        chip.setOnClickListener(v -> {
            selectedPlaylistId = id;
            refreshChipsAndList();
        });

        // Long press to delete custom playlist (not All Songs or Favorites)
        if (!"all_songs".equals(id) && !"favorites_id".equals(id)) {
            chip.setOnLongClickListener(v -> {
                new AlertDialog.Builder(requireContext())
                        .setTitle("Delete Playlist")
                        .setMessage("Are you sure you want to delete '" + label + "'?")
                        .setPositiveButton("Delete", (dialog, which) -> {
                            PlaylistManager.deletePlaylist(requireContext(), id);
                            selectedPlaylistId = "all_songs";
                            refreshChipsAndList();
                            Toast.makeText(getContext(), "Playlist deleted", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
                return true;
            });
        }

        llPlaylistChips.addView(chip);
    }

    private void showAddToPlaylistDialog(Song song) {
        if (getContext() == null) return;

        List<Playlist> playlists = PlaylistManager.getPlaylists(getContext());
        String[] names = new String[playlists.size() + 1];
        names[0] = "+ Create New Playlist";
        for (int i = 0; i < playlists.size(); i++) {
            names[i + 1] = playlists.get(i).getName();
        }

        new AlertDialog.Builder(getContext())
                .setTitle("Add '" + song.getTitle() + "' to...")
                .setItems(names, (dialog, which) -> {
                    if (which == 0) {
                        showCreatePlaylistDialog();
                    } else {
                        Playlist target = playlists.get(which - 1);
                        PlaylistManager.addSongToPlaylist(requireContext(), target.getId(), song.getId());
                        refreshChipsAndList();
                        Toast.makeText(getContext(), "Added to " + target.getName(), Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }

    @Override
    public void onSongChanged(@Nullable Song song) {
        if (getActivity() instanceof MainActivity) {
            MusicPlaybackService service = ((MainActivity) getActivity()).getPlaybackService();
            if (service != null) {
                currentPlayingIndex = service.getQueueManager().getCurrentIndex();
                if (adapter != null) {
                    adapter.notifyDataSetChanged();
                }
            }
        }
    }

    @Override
    public void onPlaybackStateChanged(boolean isPlaying) {}

    @Override
    public void onProgressUpdate(int currentPositionMs, int durationMs) {}

    private class SongAdapter extends RecyclerView.Adapter<SongAdapter.SongViewHolder> {

        @NonNull
        @Override
        public SongViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_song, parent, false);
            return new SongViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull SongViewHolder holder, int position) {
            Song song = displayedSongs.get(position);
            holder.tvNumber.setText(String.valueOf(position + 1));
            holder.tvTitle.setText(song.getTitle());
            holder.tvArtist.setText(song.getArtist());
            holder.tvDuration.setText(song.getFormattedDuration());

            boolean isCurrent = (allSongs.indexOf(song) == currentPlayingIndex);
            if (isCurrent) {
                holder.tvTitle.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.primary));
                holder.tvTitle.setTypeface(null, Typeface.BOLD);
                holder.tvNumber.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.primary));
            } else {
                holder.tvTitle.setTextColor(Color.WHITE);
                holder.tvTitle.setTypeface(null, Typeface.NORMAL);
                holder.tvNumber.setTextColor(Color.parseColor("#80FFFFFF"));
            }

            holder.itemView.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity) {
                    MainActivity activity = (MainActivity) getActivity();
                    int originalIndex = allSongs.indexOf(song);
                    if (originalIndex >= 0) {
                        activity.playSongAtIndex(originalIndex);
                    }
                    activity.switchToPlayerPage();
                }
            });

            holder.ivMore.setOnClickListener(v -> {
                PopupMenu popup = new PopupMenu(holder.itemView.getContext(), holder.ivMore);
                popup.getMenuInflater().inflate(R.menu.song_options_menu, popup.getMenu());
                popup.setOnMenuItemClickListener(item -> {
                    if (item.getItemId() == R.id.action_play_now) {
                        int originalIndex = allSongs.indexOf(song);
                        if (originalIndex >= 0 && getActivity() instanceof MainActivity) {
                            ((MainActivity) getActivity()).playSongAtIndex(originalIndex);
                            ((MainActivity) getActivity()).switchToPlayerPage();
                        }
                        return true;
                    } else if (item.getItemId() == R.id.action_add_to_playlist) {
                        showAddToPlaylistDialog(song);
                        return true;
                    }
                    return false;
                });
                popup.show();
            });
        }

        @Override
        public int getItemCount() {
            return displayedSongs.size();
        }

        class SongViewHolder extends RecyclerView.ViewHolder {
            TextView tvNumber, tvTitle, tvArtist, tvDuration;
            ImageView ivMore;

            SongViewHolder(@NonNull View itemView) {
                super(itemView);
                tvNumber = itemView.findViewById(R.id.tvTrackNumber);
                tvTitle = itemView.findViewById(R.id.tvItemTitle);
                tvArtist = itemView.findViewById(R.id.tvItemArtist);
                tvDuration = itemView.findViewById(R.id.tvItemDuration);
                ivMore = itemView.findViewById(R.id.ivItemMore);
            }
        }
    }
}
