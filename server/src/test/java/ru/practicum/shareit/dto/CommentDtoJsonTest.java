package ru.practicum.shareit.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;
import org.springframework.test.context.ActiveProfiles;
import ru.practicum.shareit.item.dto.CommentDto;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@JsonTest
@ActiveProfiles("test")
public class CommentDtoJsonTest {

    @Autowired
    private JacksonTester<CommentDto> json;

    @Test // Тест сериализации
    @DisplayName("Тест сериализации CommentDto в JSON")
    void testCommentDtoSerialization() throws Exception {
        LocalDateTime created = LocalDateTime.of(2023, 1, 1, 12, 0);

        CommentDto commentDto = CommentDto.builder()
                .id(1L)
                .text("Test Comment")
                .authorName("Author")
                .created(created)
                .build();

        JsonContent<CommentDto> result = json.write(commentDto);

        assertThat(result).extractingJsonPathNumberValue("$.id").isEqualTo(1);
        assertThat(result).extractingJsonPathStringValue("$.text").isEqualTo("Test Comment");
        assertThat(result).extractingJsonPathStringValue("$.authorName").isEqualTo("Author");
        assertThat(result).extractingJsonPathStringValue("$.created").isNotEmpty();
    }

    @Test // Тест десериализации
    @DisplayName("Тест десериализации JSON в CommentDto")
    void testCommentDtoDeserialization() throws Exception {
        String jsonContent = "{\"id\":1,\"text\":\"Test Comment\",\"authorName\":\"Author\",\"created\":\"2023-01-01T12:00:00\"}";

        CommentDto commentDto = json.parse(jsonContent).getObject();

        assertThat(commentDto.getId()).isEqualTo(1L);
        assertThat(commentDto.getText()).isEqualTo("Test Comment");
        assertThat(commentDto.getAuthorName()).isEqualTo("Author");
        assertThat(commentDto.getCreated()).isEqualTo(LocalDateTime.of(2023, 1, 1, 12, 0, 0));
    }
}