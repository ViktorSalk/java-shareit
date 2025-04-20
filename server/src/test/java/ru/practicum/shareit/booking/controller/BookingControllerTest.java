package ru.practicum.shareit.booking.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.ShareItServerApp;
import ru.practicum.shareit.booking.BookingStatus;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.exception.ShareItException;
import ru.practicum.shareit.booking.service.BookingService;
import ru.practicum.shareit.item.ItemMapper;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.service.ItemService;
import ru.practicum.shareit.user.UserMapper;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.service.UserService;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@SpringBootTest(classes = ShareItServerApp.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class BookingControllerTest {

    @Autowired
    private BookingController bookingController;

    @MockBean
    private BookingService bookingService;

    @Autowired
    private ItemService itemService;

    @Autowired
    private UserService userService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ItemMapper itemMapper;

    private UserDto ownerDto;
    private UserDto bookerDto;
    private ItemDto itemDto;
    private BookingDto bookingDto;

    @BeforeEach
    void setUp() {
        String ownerEmail = "owner" + System.currentTimeMillis() + "@email.com";
        User owner = User.builder()
                .name("Item Owner")
                .email(ownerEmail)
                .build();
        ownerDto = userService.create(owner);

        String bookerEmail = "booker" + System.currentTimeMillis() + "@email.com";
        User booker = User.builder()
                .name("Booker User")
                .email(bookerEmail)
                .build();
        bookerDto = userService.create(booker);

        Item item = Item.builder()
                .name("Test Item")
                .description("Test Description")
                .available(true)
                .build();
        itemDto = itemService.create(itemMapper.toItemDto(item), ownerDto.getId());

        LocalDateTime start = LocalDateTime.now().plusDays(1);
        LocalDateTime end = LocalDateTime.now().plusDays(2);
        bookingDto = BookingDto.builder()
                .itemId(itemDto.getId())
                .start(start)
                .end(end)
                .build();

        when(bookingService.create(any(BookingDto.class), anyLong())).thenAnswer(invocation -> {
            BookingDto dto = invocation.getArgument(0);

            return BookingDto.builder()
                    .id(1L)
                    .start(dto.getStart())
                    .end(dto.getEnd())
                    .item(itemDto)
                    .booker(bookerDto)
                    .status(BookingStatus.WAITING)
                    .build();
        });

        when(bookingService.approve(anyLong(), anyLong(), anyBoolean())).thenAnswer(invocation -> {
            Long bookingId = invocation.getArgument(0);
            Boolean approved = invocation.getArgument(2);

            return BookingDto.builder()
                    .id(bookingId)
                    .start(bookingDto.getStart())
                    .end(bookingDto.getEnd())
                    .item(itemDto)
                    .booker(bookerDto)
                    .status(approved ? BookingStatus.APPROVED : BookingStatus.REJECTED)
                    .build();
        });

        when(bookingService.getById(anyLong(), anyLong())).thenAnswer(invocation -> {
            Long bookingId = invocation.getArgument(0);

            return BookingDto.builder()
                    .id(bookingId)
                    .start(bookingDto.getStart())
                    .end(bookingDto.getEnd())
                    .item(itemDto)
                    .booker(bookerDto)
                    .status(BookingStatus.WAITING)
                    .build();
        });

        when(bookingService.getAllByBooker(anyLong(), anyString(), anyInt(), anyInt())).thenReturn(
                List.of(BookingDto.builder()
                        .id(1L)
                        .start(bookingDto.getStart())
                        .end(bookingDto.getEnd())
                        .item(itemDto)
                        .booker(bookerDto)
                        .status(BookingStatus.WAITING)
                        .build())
        );

        when(bookingService.getAllByOwner(anyLong(), anyString(), anyInt(), anyInt())).thenReturn(
                List.of(BookingDto.builder()
                        .id(1L)
                        .start(bookingDto.getStart())
                        .end(bookingDto.getEnd())
                        .item(itemDto)
                        .booker(bookerDto)
                        .status(BookingStatus.WAITING)
                        .build())
        );
    }

    @Nested // Тесты для создания бронирований
    @DisplayName("Creating bookings")
    class CreateBookingTests {
        @Test
        @Transactional
        @DisplayName("Successful booking creation")
        void createBookingTest() {
            BookingDto createdBooking = bookingController.createBooking(bookingDto, bookerDto.getId());

            assertNotNull(createdBooking);
            assertNotNull(createdBooking.getId());
            assertEquals(bookingDto.getStart(), createdBooking.getStart());
            assertEquals(bookingDto.getEnd(), createdBooking.getEnd());
            assertEquals(itemDto.getId(), createdBooking.getItem().getId());
            assertEquals(bookerDto.getId(), createdBooking.getBooker().getId());
            assertEquals(BookingStatus.WAITING, createdBooking.getStatus());
        }

        @Test
        @Transactional
        @DisplayName("Error when owner tries to book their own item")
        void ownerBookingOwnItemTest() {
            when(bookingService.create(any(BookingDto.class), eq(ownerDto.getId())))
                    .thenThrow(new ShareItException.NotFoundException("Владелец не может забронировать свою вещь"));

            assertThrows(ShareItException.NotFoundException.class,
                    () -> bookingController.createBooking(bookingDto, ownerDto.getId()));
        }

        @Test
        @Transactional
        @DisplayName("Error when booking unavailable item")
        void bookingUnavailableItemTest() {
            when(bookingService.create(any(BookingDto.class), eq(bookerDto.getId())))
                    .thenThrow(new ShareItException.BadRequestException("Вещь недоступна для бронирования"));

            assertThrows(ShareItException.BadRequestException.class,
                    () -> bookingController.createBooking(bookingDto, bookerDto.getId()));
        }

        @Test
        @Transactional
        @DisplayName("Error when booking with invalid dates")
        void bookingWithInvalidDatesTest() {
            BookingDto invalidBooking = BookingDto.builder()
                    .itemId(itemDto.getId())
                    .start(LocalDateTime.now().plusDays(2))
                    .end(LocalDateTime.now().plusDays(1))
                    .build();

            when(bookingService.create(eq(invalidBooking), eq(bookerDto.getId())))
                    .thenThrow(new ShareItException.BadRequestException("Некорректные даты бронирования"));

            assertThrows(ShareItException.BadRequestException.class,
                    () -> bookingController.createBooking(invalidBooking, bookerDto.getId()));
        }
    }

    @Nested // Тесты для утверждения бронирований
    @DisplayName("Approving bookings")
    class ApproveBookingTests {
        @Test
        @Transactional
        @DisplayName("Successful booking approval")
        void approveBookingTest() {
            BookingDto createdBooking = bookingController.createBooking(bookingDto, bookerDto.getId());

            BookingDto approvedBooking = bookingController.approve(createdBooking.getId(), ownerDto.getId(), true);

            assertEquals(BookingStatus.APPROVED, approvedBooking.getStatus());
        }

        @Test
        @Transactional
        @DisplayName("Successful booking rejection")
        void rejectBookingTest() {
            BookingDto createdBooking = bookingController.createBooking(bookingDto, bookerDto.getId());

            BookingDto rejectedBooking = bookingController.approve(createdBooking.getId(), ownerDto.getId(), false);

            assertEquals(BookingStatus.REJECTED, rejectedBooking.getStatus());
        }

        @Test
        @Transactional
        @DisplayName("Error when non-owner tries to approve booking")
        void nonOwnerApprovingBookingTest() {
            BookingDto createdBooking = bookingController.createBooking(bookingDto, bookerDto.getId());

            String randomEmail = "random" + System.currentTimeMillis() + "@email.com";
            User randomUser = User.builder()
                    .name("Random User")
                    .email(randomEmail)
                    .build();
            UserDto randomUserDto = userService.create(randomUser);

            when(bookingService.approve(eq(createdBooking.getId()), eq(randomUserDto.getId()), anyBoolean()))
                    .thenThrow(new ShareItException.ForbiddenException("Только владелец вещи может подтвердить бронирование"));

            assertThrows(ShareItException.ForbiddenException.class,
                    () -> bookingController.approve(createdBooking.getId(), randomUserDto.getId(), true));
        }

        @Test
        @Transactional
        @DisplayName("Error when approving already processed booking")
        void approvingProcessedBookingTest() {
            BookingDto createdBooking = bookingController.createBooking(bookingDto, bookerDto.getId());

            bookingController.approve(createdBooking.getId(), ownerDto.getId(), true);

            when(bookingService.approve(eq(createdBooking.getId()), eq(ownerDto.getId()), anyBoolean()))
                    .thenThrow(new ShareItException.BadRequestException("Бронирование уже обработано"));

            assertThrows(ShareItException.BadRequestException.class,
                    () -> bookingController.approve(createdBooking.getId(), ownerDto.getId(), true));
        }
    }

    @Nested // Тесты для получения бронирований
    @DisplayName("Getting bookings")
    class GetBookingTests {
        @Test
        @Transactional
        @DisplayName("Getting booking by ID")
        void getBookingByIdTest() {
            BookingDto createdBooking = bookingController.createBooking(bookingDto, bookerDto.getId());

            BookingDto retrievedBooking = bookingController.getById(createdBooking.getId(), bookerDto.getId());

            assertNotNull(retrievedBooking);
            assertEquals(createdBooking.getId(), retrievedBooking.getId());
            assertEquals(createdBooking.getStart(), retrievedBooking.getStart());
            assertEquals(createdBooking.getEnd(), retrievedBooking.getEnd());
        }

        @Test
        @Transactional
        @DisplayName("Error when unauthorized user tries to get booking")
        void unauthorizedGetBookingTest() {
            BookingDto createdBooking = bookingController.createBooking(bookingDto, bookerDto.getId());

            String randomEmail = "random" + System.currentTimeMillis() + "@email.com";
            User randomUser = User.builder()
                    .name("Random User")
                    .email(randomEmail)
                    .build();
            UserDto randomUserDto = userService.create(randomUser);

            when(bookingService.getById(eq(createdBooking.getId()), eq(randomUserDto.getId())))
                    .thenThrow(new ShareItException.NotFoundException("Доступ запрещен"));

            assertThrows(ShareItException.NotFoundException.class,
                    () -> bookingController.getById(createdBooking.getId(), randomUserDto.getId()));
        }

        @Test
        @Transactional
        @DisplayName("Getting all bookings by booker")
        void getAllBookingsByBookerTest() {
            bookingController.createBooking(bookingDto, bookerDto.getId());

            List<BookingDto> bookings = bookingController.getAllByBooker(bookerDto.getId(), "ALL", 0, 10);

            assertNotNull(bookings);
            assertFalse(bookings.isEmpty());
            assertEquals(1, bookings.size());
            assertEquals(bookingDto.getStart(), bookings.get(0).getStart());
            assertEquals(bookingDto.getEnd(), bookings.get(0).getEnd());
        }

        @Test
        @Transactional
        @DisplayName("Getting all bookings by owner")
        void getAllBookingsByOwnerTest() {
            bookingController.createBooking(bookingDto, bookerDto.getId());

            List<BookingDto> bookings = bookingController.getAllByOwner(ownerDto.getId(), "ALL", 0, 10);

            assertNotNull(bookings);
            assertFalse(bookings.isEmpty());
            assertEquals(1, bookings.size());
            assertEquals(bookingDto.getStart(), bookings.get(0).getStart());
            assertEquals(bookingDto.getEnd(), bookings.get(0).getEnd());
        }

        @Test
        @Transactional
        @DisplayName("Getting bookings with different states")
        void getBookingsWithDifferentStatesTest() {
            bookingController.createBooking(bookingDto, bookerDto.getId());

            String[] states = {"CURRENT", "PAST", "FUTURE", "WAITING", "REJECTED"};

            for (String state : states) {
                List<BookingDto> bookings = bookingController.getAllByBooker(bookerDto.getId(), state, 0, 10);

                assertNotNull(bookings);
            }
        }

        @Test
        @Transactional
        @DisplayName("Error when using invalid state")
        void invalidStateTest() {
            when(bookingService.getAllByBooker(eq(bookerDto.getId()), eq("INVALID_STATE"), anyInt(), anyInt()))
                    .thenThrow(new ShareItException.BadRequestException("Unknown state: INVALID_STATE"));

            assertThrows(ShareItException.BadRequestException.class,
                    () -> bookingController.getAllByBooker(bookerDto.getId(), "INVALID_STATE", 0, 10));
        }
    }

    @Nested // Тесты для пагинации
    @DisplayName("Pagination tests")
    class PaginationTests {
        @Test
        @Transactional
        @DisplayName("Pagination for booker bookings")
        void paginationForBookerBookingsTest() {
            bookingController.createBooking(bookingDto, bookerDto.getId());

            List<BookingDto> response1 = bookingController.getAllByBooker(bookerDto.getId(), "ALL", 0, 5);
            List<BookingDto> response2 = bookingController.getAllByBooker(bookerDto.getId(), "ALL", 0, 10);
            List<BookingDto> response3 = bookingController.getAllByBooker(bookerDto.getId(), "ALL", 5, 5);

            assertNotNull(response1);
            assertNotNull(response2);
            assertNotNull(response3);
        }

        @Test
        @Transactional
        @DisplayName("Pagination for owner bookings")
        void paginationForOwnerBookingsTest() {
            bookingController.createBooking(bookingDto, bookerDto.getId());

            List<BookingDto> response1 = bookingController.getAllByOwner(ownerDto.getId(), "ALL", 0, 5);
            List<BookingDto> response2 = bookingController.getAllByOwner(ownerDto.getId(), "ALL", 0, 10);
            List<BookingDto> response3 = bookingController.getAllByOwner(ownerDto.getId(), "ALL", 5, 5);

            assertNotNull(response1);
            assertNotNull(response2);
            assertNotNull(response3);
        }
    }

    @Nested // Тесты для проверки ошибок
    @DisplayName("Error handling tests")
    class ErrorHandlingTests {
        @Test
        @Transactional
        @DisplayName("Error when booking not found")
        void bookingNotFoundTest() {
            Long nonExistentBookingId = 9999L;

            when(bookingService.getById(eq(nonExistentBookingId), anyLong()))
                    .thenThrow(new ShareItException.NotFoundException("Бронирование не найдено"));

            assertThrows(ShareItException.NotFoundException.class,
                    () -> bookingController.getById(nonExistentBookingId, bookerDto.getId()));
        }

        @Test
        @Transactional
        @DisplayName("Error when user not found")
        void userNotFoundTest() {
            Long nonExistentUserId = 9999L;

            when(bookingService.getAllByBooker(eq(nonExistentUserId), anyString(), anyInt(), anyInt()))
                    .thenThrow(new ShareItException.NotFoundException("Пользователь не найден"));

            assertThrows(ShareItException.NotFoundException.class,
                    () -> bookingController.getAllByBooker(nonExistentUserId, "ALL", 0, 10));
        }
    }
}