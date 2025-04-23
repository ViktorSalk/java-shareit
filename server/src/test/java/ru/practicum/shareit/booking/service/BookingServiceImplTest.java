package ru.practicum.shareit.booking.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
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
            when(bookingMapper.toBooking(any(BookingDto.class))).thenReturn(booking);

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
            when(userRepository.existsById(anyLong())).thenReturn(true);
            PageRequest pageRequest = PageRequest.of(0, 10, Sort.by("start").descending());
            when(bookerStateProcessor.process(eq(BookingState.ALL), eq(booker.getId()), any(LocalDateTime.class), eq(pageRequest)))
                    .thenReturn(Collections.singletonList(booking));
            when(bookingMapper.toBookingDto(any(Booking.class))).thenReturn(bookingDto);

            List<BookingDto> result = bookingService.getAllByBooker(booker.getId(), "ALL", 0, 10);

            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals(bookingDto.getId(), result.get(0).getId());
            verify(bookerStateProcessor).process(eq(BookingState.ALL), eq(booker.getId()), any(LocalDateTime.class), eq(pageRequest));
        }

        @Test
        @DisplayName("Should throw exception when user doesn't exist")
        void getAllBookingsByNonExistentBooker() {
            when(userRepository.existsById(anyLong())).thenReturn(false);

            assertThrows(ShareItException.NotFoundException.class,
                    () -> bookingService.getAllByBooker(999L, "ALL", 0, 10));
            verify(bookerStateProcessor, never()).process(any(), anyLong(), any(), any());
        }

        @Test
        @DisplayName("Should throw exception when state is invalid")
        void getAllBookingsByBookerWithInvalidState() {
            when(userRepository.existsById(anyLong())).thenReturn(true);

            assertThrows(ShareItException.BadRequestException.class,
                    () -> bookingService.getAllByBooker(booker.getId(), "INVALID_STATE", 0, 10));
            verify(bookerStateProcessor, never()).process(any(), anyLong(), any(), any());
        }

        @Test
        @DisplayName("Should get bookings by booker with different states")
        void getBookingsByBookerWithDifferentStates() {
            when(userRepository.existsById(anyLong())).thenReturn(true);

            BookingState[] states = BookingState.values();
            for (int i = 0; i < states.length; i++) {
                BookingState state = states[i];
                PageRequest pageRequest = PageRequest.of(0, 10, Sort.by("start").descending());

                reset(bookerStateProcessor, bookingMapper);

                when(bookerStateProcessor.process(eq(state), eq(booker.getId()), any(LocalDateTime.class), eq(pageRequest)))
                        .thenReturn(Collections.singletonList(booking));
                when(bookingMapper.toBookingDto(any(Booking.class))).thenReturn(bookingDto);

                List<BookingDto> result = bookingService.getAllByBooker(booker.getId(), state.name(), 0, 10);

                assertNotNull(result);
                assertEquals(1, result.size());
                assertEquals(bookingDto.getId(), result.get(0).getId());
                verify(bookerStateProcessor).process(eq(state), eq(booker.getId()), any(LocalDateTime.class), eq(pageRequest));
            }
        }
    }

    @Nested // Тесты для получения бронирований по владельцу
    @DisplayName("Get Bookings By Owner Tests")
    class GetBookingsByOwnerTests {
        @Test
        @DisplayName("Should get all bookings by owner")
        void getAllBookingsByOwner() {
            when(userRepository.existsById(anyLong())).thenReturn(true);
            PageRequest pageRequest = PageRequest.of(0, 10, Sort.by("start").descending());
            when(ownerStateProcessor.process(eq(BookingState.ALL), eq(owner.getId()), any(LocalDateTime.class), eq(pageRequest)))
                    .thenReturn(Collections.singletonList(booking));
            when(bookingMapper.toBookingDto(any(Booking.class))).thenReturn(bookingDto);

            List<BookingDto> result = bookingService.getAllByOwner(owner.getId(), "ALL", 0, 10);

            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals(bookingDto.getId(), result.get(0).getId());
            verify(ownerStateProcessor).process(eq(BookingState.ALL), eq(owner.getId()), any(LocalDateTime.class), eq(pageRequest));
        }

        @Test
        @DisplayName("Should throw exception when user doesn't exist")
        void getAllBookingsByNonExistentOwner() {
            when(userRepository.existsById(anyLong())).thenReturn(false);

            assertThrows(ShareItException.NotFoundException.class,
                    () -> bookingService.getAllByOwner(999L, "ALL", 0, 10));
            verify(ownerStateProcessor, never()).process(any(), anyLong(), any(), any());
        }

        @Test
        @DisplayName("Should throw exception when state is invalid")
        void getAllBookingsByOwnerWithInvalidState() {
            when(userRepository.existsById(anyLong())).thenReturn(true);

            assertThrows(ShareItException.BadRequestException.class,
                    () -> bookingService.getAllByOwner(owner.getId(), "INVALID_STATE", 0, 10));
            verify(ownerStateProcessor, never()).process(any(), anyLong(), any(), any());
        }

        @Test
        @DisplayName("Should get bookings by owner with different states")
        void getBookingsByOwnerWithDifferentStates() {
            when(userRepository.existsById(anyLong())).thenReturn(true);

            BookingState[] states = BookingState.values();
            for (int i = 0; i < states.length; i++) {
                BookingState state = states[i];
                PageRequest pageRequest = PageRequest.of(0, 10, Sort.by("start").descending());

                reset(ownerStateProcessor, bookingMapper);

                when(ownerStateProcessor.process(eq(state), eq(owner.getId()), any(LocalDateTime.class), eq(pageRequest)))
                        .thenReturn(Collections.singletonList(booking));
                when(bookingMapper.toBookingDto(any(Booking.class))).thenReturn(bookingDto);

                List<BookingDto> result = bookingService.getAllByOwner(owner.getId(), state.name(), 0, 10);

                assertNotNull(result);
                assertEquals(1, result.size());
                assertEquals(bookingDto.getId(), result.get(0).getId());
                verify(ownerStateProcessor).process(eq(state), eq(owner.getId()), any(LocalDateTime.class), eq(pageRequest));
            }
        }
    }

    @Nested // Тесты на пагинацию
    @DisplayName("Pagination Tests")
    class PaginationTests {
        @Test
        @DisplayName("Should handle pagination correctly for booker")
        void paginationForBookerBookings() {
            when(userRepository.existsById(anyLong())).thenReturn(true);

            int[][] paginations = {{0, 5}, {0, 10}, {1, 5}, {2, 3}};

            for (int i = 0; i < paginations.length; i++) {
                int[] pagination = paginations[i];
                int from = pagination[0];
                int size = pagination[1];
                int page = from / size;

                reset(bookerStateProcessor, bookingMapper);

                PageRequest pageRequest = PageRequest.of(page, size, Sort.by("start").descending());
                when(bookerStateProcessor.process(eq(BookingState.ALL), eq(booker.getId()), any(LocalDateTime.class), eq(pageRequest)))
                        .thenReturn(Collections.singletonList(booking));
                when(bookingMapper.toBookingDto(any(Booking.class))).thenReturn(bookingDto);

                List<BookingDto> result = bookingService.getAllByBooker(booker.getId(), "ALL", from, size);

                assertNotNull(result);
                assertEquals(1, result.size());
                verify(bookerStateProcessor).process(eq(BookingState.ALL), eq(booker.getId()), any(LocalDateTime.class), eq(pageRequest));
            }
        }

        @Test
        @DisplayName("Should handle pagination correctly for owner")
        void paginationForOwnerBookings() {
            when(userRepository.existsById(anyLong())).thenReturn(true);

            int[][] paginations = {{0, 5}, {0, 10}, {1, 5}, {2, 3}};

            for (int i = 0; i < paginations.length; i++) {
                int[] pagination = paginations[i];
                int from = pagination[0];
                int size = pagination[1];
                int page = from / size;

                reset(ownerStateProcessor, bookingMapper);

                PageRequest pageRequest = PageRequest.of(page, size, Sort.by("start").descending());
                when(ownerStateProcessor.process(eq(BookingState.ALL), eq(owner.getId()), any(LocalDateTime.class), eq(pageRequest)))
                        .thenReturn(Collections.singletonList(booking));
                when(bookingMapper.toBookingDto(any(Booking.class))).thenReturn(bookingDto);

                List<BookingDto> result = bookingService.getAllByOwner(owner.getId(), "ALL", from, size);

                assertNotNull(result);
                assertEquals(1, result.size());
                verify(ownerStateProcessor).process(eq(BookingState.ALL), eq(owner.getId()), any(LocalDateTime.class), eq(pageRequest));
            }
        }
    }
}