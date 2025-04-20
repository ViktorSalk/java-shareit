package ru.practicum.shareit.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;
import org.springframework.test.context.ActiveProfiles;
import ru.practicum.shareit.booking.dto.BookingShortDto;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.ItemDto;

import java.time.LocalDateTime;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

@JsonTest
@ActiveProfiles("test")
public class ItemDtoJsonTest {

    @Autowired
    private JacksonTester<ItemDto> json;

    @Test // Тест сериализации JSON
    @DisplayName("Тест сериализации ItemDto в JSON")
    void testItemDtoSerialization() throws Exception {
        LocalDateTime now = LocalDateTime.now();

        BookingShortDto lastBooking = BookingShortDto.builder()
                .id(1L)
                .bookerId(2L)
                .start(now.minusDays(2))
                .end(now.minusDays(1))
                .build();

        BookingShortDto nextBooking = BookingShortDto.builder()
                .id(2L)
                .bookerId(3L)
                .start(now.plusDays(1))
                .end(now.plusDays(2))
                .build();

        CommentDto comment = CommentDto.builder()
                .id(1L)
                .text("Test Comment")
                .authorName("Author")
                .created(now)
                .build();

        ItemDto itemDto = ItemDto.builder()
                .id(1L)
                .name("Test Item")
                .description("Test Description")
                .available(true)
                .lastBooking(lastBooking)
                .nextBooking(nextBooking)
                .comments(Arrays.asList(comment))
                .build();

        JsonContent<ItemDto> result = json.write(itemDto);

        assertThat(result).extractingJsonPathNumberValue("$.id").isEqualTo(1);
        assertThat(result).extractingJsonPathStringValue("$.name").isEqualTo("Test Item");
        assertThat(result).extractingJsonPathStringValue("$.description").isEqualTo("Test Description");
        assertThat(result).extractingJsonPathBooleanValue("$.available").isEqualTo(true);
        assertThat(result).extractingJsonPathNumberValue("$.lastBooking.id").isEqualTo(1);
        assertThat(result).extractingJsonPathNumberValue("$.nextBooking.id").isEqualTo(2);
        assertThat(result).extractingJsonPathArrayValue("$.comments").hasSize(1);
        assertThat(result).extractingJsonPathStringValue("$.comments[0].text").isEqualTo("Test Comment");
    }

    @Test // Тест десериализации JSON
    @DisplayName("Тест десериализации JSON в ItemDto")
    void testItemDtoDeserialization() throws Exception {
        String jsonContent = "{\"id\":1,\"name\":\"Test Item\",\"description\":\"Test Description\",\"available\":true,\"lastBooking\":{\"id\":1,\"bookerId\":2},\"nextBooking\":{\"id\":2,\"bookerId\":3},\"comments\":[{\"id\":1,\"text\":\"Test Comment\",\"authorName\":\"Author\"}]}";

        ItemDto itemDto = json.parse(jsonContent).getObject();

        assertThat(itemDto.getId()).isEqualTo(1L);
        assertThat(itemDto.getName()).isEqualTo("Test Item");
        assertThat(itemDto.getDescription()).isEqualTo("Test Description");
        assertThat(itemDto.getAvailable()).isEqualTo(true);
        assertThat(itemDto.getLastBooking().getId()).isEqualTo(1L);
        assertThat(itemDto.getLastBooking().getBookerId()).isEqualTo(2L);
        assertThat(itemDto.getNextBooking().getId()).isEqualTo(2L);
        assertThat(itemDto.getNextBooking().getBookerId()).isEqualTo(3L);
        assertThat(itemDto.getComments()).hasSize(1);
        assertThat(itemDto.getComments().get(0).getText()).isEqualTo("Test Comment");
        assertThat(itemDto.getComments().get(0).getAuthorName()).isEqualTo("Author");
    }
}