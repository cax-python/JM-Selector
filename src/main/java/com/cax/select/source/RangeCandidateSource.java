package com.cax.select.source;

import io.github.jukomu.jmcomic.api.client.JmClient;
import io.github.jukomu.jmcomic.api.exception.AlbumNotFoundException;
import io.github.jukomu.jmcomic.api.model.JmAlbum;

import java.util.Iterator;
import java.util.NoSuchElementException;

// 逐个 getAlbum(id)，不存在则跳过,支持 "10000-20000" 或单个 "12345"

public class RangeCandidateSource implements CandidateSource {

    private final JmClient client;
    private final String rangeSpec;

    public RangeCandidateSource(JmClient client, String rangeSpec) {
        this.client = client;
        this.rangeSpec = rangeSpec;
    }

    @Override
    public Iterator<Candidate> iterator() {
        long start;
        long end;
        if (rangeSpec.contains("-")) {
            String[] parts = rangeSpec.trim().split("-", 2);
            start = Long.parseLong(parts[0].trim());
            end = Long.parseLong(parts[1].trim());
        } else {
            start = Long.parseLong(rangeSpec.trim());
            end = start;
        }

        final long from = start;
        final long to = end;
        return new Iterator<>() {
            private long cur = from;
            private Candidate saved = null;
            private boolean done = false;

            @Override
            public boolean hasNext() {
                if (saved != null) return true;
                if (done) return false;
                pull();
                return saved != null;
            }

            @Override
            public Candidate next() {
                if (!hasNext()) throw new NoSuchElementException();
                Candidate c = saved;
                saved = null;
                return c;
            }

            private void pull() {
                while (cur <= to) {
                    String idStr = String.valueOf(cur);
                    cur++;
                    try {
                        JmAlbum album = client.getAlbum(idStr);
                        if (album == null || album.id() == null || !album.id().equals(idStr)) {
                            continue; // 跳过
                        }
                        saved = Candidate.fromAlbum(album);
                        return;
                    } catch (AlbumNotFoundException e) {
                        // 不存在，跳过
                    }
                }
                done = true;
            }
        };
    }
}
