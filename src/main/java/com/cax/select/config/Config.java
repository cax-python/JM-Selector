package com.cax.select.config;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Config {
    @JsonProperty("API_KEY")
    private String apiKey = null;

    @JsonProperty("MODEL")
    private String model = "deepseek-v4-flash-vision-exp";

    // filter

    @JsonProperty("SEARCH")
    private String search;

    @JsonProperty("SEARCH_MAX_PAGES")
    private Integer searchMaxPages;

    @JsonProperty("SEARCH_MAX_RESULTS")
    private Integer searchMaxResults;

    @JsonProperty("RANGE")
    private String range;

    @JsonProperty("IDS")
    private String ids;

    @JsonProperty("PREFERENCE")
    private String preference;

    @JsonProperty("PREFERENCE_FILE")
    private String preferenceFile;

    @JsonProperty("JAILBREAK")
    private String jailbreak;

    @JsonProperty("JAILBREAK_FILE")
    private String jailbreakFile;

    @JsonProperty("IMAGE_LEVEL")
    private String imageLevel;

    @JsonProperty("IMAGE_N")
    private Integer imageN;

    @JsonProperty("CHAPTER_STRATEGY")
    private String chapterStrategy;

    @JsonProperty("CHAPTER_PICK")
    private Integer chapterPick;

    @JsonProperty("CHAPTER_N")
    private Integer chapterN;

    @JsonProperty("PASS_RATIO")
    private Double passRatio;

    @JsonProperty("DB_PATH")
    private String dbPath;

    @JsonProperty("RECHECK")
    private Boolean recheck;

    @JsonProperty("OUTPUT")
    private String output;

    @JsonProperty("DRY_RUN")
    private Boolean dryRun;

    @JsonProperty("VERBOSE")
    private Boolean verbose;

    @JsonProperty("BATCH_MAX_IMAGES")
    private Integer batchMaxImages;

    @JsonProperty("BATCH_MAX_BYTES")
    private Long batchMaxBytes;

    @JsonProperty("LIMIT")
    private Integer limit;

    public Config(){}

    public String getApiKey() {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalArgumentException("未配置 API_KEY，请在config文件中填写");
        }
        return apiKey;
    }
    public String getModel() {
        if (model == null || model.isEmpty()) {
            return "deepseek-v4-flash-vision-exp";
        }
        return model;
    }

    public String getSearch() { return search; }
    public Integer getSearchMaxPages() { return searchMaxPages; }
    public Integer getSearchMaxResults() { return searchMaxResults; }
    public String getRange() { return range; }
    public String getIds() { return ids; }
    public String getPreference() { return preference; }
    public String getPreferenceFile() { return preferenceFile; }
    public String getJailbreak() { return jailbreak; }
    public String getJailbreakFile() { return jailbreakFile; }
    public String getImageLevel() { return imageLevel; }
    public Integer getImageN() { return imageN; }
    public String getChapterStrategy() { return chapterStrategy; }
    public Integer getChapterPick() { return chapterPick; }
    public Integer getChapterN() { return chapterN; }
    public Double getPassRatio() { return passRatio; }
    public String getDbPath() { return dbPath; }
    public Boolean getRecheck() { return recheck; }
    public String getOutput() { return output; }
    public Boolean getDryRun() { return dryRun; }
    public Boolean getVerbose() { return verbose; }
    public Integer getBatchMaxImages() { return batchMaxImages; }
    public Long getBatchMaxBytes() { return batchMaxBytes; }
    public Integer getLimit() { return limit; }
}
