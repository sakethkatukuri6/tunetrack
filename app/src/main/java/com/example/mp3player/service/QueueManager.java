package com.example.mp3player.service;

import com.example.mp3player.model.Song;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class QueueManager {

    public enum RepeatMode {
        OFF,
        ALL,
        ONE
    }

    private final List<Song> queue = new ArrayList<>();
    private final List<Integer> shuffleOrder = new ArrayList<>();
    private int currentIndex = -1;
    private boolean isShuffle = false;
    private RepeatMode repeatMode = RepeatMode.ALL;

    public synchronized void setQueue(List<Song> songs, int startIndex) {
        queue.clear();
        if (songs != null) {
            queue.addAll(songs);
        }
        rebuildShuffleOrder();
        if (startIndex >= 0 && startIndex < queue.size()) {
            currentIndex = startIndex;
        } else {
            currentIndex = queue.isEmpty() ? -1 : 0;
        }
    }

    public synchronized List<Song> getQueue() {
        return new ArrayList<>(queue);
    }

    public synchronized int getQueueSize() {
        return queue.size();
    }

    public synchronized int getCurrentIndex() {
        return currentIndex;
    }

    public synchronized void setCurrentIndex(int index) {
        if (index >= 0 && index < queue.size()) {
            currentIndex = index;
        }
    }

    public synchronized Song getCurrentSong() {
        if (currentIndex >= 0 && currentIndex < queue.size()) {
            return queue.get(currentIndex);
        }
        return null;
    }

    public synchronized boolean hasSongs() {
        return !queue.isEmpty();
    }

    public synchronized int getNextIndex() {
        if (queue.isEmpty()) return -1;
        if (repeatMode == RepeatMode.ONE) return currentIndex;

        if (isShuffle) {
            int currentPosInShuffle = shuffleOrder.indexOf(currentIndex);
            if (currentPosInShuffle >= 0 && currentPosInShuffle < shuffleOrder.size() - 1) {
                return shuffleOrder.get(currentPosInShuffle + 1);
            } else if (repeatMode == RepeatMode.ALL) {
                return shuffleOrder.get(0);
            } else {
                return -1; // End of playback in OFF mode
            }
        } else {
            if (currentIndex < queue.size() - 1) {
                return currentIndex + 1;
            } else if (repeatMode == RepeatMode.ALL) {
                return 0;
            } else {
                return -1;
            }
        }
    }

    public synchronized int getPreviousIndex() {
        if (queue.isEmpty()) return -1;
        if (repeatMode == RepeatMode.ONE) return currentIndex;

        if (isShuffle) {
            int currentPosInShuffle = shuffleOrder.indexOf(currentIndex);
            if (currentPosInShuffle > 0) {
                return shuffleOrder.get(currentPosInShuffle - 1);
            } else if (repeatMode == RepeatMode.ALL) {
                return shuffleOrder.get(shuffleOrder.size() - 1);
            } else {
                return -1;
            }
        } else {
            if (currentIndex > 0) {
                return currentIndex - 1;
            } else if (repeatMode == RepeatMode.ALL) {
                return queue.size() - 1;
            } else {
                return -1;
            }
        }
    }

    public synchronized boolean isShuffle() {
        return isShuffle;
    }

    public synchronized void setShuffle(boolean shuffle) {
        if (this.isShuffle != shuffle) {
            this.isShuffle = shuffle;
            if (shuffle) {
                rebuildShuffleOrder();
            }
        }
    }

    public synchronized boolean toggleShuffle() {
        setShuffle(!isShuffle);
        return isShuffle;
    }

    public synchronized RepeatMode getRepeatMode() {
        return repeatMode;
    }

    public synchronized void setRepeatMode(RepeatMode repeatMode) {
        this.repeatMode = repeatMode != null ? repeatMode : RepeatMode.OFF;
    }

    public synchronized RepeatMode cycleRepeatMode() {
        switch (repeatMode) {
            case OFF:
                repeatMode = RepeatMode.ALL;
                break;
            case ALL:
                repeatMode = RepeatMode.ONE;
                break;
            case ONE:
                repeatMode = RepeatMode.OFF;
                break;
        }
        return repeatMode;
    }

    private void rebuildShuffleOrder() {
        shuffleOrder.clear();
        for (int i = 0; i < queue.size(); i++) {
            shuffleOrder.add(i);
        }
        Collections.shuffle(shuffleOrder);
        // Put current song first in shuffle order so it doesn't get repeated immediately
        if (currentIndex >= 0 && shuffleOrder.contains(currentIndex)) {
            shuffleOrder.remove(Integer.valueOf(currentIndex));
            shuffleOrder.add(0, currentIndex);
        }
    }
}

