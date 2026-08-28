package com.cax.select.ai;

import java.util.ArrayList;
import java.util.List;

// 把一批 base64 化的图片切成若干个请求批，
public final class BatchSplitter {

    private BatchSplitter() { }

    /**
     * prepared  已 base64 化的图片
     * maxImages 单请求最大图片数
     * maxBytes  单请求 base64 累计字节上限
     */
    public static List<List<PreparedImage>> split(List<PreparedImage> prepared, int maxImages, long maxBytes) {
        List<List<PreparedImage>> batches = new ArrayList<>();
        if (prepared == null || prepared.isEmpty()) {
            return batches;
        }

        List<PreparedImage> cur = new ArrayList<>();
        long curBytes = 0;

        for (PreparedImage p : prepared) {
            long b64 = p.sizeBytes();

            // 当前批已满 --→ 先 flush，再放入新批
            if (!cur.isEmpty() && (cur.size() >= maxImages || curBytes + b64 > maxBytes)) {
                batches.add(cur);
                cur = new ArrayList<>();
                curBytes = 0;
            }

            cur.add(p);
            curBytes += b64;
        }

        // if (!cur.IsEmpty()){
        batches.add(cur);
        // }
        return batches;
    }
}
