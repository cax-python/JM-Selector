package com.cax.select.deepseek;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

public class DeepSeekClient {
    private static final String API_URL = "https://api.deepseek.com/chat/completions";
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public DeepSeekClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(100))
                .build();
        this.objectMapper = new ObjectMapper();
    }


    /**
     * jailbreak      system prompt 破甲
     * userText       user 偏好 + 输出规则 + 本子信息
     * imageDataUrls  每张图已 base64 化的 data: URL
     * 与 imageDataUrls 顺序一致的 true/false 列表
     */
    public List<Boolean> evaluateImages(String apiKey, String model,
                                        String jailbreak, String userText,
                                        List<String> imageDataUrls) throws Exception {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("model", model);

        ArrayNode messages = payload.putArray("messages");
        messages.addObject().put("role", "system").put("content", jailbreak);

        ObjectNode user = messages.addObject();
        user.put("role", "user");
        ArrayNode content = user.putArray("content");
        content.addObject().put("type", "text").put("text", userText);
        for (String url : imageDataUrls) {
            content.addObject()
                    .put("type", "image_url")
                    .putObject("image_url").put("url", url);
        }

        String requestBody = objectMapper.writeValueAsString(payload);

        // 图片处理较重，给更长超时
        HttpResponse<String> response = send("Bearer " + apiKey, requestBody, Duration.ofSeconds(180));

        String responseBody = response.body();
        JsonNode contentNode = objectMapper.readTree(responseBody)
                .path("choices").path(0).path("message").path("content");
        if (contentNode.isMissingNode() || !contentNode.isTextual()) {
            throw new RuntimeException("无法解析API响应内容: " + responseBody);
        }
        return parseBooleanArray(contentNode.asText());
    }

    /**
     * [true,false,true]
     * 找不到合法数组时返回 null
     */
    public static List<Boolean> parseBooleanArray(String text) {
        if (text == null) return null;
        int start = text.indexOf('[');
        int end = text.lastIndexOf(']');
        if (start < 0 || end <= start) return null;
        String json = text.substring(start, end + 1);
        try {
            ObjectMapper mapper = new ObjectMapper();
            JavaType type = mapper.getTypeFactory().constructCollectionType(List.class, Boolean.class);
            return mapper.readValue(json, type);
        } catch (Exception e) {
            return null;
        }
    }

    // 公共发送

    private HttpResponse<String> send(String auth, String requestBody, Duration timeout) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", auth)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .timeout(timeout)
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 401) {
            throw new RuntimeException("魔理沙偷走了API Key，请检查 config.json 中的 API_KEY");
        }
        if (response.statusCode() != 200) {
            throw new RuntimeException("API 请求失败（HTTP " + response.statusCode() + "）：" + response.body());
        }
        return response;
    }
}
