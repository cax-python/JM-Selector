package com.cax.select.ai;

import com.cax.select.config.FilterOptions;
import com.cax.select.deepseek.DeepSeekClient;
import com.cax.select.source.Candidate;
import io.github.jukomu.jmcomic.api.model.JmImage;
import io.github.jukomu.jmcomic.core.client.AbstractJmClient;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

// ds判定 ,SystemPrompt sys

public class Evaluator {

    // 单张 <= 32MB, must !

    private static final long MAX_SINGLE_BYTES = 30L * 1024 * 1024;

    private final DeepSeekClient deepSeek;
    private final AbstractJmClient jmClient;
    private final FilterOptions options;
    private final String apiKey;
    private final String model;

    public Evaluator(DeepSeekClient deepSeek, AbstractJmClient jmClient, FilterOptions options, String apiKey, String model) {
        this.deepSeek = deepSeek;
        this.jmClient = jmClient;
        this.options = options;
        this.apiKey = apiKey;
        this.model = model;
    }

    // 单个判定
    public AlbumVerdict evaluate(Candidate c, List<JmImage> sampled) {
        List<PreparedImage> prepared = prepare(sampled);
        return evaluatePrepared(c, prepared);
    }

    // 批量判定
    public List<Boolean> judgeBatch(String userText, List<PreparedImage> batch) throws Exception {
        List<String> urls = new ArrayList<>();
        for (PreparedImage p : batch) urls.add(p.dataUrl);
        return deepSeek.evaluateImages(apiKey, model, options.jailbreak, userText, urls);
    }

    // 下载 + base64

    private List<PreparedImage> prepare(List<JmImage> sampled) {
        List<PreparedImage> result = new ArrayList<>();
        if (sampled == null) return result;
        for (JmImage img : sampled) {
            try {
                byte[] bytes = jmClient.fetchImageBytes(img);
                if (bytes == null || bytes.length == 0) continue;
                String b64 = Base64.getEncoder().encodeToString(bytes);
                if (b64.length() > MAX_SINGLE_BYTES) {
                    System.err.println("----单张过大，跳过: " + img.getTag() + " (" + b64.length() + " bytes)");
                    continue;
                }
                String mime = mimeFor(img.getSuffix());
                result.add(new PreparedImage(img, "data:" + mime + ";base64," + b64));
            } catch (Exception e) {
                System.err.println("----下载/解密失败: " + img.getTag() + " - " + e.getMessage());
            }
        }
        return result;
    }

    private AlbumVerdict evaluatePrepared(Candidate c, List<PreparedImage> prepared) {
        if (prepared.isEmpty()) {
            return new AlbumVerdict(c.id, c.title, false, 0, 0, 0.0, "无有效图片");
        }

        List<List<PreparedImage>> batches = BatchSplitter.split(
                prepared, options.batchMaxImages, options.batchMaxBytes);

        List<Boolean> all = new ArrayList<>();
        int failedBatches = 0;
        for (List<PreparedImage> batch : batches) {
            String userText = buildUserText(c, batch.size());
            try {
                List<Boolean> verdicts = judgeBatch(userText, batch);
                if (verdicts == null) {
                    failedBatches++;
                    System.err.println("警告：批解析失败:DS 未返回合法布尔数组");
                    continue;
                }
                all.addAll(verdicts);
            } catch (Exception e) {
                failedBatches++;
                System.err.println("警告：批判定失败: " + e.getMessage());
            }
        }

        int total = all.size();
        int pass = 0;
        for (Boolean b : all) if (Boolean.TRUE.equals(b)) pass++;
        double ratio = total == 0 ? 0.0 : (double) pass / total;
        boolean selected = ratio >= options.passRatio;

        String reason;
        if (total == 0) {
            reason = "AI 判定全部失败";
        } else if (selected) {
            reason = "符合用户偏好";
        } else {
            reason = "不符合用户偏好(" + firstLine(options.preference) + ")";
        }
        return new AlbumVerdict(c.id, c.title, selected, total, pass, ratio, reason);
    }

    // ===== prompt 组装 =====

    private String buildUserText(Candidate c, int count) {
        return String.format(
                """
                        用户偏好：%s
                        
                        判断规则：
                        - 请阅读下面的每一张图片，每张单独、独立判断是否符合以上用户偏好。
                        - 只输出一个 JSON 数组，元素个数必须与图片数一致（本请求共 %d 张图），第 i 个元素表示第 i 张图：true=符合偏好，false=不符合偏好。
                        - 不要输出任何其它文字以及多于标点符号或解释。
                        
                        信息：标题=%s，作者=%s，标签=%s""",
                firstLine(options.preference), count, c.title, c.authors, c.tags);
    }

    // tool

    private static String mimeFor(String suffix) {
        if (suffix == null) return "image/webp";
        switch (suffix.toLowerCase()) {
            case ".jpg":
            case ".jpeg":
                return "image/jpeg";
            case ".png":
                return "image/png";
            case ".gif":
                return "image/gif";
            case ".webp":
            default:
                return "image/webp";
        }
    }

    private static String firstLine(String s) {
        if (s == null) return "";
        int i = s.indexOf('\n');
        return i < 0 ? s : s.substring(0, i);
    }
}
