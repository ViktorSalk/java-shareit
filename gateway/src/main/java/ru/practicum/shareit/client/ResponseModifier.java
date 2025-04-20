package ru.practicum.shareit.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class ResponseModifier {

    private final ObjectMapper objectMapper;

    public ResponseModifier(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ResponseEntity<Object> modifyItemResponse(ResponseEntity<Object> response, String path) {
        if (response.getBody() == null || !path.matches("/items/\\d+")) {
            return response;
        }

        try {
            Map<String, Object> bodyMap;
            if (response.getBody() instanceof Map) {
                bodyMap = new LinkedHashMap<>((Map<String, Object>) response.getBody());
            } else {
                bodyMap = objectMapper.convertValue(response.getBody(), Map.class);
            }

            if (!bodyMap.containsKey("lastBooking")) {
                bodyMap.put("lastBooking", null);
            }
            if (!bodyMap.containsKey("nextBooking")) {
                bodyMap.put("nextBooking", null);
            }
            if (!bodyMap.containsKey("comments")) {
                bodyMap.put("comments", new ArrayList<>());
            }

            return ResponseEntity.status(response.getStatusCode()).body(bodyMap);
        } catch (Exception e) {
            return response;
        }
    }
}