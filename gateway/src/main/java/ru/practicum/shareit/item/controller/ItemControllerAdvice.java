package ru.practicum.shareit.item.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.util.ArrayList;
import java.util.Map;

@ControllerAdvice(basePackages = "ru.practicum.shareit.item.controller")
public class ItemControllerAdvice implements ResponseBodyAdvice<Object> {

    private final ObjectMapper objectMapper;

    public ItemControllerAdvice(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        if (body == null) {
            return body;
        }

        if (request.getURI().getPath().matches("/items/\\d+") && "GET".equals(request.getMethod().name())) {
            try {
                Map<String, Object> bodyMap;
                if (body instanceof Map) {
                    bodyMap = (Map<String, Object>) body;
                } else {
                    bodyMap = objectMapper.convertValue(body, Map.class);
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

                return bodyMap;
            } catch (Exception e) {
                return body;
            }
        }
        return body;
    }
}