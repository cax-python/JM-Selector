package com.cax.select.cli;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.jukomu.jmcomic.api.enums.ClientType;
import io.github.jukomu.jmcomic.api.model.JmAlbum;
import io.github.jukomu.jmcomic.core.JmComic;
import io.github.jukomu.jmcomic.core.client.AbstractJmClient;
import io.github.jukomu.jmcomic.core.config.JmConfiguration;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;


// download 子命令：按编号批量下载本子
// 大家共同爱护JM服务器：不使用并行下载，支持批量

@Command(name = "download", mixinStandardHelpOptions = true, description = "批量下载本子")
public class DownloadCommand implements Callable<Integer> {

    @Option(names = "--ids", description = "本子ID列表，逗号分隔")
    private String ids;

    @Option(names = "--id-file", description = "从文件读ID（每行一个 / JSON数组 / filter 报告JSON）")
    private String idFile;

    @Option(names = {"--path", "-p"}, defaultValue = "./JMdownloads", description = "下载目录")
    private String path;

    @Option(names = "--verbose", description = "打印日志")
    private Boolean verbose;

    @Override
    public Integer call() throws Exception {
        List<String> idList = collectIds();
        if (idList.isEmpty()) {
            System.err.println("请提供编号：--ids 或 --id-file");
            return 1;
        }

        Path dir = Path.of(path);
        Files.createDirectories(dir);

        System.out.println("下载目录: " + dir.toAbsolutePath());
        System.out.println("待下载: " + idList.size() + " 本");

        int ok = 0, fail = 0;
        try (AbstractJmClient client = JmComic.newApiClient(
                new JmConfiguration.Builder().clientType(ClientType.API)
                        .timeout(java.time.Duration.ofSeconds(20))
                        .build())) {
            for (String id : idList) {
                try {
                    JmAlbum album = getAlbumWithRetry(client, id);
                    if (album == null || album.id() == null || !album.id().equals(id)) {
                        System.out.println("  不存在: " + id);
                        continue;
                    }
                    System.out.println("  下载中: " + id + " " + album.getTitle());
                    // 每个本子放到以本子编号命名的子文件夹下，避免全部平铺
                    client.downloadAlbum(album, al -> dir.resolve(al.getId()));
                    ok++;
                    System.out.println("    完成: " + id);
                } catch (Exception e) {
                    fail++;
                    System.err.println("  下载失败: " + id + " --- " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("下载失败: " + e.getMessage());
            return 1;
        }

        System.out.println("下载完成：成功 " + ok + "，失败 " + fail + "。");
        return fail == 0 ? 0 : 1;
    }

    // getAlbum，应对 JM 服务器偶发的 MySQL 抽风
    private JmAlbum getAlbumWithRetry(AbstractJmClient client, String id) throws Exception {
        final int retries = 3;
        Exception last = null;
        for (int i = 1; i <= retries; i++) {
            try {
                return client.getAlbum(id);
            } catch (Exception e) {
                last = e;
                if (i < retries) {
                    Thread.sleep(1500);
                }
            }
        }
        throw new RuntimeException("获取本子失败（已重试 " + retries + " 次）: " + last.getMessage(), last);
    }

    // 去重编号
    private List<String> collectIds() throws Exception {
        Set<String> set = new LinkedHashSet<>();

        if (ids != null && !ids.isEmpty()) {
            for (String p : ids.split(",")) {
                String t = p.trim();
                if (!t.isEmpty()) set.add(t);
            }
        }

        if (idFile != null && !idFile.isEmpty()) {
            Path f = Path.of(idFile);
            if (!Files.exists(f)) throw new IllegalArgumentException("id-file 不存在: " + f);
            String content = Files.readString(f, StandardCharsets.UTF_8).trim();
            set.addAll(parseIds(content));
        }

        return new ArrayList<>(set);
    }

    // 解析 id-file （纯文本行 / JSON数组 / filter 报告JSON中的 results[].id）
    private List<String> parseIds(String content) {
        List<String> ids = new ArrayList<>();
        if (content.isEmpty()) return ids;

        if (content.startsWith("[") || content.startsWith("{")) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode node = mapper.readTree(content);
                JsonNode array = node;
                if (node.isObject()) {
                    array = node.path("results");
                }
                if (array.isArray()) {
                    for (JsonNode item : array) {
                        String id = item.isTextual() ? item.asText() : item.path("id").asText("");
                        if (!id.isEmpty()) ids.add(id);
                    }
                    return ids;
                }
            } catch (Exception ignore) {
                // 不是合法 JSON，退回按行解析
            }
        }

        for (String line : content.split("\\R")) {
            String t = line.trim();
            if (!t.isEmpty()) ids.add(t);
        }
        return ids;
    }
}
