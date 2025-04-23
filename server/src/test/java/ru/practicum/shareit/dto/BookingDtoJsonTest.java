package ru.practicum.shareit.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;
import org.springframework.test.context.ActiveProfiles;
import ru.practicum.shareit.booking.BookingStatus;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.user.dto.UserDto;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@JsonTest
@ActiveProfiles("test")
public class BookingDtoJsonTest {

    @Autowired
    private JacksonTester<BookingDto> json;

    @Test // Тест сериализации
    @DisplayName("Тест сериализации BookingDto в JSON")
    void testBookingDtoSerialization() throws Exception {
        LocalDateTime start = LocalDateTime.of(2023, 1, 1, 12, 0);
        LocalDateTime end = LocalDateTime.of(2023, 1, 2, 12, 0);

        ItemDto itemDto = ItemDto.builder()
                .id(1L)
                .name("Test Item")
                .build();

        UserDto userDto = UserDto.builder()
                .id(2L)
                .name("Test User")
                .build();

        BookingDto bookingDto = BookingDto.builder()
                .id(1L)
                .start(start)
                .end(end)
                .item(itemDto)
                .booker(userDto)
                .status(BookingStatus.WAITING)
                .build();

        JsonContent<BookingDto> result = json.write(bookingDto);

        assertThat(result).extractingJsonPathNumberValue("$.id").isEqualTo(1);
        assertThat(result).extractingJsonPathStringValue("$.start").isNotEmpty();
        assertThat(result).extractingJsonPathStringValue("$.end").isNotEmpty();
        assertThat(result).extractingJsonPathNumberValue("$.item.id").isEqualTo(1);
        assertThat(result).extractingJsonPathStringValue("$.item.name").isEqualTo("Test Item");
        assertThat(result).extractingJsonPathNumberValue("$.booker.id").isEqualTo(2);
        assertThat(result).extractingJsonPathStringValue("$.booker.name").isEqualTo("Test User");
        assertThat(result).extractingJsonPathStringValue("$.status").isEqualTo("WAITING");
    }

    @Test // Тест десериализации
    @DisplayName("Тест десериализации JSON в BookingDto")
    void testBookingDtoDeserialization() throws Exception {
        String jsonContent = "{\"id\":1,\"start\":\"2023-01-01T12:00:00\",\"end\":\"2023-01-02T12:00:00\",\"item\":{\"id\":1,\"name\":\"Test Item\"},\"booker\":{\"id\":2,\"name\":\"Test User\"},\"status\":\"WAITING\"}";

        BookingDto bookingDto = json.parse(jsonContent).getObject();

        assertThat(bookingDto.getId()).isEqualTo(1L);
        assertThat(bookingDto.getStart()).isEqualTo(LocalDateTime.of(2023, 1, 1, 12, 0, 0));
        assertThat(bookingDto.getEnd()).isEqualTo(LocalDateTime.of(2023, 1, 2, 12, 0, 0));
        assertThat(bookingDto.getItem().getId()).isEqualTo(1L);
        assertThat(bookingDto.getItem().getName()).isEqualTo("Test Item");
        assertThat(bookingDto.getBooker().getId()).isEqualTo(2L);
        assertThat(bookingDto.getBooker().getName()).isEqualTo("Test User");
        assertThat(bookingDto.getStatus()).isEqualTo(BookingStatus.WAITING);
    }
}