package com.example.mp3player.repository;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import com.example.mp3player.model.Song;

import java.util.ArrayList;
import java.util.List;

public class MusicRepository {

    public static List<Song> loadSongs(Context context) {
        List<Song> songs = new ArrayList<>();

        Uri collection;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            collection = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL);
        } else {
            collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        }

        String[] projection = new String[]{
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.DATA
        };

        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0 AND " +
                MediaStore.Audio.Media.DURATION + " >= 5000";
        String sortOrder = MediaStore.Audio.Media.TITLE + " ASC";

        try (Cursor cursor = context.getContentResolver().query(
                collection,
                projection,
                selection,
                null,
                sortOrder
        )) {
            if (cursor != null) {
                int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
                int titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE);
                int artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST);
                int albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM);
                int durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION);
                int dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);

                while (cursor.moveToNext()) {
                    long id = cursor.getLong(idColumn);
                    String title = cursor.getString(titleColumn);
                    String artist = cursor.getString(artistColumn);
                    String album = cursor.getString(albumColumn);
                    long duration = cursor.getLong(durationColumn);
                    String data = cursor.getString(dataColumn);

                    Uri contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
                    songs.add(new Song(id, title, artist, album, duration, data, contentUri.toString()));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // If no songs found on device storage, provide demo songs for immediate testing
        if (songs.isEmpty()) {
            songs.addAll(getDemoSongs());
        }

        return songs;
    }

    public static List<Song> getDemoSongs() {
        List<Song> demo = new ArrayList<>();
        // Open-source royalty-free audio tracks (SoundHelix & public domain)
        demo.add(new Song(
                1001,
                "Song 1 (Acoustic Sunrise)",
                "Artist A",
                "Morning Echoes",
                225000,
                null,
                "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3"
        ));
        demo.add(new Song(
                1002,
                "Neon Horizon",
                "Astral Drift",
                "Neon Horizon",
                184000,
                null,
                "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3"
        ));
        demo.add(new Song(
                1003,
                "Song 3 (Midnight Drive)",
                "Artist C",
                "Cyber Sunset",
                198000,
                null,
                "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3"
        ));
        return demo;
    }
}

