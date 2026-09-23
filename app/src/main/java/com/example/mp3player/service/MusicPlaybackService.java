package com.example.mp3player.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.mp3player.MainActivity;
import com.example.mp3player.R;
import com.example.mp3player.model.Song;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MusicPlaybackService extends Service implements
        MediaPlayer.OnPreparedListener,
        MediaPlayer.OnCompletionListener,
        MediaPlayer.OnErrorListener,
        AudioManager.OnAudioFocusChangeListener {

    public static final String ACTION_PLAY = "com.example.mp3player.ACTION_PLAY";
    public static final String ACTION_PAUSE = "com.example.mp3player.ACTION_PAUSE";
    public static final String ACTION_TOGGLE = "com.example.mp3player.ACTION_TOGGLE";
    public static final String ACTION_NEXT = "com.example.mp3player.ACTION_NEXT";
    public static final String ACTION_PREV = "com.example.mp3player.ACTION_PREV";

    private static final String CHANNEL_ID = "music_player_channel";
    private static final int NOTIFICATION_ID = 101;

    public interface PlaybackListener {
        void onSongChanged(@Nullable Song song);
        void onPlaybackStateChanged(boolean isPlaying);
        void onProgressUpdate(int currentPositionMs, int durationMs);
    }

    private final IBinder binder = new MusicBinder();
    private final QueueManager queueManager = new QueueManager();
    private final List<PlaybackListener> listeners = new ArrayList<>();

    private MediaPlayer mediaPlayer;
    private AudioManager audioManager;
    private AudioFocusRequest audioFocusRequest;
    private boolean hasAudioFocus = false;

    private final Handler progressHandler = new Handler(Looper.getMainLooper());
    private final Runnable progressRunnable = new Runnable() {
        @Override
        public void run() {
            if (mediaPlayer != null && isPlaying()) {
                int pos = mediaPlayer.getCurrentPosition();
                int dur = mediaPlayer.getDuration();
                notifyProgress(pos, dur);
            }
            progressHandler.postDelayed(this, 500);
        }
    };

    public class MusicBinder extends Binder {
        public MusicPlaybackService getService() {
            return MusicPlaybackService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        createNotificationChannel();
        initMediaPlayer();
        progressHandler.post(progressRunnable);
    }

    private void initMediaPlayer() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .build());
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setOnCompletionListener(this);
        mediaPlayer.setOnErrorListener(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            String action = intent.getAction();
            switch (action) {
                case ACTION_PLAY:
                    play();
                    break;
                case ACTION_PAUSE:
                    pause();
                    break;
                case ACTION_TOGGLE:
                    togglePlayPause();
                    break;
                case ACTION_NEXT:
                    next();
                    break;
                case ACTION_PREV:
                    previous();
                    break;
            }
        }
        return START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public QueueManager getQueueManager() {
        return queueManager;
    }

    public void addListener(PlaybackListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
            listener.onSongChanged(queueManager.getCurrentSong());
            listener.onPlaybackStateChanged(isPlaying());
        }
    }

    public void removeListener(PlaybackListener listener) {
        listeners.remove(listener);
    }

    public void playSong(int index) {
        queueManager.setCurrentIndex(index);
        Song song = queueManager.getCurrentSong();
        if (song == null) return;

        if (!requestAudioFocus()) {
            return;
        }

        try {
            mediaPlayer.reset();
            Uri uri = song.getUri();
            if (uri != null) {
                mediaPlayer.setDataSource(this, uri);
            } else if (song.getDataPath() != null) {
                mediaPlayer.setDataSource(song.getDataPath());
            } else {
                return;
            }
            mediaPlayer.prepareAsync();
            notifySongChanged(song);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void play() {
        if (mediaPlayer != null) {
            if (!mediaPlayer.isPlaying()) {
                if (requestAudioFocus()) {
                    mediaPlayer.start();
                    notifyPlaybackState(true);
                    updateNotification(queueManager.getCurrentSong(), true);
                }
            }
        } else if (queueManager.hasSongs()) {
            playSong(queueManager.getCurrentIndex() >= 0 ? queueManager.getCurrentIndex() : 0);
        }
    }

    public void pause() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            notifyPlaybackState(false);
            updateNotification(queueManager.getCurrentSong(), false);
        }
    }

    public void togglePlayPause() {
        if (isPlaying()) {
            pause();
        } else {
            play();
        }
    }

    public void next() {
        int nextIndex = queueManager.getNextIndex();
        if (nextIndex >= 0) {
            playSong(nextIndex);
        } else {
            pause();
        }
    }

    public void previous() {
        if (mediaPlayer != null && mediaPlayer.getCurrentPosition() > 3000) {
            seekTo(0);
            return;
        }
        int prevIndex = queueManager.getPreviousIndex();
        if (prevIndex >= 0) {
            playSong(prevIndex);
        }
    }

    public void seekTo(int positionMs) {
        if (mediaPlayer != null) {
            mediaPlayer.seekTo(positionMs);
        }
    }

    public boolean isPlaying() {
        return mediaPlayer != null && mediaPlayer.isPlaying();
    }

    public int getCurrentPosition() {
        if (mediaPlayer != null) {
            try {
                return mediaPlayer.getCurrentPosition();
            } catch (IllegalStateException ignored) {}
        }
        return 0;
    }

    public int getDuration() {
        if (mediaPlayer != null) {
            try {
                return mediaPlayer.getDuration();
            } catch (IllegalStateException ignored) {}
        }
        Song current = queueManager.getCurrentSong();
        return current != null ? (int) current.getDurationMs() : 0;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        mp.start();
        notifyPlaybackState(true);
        updateNotification(queueManager.getCurrentSong(), true);
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        notifyPlaybackState(false);
        if (queueManager.getRepeatMode() == QueueManager.RepeatMode.ONE) {
            mp.start();
            notifyPlaybackState(true);
        } else {
            next();
        }
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        notifyPlaybackState(false);
        initMediaPlayer();
        return true;
    }

    private boolean requestAudioFocus() {
        if (hasAudioFocus) return true;
        int res;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(new AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build())
                    .setOnAudioFocusChangeListener(this)
                    .build();
            res = audioManager.requestAudioFocus(audioFocusRequest);
        } else {
            res = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        }
        hasAudioFocus = (res == AudioManager.AUDIOFOCUS_REQUEST_GRANTED);
        return hasAudioFocus;
    }

    private void abandonAudioFocus() {
        if (!hasAudioFocus) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && audioFocusRequest != null) {
            audioManager.abandonAudioFocusRequest(audioFocusRequest);
        } else {
            audioManager.abandonAudioFocus(this);
        }
        hasAudioFocus = false;
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_LOSS:
                pause();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                pause();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    mediaPlayer.setVolume(0.2f, 0.2f);
                }
                break;
            case AudioManager.AUDIOFOCUS_GAIN:
                if (mediaPlayer != null) {
                    mediaPlayer.setVolume(1.0f, 1.0f);
                    play();
                }
                break;
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Music Playback",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Controls for music player");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private void updateNotification(@Nullable Song song, boolean isPlaying) {
        if (song == null) {
            stopForeground(true);
            return;
        }

        Intent openApp = new Intent(this, MainActivity.class);
        PendingIntent pendingOpenApp = PendingIntent.getActivity(
                this, 0, openApp, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        PendingIntent pPrev = PendingIntent.getService(this, 1,
                new Intent(this, MusicPlaybackService.class).setAction(ACTION_PREV),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        PendingIntent pToggle = PendingIntent.getService(this, 2,
                new Intent(this, MusicPlaybackService.class).setAction(ACTION_TOGGLE),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        PendingIntent pNext = PendingIntent.getService(this, 3,
                new Intent(this, MusicPlaybackService.class).setAction(ACTION_NEXT),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        int playPauseIcon = isPlaying ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play;

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(song.getTitle())
                .setContentText(song.getArtist())
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setContentIntent(pendingOpenApp)
                .addAction(android.R.drawable.ic_media_previous, "Previous", pPrev)
                .addAction(playPauseIcon, isPlaying ? "Pause" : "Play", pToggle)
                .addAction(android.R.drawable.ic_media_next, "Next", pNext)
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        .setShowActionsInCompactView(0, 1, 2))
                .setOngoing(isPlaying)
                .build();

        startForeground(NOTIFICATION_ID, notification);
    }

    private void notifySongChanged(Song song) {
        for (PlaybackListener listener : new ArrayList<>(listeners)) {
            listener.onSongChanged(song);
        }
    }

    private void notifyPlaybackState(boolean isPlaying) {
        for (PlaybackListener listener : new ArrayList<>(listeners)) {
            listener.onPlaybackStateChanged(isPlaying);
        }
    }

    private void notifyProgress(int pos, int dur) {
        for (PlaybackListener listener : new ArrayList<>(listeners)) {
            listener.onProgressUpdate(pos, dur);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        progressHandler.removeCallbacks(progressRunnable);
        abandonAudioFocus();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}

