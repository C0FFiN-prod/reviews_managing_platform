package com.example.demo.service;

import com.example.demo.component.AppConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static com.example.demo.Utils.logError;

@Service
@AllArgsConstructor
public class TextGenerationService {
    private final Logger logger = LoggerFactory.getLogger(TextGenerationService.class);
    private final AppConfig appConfig;

    public String askQuestion(String question) {
        RestTemplate restTemplate = new RestTemplate();
        String endpoint = appConfig.getLlm().getHost() + "/generate";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> requestBody = Map.of(
                "model", appConfig.getLlm().getModel(),
                "system", "Ты - модератор сайта с обзорами на товары и услуги. Твоя задача - анализировать текст на предмет ненормативной лексики, а также неуместной информации и политической информации. Если ты находишь такой фрагмент - выписывай его в формате: каждый фрагмент на новой строке. Оценивай каждый из таких фрагментов как: WARN:фрагмент текста, если нарушение не критичное и SEVERE:фрагмент текста, если нарушение серьезное. Мат нужно расценивать как SEVERE. Фрагменты не более 1 предложения. После фрагмента добавляй # и причину выделения. Review - текст самого обзора. Reports - недавние жалобы на обзор\n",
                "prompt", question,
                "stream", false);

        HttpEntity<String> request = new HttpEntity<>(new JSONObject(requestBody).toString(), headers);

        try {
            String raw_response = restTemplate.postForObject(endpoint, request, String.class);
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode rootNode = objectMapper.readTree(raw_response);
                return rootNode.get("response").asText();
            } catch (Exception e) {
                logError(logger, e);
            }
        } catch (Exception e) {
            logError(logger, e);
            return "Error: " + e.getMessage();
        }
        return null;
    }
}
