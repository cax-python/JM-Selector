package com.cax.select.sample;

//图片等级：对一个章节看多少内页
public enum ImageLevel {
    // 该章节所有内页
    FULL,
    // 等距抽 6 张内页
    MEDIUM,
    // 等距抽总页数的 1/n
    DEEP,
    // 按章节内页数自适应
    AUTO;

    public static ImageLevel from(String s) {
        if (s == null || s.isEmpty()) return AUTO;
        switch (s.trim().toLowerCase()) {
            case "full": return FULL;
            case "medium": return MEDIUM;
            case "deep": return DEEP;
            case "auto": return AUTO;
            default: throw new IllegalArgumentException("未知图片等级: " + s);
        }
    }
}
