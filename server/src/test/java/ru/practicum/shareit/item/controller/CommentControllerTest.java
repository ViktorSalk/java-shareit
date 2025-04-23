package ru.practicum.shareit.item.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import ru.practicum.shareit.ShareItServerApp;
import ru.practicum.shareit.booking.BookingStatus;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.exception.ShareItException;
import ru.practicum.shareit.booking.repository.BookingRepository;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.service.ItemService;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.service.UserService;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@SpringBootTest(classes = ShareItServerApp.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class CommentControllerTest {

    @Autowired
    private ItemController itemController;

    @Autowired
    private UserService userService;

    @Autowired
    private ItemService itemService;

    @MockBean
    private BookingRepository bookingRepository;

    private User owner;
    private User booker;
    private ItemDto itemDto;
    private BookingDto bookingDto;

    @BeforeEach
    void setUp() {
        owner = User.builder()
                .name("Item Owner")
                .email("owner" + System.currentTimeMillis() + "@email.com")
                .build();
        UserDto ownerDto = userService.create(owner);
        owner.setId(ownerDto.getId());

        booker = User.builder()
                .name("Booker User")
                .email("booker" + System.currentTimeMillis() + "@email.com")
                .build();
        UserDto bookerDto = userService.create(booker);
        booker.setId(bookerDto.getId());

        Item item = Item.builder()
                .name("Test Item")
                .description("Test Description")
                .available(true)
                .build();
        itemDto = itemService.create(ItemDto.builder()
                .name(item.getName())
                .description(item.getDescription())
                .available(item.getAvailable())
                .build(), owner.getId());

        LocalDateTime start = LocalDateTime.now().plusHours(1);
        LocalDateTime end = LocalDateTime.now().plusHours(2);
        bookingDto = BookingDto.builder()
                .itemId(itemDto.getId())
                .start(start)
                .end(end)
                .build();
    }

    @Test
    @DisplayName("Error when adding comment without booking")
    void errorWhenAddingCommentWithoutBooking() {
        when(bookingRepository.hasUserBookedItem(anyLong(), anyLong(), ArgumentMatchers.any(LocalDateTime.class)))
                .thenReturn(false);

        CommentDto commentDto = CommentDto.builder()
                .text("I haven't rented this item")
                .build();

        assertThrows(ShareItException.BadRequestException.class,
                () -> itemController.createComment(itemDto.getId(), commentDto, booker.getId()));
    }

    @Test
    @DisplayName("Error when adding comment with future booking")
    void errorWhenAddingCommentWithFutureBooking() {
        when(bookingRepository.hasUserBookedItem(anyLong(), anyLong(), ArgumentMatchers.any(LocalDateTime.class)))
                .thenReturn(false);

        LocalDateTime start = LocalDateTime.now().plusDays(1);
        LocalDateTime end = LocalDateTime.now().plusDays(2);
        BookingDto futureBooking = BookingDto.builder()
                .itemId(itemDto.getId())
                .start(start)
                .end(end)
                .status(BookingStatus.APPROVED)
                .build();

        CommentDto commentDto = CommentDto.builder()
                .text("I haven't used this item yet")
                .build();

        assertThrows(ShareItException.BadRequestException.class,
                () -> itemController.createComment(itemDto.getId(), commentDto, booker.getId()));
    }
}