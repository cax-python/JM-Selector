package com.cax.select.output;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;


// 一次筛选的完整报告：参数、统计、结果列表、推荐编号

public class FilterReport {

    public String preference;
    public String imageLevel;
    public String chapterStrategy;
    public double passRatio;

    public int totalCandidates;
    public int historyHit;
    public int evaluated;
    public int failed;
    public int recommended;

    public List<ResultEntry> results = new ArrayList<>();
    public List<String> recommendedIds = new ArrayList<>();

    public void tally(ResultEntry entry) {
        results.add(entry);
        if (entry.selected) {
            recommended++;
            recommendedIds.add(entry.id);
        }
    }

    public String toJson() {
        try {
            return new ObjectMapper().writeValueAsString(this);
        } catch (Exception e) {
            throw new RuntimeException("生成JSON失败: " + e.getMessage(), e);
        }
    }
}
