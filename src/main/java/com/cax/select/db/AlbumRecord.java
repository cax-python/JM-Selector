package com.cax.select.db;

// 一个本子的筛选历史记录

public final class AlbumRecord {
    public final String albumId;
    public final String title;
    public final boolean selected;
    public final String reason;
    public final double passRatio;
    public final int totalImages;
    public final int passImages;
    public final String preference;
    public final String params;
    public final String scannedAt;

    public AlbumRecord(String albumId, String title, boolean selected, String reason,
                       double passRatio, int totalImages, int passImages,
                       String preference, String params, String scannedAt) {
        this.albumId = albumId;
        this.title = title;
        this.selected = selected;
        this.reason = reason;
        this.passRatio = passRatio;
        this.totalImages = totalImages;
        this.passImages = passImages;
        this.preference = preference;
        this.params = params;
        this.scannedAt = scannedAt;
    }
}
