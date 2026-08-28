package com.cax.select.sample;

import com.cax.select.config.FilterOptions;
import io.github.jukomu.jmcomic.api.client.JmClient;
import io.github.jukomu.jmcomic.api.model.JmAlbum;
import io.github.jukomu.jmcomic.api.model.JmImage;
import io.github.jukomu.jmcomic.api.model.JmPhoto;
import io.github.jukomu.jmcomic.api.model.JmPhotoMeta;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

// 采样

public class Sampler {

    private final JmClient client;
    private final FilterOptions options;

    public Sampler(JmClient client, FilterOptions options) {
        this.client = client;
        this.options = options;
    }

    // 从本子抽取所有抽样内页图片
    public List<JmImage> sample(JmAlbum album) {
        ImageLevel level = ImageLevel.from(options.imageLevel);
        ChapterStrategy strategy = ChapterStrategy.from(options.chapterStrategy);

        List<JmPhotoMeta> chapters = pickChapters(album, strategy);
        List<JmImage> result = new ArrayList<>();
        for (JmPhotoMeta meta : chapters) {
            JmPhoto photo = client.getPhoto(meta.id());
            result.addAll(pickInnerPages(photo, level));
        }
        return result;
    }

    // 章节挑选

    public List<JmPhotoMeta> pickChapters(JmAlbum album, ChapterStrategy strategy) {
        List<JmPhotoMeta> metas = album.getPhotoMetas();
        if (metas == null || metas.isEmpty()) return new ArrayList<>();
        int m = metas.size();

        switch (strategy) {
            case ALL:
                return new ArrayList<>(metas);
            case FIRST:
                return new ArrayList<>(metas.subList(0, 1));
            case FIRST_LAST:
                if (m == 1) return new ArrayList<>(metas.subList(0, 1));
                return new ArrayList<>(List.of(metas.get(0), metas.get(m - 1)));
            case PICK:
                return pickEvenly(metas, Math.min(options.chapterPick, m));
            case FRACTION:
                return pickEvenly(metas, Math.max(1, (int) Math.ceil(m / (double) options.chapterN)));
            case AUTO:
                return pickChaptersAuto(metas, m);
            default:
                return new ArrayList<>();
        }
    }

    // auto 章节规则：M≤3→all；3<M≤10→ 3；M>10→fraction
    private List<JmPhotoMeta> pickChaptersAuto(List<JmPhotoMeta> metas, int m) {
        if (m <= 3) return new ArrayList<>(metas);
        if (m <= 10) return pickEvenly(metas, 3);
        return pickEvenly(metas, Math.max(1, (int) Math.ceil(m / (double) options.chapterN)));
    }

    // 内页挑选

    public List<JmImage> pickInnerPages(JmPhoto photo, ImageLevel level) {
        List<JmImage> images = photo.getImages();
        if (images == null || images.isEmpty()) return new ArrayList<>();

        // 按 sortOrder 排序  保证等距抽页顺序正确
        List<JmImage> ordered = new ArrayList<>(images);
        ordered.sort(Comparator.comparingInt(JmImage::getSortOrder));
        int n = ordered.size();

        switch (level) {
            case FULL:
                return new ArrayList<>(ordered);
            case MEDIUM:
                return pickEvenly(ordered, Math.min(6, n));
            case DEEP:
                return pickEvenly(ordered, Math.max(1, (int) Math.ceil(n / (double) options.imageN)));
            case AUTO:
                return pickInnerPagesAuto(ordered, n);
            default:
                return new ArrayList<>();
        }
    }

    //auto 图片规则：N≤10→full；10<N≤50→medium；N>50→deep
    private List<JmImage> pickInnerPagesAuto(List<JmImage> ordered, int n) {
        if (n <= 10) return new ArrayList<>(ordered);
        if (n <= 50) return pickEvenly(ordered, 6);
        return pickEvenly(ordered, Math.max(1, (int) Math.ceil(n / (double) options.imageN)));
    }

    // 等距

    public static <T> List<T> pickEvenly(List<T> list, int k) {
        int n = list.size();
        if (n == 0) return new ArrayList<>();
        if (k >= n) return new ArrayList<>(list);
        if (k <= 1) return new ArrayList<>(List.of(list.get(0)));

        List<T> result = new ArrayList<>();
        double step = (double) (n - 1) / (k - 1);
        for (int i = 0; i < k; i++) {
            result.add(list.get((int) Math.round(i * step)));
        }
        return result;
    }
}
