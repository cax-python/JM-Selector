package com.cax.select.source;

import io.github.jukomu.jmcomic.api.client.JmClient;
import io.github.jukomu.jmcomic.api.model.JmAlbumMeta;
import io.github.jukomu.jmcomic.api.model.JmSearchPage;
import io.github.jukomu.jmcomic.api.model.SearchQuery;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * 关键词搜索来源（惰性，单关键词）：按「页」逐步产出候选。
 * 对偶发的服务器错误做有限重试。
 */
public class SearchCandidateSource implements CandidateSource {

    private static final int RETRY_TIMES = 2;
    private static final long RETRY_DELAY_MS = 1000;

    private final JmClient client;
    private final String keyword;
    private final int maxPages;    // 0 = 不限
    private final int maxResults;  // 0 = 不限

    public SearchCandidateSource(JmClient client, String keyword, int maxPages, int maxResults) {
        this.client = client;
        this.keyword = keyword;
        this.maxPages = maxPages;
        this.maxResults = maxResults;
    }

    @Override
    public Iterator<Candidate> iterator() {
        return new Iterator<>() {
            private int page = 1;
            private List<Candidate> current = Collections.emptyList();
            private int pos = 0;
            private int collected = 0;
            private Candidate saved = null;
            private boolean exhausted = false;

            @Override
            public boolean hasNext() {
                if (saved != null) return true;
                if (exhausted) return false;
                if (maxResults > 0 && collected >= maxResults) return false;
                fill();
                return saved != null;
            }

            @Override
            public Candidate next() {
                if (!hasNext()) throw new NoSuchElementException();
                Candidate c = saved;
                saved = null;
                collected++;
                return c;
            }

            private void fill() {
                while (true) {
                    if (pos < current.size()) {
                        saved = current.get(pos++);
                        return;
                    }
                    JmSearchPage pr = searchWithRetry(keyword, page);
                    current = new ArrayList<>();
                    for (JmAlbumMeta m : pr.content()) current.add(Candidate.fromMeta(m));
                    pos = 0;

                    int totalPages = pr.getTotalPages();
                    if ((maxPages > 0 && page >= maxPages) || page >= totalPages) {
                        exhausted = true; // 单关键词：搜索完即结束
                    } else {
                        page++;
                    }
                }
            }
        };
    }

    /** 带重试的搜索调用。 */
    private JmSearchPage searchWithRetry(String kw, int page) {
        Exception last = null;
        for (int attempt = 1; attempt <= RETRY_TIMES; attempt++) {
            try {
                return client.search(new SearchQuery.Builder().text(kw).page(page).build());
            } catch (Exception e) {
                last = e;
                if (attempt < RETRY_TIMES) {
                    try {
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        throw new RuntimeException("搜索失败（已重试 " + RETRY_TIMES + " 次）: " + last.getMessage(), last);
    }
}
