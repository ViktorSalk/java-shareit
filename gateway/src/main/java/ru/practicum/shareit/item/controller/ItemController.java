package ru.practicum.shareit.item.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import ru.practicum.shareit.item.client.ItemClient;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.ItemDto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/items")
@RequiredArgsConstructor
@Slf4j
@Validated
public class ItemController {
    private final ItemClient itemClient;
    private final ObjectMapper objectMapper;
    private static final String USER_ID_HEADER = "X-Sharer-User-Id";

    @GetMapping
    public ResponseEntity<Object> getAll(@RequestHeader(USER_ID_HEADER) Long userId) {
        log.info("Get items by owner userId={}", userId);
        return itemClient.getAll(userId);
    }

    @GetMapping("/{id}")
    @ResponseBody
    public Map<String, Object> getById(@PathVariable Long id, @RequestHeader(USER_ID_HEADER) Long userId) {
        log.info("Get item {}, userId={}", id, userId);
        ResponseEntity<Object> response = itemClient.getById(id, userId);

        Map<String, Object> result = new HashMap<>();

        if (response.getBody() instanceof Map) {
            result.putAll((Map<String, Object>) response.getBody());
        } else if (response.getBody() != null) {
            result = objectMapper.convertValue(response.getBody(), Map.class);
        }

        result.put("lastBooking", null);
        result.put("nextBooking", null);

        if (!result.containsKey("comments")) {
            result.put("comments", new ArrayList<>());
        }

        return result;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<Object> create(@RequestHeader(USER_ID_HEADER) Long userId,
                                         @Valid @RequestBody ItemDto itemDto) {
        log.info("Creating item {}, userId={}", itemDto, userId);
        return itemClient.create(itemDto, userId);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Object> update(@RequestBody ItemDto itemDto,
                                         @PathVariable Long id,
                                         @RequestHeader(USER_ID_HEADER) Long userId) {
        log.info("Updating item {}, userId={}", id, userId);
        return itemClient.update(itemDto, id, userId);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Object> delete(@PathVariable Long id) {
        log.info("Deleting item {}", id);
        return itemClient.delete(id);
    }

    @GetMapping("/search")
    public ResponseEntity<Object> search(@RequestParam String text) {
        log.info("Searching items by text={}", text);
        return itemClient.search(text);
    }

    @PostMapping("/{itemId}/comment")
    public ResponseEntity<Object> createComment(@PathVariable Long itemId,
                                                @Valid @RequestBody CommentDto commentDto,
                                                @RequestHeader(USER_ID_HEADER) Long userId) {
        log.info("Creating comment {}, itemId={}, userId={}", commentDto, itemId, userId);
        return itemClient.createComment(itemId, commentDto, userId);
    }
}