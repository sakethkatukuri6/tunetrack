package com.example.mp3player.repository;

import android.content.Context;
import android.content.SharedPreferences;
import com.example.mp3player.model.Playlist;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PlaylistManager {

    private static final String PREF_NAME = "music_playlists_pref";
    private static final String KEY_PLAYLISTS = "custom_playlists";

    public static List<Playlist> getPlaylists(Context context) {
        List<Playlist> list = new ArrayList<>();
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String jsonStr = prefs.getString(KEY_PLAYLISTS, null);

        if (jsonStr == null || jsonStr.trim().isEmpty()) {
            // Default "Favorites" playlist
            Playlist fav = new Playlist("favorites_id", "Favorites");
            list.add(fav);
            savePlaylists(context, list);
            return list;
        }

        try {
            JSONArray array = new JSONArray(jsonStr);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                String id = obj.getString("id");
                String name = obj.getString("name");
                Playlist p = new Playlist(id, name);
                JSONArray songs = obj.optJSONArray("songs");
                if (songs != null) {
                    for (int j = 0; j < songs.length(); j++) {
                        p.addSongId(songs.getLong(j));
                    }
                }
                list.add(p);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    public static Playlist createPlaylist(Context context, String name) {
        List<Playlist> list = getPlaylists(context);
        String id = UUID.randomUUID().toString();
        Playlist newP = new Playlist(id, name.trim());
        list.add(newP);
        savePlaylists(context, list);
        return newP;
    }

    public static void addSongToPlaylist(Context context, String playlistId, long songId) {
        List<Playlist> list = getPlaylists(context);
        for (Playlist p : list) {
            if (p.getId().equals(playlistId)) {
                p.addSongId(songId);
                break;
            }
        }
        savePlaylists(context, list);
    }

    public static void removeSongFromPlaylist(Context context, String playlistId, long songId) {
        List<Playlist> list = getPlaylists(context);
        for (Playlist p : list) {
            if (p.getId().equals(playlistId)) {
                p.removeSongId(songId);
                break;
            }
        }
        savePlaylists(context, list);
    }

    public static void deletePlaylist(Context context, String playlistId) {
        List<Playlist> list = getPlaylists(context);
        list.removeIf(p -> p.getId().equals(playlistId));
        savePlaylists(context, list);
    }

    private static void savePlaylists(Context context, List<Playlist> list) {
        try {
            JSONArray array = new JSONArray();
            for (Playlist p : list) {
                JSONObject obj = new JSONObject();
                obj.put("id", p.getId());
                obj.put("name", p.getName());
                JSONArray songs = new JSONArray();
                for (Long sId : p.getSongIds()) {
                    songs.put(sId);
                }
                obj.put("songs", songs);
                array.put(obj);
            }
            SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            prefs.edit().putString(KEY_PLAYLISTS, array.toString()).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

