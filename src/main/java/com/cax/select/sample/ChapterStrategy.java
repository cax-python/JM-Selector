package com.cax.select.sample;

//章节策略

public enum ChapterStrategy {
    // 全部章节
    ALL,
    // 只看第 1 章
    FIRST,
    // 第 1 章 + 最后一章
    FIRST_LAST,
    // 等距挑 k 章
    PICK,
    // 挑 1/n 的章节
    FRACTION,
    // 按章节数自适应
    AUTO;

    public static ChapterStrategy from(String s) {
        if (s == null || s.isEmpty()) return AUTO;
        switch (s.trim().toLowerCase()) {
            case "all": return ALL;
            case "first": return FIRST;
            case "first-last": return FIRST_LAST;
            case "pick": return PICK;
            case "fraction": return FRACTION;
            case "auto": return AUTO;
            default: throw new IllegalArgumentException("未知章节策略: " + s);
        }
    }
}
