package com.ssafy.ssafy_project.copilot.application.service;

import com.ssafy.ssafy_project.global.exception.CommonErrorCode;
import com.ssafy.ssafy_project.global.exception.CustomException;
import com.ssafy.ssafy_project.global.infrastructure.openai.OpenAiAssistClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TranslateService {

    private static final Map<String, String> SUPPORTED_LANGUAGES = Map.of(
            "en", "English",
            "ja", "Japanese",
            "zh", "Simplified Chinese",
            "ko", "Korean"
    );

    private final OpenAiAssistClient assistClient;
    private final ObjectMapper objectMapper;

    public TranslateResult translate(List<String> texts, String targetLang) {
        String language = SUPPORTED_LANGUAGES.get(targetLang);
        if (language == null) {
            throw new CustomException(CommonErrorCode.VALIDATION_ERROR);
        }

        String json = assistClient.translateJson(texts, language);
        JsonNode node = objectMapper.readTree(json).path("translations");

        List<String> translations = new ArrayList<>();
        for (int i = 0; i < texts.size(); i++) {
            // 개수가 어긋나면 원문으로 폴백해 자막이 비지 않게 한다
            translations.add(node.has(i) ? node.get(i).asString() : texts.get(i));
        }
        return new TranslateResult(translations);
    }

    public record TranslateResult(List<String> translations) {
    }
}
