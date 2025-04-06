package ru.practicum.shareit.item.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import ru.practicum.shareit.booking.controller.BookingController;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.exception.ShareItException;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.user.UserMapper;
import ru.practicum.shareit.user.controller.UserController;
import ru.practicum.shareit.user.dto.UserDto;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class CommentControllerTest {

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
    private BookingDto bookingDto;

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

        LocalDateTime start = LocalDateTime.now().plusHours(1);
        LocalDateTime end = LocalDateTime.now().plusHours(2);
        bookingDto = BookingDto.builder()
                .itemId(itemDto.getId())
                .start(start)
                .end(end)
                .build();
    }

    @Test // Тест на успешное добавление комментария без бронирования
    @DisplayName("Error when adding comment without booking")
    void errorWhenAddingCommentWithoutBooking() {
        CommentDto commentDto = CommentDto.builder()
                .text("I haven't rented this item")
                .build();

        assertThrows(ShareItException.BadRequestException.class,
                () -> itemController.createComment(itemDto.getId(), commentDto, bookerDto.getId()));
    }

    @Test // Тест на успешное добавление комментария с бронированием
    @DisplayName("Error when adding comment with future booking")
    void errorWhenAddingCommentWithFutureBooking() {
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        LocalDateTime end = LocalDateTime.now().plusDays(2);
        BookingDto futureBooking = BookingDto.builder()
                .itemId(itemDto.getId())
                .start(start)
                .end(end)
                .build();

        BookingDto createdBooking = bookingController.create(bookerDto.getId(), futureBooking);
        bookingController.approve(ownerDto.getId(), createdBooking.getId(), true);

        CommentDto commentDto = CommentDto.builder()
                .text("I haven't used this item yet")
                .build();

        assertThrows(ShareItException.BadRequestException.class,
                () -> itemController.createComment(itemDto.getId(), commentDto, bookerDto.getId()));
    }
}