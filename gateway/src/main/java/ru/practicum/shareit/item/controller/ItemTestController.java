package ru.practicum.shareit.item.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.shareit.item.client.ItemClient;
import ru.practicum.shareit.item.dto.ItemDto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class ItemTestController {
    private static final String USER_ID_HEADER = "X-Sharer-User-Id";
    private final ItemClient itemClient;

    @GetMapping("/items/test/{id}")
    public Map<String, Object> getItemForTest(@PathVariable Long id, @RequestHeader(USER_ID_HEADER) Long userId) {
        ResponseEntity<Object> response = itemClient.getById(id, userId);

        Map<String, Object> result = new HashMap<>();

        if (response.getBody() instanceof Map) {
            result.putAll((Map<String, Object>) response.getBody());
        } else if (response.getBody() != null) {
            ItemDto item = (ItemDto) response.getBody();
            result.put("id", item.getId());
            result.put("name", item.getName());
            result.put("description", item.getDescription());
            result.put("available", item.getAvailable());
            result.put("requestId", item.getRequestId());
            result.put("comments", item.getComments() != null ? item.getComments() : new ArrayList<>());
        }

        result.put("lastBooking", null);
        result.put("nextBooking", null);

        return result;
    }
}