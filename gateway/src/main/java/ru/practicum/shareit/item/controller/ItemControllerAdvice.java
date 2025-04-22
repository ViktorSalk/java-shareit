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
import java.util.HashMap;
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
                Map<String, Object> bodyMap = convertToMap(body);

                ensureFieldExists(bodyMap, "lastBooking", null);
                ensureFieldExists(bodyMap, "nextBooking", null);
                ensureFieldExists(bodyMap, "comments", new ArrayList<>());

                return bodyMap;
            } catch (Exception e) {
                return body;
            }
        }
        return body;
    }

    private Map<String, Object> convertToMap(Object object) {
        if (object instanceof Map) {
            return new HashMap<>((Map<String, Object>) object);
        }

        return objectMapper.convertValue(object, Map.class);
    }

    private void ensureFieldExists(Map<String, Object> map, String fieldName, Object defaultValue) {
        if (!map.containsKey(fieldName)) {
            map.put(fieldName, defaultValue);
        }
    }
}