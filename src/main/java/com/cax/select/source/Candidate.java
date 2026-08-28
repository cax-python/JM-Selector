package com.cax.select.source;

import io.github.jukomu.jmcomic.api.model.JmAlbum;
import io.github.jukomu.jmcomic.api.model.JmAlbumMeta;

import java.util.List;


// 候选本子，承载后续采样/评估所需的元数据

public final class Candidate {
    public final String id;
    public final String title;
    public final List<String> authors;
    public final List<String> tags;
    public final String description;
    public final JmAlbum album;

    public Candidate(String id, String title, List<String> authors, List<String> tags,
                     String description, JmAlbum album) {
        this.id = id;
        this.title = title;
        this.authors = authors;
        this.tags = tags;
        this.description = description;
        this.album = album;
    }

    // 从搜索返回的 JmAlbumMeta 构造
    public static Candidate fromMeta(JmAlbumMeta m) {
        return new Candidate(m.id(), m.title(), m.authors(), m.tags(), m.description(), null);
    }

    // 从 JmAlbum 构造
    public static Candidate fromAlbum(JmAlbum a) {
        return new Candidate(a.id(), a.title(), a.authors(), a.tags(), a.description(), a);
    }

    @Override
    public String toString() {
        return String.format("[%s] %s", id, title);
    }
}
