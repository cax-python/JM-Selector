package com.cax.select.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;

public class ConfigLoader {
    // 忽略 JSON 中未映射的未知字段
    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public Config load(File configFile) throws IOException {
        if (!configFile.exists()) {
            throw new IllegalArgumentException("配置文件不存在: " + configFile.getAbsolutePath());
        }
        return objectMapper.readValue(configFile, Config.class);
    }
}