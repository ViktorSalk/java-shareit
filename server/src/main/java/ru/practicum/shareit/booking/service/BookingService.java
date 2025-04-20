package ru.practicum.shareit.booking.service;

import ru.practicum.shareit.booking.dto.BookingDto;

import java.util.List;

public interface BookingService {
    BookingDto create(BookingDto bookingDto, Long userId);

    BookingDto approve(Long bookingId, Long userId, Boolean approved);

    BookingDto getById(Long bookingId, Long userId);

    List<BookingDto> getAllByBooker(Long userId, String state, int from, int size);

    List<BookingDto> getAllByOwner(Long userId, String state, int from, int size);

    default List<BookingDto> getAllByBooker(Long userId, String state) {
        return getAllByBooker(userId, state, 0, 10);
    }

    default List<BookingDto> getAllByOwner(Long userId, String state) {
        return getAllByOwner(userId, state, 0, 10);
    }
}