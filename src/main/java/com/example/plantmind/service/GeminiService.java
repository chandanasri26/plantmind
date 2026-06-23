package com.example.plantmind.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GeminiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.url}")
    private String apiUrl;

    private final RestTemplate restTemplate;

    public GeminiService() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * Interacts with Gemini API by sending the contextual prompt and returning the response text.
     */
    public String askGemini(String systemContext, String question) {
        if (apiKey == null || apiKey.trim().isEmpty() || apiKey.equals("YOUR_GEMINI_API_KEY_HERE")) {
            return "❌ [Gemini API Config Error]: The Gemini API Key is not configured on the backend.\n\n" +
                   "Please set the `GEMINI_API_KEY` environment variable or update `src/main/resources/application.properties` " +
                   "with your active Gemini API key, then restart the Spring Boot server.";
        }

        // Build the full RAG prompt
        String fullPrompt = String.format(
                "You are PlantMind AI, an advanced Industrial Knowledge Intelligence Platform. " +
                "Your goal is to assist industrial engineers, maintenance teams, and operators with unified brain intelligence. " +
                "Use the following technical/maintenance document context to answer the user's question. " +
                "Be highly detailed, structured, and prioritize safety protocols, step-by-step procedures, and mechanical troubleshooting.\n\n" +
                "=== DOCUMENT CONTEXT ===\n%s\n========================\n\n" +
                "Question: %s\n\n" +
                "If the answer is found in the document, refer to the document details and cite sections/details. " +
                "If the context is insufficient to answer the question, state that the context did not contain the exact details, " +
                "but provide an expert engineering response based on general industrial knowledge, clearly marking the boundary of general knowledge vs document-sourced data.",
                systemContext, question
        );

        String endpoint = apiUrl + "?key=" + apiKey;

        try {
            // Setup headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Construct Gemini request body
            // Format: { "contents": [ { "parts": [ { "text": "..." } ] } ] }
            Map<String, Object> requestBody = new HashMap<>();
            Map<String, Object> contentMap = new HashMap<>();
            Map<String, Object> partMap = new HashMap<>();
            
            partMap.put("text", fullPrompt);
            contentMap.put("parts", Collections.singletonList(partMap));
            requestBody.put("contents", Collections.singletonList(contentMap));

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> responseEntity = restTemplate.postForEntity(endpoint, requestEntity, Map.class);
            Map responseBody = responseEntity.getBody();

            if (responseBody != null && responseBody.containsKey("candidates")) {
                List candidates = (List) responseBody.get("candidates");
                if (!candidates.isEmpty()) {
                    Map candidate = (Map) candidates.get(0);
                    Map content = (Map) candidate.get("content");
                    if (content != null && content.containsKey("parts")) {
                        List parts = (List) content.get("parts");
                        if (!parts.isEmpty()) {
                            Map part = (Map) parts.get(0);
                            return (String) part.get("text");
                        }
                    }
                }
            }

            return "Error: Gemini returned an empty or invalid response structure.";

        } catch (Exception e) {
            e.printStackTrace();
            return "❌ [Gemini API Request Failed]: " + e.getMessage() + 
                   "\n\nPlease ensure your API Key is valid and that your network has access to Google Gemini API.";
        }
    }
}
