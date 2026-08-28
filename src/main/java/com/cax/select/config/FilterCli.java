package com.cax.select.config;

// Filter 子命令从命令行解析到的原始参数
// 所有字段用未在命令行指定时为 null
// CLI > config > default

public class FilterCli {
    public String search;
    public Integer searchMaxPages;
    public Integer searchMaxResults;
    public String range;
    public String ids;
    public String preference;
    public String preferenceFile;
    public String jailbreak;
    public String jailbreakFile;
    public String imageLevel;
    public Integer imageN;
    public String chapterStrategy;
    public Integer chapterPick;
    public Integer chapterN;
    public Double passRatio;
    public String dbPath;
    public Boolean recheck;
    public String output;

    public Boolean dryRun;
    public Boolean verbose;

    public Integer batchMaxImages;
    public Long batchMaxBytes;

    public Integer limit;
}
