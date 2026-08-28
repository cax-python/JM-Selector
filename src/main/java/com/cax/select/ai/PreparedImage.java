package com.cax.select.ai;

import io.github.jukomu.jmcomic.api.model.JmImage;

// 解密图片以及url
public final class PreparedImage {
    public final JmImage image;
    public final String dataUrl;

    public PreparedImage(JmImage image, String dataUrl) {
        this.image = image;
        this.dataUrl = dataUrl;
    }

    public long sizeBytes() {
        return dataUrl.length();
    }
}
