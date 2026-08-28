package com.cax.select.cli;

import com.cax.select.ai.AlbumVerdict;
import com.cax.select.ai.Evaluator;
import com.cax.select.config.Config;
import com.cax.select.config.ConfigLoader;
import com.cax.select.config.FilterCli;
import com.cax.select.config.FilterOptions;
import com.cax.select.db.AlbumRecord;
import com.cax.select.db.SqliteHistoryRepository;
import com.cax.select.deepseek.DeepSeekClient;
import com.cax.select.output.FilterReport;
import com.cax.select.output.ResultEntry;
import com.cax.select.sample.Sampler;
import com.cax.select.source.Candidate;
import com.cax.select.source.CandidateCollector;
import io.github.jukomu.jmcomic.api.model.JmAlbum;
import io.github.jukomu.jmcomic.api.model.JmImage;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import io.github.jukomu.jmcomic.api.client.JmClient;
import io.github.jukomu.jmcomic.api.enums.ClientType;
import io.github.jukomu.jmcomic.core.JmComic;
import io.github.jukomu.jmcomic.core.config.JmConfiguration;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

// filter 子命令

@Command(name = "filter", mixinStandardHelpOptions = true, description = "漫画筛选")
public class FilterCommand implements Callable<Integer> {

    @Option(names = {"-of", "--option-file"}, defaultValue = "config.json", description = "指定配置文件")
    private File configFile;

    // 候选来源
    @Option(names = "--search", description = "搜索关键词（单个）")
    private String search;

    @Option(names = "--search-max-pages", description = "最多翻页数（0=不限）")
    private Integer searchMaxPages;

    @Option(names = "--search-max-results", description = "最多收集结果数（0=不限）")
    private Integer searchMaxResults;

    @Option(names = "--range", description = "ID 范围，如 10000-20000")
    private String range;

    @Option(names = "--ids", description = "精确 ID 列表，逗号分隔")
    private String ids;

    // 偏好
    @Option(names = "--preference", description = "用户偏好描述")
    private String preference;

    @Option(names = "--preference-file", description = "从文件读偏好")
    private String preferenceFile;

    // 破甲提示词
    @Option(names = "--jailbreak", description = "破甲提示词")
    private String jailbreak;

    @Option(names = "--jailbreak-file", description = "从文件读破甲提示词")
    private String jailbreakFile;

    // 图片等级
    @Option(names = "--image-level", description = "full | medium | deep | auto")
    private String imageLevel;

    @Option(names = "--image-n", description = "配合 deep，1/n 抽样，默认 10")
    private Integer imageN;

    // 章节策略
    @Option(names = "--chapter-strategy", description = "all | first | first-last | pick | fraction | auto")
    private String chapterStrategy;

    @Option(names = "--chapter-pick", description = "配合 pick，等距挑 k 章")
    private Integer chapterPick;

    @Option(names = "--chapter-n", description = "配合 fraction，1/n 章节")
    private Integer chapterN;

    @Option(names = "--pass-ratio", description = "宽容度阈值，0-1，默认 0.6")
    private Double passRatio;

    @Option(names = "--db", description = "SQLite 数据库路径，默认 ./filter.db")
    private String dbPath;

    @Option(names = "--recheck", description = "强制重扫")
    private Boolean recheck;

    @Option(names = "--output", description = "结果输出到 JSON 文件")
    private String output;

    @Option(names = "--dry-run", description = "只列候选，不调用 DeepSeek")
    private Boolean dryRun;

    @Option(names = "--verbose", description = "打印详细日志")
    private Boolean verbose;

    @Option(names = "--batch-max-images", description = "单请求最大图片数，默认 30")
    private Integer batchMaxImages;

    @Option(names = "--batch-max-bytes", description = "单请求 base64 字节上限，默认 44MiB")
    private Long batchMaxBytes;

    @Option(names = "--limit", description = "单次最多扫多少个本子，默认 100；0=不限")
    private Integer limit;

    @Override
    public Integer call() throws Exception {
        if (!configFile.exists()) {
            System.err.println("配置文件不存在: " + configFile.getAbsolutePath());
            return 1;
        }

        ConfigLoader loader = new ConfigLoader();
        Config config = loader.load(configFile);

        FilterOptions opts;
        try {
            opts = FilterOptions.resolve(config, buildCli());
        } catch (IllegalArgumentException e) {
            System.err.println("配置错误: " + e.getMessage());
            return 1;
        }

        List<String> errors = opts.validate();
        if (!errors.isEmpty()) {
            errors.forEach(e -> System.err.println("配置错误: " + e));
            return 1;
        }

        if (opts.verbose) {
            System.out.println("== filter 参数==");
            System.out.println("  sources: " + describeSources(opts));
            System.out.println("  preference: " + firstLine(opts.preference));
            System.out.println("  imageLevel: " + opts.imageLevel + (opts.imageLevel.equals("deep") ? " (n=" + opts.imageN + ")" : ""));
            System.out.println("  chapterStrategy: " + opts.chapterStrategy);
            System.out.println("  passRatio: " + opts.passRatio);
            System.out.println("  db: " + opts.dbPath);
            System.out.println("  dryRun: " + opts.dryRun + ", verbose: " + opts.verbose);
        }

        // F2：建 JMComic 客户端，收集候选
        try (io.github.jukomu.jmcomic.core.client.AbstractJmClient client = JmComic.newApiClient(
                new JmConfiguration.Builder().clientType(ClientType.API)
                        .timeout(java.time.Duration.ofSeconds(20))
                        .build())) {

            int maxPulls = opts.limit > 0 ? Math.max(opts.limit * 10, 200) : 0; // 安全上限，防一次性拉太多
            java.util.Iterator<Candidate> it = new CandidateCollector(client, opts).iterator(maxPulls);

            if (opts.dryRun) {
                int preview = opts.limit > 0 ? opts.limit : 100;
                int shown = 0;
                while (it.hasNext() && shown < preview) {
                    Candidate c = it.next();
                    shown++;
                    System.out.printf("  [%s] %s  作者=%s  标签=%s%n",
                            c.id, c.title, c.authors, c.tags);
                }
                System.out.println("预览前 " + shown + " 个候选（上限由 --limit / --search-max-results 决定！）");
                return 0;
            }

            System.out.println("开始筛选...");
            Sampler sampler = new Sampler(client, opts);
            Evaluator evaluator = new Evaluator(new DeepSeekClient(), client, opts,
                    config.getApiKey(), config.getModel());

            FilterReport report = new FilterReport();
            report.preference = opts.preference;
            report.imageLevel = opts.imageLevel;
            report.chapterStrategy = opts.chapterStrategy;
            report.passRatio = opts.passRatio;

            int newScanned = 0;
            int pulled = 0;
            try (SqliteHistoryRepository repo = new SqliteHistoryRepository(opts.dbPath)) {
                while (true) {
                    // 已达本次新扫上限 → 停止（不再多拉）
                    if (opts.limit > 0 && newScanned >= opts.limit) {
                        System.out.println("  已达本次上限 --limit " + opts.limit + "，其余本子本次不扫描。");
                        break;
                    }
                    if (!it.hasNext()) break; // 候选池耗尽
                    Candidate c = it.next();
                    pulled++;
                    try {
                        // 历史复用：同编号 + 同偏好，且未强制重扫 则 直接用历史判定（不占 limit）
                        if (!opts.recheck) {
                            AlbumRecord rec = repo.find(c.id, opts.preference);
                            if (rec != null) {
                                report.historyHit++;
                                report.tally(new ResultEntry(c.id, c.title, rec.selected, rec.reason,
                                        rec.passRatio, rec.totalImages, rec.passImages, true));
                                System.out.printf("  [%s] %s  (历史命中) -> %s%n",
                                        c.id, c.title, rec.selected ? "--入选" : "--淘汰");
                                continue;
                            }
                        }

                        // 实际筛选
                        JmAlbum album = c.album != null ? c.album : getAlbumWithRetry(client, c.id);
                        List<JmImage> sampled = sampler.sample(album);
                        if (opts.verbose) {
                            System.out.println("    [" + c.id + "] 抽样 " + sampled.size() + " 张");
                        }
                        AlbumVerdict v = evaluator.evaluate(c, sampled);
                        newScanned++;
                        report.evaluated++;
                        report.tally(new ResultEntry(c.id, c.title, v.selected, v.reason,
                                v.passRatio, v.totalImages, v.passImages, false));
                        System.out.printf("  [%s] %s  通过率=%.2f (%d/%d)  -> %s%n",
                                c.id, c.title, v.passRatio, v.passImages, v.totalImages,
                                v.selected ? "--入选" : "--淘汰");

                        // db持久化
                        repo.upsert(new AlbumRecord(
                                c.id, c.title, v.selected, v.reason,
                                v.passRatio, v.totalImages, v.passImages,
                                opts.preference, paramsOf(opts),
                                java.time.LocalDateTime.now().toString()));
                    } catch (Exception e) {
                        report.failed++;
                        System.err.println("  [" + c.id + "] 筛选失败: " + e.getMessage());
                    }
                }
                report.totalCandidates = pulled;
            }

            // 输出统计
            System.out.println("==============================");
            System.out.printf("候选总数: %d%n", report.totalCandidates);
            System.out.printf("历史命中: %d%n", report.historyHit);
            System.out.printf("实际筛选: %d%n", report.evaluated);
            System.out.printf("失败: %d%n", report.failed);
            System.out.printf("推荐: %d 本%n", report.recommended);
            System.out.println("==============================");
            System.out.println("通过：");
            for (String id : report.recommendedIds) {
                System.out.printf(id+",");
            }

            // JSON 输出
            if (opts.output != null && !opts.output.isEmpty()) {
                try {
                    Files.writeString(Path.of(opts.output), report.toJson(), StandardCharsets.UTF_8);
                    System.out.println("结果写入: " + opts.output);
                } catch (Exception e) {
                    System.err.println("写入失败: " + e.getMessage());
                }
            }
            return 0;
        } catch (IllegalArgumentException e) {
            System.err.println("配置错误: " + e.getMessage());
            return 1;
        } catch (Exception e) {
            System.err.println("候选收集失败: " + e.getMessage());
            return 1;
        }
    }

    private FilterCli buildCli() {
        FilterCli cli = new FilterCli();
        cli.search = (search == null || search.isEmpty()) ? null : search;
        cli.searchMaxPages = searchMaxPages;
        cli.searchMaxResults = searchMaxResults;
        cli.range = range;
        cli.ids = ids;
        cli.preference = preference;
        cli.preferenceFile = preferenceFile;
        cli.jailbreak = jailbreak;
        cli.jailbreakFile = jailbreakFile;
        cli.imageLevel = imageLevel;
        cli.imageN = imageN;
        cli.chapterStrategy = chapterStrategy;
        cli.chapterPick = chapterPick;
        cli.chapterN = chapterN;
        cli.passRatio = passRatio;
        cli.dbPath = dbPath;
        cli.recheck = recheck;
        cli.output = output;
        cli.dryRun = dryRun;
        cli.verbose = verbose;
        cli.batchMaxImages = batchMaxImages;
        cli.batchMaxBytes = batchMaxBytes;
        cli.limit = limit;
        return cli;
    }

    private String describeSources(FilterOptions o) {
        List<String> parts = new ArrayList<>();
        if (o.search != null && !o.search.isEmpty()) parts.add("search=" + o.search);
        if (o.range != null && !o.range.isEmpty()) parts.add("range=" + o.range);
        if (o.ids != null && !o.ids.isEmpty()) parts.add("ids=" + o.ids);
        return String.join(" + ", parts);
    }

    private String paramsOf(FilterOptions o) {
        return "imageLevel=" + o.imageLevel
                + ",chapterStrategy=" + o.chapterStrategy
                + ",imageN=" + o.imageN
                + ",chapterPick=" + o.chapterPick
                + ",chapterN=" + o.chapterN
                + ",passRatio=" + o.passRatio;
    }

    // 重试 getAlbum，应对 JM 服务器偶发的 MySQL 抽分
    private JmAlbum getAlbumWithRetry(io.github.jukomu.jmcomic.core.client.AbstractJmClient client, String id) throws Exception {
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

    private String firstLine(String s) {
        if (s == null) return "";
        int idx = s.indexOf('\n');
        return idx < 0 ? s : s.substring(0, idx);
    }
}
