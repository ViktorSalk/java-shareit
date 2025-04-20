package ru.practicum.shareit.booking.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import ru.practicum.shareit.booking.BookingStatus;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.exception.ShareItException;
import ru.practicum.shareit.booking.service.BookingService;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.user.dto.UserDto;

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

@DisplayName("Booking Controller Tests")
class BookingControllerTest {

    @Mock
    private BookingService bookingService;

    @InjectMocks
    private BookingController bookingController;

    private UserDto ownerDto;
    private UserDto bookerDto;
    private ItemDto itemDto;
    private BookingDto bookingDto;
    private BookingDto createdBookingDto;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Подготовка тестовых данных
        ownerDto = UserDto.builder()
                .id(1L)
                .name("Item Owner")
                .email("owner@email.com")
                .build();

        bookerDto = UserDto.builder()
                .id(2L)
                .name("Booker User")
                .email("booker@email.com")
                .build();

        itemDto = ItemDto.builder()
                .id(1L)
                .name("Test Item")
                .description("Test Description")
                .available(true)
                .build();

        LocalDateTime start = LocalDateTime.now().plusDays(1);
        LocalDateTime end = LocalDateTime.now().plusDays(2);

        bookingDto = BookingDto.builder()
                .itemId(itemDto.getId())
                .start(start)
                .end(end)
                .build();

        createdBookingDto = BookingDto.builder()
                .id(1L)
                .start(start)
                .end(end)
                .item(itemDto)
                .booker(bookerDto)
                .status(BookingStatus.WAITING)
                .build();

        // Настройка поведения мока
        when(bookingService.create(any(BookingDto.class), anyLong())).thenReturn(createdBookingDto);

        when(bookingService.approve(anyLong(), eq(ownerDto.getId()), eq(true)))
                .thenReturn(BookingDto.builder()
                        .id(1L)
                        .start(start)
                        .end(end)
                        .item(itemDto)
                        .booker(bookerDto)
                        .status(BookingStatus.APPROVED)
                        .build());

        when(bookingService.approve(anyLong(), eq(ownerDto.getId()), eq(false)))
                .thenReturn(BookingDto.builder()
                        .id(1L)
                        .start(start)
                        .end(end)
                        .item(itemDto)
                        .booker(bookerDto)
                        .status(BookingStatus.REJECTED)
                        .build());

        when(bookingService.getById(anyLong(), anyLong())).thenReturn(createdBookingDto);

        when(bookingService.getAllByBooker(anyLong(), anyString(), anyInt(), anyInt()))
                .thenReturn(List.of(createdBookingDto));

        when(bookingService.getAllByOwner(anyLong(), anyString(), anyInt(), anyInt()))
                .thenReturn(List.of(createdBookingDto));
    }

    @Test
    @DisplayName("Создание бронирования - успешный случай")
    void createBooking_Success() {
        BookingDto result = bookingController.createBooking(bookingDto, bookerDto.getId());

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(bookingDto.getStart(), result.getStart());
        assertEquals(bookingDto.getEnd(), result.getEnd());
        assertEquals(itemDto.getId(), result.getItem().getId());
        assertEquals(bookerDto.getId(), result.getBooker().getId());
        assertEquals(BookingStatus.WAITING, result.getStatus());
    }

    @Test
    @DisplayName("Ошибка при попытке владельца забронировать свою вещь")
    void createBooking_OwnerBookingOwnItem() {
        when(bookingService.create(any(BookingDto.class), eq(ownerDto.getId())))
                .thenThrow(new ShareItException.NotFoundException("Владелец не может забронировать свою вещь"));

        assertThrows(ShareItException.NotFoundException.class,
                () -> bookingController.createBooking(bookingDto, ownerDto.getId()));
    }

    @Test
    @DisplayName("Ошибка при бронировании недоступной вещи")
    void createBooking_UnavailableItem() {
        when(bookingService.create(any(BookingDto.class), eq(bookerDto.getId())))
                .thenThrow(new ShareItException.BadRequestException("Вещь недоступна для бронирования"));

        assertThrows(ShareItException.BadRequestException.class,
                () -> bookingController.createBooking(bookingDto, bookerDto.getId()));
    }

    @Test
    @DisplayName("Ошибка при бронировании с некорректными датами")
    void createBooking_InvalidDates() {
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

    @Test
    @DisplayName("Подтверждение бронирования - успешный случай")
    void approve_Success() {
        BookingDto result = bookingController.approve(1L, ownerDto.getId(), true);

        assertNotNull(result);
        assertEquals(BookingStatus.APPROVED, result.getStatus());
    }

    @Test
    @DisplayName("Отклонение бронирования - успешный случай")
    void reject_Success() {
        BookingDto result = bookingController.approve(1L, ownerDto.getId(), false);

        assertNotNull(result);
        assertEquals(BookingStatus.REJECTED, result.getStatus());
    }

    @Test
    @DisplayName("Ошибка при попытке не владельца подтвердить бронирование")
    void approve_NonOwner() {
        when(bookingService.approve(eq(1L), eq(bookerDto.getId()), anyBoolean()))
                .thenThrow(new ShareItException.ForbiddenException("Только владелец вещи может подтвердить бронирование"));

        assertThrows(ShareItException.ForbiddenException.class,
                () -> bookingController.approve(1L, bookerDto.getId(), true));
    }

    @Test
    @DisplayName("Ошибка при подтверждении уже обработанного бронирования")
    void approve_AlreadyProcessed() {
        when(bookingService.approve(eq(1L), eq(ownerDto.getId()), eq(true)))
                .thenThrow(new ShareItException.BadRequestException("Бронирование уже обработано"));

        assertThrows(ShareItException.BadRequestException.class,
                () -> bookingController.approve(1L, ownerDto.getId(), true));
    }

    @Test
    @DisplayName("Получение бронирования по ID - успешный случай")
    void getById_Success() {
        BookingDto result = bookingController.getById(1L, bookerDto.getId());

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(itemDto.getId(), result.getItem().getId());
        assertEquals(bookerDto.getId(), result.getBooker().getId());
    }

    @Test
    @DisplayName("Ошибка при попытке неавторизованного пользователя получить бронирование")
    void getById_Unauthorized() {
        Long randomUserId = 999L;
        when(bookingService.getById(eq(1L), eq(randomUserId)))
                .thenThrow(new ShareItException.NotFoundException("Доступ запрещен"));

        assertThrows(ShareItException.NotFoundException.class,
                () -> bookingController.getById(1L, randomUserId));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ALL", "CURRENT", "PAST", "FUTURE", "WAITING", "REJECTED"})
    @DisplayName("Получение всех бронирований арендатора с разными состояниями")
    void getAllByBooker_DifferentStates(String state) {
        List<BookingDto> result = bookingController.getAllByBooker(bookerDto.getId(), state, 0, 10);

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
    }

    @ParameterizedTest
    @ValueSource(strings = {"ALL", "CURRENT", "PAST", "FUTURE", "WAITING", "REJECTED"})
    @DisplayName("Получение всех бронирований владельца с разными состояниями")
    void getAllByOwner_DifferentStates(String state) {
        List<BookingDto> result = bookingController.getAllByOwner(ownerDto.getId(), state, 0, 10);

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("Ошибка при использовании некорректного состояния")
    void getBookings_InvalidState() {
        when(bookingService.getAllByBooker(eq(bookerDto.getId()), eq("INVALID_STATE"), anyInt(), anyInt()))
                .thenThrow(new ShareItException.BadRequestException("Unknown state: INVALID_STATE"));

        assertThrows(ShareItException.BadRequestException.class,
                () -> bookingController.getAllByBooker(bookerDto.getId(), "INVALID_STATE", 0, 10));
    }

    @ParameterizedTest
    @DisplayName("Пагинация для бронирований арендатора")
    @ValueSource(ints = {0, 5, 10})
    void getAllByBooker_Pagination(int from) {
        List<BookingDto> result = bookingController.getAllByBooker(bookerDto.getId(), "ALL", from, 5);

        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @ParameterizedTest
    @DisplayName("Пагинация для бронирований владельца")
    @ValueSource(ints = {0, 5, 10})
    void getAllByOwner_Pagination(int from) {
        List<BookingDto> result = bookingController.getAllByOwner(ownerDto.getId(), "ALL", from, 5);

        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    @DisplayName("Ошибка при попытке получить несуществующее бронирование")
    void getById_NotFound() {
        Long nonExistentBookingId = 9999L;
        when(bookingService.getById(eq(nonExistentBookingId), anyLong()))
                .thenThrow(new ShareItException.NotFoundException("Бронирование не найдено"));

        assertThrows(ShareItException.NotFoundException.class,
                () -> bookingController.getById(nonExistentBookingId, bookerDto.getId()));
    }

    @Test
    @DisplayName("Ошибка при попытке получить бронирования несуществующего пользователя")
    void getAllByBooker_UserNotFound() {
        Long nonExistentUserId = 9999L;
        when(bookingService.getAllByBooker(eq(nonExistentUserId), anyString(), anyInt(), anyInt()))
                .thenThrow(new ShareItException.NotFoundException("Пользователь не найден"));

        assertThrows(ShareItException.NotFoundException.class,
                () -> bookingController.getAllByBooker(nonExistentUserId, "ALL", 0, 10));
    }
}