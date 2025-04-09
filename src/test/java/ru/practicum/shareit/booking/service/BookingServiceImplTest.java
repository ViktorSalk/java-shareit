package ru.practicum.shareit.booking.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import ru.practicum.shareit.ShareItApp;
import ru.practicum.shareit.TestConfig;
import ru.practicum.shareit.booking.BookingMapper;
import ru.practicum.shareit.booking.BookingState;
import ru.practicum.shareit.booking.BookingStatus;
import ru.practicum.shareit.booking.Impl.BookingServiceImpl;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.exception.ShareItException;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.booking.repository.BookingRepository;
import ru.practicum.shareit.booking.service.handler.owner.OwnerStateProcessor;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@ContextConfiguration(classes = {ShareItApp.class, TestConfig.class})
@ExtendWith(MockitoExtension.class)
@DisplayName("Booking Service Tests")
class BookingServiceImplTest {
    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ItemRepository itemRepository;
    @Mock
    private BookingMapper bookingMapper;
    @Mock
    private BookerStateProcessor bookerStateProcessor;
    @Mock
    private OwnerStateProcessor ownerStateProcessor;
    @InjectMocks
    private BookingServiceImpl bookingService;

    private User owner;
    private User booker;
    private Item item;
    private Booking booking;
    private BookingDto bookingDto;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        now = LocalDateTime.now();
        owner = User.builder()
                .id(1L)
                .name("Owner")
                .email("owner@test.com")
                .build();
        booker = User.builder()
                .id(2L)
                .name("Booker")
                .email("booker@test.com")
                .build();
        item = Item.builder()
                .id(1L)
                .name("Test Item")
                .description("Test Description")
                .available(true)
                .owner(owner)
                .build();
        booking = Booking.builder()
                .id(1L)
                .start(now.plusDays(1))
                .end(now.plusDays(2))
                .item(item)
                .booker(booker)
                .status(BookingStatus.WAITING)
                .build();
        bookingDto = BookingDto.builder()
                .id(1L)
                .start(now.plusDays(1))
                .end(now.plusDays(2))
                .itemId(1L)
                .item(ItemDto.builder().id(1L).build())
                .booker(UserDto.builder().id(2L).build())
                .status(BookingStatus.WAITING)
                .build();
    }

    @Nested // Тесты на создание бронирования
    @DisplayName("Create Booking Tests")
    class CreateBookingTests {
        @Test
        @DisplayName("Should create booking successfully")
        void createBookingSuccessfully() {
            when(userRepository.findById(anyLong())).thenReturn(Optional.of(booker));
            when(itemRepository.findById(anyLong())).thenReturn(Optional.of(item));
            when(bookingRepository.save(any(Booking.class))).thenReturn(booking);
            when(bookingMapper.toBookingDto(any(Booking.class))).thenReturn(bookingDto);

            BookingDto result = bookingService.create(bookingDto, booker.getId());

            assertNotNull(result);
            assertEquals(bookingDto.getId(), result.getId());
            assertEquals(bookingDto.getStart(), result.getStart());
            assertEquals(bookingDto.getEnd(), result.getEnd());
            assertEquals(bookingDto.getStatus(), result.getStatus());
            verify(bookingRepository).save(any(Booking.class));
        }

        @Test
        @DisplayName("Should throw exception when user doesn't exist")
        void createBookingWithNonExistentUser() {
            when(userRepository.findById(anyLong())).thenReturn(Optional.empty());

            assertThrows(ShareItException.NotFoundException.class,
                    () -> bookingService.create(bookingDto, 999L));
            verify(bookingRepository, never()).save(any(Booking.class));
        }

        @Test
        @DisplayName("Should throw exception when item doesn't exist")
        void createBookingWithNonExistentItem() {
            when(userRepository.findById(anyLong())).thenReturn(Optional.of(booker));
            when(itemRepository.findById(anyLong())).thenReturn(Optional.empty());

            assertThrows(ShareItException.NotFoundException.class,
                    () -> bookingService.create(bookingDto, booker.getId()));
            verify(bookingRepository, never()).save(any(Booking.class));
        }

        @Test
        @DisplayName("Should throw exception when item is unavailable")
        void createBookingWithUnavailableItem() {
            item.setAvailable(false);
            when(userRepository.findById(anyLong())).thenReturn(Optional.of(booker));
            when(itemRepository.findById(anyLong())).thenReturn(Optional.of(item));

            assertThrows(ShareItException.BadRequestException.class,
                    () -> bookingService.create(bookingDto, booker.getId()));
            verify(bookingRepository, never()).save(any(Booking.class));
        }

        @Test
        @DisplayName("Should throw exception when owner tries to book own item")
        void createBookingByOwner() {
            when(userRepository.findById(anyLong())).thenReturn(Optional.of(owner));
            when(itemRepository.findById(anyLong())).thenReturn(Optional.of(item));

            assertThrows(ShareItException.NotFoundException.class,
                    () -> bookingService.create(bookingDto, owner.getId()));
            verify(bookingRepository, never()).save(any(Booking.class));
        }

        @Test
        @DisplayName("Should throw exception when booking dates are invalid")
        void createBookingWithInvalidDates() {
            bookingDto.setStart(now.plusDays(2));
            bookingDto.setEnd(now.plusDays(1));
            when(userRepository.findById(anyLong())).thenReturn(Optional.of(booker));
            when(itemRepository.findById(anyLong())).thenReturn(Optional.of(item));

            assertThrows(ShareItException.BadRequestException.class,
                    () -> bookingService.create(bookingDto, booker.getId()));
            verify(bookingRepository, never()).save(any(Booking.class));
        }
    }

    @Nested // Тесты на подтверждение бронирования
    @DisplayName("Approve Booking Tests")
    class ApproveBookingTests {
        @Test
        @DisplayName("Should approve booking successfully")
        void approveBookingSuccessfully() {
            booking.setStatus(BookingStatus.WAITING);
            when(bookingRepository.findById(anyLong())).thenReturn(Optional.of(booking));
            Booking approvedBooking = Booking.builder()
                    .id(booking.getId())
                    .start(booking.getStart())
                    .end(booking.getEnd())
                    .item(booking.getItem())
                    .booker(booking.getBooker())
                    .status(BookingStatus.APPROVED)
                    .build();
            when(bookingRepository.save(any(Booking.class))).thenReturn(approvedBooking);
            BookingDto approvedBookingDto = BookingDto.builder()
                    .id(bookingDto.getId())
                    .start(bookingDto.getStart())
                    .end(bookingDto.getEnd())
                    .item(bookingDto.getItem())
                    .booker(bookingDto.getBooker())
                    .status(BookingStatus.APPROVED)
                    .build();
            when(bookingMapper.toBookingDto(approvedBooking)).thenReturn(approvedBookingDto);

            BookingDto result = bookingService.approve(booking.getId(), owner.getId(), true);

            assertNotNull(result);
            assertEquals(BookingStatus.APPROVED, result.getStatus());
            verify(bookingRepository).save(any(Booking.class));
        }

        @Test
        @DisplayName("Should reject booking successfully")
        void rejectBookingSuccessfully() {
            booking.setStatus(BookingStatus.WAITING);
            when(bookingRepository.findById(anyLong())).thenReturn(Optional.of(booking));
            Booking rejectedBooking = Booking.builder()
                    .id(booking.getId())
                    .start(booking.getStart())
                    .end(booking.getEnd())
                    .item(booking.getItem())
                    .booker(booking.getBooker())
                    .status(BookingStatus.REJECTED)
                    .build();
            when(bookingRepository.save(any(Booking.class))).thenReturn(rejectedBooking);
            BookingDto rejectedBookingDto = BookingDto.builder()
                    .id(bookingDto.getId())
                    .start(bookingDto.getStart())
                    .end(bookingDto.getEnd())
                    .item(bookingDto.getItem())
                    .booker(bookingDto.getBooker())
                    .status(BookingStatus.REJECTED)
                    .build();
            when(bookingMapper.toBookingDto(rejectedBooking)).thenReturn(rejectedBookingDto);

            BookingDto result = bookingService.approve(booking.getId(), owner.getId(), false);

            assertNotNull(result);
            assertEquals(BookingStatus.REJECTED, result.getStatus());
            verify(bookingRepository).save(any(Booking.class));
        }

        @Test
        @DisplayName("Should throw exception when booking doesn't exist")
        void approveBookingWithNonExistentBooking() {
            when(bookingRepository.findById(anyLong())).thenReturn(Optional.empty());

            assertThrows(ShareItException.NotFoundException.class,
                    () -> bookingService.approve(999L, owner.getId(), true));
            verify(bookingRepository, never()).save(any(Booking.class));
        }

        @Test
        @DisplayName("Should throw exception when non-owner tries to approve")
        void approveBookingByNonOwner() {
            when(bookingRepository.findById(anyLong())).thenReturn(Optional.of(booking));

            assertThrows(ShareItException.ForbiddenException.class,
                    () -> bookingService.approve(booking.getId(), booker.getId(), true));
            verify(bookingRepository, never()).save(any(Booking.class));
        }

        @Test
        @DisplayName("Should throw exception when booking already processed")
        void approveAlreadyProcessedBooking() {
            booking.setStatus(BookingStatus.APPROVED);
            when(bookingRepository.findById(anyLong())).thenReturn(Optional.of(booking));

            assertThrows(ShareItException.BadRequestException.class,
                    () -> bookingService.approve(booking.getId(), owner.getId(), true));
            verify(bookingRepository, never()).save(any(Booking.class));
        }
    }

    @Nested // Тесты на получение бронирования
    @DisplayName("Get Booking Tests")
    class GetBookingTests {
        @Test
        @DisplayName("Should get booking by ID successfully")
        void getBookingByIdSuccessfully() {
            when(bookingRepository.findById(anyLong())).thenReturn(Optional.of(booking));
            when(bookingMapper.toBookingDto(any(Booking.class))).thenReturn(bookingDto);

            BookingDto result = bookingService.getById(booking.getId(), booker.getId());

            assertNotNull(result);
            assertEquals(bookingDto.getId(), result.getId());
            verify(bookingMapper).toBookingDto(booking);
        }

        @Test
        @DisplayName("Should throw exception when booking doesn't exist")
        void getBookingByIdWithNonExistentBooking() {
            when(bookingRepository.findById(anyLong())).thenReturn(Optional.empty());

            assertThrows(ShareItException.NotFoundException.class,
                    () -> bookingService.getById(999L, booker.getId()));
        }

        @Test
        @DisplayName("Should throw exception when unauthorized user tries to get booking")
        void getBookingByIdByUnauthorizedUser() {
            User randomUser = User.builder().id(3L).build();
            when(bookingRepository.findById(anyLong())).thenReturn(Optional.of(booking));

            assertThrows(ShareItException.NotFoundException.class,
                    () -> bookingService.getById(booking.getId(), randomUser.getId()));
        }
    }

    @Nested // Тесты на получение бронирований по статусу
    @DisplayName("Get Bookings By Booker Tests")
    class GetBookingsByBookerTests {
        @Test
        @DisplayName("Should get all bookings by booker")
        void getAllBookingsByBooker() {
            when(userRepository.findById(anyLong())).thenReturn(Optional.of(booker));
            when(bookerStateProcessor.process(eq(BookingState.ALL), eq(booker.getId()), any(LocalDateTime.class)))
                    .thenReturn(Collections.singletonList(booking));
            when(bookingMapper.toBookingDto(any(Booking.class))).thenReturn(bookingDto);

            List<BookingDto> result = bookingService.getAllByBooker(booker.getId(), "ALL");

            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals(bookingDto.getId(), result.get(0).getId());
            verify(bookerStateProcessor).process(eq(BookingState.ALL), eq(booker.getId()), any(LocalDateTime.class));
        }

        @Test
        @DisplayName("Should get current bookings by booker")
        void getCurrentBookingsByBooker() {
            when(userRepository.findById(anyLong())).thenReturn(Optional.of(booker));
            when(bookerStateProcessor.process(eq(BookingState.CURRENT), eq(booker.getId()), any(LocalDateTime.class)))
                    .thenReturn(Collections.singletonList(booking));
            when(bookingMapper.toBookingDto(any(Booking.class))).thenReturn(bookingDto);

            List<BookingDto> result = bookingService.getAllByBooker(booker.getId(), "CURRENT");

            assertNotNull(result);
            assertEquals(1, result.size());
            verify(bookerStateProcessor).process(eq(BookingState.CURRENT), eq(booker.getId()), any(LocalDateTime.class));
        }

        @Test
        @DisplayName("Should get past bookings by booker")
        void getPastBookingsByBooker() {
            when(userRepository.findById(anyLong())).thenReturn(Optional.of(booker));
            when(bookerStateProcessor.process(eq(BookingState.PAST), eq(booker.getId()), any(LocalDateTime.class)))
                    .thenReturn(Collections.singletonList(booking));
            when(bookingMapper.toBookingDto(any(Booking.class))).thenReturn(bookingDto);

            List<BookingDto> result = bookingService.getAllByBooker(booker.getId(), "PAST");

            assertNotNull(result);
            assertEquals(1, result.size());
            verify(bookerStateProcessor).process(eq(BookingState.PAST), eq(booker.getId()), any(LocalDateTime.class));
        }

        @Test
        @DisplayName("Should get future bookings by booker")
        void getFutureBookingsByBooker() {
            when(userRepository.findById(anyLong())).thenReturn(Optional.of(booker));
            when(bookerStateProcessor.process(eq(BookingState.FUTURE), eq(booker.getId()), any(LocalDateTime.class)))
                    .thenReturn(Collections.singletonList(booking));
            when(bookingMapper.toBookingDto(any(Booking.class))).thenReturn(bookingDto);

            List<BookingDto> result = bookingService.getAllByBooker(booker.getId(), "FUTURE");

            assertNotNull(result);
            assertEquals(1, result.size());
            verify(bookerStateProcessor).process(eq(BookingState.FUTURE), eq(booker.getId()), any(LocalDateTime.class));
        }

        @Test
        @DisplayName("Should get waiting bookings by booker")
        void getWaitingBookingsByBooker() {
            when(userRepository.findById(anyLong())).thenReturn(Optional.of(booker));
            when(bookerStateProcessor.process(eq(BookingState.WAITING), eq(booker.getId()), any(LocalDateTime.class)))
                    .thenReturn(Collections.singletonList(booking));
            when(bookingMapper.toBookingDto(any(Booking.class))).thenReturn(bookingDto);

            List<BookingDto> result = bookingService.getAllByBooker(booker.getId(), "WAITING");

            assertNotNull(result);
            assertEquals(1, result.size());
            verify(bookerStateProcessor).process(eq(BookingState.WAITING), eq(booker.getId()), any(LocalDateTime.class));
        }

        @Test
        @DisplayName("Should get rejected bookings by booker")
        void getRejectedBookingsByBooker() {
            when(userRepository.findById(anyLong())).thenReturn(Optional.of(booker));
            when(bookerStateProcessor.process(eq(BookingState.REJECTED), eq(booker.getId()), any(LocalDateTime.class)))
                    .thenReturn(Collections.singletonList(booking));
            when(bookingMapper.toBookingDto(any(Booking.class))).thenReturn(bookingDto);

            List<BookingDto> result = bookingService.getAllByBooker(booker.getId(), "REJECTED");

            assertNotNull(result);
            assertEquals(1, result.size());
            verify(bookerStateProcessor).process(eq(BookingState.REJECTED), eq(booker.getId()), any(LocalDateTime.class));
        }

        @Test
        @DisplayName("Should throw exception when state is invalid")
        void getBookingsByBookerWithInvalidState() {
            when(userRepository.findById(anyLong())).thenReturn(Optional.of(booker));

            assertThrows(ShareItException.BadRequestException.class,
                    () -> bookingService.getAllByBooker(booker.getId(), "INVALID_STATE"));
        }
    }

    @Nested // Тесты для получения бронирований по владельцу
    @DisplayName("Get Bookings By Owner Tests")
    class GetBookingsByOwnerTests {
        @Test
        @DisplayName("Should get all bookings by owner")
        void getAllBookingsByOwner() {
            when(userRepository.findById(anyLong())).thenReturn(Optional.of(owner));
            when(ownerStateProcessor.process(eq(BookingState.ALL), eq(owner.getId()), any(LocalDateTime.class)))
                    .thenReturn(Collections.singletonList(booking));
            when(bookingMapper.toBookingDto(any(Booking.class))).thenReturn(bookingDto);

            List<BookingDto> result = bookingService.getAllByOwner(owner.getId(), "ALL");

            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals(bookingDto.getId(), result.get(0).getId());
            verify(ownerStateProcessor).process(eq(BookingState.ALL), eq(owner.getId()), any(LocalDateTime.class));
        }

        @Test
        @DisplayName("Should get current bookings by owner")
        void getCurrentBookingsByOwner() {
            when(userRepository.findById(anyLong())).thenReturn(Optional.of(owner));
            when(ownerStateProcessor.process(eq(BookingState.CURRENT), eq(owner.getId()), any(LocalDateTime.class)))
                    .thenReturn(Collections.singletonList(booking));
            when(bookingMapper.toBookingDto(any(Booking.class))).thenReturn(bookingDto);

            List<BookingDto> result = bookingService.getAllByOwner(owner.getId(), "CURRENT");

            assertNotNull(result);
            assertEquals(1, result.size());
            verify(ownerStateProcessor).process(eq(BookingState.CURRENT), eq(owner.getId()), any(LocalDateTime.class));
        }

        @Test
        @DisplayName("Should get past bookings by owner")
        void getPastBookingsByOwner() {
            when(userRepository.findById(anyLong())).thenReturn(Optional.of(owner));
            when(ownerStateProcessor.process(eq(BookingState.PAST), eq(owner.getId()), any(LocalDateTime.class)))
                    .thenReturn(Collections.singletonList(booking));
            when(bookingMapper.toBookingDto(any(Booking.class))).thenReturn(bookingDto);

            List<BookingDto> result = bookingService.getAllByOwner(owner.getId(), "PAST");

            assertNotNull(result);
            assertEquals(1, result.size());
            verify(ownerStateProcessor).process(eq(BookingState.PAST), eq(owner.getId()), any(LocalDateTime.class));
        }

        @Test
        @DisplayName("Should get future bookings by owner")
        void getFutureBookingsByOwner() {
            when(userRepository.findById(anyLong())).thenReturn(Optional.of(owner));
            when(ownerStateProcessor.process(eq(BookingState.FUTURE), eq(owner.getId()), any(LocalDateTime.class)))
                    .thenReturn(Collections.singletonList(booking));
            when(bookingMapper.toBookingDto(any(Booking.class))).thenReturn(bookingDto);

            List<BookingDto> result = bookingService.getAllByOwner(owner.getId(), "FUTURE");

            assertNotNull(result);
            assertEquals(1, result.size());
            verify(ownerStateProcessor).process(eq(BookingState.FUTURE), eq(owner.getId()), any(LocalDateTime.class));
        }

        @Test
        @DisplayName("Should get waiting bookings by owner")
        void getWaitingBookingsByOwner() {
            when(userRepository.findById(anyLong())).thenReturn(Optional.of(owner));
            when(ownerStateProcessor.process(eq(BookingState.WAITING), eq(owner.getId()), any(LocalDateTime.class)))
                    .thenReturn(Collections.singletonList(booking));
            when(bookingMapper.toBookingDto(any(Booking.class))).thenReturn(bookingDto);

            List<BookingDto> result = bookingService.getAllByOwner(owner.getId(), "WAITING");

            assertNotNull(result);
            assertEquals(1, result.size());
            verify(ownerStateProcessor).process(eq(BookingState.WAITING), eq(owner.getId()), any(LocalDateTime.class));
        }

        @Test
        @DisplayName("Should get rejected bookings by owner")
        void getRejectedBookingsByOwner() {
            when(userRepository.findById(anyLong())).thenReturn(Optional.of(owner));
            when(ownerStateProcessor.process(eq(BookingState.REJECTED), eq(owner.getId()), any(LocalDateTime.class)))
                    .thenReturn(Collections.singletonList(booking));
            when(bookingMapper.toBookingDto(any(Booking.class))).thenReturn(bookingDto);

            List<BookingDto> result = bookingService.getAllByOwner(owner.getId(), "REJECTED");

            assertNotNull(result);
            assertEquals(1, result.size());
            verify(ownerStateProcessor).process(eq(BookingState.REJECTED), eq(owner.getId()), any(LocalDateTime.class));
        }

        @Test
        @DisplayName("Should throw exception when state is invalid")
        void getBookingsByOwnerWithInvalidState() {
            when(userRepository.findById(anyLong())).thenReturn(Optional.of(owner));

            assertThrows(ShareItException.BadRequestException.class,
                    () -> bookingService.getAllByOwner(owner.getId(), "INVALID_STATE"));
        }
    }

    @Nested // Тесты для получения бронирований пользователя
    @DisplayName("User Validation Tests")
    class UserValidationTests {
        @Test
        @DisplayName("Should throw exception when user doesn't exist")
        void getBookingsByNonExistentUser() {
            when(userRepository.findById(anyLong())).thenReturn(Optional.empty());

            assertThrows(ShareItException.NotFoundException.class,
                    () -> bookingService.getAllByBooker(999L, "ALL"));
            assertThrows(ShareItException.NotFoundException.class,
                    () -> bookingService.getAllByOwner(999L, "ALL"));
        }
    }
}