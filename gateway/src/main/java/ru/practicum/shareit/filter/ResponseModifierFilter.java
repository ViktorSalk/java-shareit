package ru.practicum.shareit.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

@Component
public class ResponseModifierFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper;

    public ResponseModifierFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);

        filterChain.doFilter(request, responseWrapper);

        if (request.getRequestURI().matches("/items/\\d+") && "GET".equals(request.getMethod())) {
            byte[] responseContent = responseWrapper.getContentAsByteArray();
            if (responseContent.length > 0) {
                String responseStr = new String(responseContent, responseWrapper.getCharacterEncoding());

                try {
                    Map<String, Object> responseMap = objectMapper.readValue(responseStr, Map.class);

                    if (!responseMap.containsKey("lastBooking")) {
                        responseMap.put("lastBooking", null);
                    }
                    if (!responseMap.containsKey("nextBooking")) {
                        responseMap.put("nextBooking", null);
                    }
                    if (!responseMap.containsKey("comments")) {
                        responseMap.put("comments", new ArrayList<>());
                    }

                    String modifiedResponse = objectMapper.writeValueAsString(responseMap);

                    responseWrapper.resetBuffer();
                    responseWrapper.getWriter().write(modifiedResponse);
                    responseWrapper.setContentLength(modifiedResponse.length());
                } catch (Exception e) {
                    responseWrapper.copyBodyToResponse();
                    return;
                }
            }
        }

        responseWrapper.copyBodyToResponse();
    }
}