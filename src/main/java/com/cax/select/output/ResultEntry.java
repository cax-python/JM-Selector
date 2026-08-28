package com.cax.select.output;

//单个本子的筛选结果

public class ResultEntry {
    public String id;
    public String title;
    public boolean selected;
    public String reason;
    public double passRatio;
    public int totalImages;
    public int passImages;
    public boolean fromHistory;

    public ResultEntry() {
    }

    public ResultEntry(String id, String title, boolean selected, String reason,
                       double passRatio, int totalImages, int passImages, boolean fromHistory) {
        this.id = id;
        this.title = title;
        this.selected = selected;
        this.reason = reason;
        this.passRatio = passRatio;
        this.totalImages = totalImages;
        this.passImages = passImages;
        this.fromHistory = fromHistory;
    }
}
