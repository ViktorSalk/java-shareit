package ru.practicum.shareit.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import ru.practicum.shareit.item.dto.ItemDto;

import java.util.ArrayList;
import java.util.Map;

@Component
public class ResponseConverter {
    private final ObjectMapper objectMapper;

    public ResponseConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ResponseEntity<Object> ensureItemFields(ResponseEntity<Object> response) {
        if (response.getBody() instanceof ItemDto) {
            ItemDto itemDto = (ItemDto) response.getBody();
            if (itemDto.getLastBooking() == null) {
                itemDto.setLastBooking(null);
            }
            if (itemDto.getNextBooking() == null) {
                itemDto.setNextBooking(null);
            }
            if (itemDto.getComments() == null) {
                itemDto.setComments(new ArrayList<>());
            }
            return ResponseEntity.status(response.getStatusCode()).body(itemDto);
        } else if (response.getBody() instanceof Map) {
            Map<String, Object> body = (Map<String, Object>) response.getBody();
            if (!body.containsKey("lastBooking")) {
                body.put("lastBooking", null);
            }
            if (!body.containsKey("nextBooking")) {
                body.put("nextBooking", null);
            }
            if (!body.containsKey("comments")) {
                body.put("comments", new ArrayList<>());
            }
            return ResponseEntity.status(response.getStatusCode()).body(body);
        }
        return response;
    }
}