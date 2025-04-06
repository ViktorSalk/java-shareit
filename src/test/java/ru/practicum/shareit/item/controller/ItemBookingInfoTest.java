package ru.practicum.shareit.item.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import ru.practicum.shareit.booking.controller.BookingController;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.dto.BookingShortDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.user.UserMapper;
import ru.practicum.shareit.user.controller.UserController;
import ru.practicum.shareit.user.dto.UserDto;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ItemBookingInfoTest {

    @Autowired
    private ItemController itemController;

    @Autowired
    private UserController userController;

    @Autowired
    private BookingController bookingController;

    @Autowired
    private UserMapper userMapper;

    private UserDto ownerDto;
    private UserDto bookerDto;
    private ItemDto itemDto;

    @BeforeEach
    void setUp() {
        String ownerEmail = "owner" + System.currentTimeMillis() + "@email.com";
        ownerDto = UserDto.builder()
                .name("Item Owner")
                .email(ownerEmail)
                .build();
        ownerDto = userController.create(userMapper.toUser(ownerDto));

        String bookerEmail = "booker" + System.currentTimeMillis() + "@email.com";
        bookerDto = UserDto.builder()
                .name("Booker User")
                .email(bookerEmail)
                .build();
        bookerDto = userController.create(userMapper.toUser(bookerDto));

        itemDto = ItemDto.builder()
                .name("Test Item")
                .description("Test Description")
                .available(true)
                .build();
        itemDto = itemController.create(ownerDto.getId(), itemDto);
    }

    @Test // Тесты на получение бронирований для вещи владельца
    @DisplayName("Item should show booking info for owner")
    void itemShouldShowBookingInfoForOwner() {
        LocalDateTime nearFutureStart = LocalDateTime.now().plusHours(1);
        LocalDateTime nearFutureEnd = LocalDateTime.now().plusHours(2);
        BookingDto nearFutureBooking = BookingDto.builder()
                .itemId(itemDto.getId())
                .start(nearFutureStart)
                .end(nearFutureEnd)
                .build();
        BookingDto createdNearFutureBooking = bookingController.create(bookerDto.getId(), nearFutureBooking);
        bookingController.approve(ownerDto.getId(), createdNearFutureBooking.getId(), true);

        LocalDateTime farFutureStart = LocalDateTime.now().plusDays(1);
        LocalDateTime farFutureEnd = LocalDateTime.now().plusDays(2);
        BookingDto farFutureBooking = BookingDto.builder()
                .itemId(itemDto.getId())
                .start(farFutureStart)
                .end(farFutureEnd)
                .build();
        BookingDto createdFarFutureBooking = bookingController.create(bookerDto.getId(), farFutureBooking);
        bookingController.approve(ownerDto.getId(), createdFarFutureBooking.getId(), true);

        ItemDto itemWithBookings = itemController.getById(itemDto.getId(), ownerDto.getId());

        assertNotNull(itemWithBookings.getNextBooking());

        BookingShortDto nextBooking = itemWithBookings.getNextBooking();
        assertNotNull(nextBooking.getId());
        assertEquals(bookerDto.getId(), nextBooking.getBookerId());

        boolean isNearFutureBooking = nextBooking.getStart().equals(nearFutureStart) &&
                nextBooking.getEnd().equals(nearFutureEnd);
        boolean isFarFutureBooking = nextBooking.getStart().equals(farFutureStart) &&
                nextBooking.getEnd().equals(farFutureEnd);

        assertTrue(isNearFutureBooking || isFarFutureBooking,
                "Next booking should match either near future or far future booking");
    }

    @Test // Тесты на получение бронирований для вещей кроме вещей владельца
    @DisplayName("Item should not show booking info for non-owner")
    void itemShouldNotShowBookingInfoForNonOwner() {
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        LocalDateTime end = LocalDateTime.now().plusDays(2);
        BookingDto bookingDto = BookingDto.builder()
                .itemId(itemDto.getId())
                .start(start)
                .end(end)
                .build();
        bookingController.create(bookerDto.getId(), bookingDto);

        ItemDto itemForNonOwner = itemController.getById(itemDto.getId(), bookerDto.getId());

        assertNull(itemForNonOwner.getLastBooking());
        assertNull(itemForNonOwner.getNextBooking());
    }

    @Test // Тесты на получение бронирований для всех вещей владельца
    @DisplayName("All owner's items should show booking info")
    void allOwnerItemsShouldShowBookingInfo() {
        ItemDto secondItem = ItemDto.builder()
                .name("Second Item")
                .description("Another Description")
                .available(true)
                .build();
        secondItem = itemController.create(ownerDto.getId(), secondItem);

        LocalDateTime start1 = LocalDateTime.now().plusDays(1);
        LocalDateTime end1 = LocalDateTime.now().plusDays(2);
        BookingDto booking1 = BookingDto.builder()
                .itemId(itemDto.getId())
                .start(start1)
                .end(end1)
                .build();
        BookingDto createdBooking1 = bookingController.create(bookerDto.getId(), booking1);
        bookingController.approve(ownerDto.getId(), createdBooking1.getId(), true);

        LocalDateTime start2 = LocalDateTime.now().plusDays(3);
        LocalDateTime end2 = LocalDateTime.now().plusDays(4);
        BookingDto booking2 = BookingDto.builder()
                .itemId(secondItem.getId())
                .start(start2)
                .end(end2)
                .build();
        BookingDto createdBooking2 = bookingController.create(bookerDto.getId(), booking2);
        bookingController.approve(ownerDto.getId(), createdBooking2.getId(), true);

        List<ItemDto> ownerItems = itemController.getAll(ownerDto.getId());

        assertEquals(2, ownerItems.size());

        ItemDto firstItemResult = ownerItems.stream()
                .filter(item -> item.getId().equals(itemDto.getId()))
                .findFirst()
                .orElseThrow();

        ItemDto finalSecondItem = secondItem;
        ItemDto secondItemResult = ownerItems.stream()
                .filter(item -> item.getId().equals(finalSecondItem.getId()))
                .findFirst()
                .orElseThrow();

        assertNotNull(firstItemResult.getNextBooking());
        assertEquals(bookerDto.getId(), firstItemResult.getNextBooking().getBookerId());
        assertEquals(start1, firstItemResult.getNextBooking().getStart());
        assertEquals(end1, firstItemResult.getNextBooking().getEnd());

        assertNotNull(secondItemResult.getNextBooking());
        assertEquals(bookerDto.getId(), secondItemResult.getNextBooking().getBookerId());
        assertEquals(start2, secondItemResult.getNextBooking().getStart());
        assertEquals(end2, secondItemResult.getNextBooking().getEnd());
    }
}