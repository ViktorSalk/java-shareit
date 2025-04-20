package ru.practicum.shareit.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.BookingStatus;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.service.BookingService;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.service.ItemService;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.service.UserService;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class BookingServiceIntegrationTest {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private ItemService itemService;

    @Autowired
    private UserService userService;

    private UserDto owner;
    private UserDto booker;
    private ItemDto item;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        now = LocalDateTime.now();

        User ownerUser = User.builder()
                .name("Owner")
                .email("owner" + System.currentTimeMillis() + "@test.com")
                .build();
        owner = userService.create(ownerUser);

        User bookerUser = User.builder()
                .name("Booker")
                .email("booker" + System.currentTimeMillis() + "@test.com")
                .build();
        booker = userService.create(bookerUser);

        ItemDto itemDto = ItemDto.builder()
                .name("Test Item")
                .description("Test Description")
                .available(true)
                .build();
        item = itemService.create(itemDto, owner.getId());
    }

    @Test // Интеграционный тест: создание и получение бронирования
    @DisplayName("Интеграционный тест: создание и получение бронирования")
    void createAndGetBookingIntegrationTest() {
        BookingDto bookingDto = BookingDto.builder()
                .itemId(item.getId())
                .start(now.plusDays(1))
                .end(now.plusDays(2))
                .build();

        BookingDto createdBooking = bookingService.create(bookingDto, booker.getId());

        assertNotNull(createdBooking);
        assertNotNull(createdBooking.getId());
        assertEquals(BookingStatus.WAITING, createdBooking.getStatus());
        assertEquals(item.getId(), createdBooking.getItem().getId());
        assertEquals(booker.getId(), createdBooking.getBooker().getId());

        BookingDto retrievedBooking = bookingService.getById(createdBooking.getId(), booker.getId());

        assertNotNull(retrievedBooking);
        assertEquals(createdBooking.getId(), retrievedBooking.getId());
        assertEquals(createdBooking.getStatus(), retrievedBooking.getStatus());
        assertEquals(createdBooking.getItem().getId(), retrievedBooking.getItem().getId());
        assertEquals(createdBooking.getBooker().getId(), retrievedBooking.getBooker().getId());
    }

    @Test // Интеграционный тест: получение бронирований по арендатору
    @DisplayName("Интеграционный тест: получение бронирований по арендатору")
    void getBookingsByBookerIntegrationTest() {
        BookingDto booking1 = BookingDto.builder()
                .itemId(item.getId())
                .start(now.plusDays(1))
                .end(now.plusDays(2))
                .build();
        BookingDto booking2 = BookingDto.builder()
                .itemId(item.getId())
                .start(now.plusDays(3))
                .end(now.plusDays(4))
                .build();

        BookingDto createdBooking1 = bookingService.create(booking1, booker.getId());
        BookingDto createdBooking2 = bookingService.create(booking2, booker.getId());

        List<BookingDto> bookings = bookingService.getAllByBooker(booker.getId(), "ALL", 0, 10);

        assertNotNull(bookings);
        assertEquals(2, bookings.size());
        assertTrue(bookings.stream().anyMatch(b -> b.getId().equals(createdBooking1.getId())));
        assertTrue(bookings.stream().anyMatch(b -> b.getId().equals(createdBooking2.getId())));
    }
}