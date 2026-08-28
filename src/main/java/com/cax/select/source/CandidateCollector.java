package com.cax.select.source;

import com.cax.select.config.FilterOptions;
import io.github.jukomu.jmcomic.api.client.JmClient;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

// 候选收集器,不一次性收集全部
public class CandidateCollector {

    private final JmClient client;
    private final FilterOptions options;

    public CandidateCollector(JmClient client, FilterOptions options) {
        this.client = client;
        this.options = options;
    }

    //maxPulls 候选池最大拉取数（0 = 不限）

    public Iterator<Candidate> iterator(int maxPulls) {
        List<Iterator<Candidate>> iters = new ArrayList<>();
        if (options.search != null && !options.search.isEmpty()) {
            iters.add(new SearchCandidateSource(client, options.search,
                    options.searchMaxPages, options.searchMaxResults).iterator());
        }
        if (options.range != null && !options.range.isEmpty()) {
            iters.add(new RangeCandidateSource(client, options.range).iterator());
        }
        if (options.ids != null && !options.ids.isEmpty()) {
            iters.add(new IdsCandidateSource(client, options.ids).iterator());
        }
        return new DedupMergedIterator(iters, maxPulls);
    }

    // 按来源顺序拉取、按编号去重、最多拉 maxPulls 个
    private static final class DedupMergedIterator implements Iterator<Candidate> {
        private final List<Iterator<Candidate>> sources;
        private final long maxPulls;
        private final Set<String> seen = new HashSet<>();
        private int srcIndex = 0;
        private long pulled = 0;
        private Candidate saved = null;

        DedupMergedIterator(List<Iterator<Candidate>> sources, long maxPulls) {
            this.sources = sources;
            this.maxPulls = maxPulls;
        }

        @Override
        public boolean hasNext() {
            if (saved != null) return true;
            if (maxPulls > 0 && pulled >= maxPulls) return false;
            find();
            return saved != null;
        }

        @Override
        public Candidate next() {
            if (!hasNext()) throw new NoSuchElementException();
            Candidate c = saved;
            saved = null;
            return c;
        }

        private void find() {
            while (srcIndex < sources.size()) {
                Iterator<Candidate> it = sources.get(srcIndex);
                if (it.hasNext()) {
                    Candidate c = it.next();
                    if (!seen.add(c.id)) continue; // 去重
                    saved = c;
                    pulled++;
                    return;
                }
                srcIndex++; // 该来源耗尽
            }
            saved = null; // 全部耗尽
        }
    }
}
