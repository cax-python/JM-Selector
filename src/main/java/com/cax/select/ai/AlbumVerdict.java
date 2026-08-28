package com.cax.select.ai;

// 单个本子的筛选结果。

public final class AlbumVerdict {
    public final String albumId;
    public final String title;
    public final boolean selected;
    public final int totalImages;  // 参与判定的图片数
    public final int passImages;
    public final double passRatio;
    public final String reason;

    public AlbumVerdict(String albumId, String title, boolean selected,
                        int totalImages, int passImages, double passRatio, String reason) {
        this.albumId = albumId;
        this.title = title;
        this.selected = selected;
        this.totalImages = totalImages;
        this.passImages = passImages;
        this.passRatio = passRatio;
        this.reason = reason;
    }
}
