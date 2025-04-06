package ru.practicum.shareit.booking.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
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
        @DisplayName("Successful booking creation")
        void createBookingTest() {
            BookingDto createdBooking = bookingController.create(bookerDto.getId(), bookingDto);

            assertNotNull(createdBooking);
            assertNotNull(createdBooking.getId());
            assertEquals(bookingDto.getStart(), createdBooking.getStart());
            assertEquals(bookingDto.getEnd(), createdBooking.getEnd());
            assertEquals(itemDto.getId(), createdBooking.getItem().getId());
            assertEquals(bookerDto.getId(), createdBooking.getBooker().getId());
            assertEquals(BookingStatus.WAITING, createdBooking.getStatus());
        }

        @Test
        @DisplayName("Error when owner tries to book their own item")
        void ownerBookingOwnItemTest() {
            assertThrows(ShareItException.NotFoundException.class,
                    () -> bookingController.create(ownerDto.getId(), bookingDto));
        }

        @Test
        @DisplayName("Error when booking unavailable item")
        void bookingUnavailableItemTest() {
            ItemDto updatedItem = ItemDto.builder()
                    .available(false)
                    .build();
            itemController.update(updatedItem, itemDto.getId(), ownerDto.getId());

            assertThrows(ShareItException.BadRequestException.class,
                    () -> bookingController.create(bookerDto.getId(), bookingDto));
        }

        @Test
        @DisplayName("Error when booking with invalid dates")
        void bookingWithInvalidDatesTest() {
            BookingDto invalidBooking = BookingDto.builder()
                    .itemId(itemDto.getId())
                    .start(LocalDateTime.now().plusDays(2))
                    .end(LocalDateTime.now().plusDays(1))
                    .build();

            assertThrows(ShareItException.BadRequestException.class,
                    () -> bookingController.create(bookerDto.getId(), invalidBooking));
        }
    }

    @Nested // Тесты для утверждения бронирований
    @DisplayName("Approving bookings")
    class ApproveBookingTests {
        @Test
        @DisplayName("Successful booking approval")
        void approveBookingTest() {
            BookingDto createdBooking = bookingController.create(bookerDto.getId(), bookingDto);
            BookingDto approvedBooking = bookingController.approve(ownerDto.getId(), createdBooking.getId(), true);

            assertEquals(BookingStatus.APPROVED, approvedBooking.getStatus());
        }

        @Test
        @DisplayName("Successful booking rejection")
        void rejectBookingTest() {
            BookingDto createdBooking = bookingController.create(bookerDto.getId(), bookingDto);
            BookingDto rejectedBooking = bookingController.approve(ownerDto.getId(), createdBooking.getId(), false);

            assertEquals(BookingStatus.REJECTED, rejectedBooking.getStatus());
        }

        @Test
        @DisplayName("Error when non-owner tries to approve booking")
        void nonOwnerApprovingBookingTest() {
            BookingDto createdBooking = bookingController.create(bookerDto.getId(), bookingDto);

            String randomEmail = "random" + System.currentTimeMillis() + "@email.com";
            UserDto randomUser = UserDto.builder()
                    .name("Random User")
                    .email(randomEmail)
                    .build();
            randomUser = userController.create(userMapper.toUser(randomUser));

            UserDto finalRandomUser = randomUser;
            assertThrows(ShareItException.ForbiddenException.class,
                    () -> bookingController.approve(finalRandomUser.getId(), createdBooking.getId(), true));
        }

        @Test
        @DisplayName("Error when approving already processed booking")
        void approvingProcessedBookingTest() {
            BookingDto createdBooking = bookingController.create(bookerDto.getId(), bookingDto);
            bookingController.approve(ownerDto.getId(), createdBooking.getId(), true);

            assertThrows(ShareItException.BadRequestException.class,
                    () -> bookingController.approve(ownerDto.getId(), createdBooking.getId(), true));
        }
    }

    @Nested // Тесты для получения бронирований
    @DisplayName("Getting bookings")
    class GetBookingTests {
        @Test
        @DisplayName("Getting booking by ID")
        void getBookingByIdTest() {
            BookingDto createdBooking = bookingController.create(bookerDto.getId(), bookingDto);
            BookingDto retrievedBooking = bookingController.getById(bookerDto.getId(), createdBooking.getId());

            assertNotNull(retrievedBooking);
            assertEquals(createdBooking.getId(), retrievedBooking.getId());
            assertEquals(createdBooking.getStart(), retrievedBooking.getStart());
            assertEquals(createdBooking.getEnd(), retrievedBooking.getEnd());
        }

        @Test
        @DisplayName("Error when unauthorized user tries to get booking")
        void unauthorizedGetBookingTest() {
            BookingDto createdBooking = bookingController.create(bookerDto.getId(), bookingDto);

            String randomEmail = "random" + System.currentTimeMillis() + "@email.com";
            UserDto randomUser = UserDto.builder()
                    .name("Random User")
                    .email(randomEmail)
                    .build();
            randomUser = userController.create(userMapper.toUser(randomUser));

            UserDto finalRandomUser = randomUser;
            assertThrows(ShareItException.NotFoundException.class,
                    () -> bookingController.getById(finalRandomUser.getId(), createdBooking.getId()));
        }

        @Test
        @DisplayName("Getting all bookings by booker")
        void getAllBookingsByBookerTest() {
            bookingController.create(bookerDto.getId(), bookingDto);

            List<BookingDto> bookings = bookingController.getAllByBooker(bookerDto.getId(), "ALL");

            assertEquals(1, bookings.size());
            assertEquals(itemDto.getId(), bookings.get(0).getItem().getId());
            assertEquals(bookerDto.getId(), bookings.get(0).getBooker().getId());
        }

        @Test
        @DisplayName("Getting all bookings by owner")
        void getAllBookingsByOwnerTest() {
            bookingController.create(bookerDto.getId(), bookingDto);

            List<BookingDto> bookings = bookingController.getAllByOwner(ownerDto.getId(), "ALL");

            assertEquals(1, bookings.size());
            assertEquals(itemDto.getId(), bookings.get(0).getItem().getId());
            assertEquals(bookerDto.getId(), bookings.get(0).getBooker().getId());
        }

        @Test
        @DisplayName("Getting bookings with different states")
        void getBookingsWithDifferentStatesTest() {
            BookingDto createdBooking = bookingController.create(bookerDto.getId(), bookingDto);
            bookingController.approve(ownerDto.getId(), createdBooking.getId(), true);

            List<BookingDto> waitingBookings = bookingController.getAllByBooker(bookerDto.getId(), "WAITING");
            assertEquals(0, waitingBookings.size());

            List<BookingDto> futureBookings = bookingController.getAllByBooker(bookerDto.getId(), "FUTURE");
            assertEquals(1, futureBookings.size());
        }

        @Test
        @DisplayName("Error when using invalid state")
        void invalidStateTest() {
            assertThrows(ShareItException.BadRequestException.class,
                    () -> bookingController.getAllByBooker(bookerDto.getId(), "INVALID_STATE"));
        }
    }
}