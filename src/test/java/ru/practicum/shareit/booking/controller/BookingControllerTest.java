package ru.practicum.shareit.booking.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.ShareItApp;
import ru.practicum.shareit.TestConfig;
import ru.practicum.shareit.booking.BookingStatus;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.exception.ShareItException;
import ru.practicum.shareit.item.controller.ItemController;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.user.UserMapper;
import ru.practicum.shareit.user.controller.UserController;
import ru.practicum.shareit.user.dto.UserDto;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ActiveProfiles("test")
@ContextConfiguration(classes = {ShareItApp.class, TestConfig.class})
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class BookingControllerTest {

    @Autowired
    private BookingController bookingController;

    @Autowired
    private ItemController itemController;

    @Autowired
    private UserController userController;

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

        LocalDateTime start = LocalDateTime.now().plusDays(1);
        LocalDateTime end = LocalDateTime.now().plusDays(2);
        bookingDto = BookingDto.builder()
                .itemId(itemDto.getId())
                .start(start)
                .end(end)
                .build();
    }

    @Nested // Тесты для создания бронирований
    @DisplayName("Creating bookings")
    class CreateBookingTests {
        @Test
        @Transactional
        @DisplayName("Successful booking creation")
        void createBookingTest() {
            BookingDto createdBooking = bookingController.createBooking(bookingDto, bookerDto.getId()).getBody();

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
            assertThrows(ShareItException.NotFoundException.class,
                    () -> bookingController.createBooking(bookingDto, ownerDto.getId()));
        }

        @Test
        @Transactional
        @DisplayName("Error when booking unavailable item")
        void bookingUnavailableItemTest() {
            ItemDto updatedItem = ItemDto.builder()
                    .available(false)
                    .build();
            itemController.update(updatedItem, itemDto.getId(), ownerDto.getId());

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
            BookingDto createdBooking = bookingController.createBooking(bookingDto, bookerDto.getId()).getBody();
            BookingDto approvedBooking = bookingController.approve(createdBooking.getId(), ownerDto.getId(), true).getBody();

            assertEquals(BookingStatus.APPROVED, approvedBooking.getStatus());
        }

        @Test
        @Transactional
        @DisplayName("Successful booking rejection")
        void rejectBookingTest() {
            BookingDto createdBooking = bookingController.createBooking(bookingDto, bookerDto.getId()).getBody();
            BookingDto rejectedBooking = bookingController.approve(createdBooking.getId(), ownerDto.getId(), false).getBody();

            assertEquals(BookingStatus.REJECTED, rejectedBooking.getStatus());
        }

        @Test
        @Transactional
        @DisplayName("Error when non-owner tries to approve booking")
        void nonOwnerApprovingBookingTest() {
            BookingDto createdBooking = bookingController.createBooking(bookingDto, bookerDto.getId()).getBody();

            String randomEmail = "random" + System.currentTimeMillis() + "@email.com";
            UserDto randomUser = UserDto.builder()
                    .name("Random User")
                    .email(randomEmail)
                    .build();
            randomUser = userController.create(userMapper.toUser(randomUser));

            UserDto finalRandomUser = randomUser;
            assertThrows(ShareItException.ForbiddenException.class,
                    () -> bookingController.approve(createdBooking.getId(), finalRandomUser.getId(), true));
        }

        @Test
        @Transactional
        @DisplayName("Error when approving already processed booking")
        void approvingProcessedBookingTest() {
            BookingDto createdBooking = bookingController.createBooking(bookingDto, bookerDto.getId()).getBody();
            bookingController.approve(createdBooking.getId(), ownerDto.getId(), true).getBody(); // Добавлен .getBody()

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
            BookingDto createdBooking = bookingController.createBooking(bookingDto, bookerDto.getId()).getBody();
            BookingDto retrievedBooking = bookingController.getById(createdBooking.getId(), bookerDto.getId()).getBody();

            assertNotNull(retrievedBooking);
            assertEquals(createdBooking.getId(), retrievedBooking.getId());
            assertEquals(createdBooking.getStart(), retrievedBooking.getStart());
            assertEquals(createdBooking.getEnd(), retrievedBooking.getEnd());
        }

        @Test
        @Transactional
        @DisplayName("Error when unauthorized user tries to get booking")
        void unauthorizedGetBookingTest() {
            BookingDto createdBooking = bookingController.createBooking(bookingDto, bookerDto.getId()).getBody();

            String randomEmail = "random" + System.currentTimeMillis() + "@email.com";
            UserDto randomUser = UserDto.builder()
                    .name("Random User")
                    .email(randomEmail)
                    .build();
            randomUser = userController.create(userMapper.toUser(randomUser));

            UserDto finalRandomUser = randomUser;
            assertThrows(ShareItException.NotFoundException.class,
                    () -> bookingController.getById(createdBooking.getId(), finalRandomUser.getId()));
        }

        @Test
        @Transactional
        @DisplayName("Getting all bookings by booker")
        void getAllBookingsByBookerTest() {
            bookingController.createBooking(bookingDto, bookerDto.getId()).getBody();

            List<BookingDto> bookings = bookingController.getAllByBooker(bookerDto.getId(), "ALL").getBody();

            assertEquals(1, bookings.size());
            assertEquals(itemDto.getId(), bookings.get(0).getItem().getId());
            assertEquals(bookerDto.getId(), bookings.get(0).getBooker().getId());
        }

        @Test
        @Transactional
        @DisplayName("Getting all bookings by owner")
        void getAllBookingsByOwnerTest() {
            bookingController.createBooking(bookingDto, bookerDto.getId()).getBody();

            List<BookingDto> bookings = bookingController.getAllByOwner(ownerDto.getId(), "ALL").getBody();

            assertEquals(1, bookings.size());
            assertEquals(itemDto.getId(), bookings.get(0).getItem().getId());
            assertEquals(bookerDto.getId(), bookings.get(0).getBooker().getId());
        }

        @Test
        @Transactional
        @DisplayName("Getting bookings with different states")
        void getBookingsWithDifferentStatesTest() {
            BookingDto createdBooking = bookingController.createBooking(bookingDto, bookerDto.getId()).getBody();
            bookingController.approve(createdBooking.getId(), ownerDto.getId(), true).getBody(); // Добавлен .getBody()

            List<BookingDto> waitingBookings = bookingController.getAllByBooker(bookerDto.getId(), "WAITING").getBody();
            assertEquals(0, waitingBookings.size());

            List<BookingDto> futureBookings = bookingController.getAllByBooker(bookerDto.getId(), "FUTURE").getBody();
            assertEquals(1, futureBookings.size());
        }

        @Test
        @Transactional
        @DisplayName("Error when using invalid state")
        void invalidStateTest() {
            assertThrows(ShareItException.BadRequestException.class,
                    () -> bookingController.getAllByBooker(bookerDto.getId(), "INVALID_STATE"));
        }
    }
}