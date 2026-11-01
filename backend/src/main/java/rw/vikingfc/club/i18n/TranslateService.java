package rw.vikingfc.club.i18n;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import rw.vikingfc.club.common.exception.BusinessException;
import rw.vikingfc.club.common.util.SecurityUtils;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class TranslateService {

    private final ObjectMapper json;
    private final RestClient http;
    private final TranslateRateLimiter limiter = new TranslateRateLimiter(8, 60_000);
    private final String apiKey;
    private final String model;

    public TranslateService(ObjectMapper json,
                            @Value("${app.anthropic.api-key:}") String apiKey,
                            @Value("${app.anthropic.model:claude-3-5-haiku-latest}") String model) {
        this.json = json;
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(3000);
        this.http = RestClient.builder().requestFactory(factory).build();
    }

    public Map<String, String> translate(String text, String from, List<String> to) {
        if (text == null || text.isBlank()) {
            throw BusinessException.badRequest("Text is required", "VALIDATION");
        }
        if (to == null || to.isEmpty()) {
            return Map.of();
        }
        String who = SecurityUtils.email();
        if (!limiter.allow(who, Instant.now())) {
            throw new BusinessException(HttpStatus.TOO_MANY_REQUESTS, "Wait a moment before translating again", "TRANSLATE_LIMIT");
        }
        if (apiKey.isBlank()) {
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "Translation is not configured", "TRANSLATE_UNAVAILABLE");
        }
        try {
            String prompt = "Translate this short club label from " + from + " into " + String.join(", ", to)
                    + ". Return only a JSON object whose keys are the language codes. Text: " + text;
            Map<String, Object> body = Map.of(
                    "model", model,
                    "max_tokens", 300,
                    "messages", List.of(Map.of("role", "user", "content", prompt))
            );
            String raw = http.post()
                    .uri("https://api.anthropic.com/v1/messages")
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", "2023-06-01")
                    .header("content-type", "application/json")
                    .body(body)
                    .retrieve()
                    .body(String.class);
            JsonNode root = json.readTree(raw == null ? "{}" : raw);
            String content = root.path("content").path(0).path("text").asText("");
            content = content.replace("```json", "").replace("```", "").trim();
            JsonNode translated = json.readTree(content);
            Map<String, String> out = new LinkedHashMap<>();
            for (String lang : to) {
                String value = translated.path(lang).asText("").trim();
                if (!value.isBlank()) {
                    out.put(lang, value);
                }
            }
            return out;
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException(HttpStatus.BAD_GATEWAY, "Translation failed", "TRANSLATE_FAILED");
        }
    }
}
