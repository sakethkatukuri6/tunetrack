package com.example.mp3player;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mp3player.model.Song;
import com.example.mp3player.service.MusicPlaybackService;
import com.example.mp3player.service.QueueManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CurrentQueueFragment extends Fragment implements MusicPlaybackService.PlaybackListener {

    private RecyclerView rvCurrentQueue;
    private TextView tvQueueTrackCount;
    private Button btnClearQueue;
    private TextView tvEmptyQueue;
    private QueueAdapter adapter;

    private final List<Song> queueSongs = new ArrayList<>();
    private int currentPlayingIndex = -1;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_current_queue, container, false);

        rvCurrentQueue = view.findViewById(R.id.rvCurrentQueue);
        tvQueueTrackCount = view.findViewById(R.id.tvQueueTrackCount);
        btnClearQueue = view.findViewById(R.id.btnClearQueue);
        tvEmptyQueue = view.findViewById(R.id.tvEmptyQueue);

        rvCurrentQueue.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new QueueAdapter();
        rvCurrentQueue.setAdapter(adapter);

        btnClearQueue.setOnClickListener(v -> promptClearQueue());

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
                refreshQueue(service);
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

    public void refreshQueue(@Nullable MusicPlaybackService service) {
        if (!isAdded() || getContext() == null) return;

        if (service == null && getActivity() instanceof MainActivity) {
            service = ((MainActivity) getActivity()).getPlaybackService();
        }
        if (service == null) return;

        QueueManager qm = service.getQueueManager();
        List<Song> currentQueue = qm.getQueue();
        currentPlayingIndex = qm.getCurrentIndex();

        queueSongs.clear();
        if (currentQueue != null) {
            queueSongs.addAll(currentQueue);
        }

        if (queueSongs.isEmpty()) {
            tvEmptyQueue.setVisibility(View.VISIBLE);
            rvCurrentQueue.setVisibility(View.GONE);
            tvQueueTrackCount.setText("0 tracks in queue");
        } else {
            tvEmptyQueue.setVisibility(View.GONE);
            rvCurrentQueue.setVisibility(View.VISIBLE);
            tvQueueTrackCount.setText(String.format(Locale.getDefault(), "%d tracks in queue", queueSongs.size()));
        }

        adapter.notifyDataSetChanged();
    }

    private void promptClearQueue() {
        if (queueSongs.isEmpty()) return;

        new AlertDialog.Builder(requireContext())
                .setTitle("Clear Queue")
                .setMessage("Are you sure you want to clear all tracks from the current playback queue?")
                .setPositiveButton("Clear", (dialog, which) -> {
                    if (getActivity() instanceof MainActivity) {
                        MusicPlaybackService service = ((MainActivity) getActivity()).getPlaybackService();
                        if (service != null) {
                            service.pause();
                            service.getQueueManager().clear();
                            refreshQueue(service);
                        }
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onSongChanged(@Nullable Song song) {
        if (!isAdded() || getActivity() == null) return;
        getActivity().runOnUiThread(() -> {
            if (getActivity() instanceof MainActivity) {
                MusicPlaybackService service = ((MainActivity) getActivity()).getPlaybackService();
                if (service != null) {
                    refreshQueue(service);
                }
            }
        });
    }

    @Override
    public void onPlaybackStateChanged(boolean isPlaying) {
        if (!isAdded() || getActivity() == null) return;
        getActivity().runOnUiThread(() -> adapter.notifyDataSetChanged());
    }

    @Override
    public void onProgressUpdate(int currentPositionMs, int durationMs) {
        // Not needed for queue list
    }

    private class QueueAdapter extends RecyclerView.Adapter<QueueAdapter.QueueViewHolder> {

        @NonNull
        @Override
        public QueueViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_queue_song, parent, false);
            return new QueueViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull QueueViewHolder holder, int position) {
            Song song = queueSongs.get(position);
            boolean isCurrent = (position == currentPlayingIndex);

            holder.tvTitle.setText(song.getTitle());
            holder.tvArtist.setText(song.getArtist());
            holder.tvDuration.setText(song.getFormattedDuration());

            Context ctx = holder.itemView.getContext();

            if (isCurrent) {
                holder.tvTrackNumber.setVisibility(View.GONE);
                holder.ivPlayingIndicator.setVisibility(View.VISIBLE);
                holder.tvTitle.setTextColor(ContextCompat.getColor(ctx, R.color.primary));
                holder.tvTitle.setTypeface(null, Typeface.BOLD);
            } else {
                holder.tvTrackNumber.setVisibility(View.VISIBLE);
                holder.ivPlayingIndicator.setVisibility(View.GONE);
                holder.tvTrackNumber.setText(String.valueOf(position + 1));
                holder.tvTitle.setTextColor(ContextCompat.getColor(ctx, R.color.text_primary));
                holder.tvTitle.setTypeface(null, Typeface.NORMAL);
            }

            holder.itemView.setOnClickListener(v -> {
                int pos = holder.getBindingAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && getActivity() instanceof MainActivity) {
                    MainActivity act = (MainActivity) getActivity();
                    MusicPlaybackService s = act.getPlaybackService();
                    if (s != null) {
                        s.playSong(pos);
                    }
                }
            });

            holder.btnRemove.setOnClickListener(v -> {
                int pos = holder.getBindingAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && getActivity() instanceof MainActivity) {
                    MusicPlaybackService s = ((MainActivity) getActivity()).getPlaybackService();
                    if (s != null) {
                        s.getQueueManager().removeSongAt(pos);
                        refreshQueue(s);
                    }
                }
            });
        }

        @Override
        public int getItemCount() {
            return queueSongs.size();
        }

        class QueueViewHolder extends RecyclerView.ViewHolder {
            TextView tvTrackNumber, tvTitle, tvArtist, tvDuration;
            ImageView ivPlayingIndicator;
            ImageButton btnRemove;

            QueueViewHolder(View itemView) {
                super(itemView);
                tvTrackNumber = itemView.findViewById(R.id.tvQueueTrackNumber);
                ivPlayingIndicator = itemView.findViewById(R.id.ivQueuePlayingIndicator);
                tvTitle = itemView.findViewById(R.id.tvQueueSongTitle);
                tvArtist = itemView.findViewById(R.id.tvQueueSongArtist);
                tvDuration = itemView.findViewById(R.id.tvQueueDuration);
                btnRemove = itemView.findViewById(R.id.btnQueueRemove);
            }
        }
    }
}

