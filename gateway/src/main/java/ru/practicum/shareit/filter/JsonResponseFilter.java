package ru.practicum.shareit.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;
import ru.practicum.shareit.util.JsonResponseModifier;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@Order(1)
public class JsonResponseFilter extends OncePerRequestFilter {

    private final JsonResponseModifier jsonResponseModifier;

    public JsonResponseFilter(JsonResponseModifier jsonResponseModifier) {
        this.jsonResponseModifier = jsonResponseModifier;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);

        filterChain.doFilter(request, responseWrapper);

        String path = request.getRequestURI();
        if (path.matches("/items/\\d+") && "GET".equals(request.getMethod()) &&
                response.getContentType() != null && response.getContentType().contains("application/json")) {

            byte[] content = responseWrapper.getContentAsByteArray();
            if (content.length > 0) {
                String json = new String(content, StandardCharsets.UTF_8);
                String modifiedJson = jsonResponseModifier.ensureItemFields(json);

                responseWrapper.resetBuffer();
                responseWrapper.getWriter().write(modifiedJson);
                responseWrapper.setContentLength(modifiedJson.length());
            }
        }

        responseWrapper.copyBodyToResponse();
    }
}