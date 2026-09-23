package com.example.mp3player.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Playlist implements Serializable {
    private final String id;
    private String name;
    private final List<Long> songIds = new ArrayList<>();

    public Playlist(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Long> getSongIds() {
        return songIds;
    }

    public void addSongId(long songId) {
        if (!songIds.contains(songId)) {
            songIds.add(songId);
        }
    }

    public void removeSongId(long songId) {
        songIds.remove(Long.valueOf(songId));
    }

    public boolean containsSongId(long songId) {
        return songIds.contains(songId);
    }
}
