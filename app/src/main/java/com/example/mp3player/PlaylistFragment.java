package com.example.mp3player;

import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mp3player.model.Song;
import com.example.mp3player.service.MusicPlaybackService;

import java.util.ArrayList;
import java.util.List;

public class PlaylistFragment extends Fragment implements MusicPlaybackService.PlaybackListener {

    private RecyclerView rvQueue;
    private SongAdapter adapter;
    private final List<Song> songList = new ArrayList<>();
    private int currentPlayingIndex = -1;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_playlist, container, false);
        rvQueue = view.findViewById(R.id.rvQueue);
        rvQueue.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new SongAdapter();
        rvQueue.setAdapter(adapter);
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
                updateList(service.getQueueManager().getQueue(), service.getQueueManager().getCurrentIndex());
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

    public void updateList(List<Song> songs, int activeIndex) {
        songList.clear();
        if (songs != null) {
            songList.addAll(songs);
        }
        currentPlayingIndex = activeIndex;
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
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
            Song song = songList.get(position);
            holder.tvNumber.setText(String.valueOf(position + 1));
            holder.tvTitle.setText(song.getTitle());
            holder.tvArtist.setText(song.getArtist());
            holder.tvDuration.setText(song.getFormattedDuration());

            boolean isCurrent = (position == currentPlayingIndex);
            if (isCurrent) {
                holder.tvTitle.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.primary));
                holder.tvTitle.setTypeface(null, Typeface.BOLD);
                holder.tvNumber.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.primary));
            } else {
                holder.tvTitle.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.text_primary_light));
                holder.tvTitle.setTypeface(null, Typeface.NORMAL);
                holder.tvNumber.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.text_secondary_light));
            }

            holder.itemView.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity) {
                    MainActivity activity = (MainActivity) getActivity();
                    activity.playSongAtIndex(holder.getAdapterPosition());
                    activity.switchToPlayerPage();
                }
            });
        }

        @Override
        public int getItemCount() {
            return songList.size();
        }

        class SongViewHolder extends RecyclerView.ViewHolder {
            TextView tvNumber, tvTitle, tvArtist, tvDuration;

            SongViewHolder(@NonNull View itemView) {
                super(itemView);
                tvNumber = itemView.findViewById(R.id.tvTrackNumber);
                tvTitle = itemView.findViewById(R.id.tvItemTitle);
                tvArtist = itemView.findViewById(R.id.tvItemArtist);
                tvDuration = itemView.findViewById(R.id.tvItemDuration);
            }
        }
    }
}

