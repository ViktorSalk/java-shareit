package ru.practicum.shareit.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class JsonResponseModifier {

    private final ObjectMapper objectMapper;

    public JsonResponseModifier(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String ensureItemFields(String json) {
        try {
            JsonNode rootNode = objectMapper.readTree(json);

            if (rootNode.isObject()) {
                ObjectNode objectNode = (ObjectNode) rootNode;

                if (!objectNode.has("lastBooking")) {
                    objectNode.putNull("lastBooking");
                }

                if (!objectNode.has("nextBooking")) {
                    objectNode.putNull("nextBooking");
                }

                if (!objectNode.has("comments")) {
                    objectNode.putArray("comments");
                }

                return objectMapper.writeValueAsString(objectNode);
            }

            return json;
        } catch (IOException e) {
            return json;
        }
    }
}