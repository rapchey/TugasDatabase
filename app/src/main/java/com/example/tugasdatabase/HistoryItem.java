package com.example.tugasdatabase;

public class HistoryItem {

    private String title;
    private int albumImageResId;

    public HistoryItem(String title, int albumImageResId) {
        this.title = title;
        this.albumImageResId = albumImageResId;
    }

    public String getTitle() {
        return title;
    }

    public int getAlbumImageResId() {
        return albumImageResId;
    }

    public String getSongTitle() {
        return title;
    }
}