package com.cax.select.source;

import io.github.jukomu.jmcomic.api.client.JmClient;
import io.github.jukomu.jmcomic.api.exception.AlbumNotFoundException;
import io.github.jukomu.jmcomic.api.model.JmAlbum;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

// 遍历逗号分隔的 ID，逐个 getAlbum(id)，不存在则跳过

public class IdsCandidateSource implements CandidateSource {

    private final JmClient client;
    private final List<String> ids;

    public IdsCandidateSource(JmClient client, String idsSpec) {
        this.client = client;
        List<String> list = new ArrayList<>();
        if (idsSpec != null && !idsSpec.isEmpty()) {
            for (String p : idsSpec.split(",")) {
                String t = p.trim();
                if (!t.isEmpty()) list.add(t);
            }
        }
        this.ids = list;
    }

    @Override
    public Iterator<Candidate> iterator() {
        return new Iterator<>() {
            private int idx = 0;
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
                while (idx < ids.size()) {
                    String id = ids.get(idx++);
                    try {
                        JmAlbum album = client.getAlbum(id);
                        if (album == null || album.id() == null || !album.id().equals(id)) {
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
